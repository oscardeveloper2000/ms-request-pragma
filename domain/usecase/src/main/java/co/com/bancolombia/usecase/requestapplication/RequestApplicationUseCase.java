package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.common.PageResponse;
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
    private final RequestApplicationRepository repository;
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
                            .flatMap(repository::save);
                } )

                .doOnSuccess(saved -> logger.info("applySave: guardado OK id={}, documentNumber={}",
                        saved.getId(), saved.getDocumentNumber()))
                .doOnError(e -> logger.error("applySave: error", e));
    }


    @Override
    public Flux<RequestReportResponse> applyFilterByStatus(PageRequest pageable, Long statusId, String token) {
        logger.info("GetPendingSolicitudesUseCase: buscando solicitudes por statusId={}", statusId);
        return repository.findAllByStatusId(statusId, pageable)
                .collectList()
                .flatMapMany(solicitudes -> {
                    List<String> emails = solicitudes.stream().map(RequestApplication::getEmail).collect(Collectors.toList());
                    return userRepository.findUsersByEmails(emails, token)
                            .flatMapMany(users -> {
                                Map<String, UserBasicInfo> userMap = users.stream().collect(Collectors.toMap(UserBasicInfo::getEmail, u -> u));
                                return Flux.fromIterable(solicitudes)
                                        .flatMap(solicitud -> Mono.zip(
                                                typeLoanRepository.findById(solicitud.getLoanTypeId()),
                                                statusRepository.findById(solicitud.getStatusId())
                                        ).map(tuple -> {
                                            var typeLoan = tuple.getT1();
                                            var status = tuple.getT2();
                                            UserBasicInfo user = userMap.get(solicitud.getEmail());
                                            return RequestReportResponse.builder()
                                                    .amount(solicitud.getAmount())
                                                    .term(solicitud.getTerm())
                                                    .email(solicitud.getEmail())
                                                    .firstName(user != null ? user.getFirstName() : null)
                                                    .lastName(user != null ? user.getLastName() : null)
                                                    .stateName(status.getName())
                                                    .loanTypeName(typeLoan.getName())
                                                    .interestRate(typeLoan.getInterestRate())
//                                                    .typeLoan(typeLoan)
//                                                    .status(status)
                                                    .baseSalary(user != null ? user.getBaseSalary() : null)
                                                    .build();
                                        }));
                            });
                });
    }

    public Mono<PageResponse<RequestReportResponse>> applyFilterByStatus2(PageRequest pageable, Long statusId, String token) {
        logger.info("GetPendingSolicitudesUseCase: buscando solicitudes por statusId={}", statusId);

        Mono<Long> totalElementsMono = repository.countByStatusId(statusId);
        Mono<List<RequestApplication>> solicitudesMono = repository.findAllByStatusId(statusId, pageable).collectList();

        return Mono.zip(totalElementsMono, solicitudesMono)
            .flatMap(tuple -> {
                long totalElements = tuple.getT1();
                List<RequestApplication> solicitudes = tuple.getT2();
                List<String> emails = solicitudes.stream().map(RequestApplication::getEmail).collect(Collectors.toList());

                return userRepository.findUsersByEmails(emails, token)
                    .flatMap(users -> {
                        Map<String, UserBasicInfo> userMap = users.stream()
                            .collect(Collectors.toMap(UserBasicInfo::getEmail, u -> u));

                        return Flux.fromIterable(solicitudes)
                            .flatMap(solicitud -> Mono.zip(
                                typeLoanRepository.findById(solicitud.getLoanTypeId()),
                                statusRepository.findById(solicitud.getStatusId())
                            ).map(tuple2 -> {
                                var typeLoan = tuple2.getT1();
                                var status = tuple2.getT2();
                                UserBasicInfo user = userMap.get(solicitud.getEmail());
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
                            .map(content -> {
                                int totalPages = (int) Math.ceil((double) totalElements / pageable.getSize());
                                boolean first = pageable.getPage() == 0;
                                boolean last = pageable.getPage() == (totalPages - 1);
                                return PageResponse.<RequestReportResponse>builder()
                                    .content(content)
                                    .page(pageable.getPage())
                                    .size(pageable.getSize())
                                    .totalElements(totalElements)
                                    .totalPages(totalPages)
                                    .first(first)
                                    .last(last)
                                    .build();
                            });
                    });
            });
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