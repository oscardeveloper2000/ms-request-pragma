package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.common.dto.UpdateStatusResponse;
import co.com.bancolombia.model.notification.NotificationGateway;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.status.StatusCode;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.user.User;
import co.com.bancolombia.model.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateStatusUseCase implements UpdateSatus{
    private final RequestApplicationRepository requestLoanRepository;
    private final StatusRepository statusRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final UserRepository userRepository;
    private final LoggerPort logger;
    private final NotificationGateway notificationGateway;
    private final TokenGateway tokenGateway;

    @Override
    public Mono<UpdateStatusResponse> updateStatus(Long requestId, String newStatus) {
        logger.info("updateStatus: starting - requestId={}, newStatus={}", requestId, newStatus);

        return validateStatus(newStatus)
            .then(requestLoanRepository.findById(requestId))
            .switchIfEmpty(Mono.error(new DomainValidationException("Request not found")))
            .flatMap(request -> processStatusUpdate(request, newStatus))
            .doOnSuccess(response -> logger.info("updateStatus: completed - requestId={}, statusName={}",
                response.getRequestId(), response.getStatusName()))
            .doOnError(e -> logger.error("updateStatus: error", e));
    }

    private Mono<Void> validateStatus(String newStatus) {
        if (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus)) {
            return Mono.error(new DomainValidationException("Invalid status. Only APPROVED or REJECTED allowed"));
        }
        return Mono.empty();
    }

    private Mono<UpdateStatusResponse> processStatusUpdate(RequestApplication request, String newStatus) {
        Long newStatusId = "APPROVED".equalsIgnoreCase(newStatus)
            ? StatusCode.APPROVED.id()
            : StatusCode.REJECTED.id();

        return validateStatusChange(request, newStatusId, newStatus)
            .then(updateRequestStatus(request, newStatusId))
            .flatMap(updatedRequest -> getUserDataAndSendNotification(updatedRequest, newStatus)
                .then(Mono.just(buildResponse(updatedRequest.getId(), newStatus)))
                .onErrorResume(e -> revertAndFail(request, e)));
    }

    private Mono<Void> validateStatusChange(RequestApplication request, Long newStatusId, String newStatus) {
        if (request.getStatusId().equals(newStatusId)) {
            return Mono.error(new DomainValidationException("Request already has status: " + newStatus));
        }

        if (!request.getStatusId().equals(StatusCode.PENDING.id())) {
            return Mono.error(new DomainValidationException("Only PENDING requests can be updated"));
        }

        return Mono.empty();
    }

    private Mono<RequestApplication> updateRequestStatus(RequestApplication request, Long newStatusId) {
        RequestApplication updatedRequest = request.toBuilder()
            .statusId(newStatusId)
            .build();

        return requestLoanRepository.save(updatedRequest)
            .doOnSuccess(saved -> logger.info("updateStatus: status updated requestId={}", saved.getId()));
    }

    private Mono<String> getUserDataAndSendNotification(RequestApplication request, String status) {
        return tokenGateway.getToken()
            .doOnSuccess(token -> logger.info("updateStatus: token obtained for user lookup"))
            .flatMap(token -> userRepository.findByDocumentNumber(request.getDocumentNumber(), token))
            .switchIfEmpty(Mono.error(new DomainValidationException("User not found with document: " + request.getDocumentNumber())))
            .doOnSuccess(user -> logger.info("updateStatus: user found documentNumber={}, userName={}",
                user.getDocumentNumber(), user.getFirstName() + " " + user.getLastName()))
            .flatMap(user -> sendNotification(request, status, user))
            .doOnSuccess(messageId -> logger.info("updateStatus: notification sent messageId={}", messageId));
    }

    private Mono<String> sendNotification(RequestApplication request, String status, User user) {
        String message = buildNotificationMessage(request, status, user);
        return notificationGateway.sendNotification(message);
    }

    private String buildNotificationMessage(RequestApplication request, String status, User user) {
        return String.format("""
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