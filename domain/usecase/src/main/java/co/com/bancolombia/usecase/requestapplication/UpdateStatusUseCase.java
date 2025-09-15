package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.common.dto.UpdateStatusResponse;
import co.com.bancolombia.model.domains.requestapplication.dto.NotificationStatusMessage;
import co.com.bancolombia.model.domains.requestapplication.dto.PaymentPlan;
import co.com.bancolombia.model.domains.status.Status;
import co.com.bancolombia.model.external.messaging.dto.MessageBody;
import co.com.bancolombia.model.external.messaging.gateway.MessagePublisherGateway;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.domains.status.StatusCode;
import co.com.bancolombia.model.domains.status.gateways.StatusRepository;
import co.com.bancolombia.model.domains.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.external.rest.user.dto.User;
import co.com.bancolombia.model.external.rest.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

@RequiredArgsConstructor
public class UpdateStatusUseCase implements UpdateSatus {
    private final RequestApplicationRepository requestLoanRepository;
    private final StatusRepository statusRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final UserRepository userRepository;
    private final LoggerPort logger;
    private final MessagePublisherGateway messagePublisherGateway;
    private static final BigDecimal PERCENTAGE_DIVISOR = new BigDecimal("100");
    private static final BigDecimal MONTHS_IN_YEAR = new BigDecimal("12");

    @Override
    public Mono<UpdateStatusResponse> updateStatus(Long requestId, String newStatus) {
        logger.info("updateStatus: starting - requestId={}, newStatus={}", requestId, newStatus);
        return validateStatus(newStatus)
                .flatMap(statusRepository::findById)
                .switchIfEmpty(Mono.error(new DomainValidationException("Status not found: " + newStatus)))
                .flatMap(status -> {
                    return requestLoanRepository.findById(requestId)
                            .switchIfEmpty(Mono.error(new DomainValidationException("Request not found with id: " + requestId)))
                            .flatMap(request -> updateRequestStatus(request, status.getId())
                                    .flatMap(savedRequest -> getUserFromExternalService(savedRequest.getDocumentNumber())
                                            .flatMap(user -> processStatusUpdate(savedRequest, user, status))
                                            .then(Mono.just(buildResponse(savedRequest.getId(), newStatus)))
                                    )
                            );
                });
    }

    private Mono<Long> validateStatus(String newStatus) {
        if ("APPROVED".equalsIgnoreCase(newStatus)) {
            return Mono.just(StatusCode.APPROVED.id());
        } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
            return Mono.just(StatusCode.REJECTED.id());
        }
        return Mono.error(new DomainValidationException("Invalid status: " + newStatus));
    }

    private Mono<RequestApplication> processStatusUpdate(RequestApplication savedApplication, User user, Status status) {
        boolean isApproved = Objects.equals(savedApplication.getStatusId(), StatusCode.APPROVED.id());
        boolean isRejected = Objects.equals(savedApplication.getStatusId(), StatusCode.REJECTED.id());
        if (!isApproved && !isRejected) {
            return Mono.just(savedApplication);
        }
        if (isApproved) {
            return handleApprovedStatus(savedApplication, user, status.getName());
        }
        return handleRejectedStatus(savedApplication, user, status.getName());
    }

    private Mono<RequestApplication> updateRequestStatus(RequestApplication request, Long newStatusId) {
        RequestApplication updatedRequest = request.toBuilder()
            .statusId(newStatusId)
            .build();

        return requestLoanRepository.save(updatedRequest);
//            .doOnNext(saved -> logger.info("updateStatus: status updated requestId={}", saved.getId())).thenReturn(e -> e. );
    }

    private Mono<RequestApplication> handleApprovedStatus(RequestApplication savedRequestApplication, User user, String status) {
        return typeLoanRepository
                .findById(savedRequestApplication.getLoanTypeId())
                .switchIfEmpty(Mono.error(new DomainValidationException("Loan type not found with id: " + savedRequestApplication.getLoanTypeId())))
                .doOnSuccess(loanType -> logger.info("updateStatus: loanType found id={}, name={}", loanType.getId(), loanType.getName()))
                .flatMap(loanType -> {
                    List<PaymentPlan> paymentPlan = calculatePaymentPlan(savedRequestApplication.getAmount(), BigDecimal.valueOf(loanType.getInterestRate()), savedRequestApplication.getTerm());
                    String message = buildNotificationMessageApproved(savedRequestApplication, status, user, paymentPlan);
                    Mono<String> notificationMono =  messagePublisherGateway.publishLoanNotificationEmail(message)
                            .doOnSuccess(messageId -> logger.info("updateStatus: notification sent messageId={}", messageId))
                            .doOnError(error -> logger.error("updateStatus: notification sending failed", error));
                    Mono<String> reportMono = messagePublisherGateway.publishReportLoan(message)
                            .doOnSuccess(messageId -> logger.info("updateStatus: report sent messageId={}", messageId))
                            .doOnError(error -> logger.error("updateStatus: report sending failed", error));
                    return Mono.zip(notificationMono, reportMono)
                            .thenReturn(savedRequestApplication);
                });
    }

    private Mono<RequestApplication> handleRejectedStatus(RequestApplication savedApplication, User user, String status) {
        String message = buildNotificationMessageRejected(savedApplication, status, user);
        return messagePublisherGateway.publishLoanNotificationEmail(message).thenReturn(savedApplication);
    }

    private Mono<User> getUserFromExternalService(String documentNumber) {
        return userRepository.findByDocumentNumber(documentNumber)
                .doOnNext(user -> logger.info("updateStatus: user found documentNumber={}", user.getDocumentNumber()))
                .switchIfEmpty(Mono.error(new DomainValidationException("User not found with document: " + documentNumber)))
                .doOnSuccess(user -> logger.info("updateStatus: user found documentNumber={}, userName={}",
                        user.getDocumentNumber(), user.getFirstName() + " " + user.getLastName()));
    }

    private List<PaymentPlan> calculatePaymentPlan(BigDecimal principal, BigDecimal annualInterestRate, Integer termInMonths) {
        if (principal == null || annualInterestRate == null || termInMonths == null || termInMonths <= 0 || annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            return Collections.emptyList();
        }
        BigDecimal monthlyPayment;
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = principal.divide(BigDecimal.valueOf(termInMonths), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal monthlyRate = annualInterestRate.divide(PERCENTAGE_DIVISOR, MathContext.DECIMAL128).divide(MONTHS_IN_YEAR, MathContext.DECIMAL128);
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            BigDecimal onePlusRToTheN = onePlusR.pow(termInMonths, MathContext.DECIMAL128);
            BigDecimal denominator = onePlusRToTheN.subtract(BigDecimal.ONE);
            BigDecimal numerator = monthlyRate.multiply(onePlusRToTheN);
            monthlyPayment = principal.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
        }
        List<PaymentPlan> plan = new ArrayList<>();
        BigDecimal remainingBalance = principal;
        LocalDate paymentDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= termInMonths; i++) {
            BigDecimal interestForMonth = remainingBalance
                    .multiply(annualInterestRate.divide(PERCENTAGE_DIVISOR, MathContext.DECIMAL128).divide(MONTHS_IN_YEAR, MathContext.DECIMAL128))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalForMonth = monthlyPayment.subtract(interestForMonth);
            remainingBalance = remainingBalance.subtract(principalForMonth);
            PaymentPlan monthlyInstallment = new PaymentPlan(i, formatCurrency(principalForMonth), principalForMonth, formatCurrency(interestForMonth),
                    formatCurrency(monthlyPayment), formatCurrency(remainingBalance.max(BigDecimal.ZERO)));
            plan.add(monthlyInstallment);
            paymentDate = paymentDate.plusMonths(1);
        }
        return plan;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        return currencyFormatter.format(amount);
    }

    private String buildNotificationMessageApproved(RequestApplication request, String status, User user, List<PaymentPlan> paymentPlan) {
        StringBuilder paymentPlanJson = new StringBuilder("[");
        for (int i = 0; i < paymentPlan.size(); i++) {
            PaymentPlan plan = paymentPlan.get(i);
            paymentPlanJson.append(String.format(
                    "{\"month\":%d,\"principalForMonth\":\"%s\",\"principalForMonthNumber\":%d,\"interestToPay\":\"%s\",\"capitalPayment\":\"%s\",\"obligationBalance\":\"%s\"}",
                    plan.month(),
                    plan.principalForMonth(),
                    plan.principalForMonthNumber().intValue(),
                    plan.interestToPay(),
                    plan.capitalPayment(),
                    plan.obligationBalance()
            ));
            if (i < paymentPlan.size() - 1) {
                paymentPlanJson.append(",");
            }
        }
        paymentPlanJson.append("]");

String messageContent = String.format(
                        "{\"requestId\":%d,\"status\":\"%s\",\"userClient\":\"%s\",\"emailClient\":\"%s\",\"amount\":%s,\"paymentPlan\":%s}",
                        request.getId(),
                        status,
                        user.getFirstName() + " " + user.getLastName(),
                        request.getEmail(),
                        request.getAmount(),
                        paymentPlanJson.toString()
                );

        return messageContent;
    }

private String buildNotificationMessageRejected(RequestApplication request, String status, User user) {
    String messageContent = String.format("""
        {
            "requestId": %d,
            "status": "%s",
            "userClient": "%s",
            "emailClient": "%s"
        }""",
        request.getId(),
        status,
        user.getFirstName() + " " + user.getLastName(),
        request.getEmail()
    );
    return messageContent;
}

    private UpdateStatusResponse buildResponse(Long requestId, String status) {
        return UpdateStatusResponse.builder()
            .requestId(requestId)
            .statusName(status)
            .build();
    }

    private Mono<UpdateStatusResponse> revertAndFail(RequestApplication originalRequest, Throwable error) {
        logger.error("updateStatus: notification failed, reverting changes", error);
        return requestLoanRepository.save(originalRequest)
            .then(Mono.error(new DomainValidationException("Status updated but notification failed. Changes reverted.")));
    }
}