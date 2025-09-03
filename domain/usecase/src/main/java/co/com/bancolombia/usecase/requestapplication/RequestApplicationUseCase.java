package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.status.StatusCode;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class RequestApplicationUseCase implements RequestApplicationEvents {
    private final RequestApplicationRepository repository;
    private final StatusRepository statusRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final UserRepository userRepository;
    private final LoggerPort logger;

    @Override
    public Mono<RequestApplication> applySave(RequestApplication requestApplication) {
        logger.info("applySave: inicio documentNumber={}, loanTypeId={}",
                requestApplication.getDocumentNumber(), requestApplication.getLoanTypeId());

        return userRepository.findByDocumentNumber(requestApplication.getDocumentNumber())
                .switchIfEmpty(Mono.error(new DomainValidationException("User not found in ms-auth")))
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
                                .map(tuple -> {
                                    var typeLoan = tuple.getT2();
                                    validateAmountInRange(ra.getAmount(), typeLoan.getMinAmount(), typeLoan.getMaxAmount());
                                    return ra;
                                })
                )
                .flatMap(repository::save)
                .doOnSuccess(saved -> logger.info("applySave: guardado OK id={}, documentNumber={}",
                        saved.getId(), saved.getDocumentNumber()))
                .doOnError(e -> logger.error("applySave: error", e));
    }




    private void validateAmountInRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        if (min != null && amount.compareTo(min) < 0)
            throw new IllegalArgumentException("Amount below minimum: " + amount + " < " + min);
        if (max != null && amount.compareTo(max) > 0)
            throw new IllegalArgumentException("Amount above maximum: " + amount + " > " + max);
    }




}