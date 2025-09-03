package co.com.bancolombia.api.error;

import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class ErrorHandler {

    private final LoggerPort logger;



    public Mono<ServerResponse> handle(Throwable error, ServerRequest request) {
        String correlationId = resolveCorrelationId(request);

        HttpStatus status = resolveStatus(error);
        String headerMessage = status.getReasonPhrase();

        String detailCode = String.valueOf(status.value());
        String detailMessage = resolveMessage(error);
        String errorDate = LocalDate.now().toString();

        logger.error("ErrorHandler: status={}, correlationId={}, message={}", status.value(), correlationId, detailMessage);

        ErrorResponse payload = new ErrorResponse(
                correlationId,
                new ErrorHeader(status.value(), headerMessage),
                new ErrorDetail(detailCode, detailMessage, errorDate)
        );

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload);
    }

    private String resolveCorrelationId(ServerRequest request) {
        return Optional.ofNullable(request.headers().firstHeader("X-Correlation-Id"))
                .orElseGet(() -> Optional.ofNullable(request.headers().firstHeader("x-correlation-id"))
                        .orElse(UUID.randomUUID().toString()));
    }

    private HttpStatus resolveStatus(Throwable error) {
        if (error instanceof DomainValidationException) {
            return HttpStatus.BAD_REQUEST; // 400
        }
        if (error instanceof WebClientResponseException e) {
            // Propaga el status remoto cuando aplica
            return (HttpStatus) e.getStatusCode();
        }
        if (error instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT; // 504
        }
        if (error instanceof IOException) {
            return HttpStatus.BAD_GATEWAY; // 502
        }
        return HttpStatus.INTERNAL_SERVER_ERROR; // 500 por defecto
    }

    private String resolveMessage(Throwable error) {
        if (error instanceof WebClientResponseException e) {
            // Mensaje del remoto o cuerpo si se requiere (simplificado aquí)
            return Optional.ofNullable(e.getResponseBodyAsString()).filter(s -> !s.isBlank()).orElse(e.getMessage());
        }
        return Optional.ofNullable(error.getMessage()).filter(s -> !s.isBlank()).orElse("Unexpected error");
    }

    public record ErrorResponse(String correlationId, ErrorHeader errorHeader, ErrorDetail errorDetail) {}
    public record ErrorHeader(int returnCode, String message) {}
    public record ErrorDetail(String code, String message, String errorDate) {}
}
