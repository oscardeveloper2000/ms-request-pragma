package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.common.PageResponse;
import co.com.bancolombia.model.common.CustomPageResponseReport;
import co.com.bancolombia.model.notification.NotificationGateway;
import co.com.bancolombia.model.requestapplication.PageRequest;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.RequestReportResponse;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.status.StatusCode;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.user.UserBasicInfo;
import co.com.bancolombia.model.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RequestApplicationUseCase implements RequestApplicationEvents {
    private final RequestApplicationRepository requestLoanRepository;
    private final StatusRepository statusRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final UserRepository userRepository;
    private final LoggerPort logger;
    private final TokenGateway tokenGateway;

    @Override
    public Mono<RequestApplication> applySave(RequestApplication requestApplication) {
        logger.info("applySave: inicio documentNumber={}, loanTypeId={}",
                requestApplication.getDocumentNumber(), requestApplication.getLoanTypeId());
        return tokenGateway.getToken()
                .zipWith(tokenGateway.getEmailFromToken())
                .flatMap(tuple1 -> {
                    String token = tuple1.getT1();
                    String emailFromToken = tuple1.getT2();
                    return userRepository.findByDocumentNumber(requestApplication.getDocumentNumber(), token)
                            .switchIfEmpty(Mono.error(new DomainValidationException("User not found in ms-auth")))
                            .flatMap(user -> validatePersonIdentity(emailFromToken, user.getEmail()).then(Mono.just(user)))
                            .map(user -> requestApplication.toBuilder()
                                    .email(user.getEmail())
                                    .statusId(StatusCode.PENDING.id()) // establecer siempre PENDIENTE
                                    .build())
                            .flatMap(ra -> Mono.zip(
                                                    statusRepository.findById(StatusCode.PENDING.id()) // validar que exista en BD
                                                            .switchIfEmpty(Mono.error(new DomainValidationException("Status not found"))),
                                                    typeLoanRepository.findById(ra.getLoanTypeId())
                                                            .switchIfEmpty(Mono.error(new DomainValidationException("TypeLoan not found")))
                                            )
                                            .map(tuple2 -> {
                                                var typeLoan = tuple2.getT2();
                                                validateAmountInRange(ra.getAmount(), typeLoan.getMinAmount(), typeLoan.getMaxAmount());
                                                return ra;
                                            })
                            )
                            .flatMap(requestLoanRepository::save);
                } )

                .doOnSuccess(saved -> logger.info("applySave: guardado OK id={}, documentNumber={}",
                        saved.getId(), saved.getDocumentNumber()))
                .doOnError(e -> logger.error("applySave: error", e));
    }




@Override
public Mono<CustomPageResponseReport<RequestReportResponse>> applyFilterByStatus(PageRequest pageable, Long statusId, String token) {
    logger.info("applyFilterByStatus: inicio - statusId={}, page={}, size={}", statusId, pageable.getPage(), pageable.getSize());

    Mono<Long> totalElementsMono = requestLoanRepository.countByStatusId(statusId)
        .doOnSuccess(total -> logger.info("applyFilterByStatus: totalElements={}", total))
        .doOnError(e -> logger.error("applyFilterByStatus: error al contar elementos", e));

    Mono<List<RequestApplication>> paginatedLoansRequest = requestLoanRepository.findAllByStatusIdWithPageable(statusId, pageable)
        .collectList()
        .doOnSuccess(list -> logger.info("applyFilterByStatus: solicitudes encontradas={}", list.size()))
        .doOnError(e -> logger.error("applyFilterByStatus: error al obtener solicitudes", e));

    Mono<List<RequestApplication>> approvedLoansMono = requestLoanRepository.findAllByStatusId(StatusCode.APPROVED.id())
        .collectList()
        .doOnSuccess(list -> logger.info("applyFilterByStatus: préstamos aprobados encontrados={}", list.size()))
        .doOnError(e -> logger.error("applyFilterByStatus: error al obtener préstamos aprobados", e));

    return Mono.zip(totalElementsMono, paginatedLoansRequest, approvedLoansMono)
        .flatMap(tuple -> {
            long totalElements = tuple.getT1();
            List<RequestApplication> paginatedLoansRequestList = tuple.getT2();
            List<RequestApplication> approvedLoans = tuple.getT3();

            if (paginatedLoansRequestList == null) {
                logger.error("applyFilterByStatus: solicitudes es null");
                return Mono.error(new DomainValidationException("No se pudo obtener la lista de solicitudes"));
            }

            List<String> emails = paginatedLoansRequestList.stream().map(RequestApplication::getEmail).collect(Collectors.toList());
            logger.info("applyFilterByStatus: emails a consultar={}", emails);

            // Calcular cuota total mensual
            Mono<BigDecimal> totalMonthlyDebtMono = Flux.fromIterable(approvedLoans)
                .flatMap(loan -> typeLoanRepository.findById(loan.getLoanTypeId())
                    .switchIfEmpty(Mono.error(new DomainValidationException("TypeLoan no encontrado para préstamo aprobado id=" + loan.getId())))
                    .map(typeLoan -> {
                        BigDecimal amount = loan.getAmount();
                        int term = loan.getTerm();
                        BigDecimal interestRate = BigDecimal.valueOf(typeLoan.getInterestRate());
                        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, BigDecimal.ROUND_HALF_UP);
                        BigDecimal onePlusRatePowTerm = BigDecimal.ONE.add(monthlyRate).pow(term);
                        BigDecimal cuota = amount.multiply(monthlyRate).multiply(onePlusRatePowTerm)
                            .divide(onePlusRatePowTerm.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
                        logger.debug("applyFilterByStatus: cuota calculada para préstamo id={}: {}", loan.getId(), cuota);
                        return cuota;
                    })
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doOnSuccess(total -> logger.info("applyFilterByStatus: totalMonthlyDebtOfApprovedLoans={}", total))
                .doOnError(e -> logger.error("applyFilterByStatus: error al calcular deuda mensual", e));

            return userRepository.findUsersByEmails(emails, token)
                .switchIfEmpty(Mono.error(new DomainValidationException("No se encontraron usuarios para los emails consultados")))
                .doOnSuccess(users -> logger.info("applyFilterByStatus: usuarios consultados={}", users.size()))
                .doOnError(e -> logger.error("applyFilterByStatus: error al consultar usuarios", e))
                .flatMap(users -> {
                    if (users == null) {
                        logger.error("applyFilterByStatus: users es null");
                        return Mono.error(new DomainValidationException("No se pudo obtener la lista de usuarios"));
                    }
                    Map<String, UserBasicInfo> userMap = users.stream()
                        .collect(Collectors.toMap(UserBasicInfo::getEmail, u -> u));

                    return Flux.fromIterable(paginatedLoansRequestList)
                        .flatMap(solicitud -> Mono.zip(
                            typeLoanRepository.findById(solicitud.getLoanTypeId())
                                .switchIfEmpty(Mono.error(new DomainValidationException("TypeLoan no encontrado para solicitud id=" + solicitud.getId()))),
                            statusRepository.findById(solicitud.getStatusId())
                                .switchIfEmpty(Mono.error(new DomainValidationException("Status no encontrado para solicitud id=" + solicitud.getId())))
                        ).map(tuple2 -> {
                            var typeLoan = tuple2.getT1();
                            var status = tuple2.getT2();
                            UserBasicInfo user = userMap.get(solicitud.getEmail());
                            logger.debug("applyFilterByStatus: armando respuesta para solicitud id={}", solicitud.getId());
                            return RequestReportResponse.builder()
                                .amount(solicitud.getAmount())
                                .term(solicitud.getTerm())
                                .email(solicitud.getEmail())
                                .firstName(user != null ? user.getFirstName() : null)
                                .lastName(user != null ? user.getLastName() : null)
                                .stateName(status.getName())
                                .loanTypeName(typeLoan.getName())
                                .interestRate(typeLoan.getInterestRate())
                                .baseSalary(user != null ? user.getBaseSalary() : null)
                                .build();
                        }))
                        .collectList()
                        .zipWith(totalMonthlyDebtMono)
                        .map(tuple3 -> {
                            List<RequestReportResponse> content = tuple3.getT1();
                            BigDecimal totalMonthlyDebt = tuple3.getT2();
                            int totalPages = (int) Math.ceil((double) totalElements / pageable.getSize());
                            boolean first = pageable.getPage() == 0;
                            boolean last = pageable.getPage() == (totalPages - 1);

                            logger.info("applyFilterByStatus: respuesta armada - content={}, totalPages={}, first={}, last={}",
                                content.size(), totalPages, first, last);

                            return CustomPageResponseReport.<RequestReportResponse>customBuilder()
                                .content(content)
                                .page(pageable.getPage())
                                .size(pageable.getSize())
                                .totalElements(totalElements)
                                .totalPages(totalPages)
                                .first(first)
                                .last(last)
                                .totalMonthlyDebtOfApprovedLoans(totalMonthlyDebt)
                                .build();
                        });
                });
        })
        .doOnError(e -> logger.error("applyFilterByStatus: error final", e));
}

    private void validateAmountInRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        if (min != null && amount.compareTo(min) < 0)
            throw new IllegalArgumentException("Amount below minimum: " + amount + " < " + min);
        if (max != null && amount.compareTo(max) > 0)
            throw new IllegalArgumentException("Amount above maximum: " + amount + " > " + max);
    }


    private Mono<Void> validatePersonIdentity(String emailFromToken, String emailFromUser) {
        if (!emailFromToken.equals(emailFromUser)) {
            return Mono.error(new DomainValidationException("The user does not have permissions to make this request for email: " + emailFromUser));
        }

        return Mono.empty();
    }

}