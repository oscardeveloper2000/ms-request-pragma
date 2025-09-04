package co.com.bancolombia.api.request;

import co.com.bancolombia.api.error.ErrorHandler;
import co.com.bancolombia.api.request.dto.RequestApplicationMapper;
import co.com.bancolombia.api.request.dto.RequestApplicationRecord;
import co.com.bancolombia.model.common.CustomPageResponseReport;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.common.PageResponse;
import co.com.bancolombia.model.requestapplication.RequestReportResponse;
import co.com.bancolombia.usecase.requestapplication.RequestApplicationEvents;
import lombok.RequiredArgsConstructor;
import co.com.bancolombia.model.requestapplication.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
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
                .doOnNext(dto -> logger.debug("listenPOSTUseCase: payload recibido documentNumber={}, loanTypeId={}",
                        dto.documentNumber(), dto.loanTypeId()))
                .map(mapper::fromRequest)
                .doOnNext(model -> logger.info("listenPOSTUseCase: mapeado a modelo documentNumber={}, loanTypeId={}",
                        model.getDocumentNumber(), model.getLoanTypeId()))
                .flatMap(useCase::applySave)
                .doOnSuccess(saved -> logger.info("listenPOSTUseCase: guardado OK id={}, documentNumber={}",
                        saved.getId(), saved.getDocumentNumber()))
                .map(mapper::toResponse)
                .flatMap(dto -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(error -> errorHandler.handle(error, serverRequest));
    }

    public Mono<ServerResponse> listenFilterByStatusId(ServerRequest request) {
        logger.info("SolicitudHandler: inicio GET /api/v1/solicitud");
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        Long statusId = request.queryParam("statusId").map(Long::parseLong).orElse(null);
        String token = request.headers().firstHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (statusId == null) {
            logger.warn("SolicitudHandler: statusId es requerido");
            return ServerResponse.badRequest().bodyValue("statusId es requerido");
        }
        PageRequest pageRequest = new PageRequest(page, size);

        Mono<CustomPageResponseReport<RequestReportResponse>> result = useCase.applyFilterByStatus(pageRequest, statusId, token);


        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result, RequestReportResponse.class)
                .doOnTerminate(() -> logger.info("SolicitudHandler: fin GET /api/v1/solicitud"));
    }



}

