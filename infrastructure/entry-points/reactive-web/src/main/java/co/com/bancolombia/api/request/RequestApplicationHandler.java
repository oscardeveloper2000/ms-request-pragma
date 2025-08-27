package co.com.bancolombia.api.request;

import co.com.bancolombia.api.error.ErrorHandler;
import co.com.bancolombia.api.request.dto.RequestApplicationMapper;
import co.com.bancolombia.api.request.dto.RequestApplicationRecord;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.usecase.requestapplication.RequestApplicationEvents;
import co.com.bancolombia.usecase.requestapplication.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
@Transactional
public class RequestApplicationHandler {

    private final RequestApplicationEvents useCase;
    private final RequestApplicationMapper mapper;
    private final LoggerPort logger;
    private final ErrorHandler errorHandler;



    public Mono<ServerResponse> listenPOSTUseCase(ServerRequest serverRequest) {
        logger.info("listenPOSTUseCase: inicio procesamiento de solicitud");
        return serverRequest
                .bodyToMono(RequestApplicationRecord.class)
                .doOnNext(dto -> logger.debug("listenPOSTUseCase: payload recibido documentNumber={}, loanTypeId={}, statusId={}",
                        dto.documentNumber(), dto.loanTypeId(), dto.statusId()))
                .map(mapper::toModel)
                .doOnNext(model -> logger.info("listenPOSTUseCase: mapeado a modelo documentNumber={}, loanTypeId={}",
                        model.getDocumentNumber(), model.getLoanTypeId()))
                .flatMap(useCase::applySave)
                .doOnSuccess(saved -> logger.info("listenPOSTUseCase: guardado OK id={}, documentNumber={}",
                        saved.getId(), saved.getDocumentNumber()))
                .map(mapper::toDTO)
                .flatMap(dto -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(error -> errorHandler.handle(error, serverRequest));
    }



}

