package co.com.bancolombia.usecase.processcapacityvalidation;



import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.external.messaging.dto.CapacityValidationEventListener;
import co.com.bancolombia.model.common.dto.UpdateStatusResponse;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import co.com.bancolombia.usecase.requestapplication.UpdateSatus;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ProcessCapacityValidationUseCase {
    private final UpdateSatus updateStatusUseCase;
    private final LoggerPort logger;

    public Mono<UpdateStatusResponse> processValidationEvent(CapacityValidationEventListener event) {
        logger.info("processValidationEvent: inicio - solicitudId={}, status={}",
                   event.getSolicitudId(), event.getStatus());

        return validateEvent(event)
                .then(extractAndUpdateStatus(event))
                .doOnSuccess(response -> logger.info("processValidationEvent: completado exitosamente - requestId={}",
                                                   response.getRequestId()))
                .doOnError(e -> logger.error("processValidationEvent: error procesando evento", e));
    }

    private Mono<Void> validateEvent(CapacityValidationEventListener event) {
        if (event == null) {
            logger.error("processValidationEvent: evento es null");
            return Mono.error(new DomainValidationException("Validation event is required"));
        }

        if (!"ValidationCompleted".equals(event.getEventType())) {
            logger.error("processValidationEvent: eventType inválido={}", event.getEventType());
            return Mono.error(new DomainValidationException("Invalid event type. Expected: ValidationCompleted"));
        }

        if (event.getSolicitudId() == null || event.getSolicitudId().trim().isEmpty()) {
            logger.error("processValidationEvent: solicitudId es null o vacío");
            return Mono.error(new DomainValidationException("Solicitud ID is required"));
        }

        if (event.getStatus() == null || event.getStatus().trim().isEmpty()) {
            logger.error("processValidationEvent: status es null o vacío");
            return Mono.error(new DomainValidationException("Status is required"));
        }

        logger.info("processValidationEvent: validaciones de entrada exitosas");
        return Mono.empty();
    }

    private Mono<UpdateStatusResponse> extractAndUpdateStatus(CapacityValidationEventListener event) {
        try {
            Long requestId = Long.parseLong(event.getSolicitudId());
            String mappedStatus = mapCapacityStatusToRequestStatus(event.getStatus());

            logger.info("processValidationEvent: mapeando status '{}' a '{}' para requestId={}",
                       event.getStatus(), mappedStatus, requestId);

            return updateStatusUseCase.updateStatus(requestId, mappedStatus)
                    .doOnSuccess(response -> logger.info("processValidationEvent: status actualizado exitosamente - requestId={}",
                                                       response.getRequestId()));

        } catch (NumberFormatException e) {
            logger.error("processValidationEvent: solicitudId no es un número válido={}",
                        event.getSolicitudId());
            return Mono.error(new DomainValidationException("Invalid solicitud ID format"));
        } catch (IllegalArgumentException e) {
            logger.error("processValidationEvent: error en mapeo de status: {}", e.getMessage());
            return Mono.error(new DomainValidationException(e.getMessage()));
        }
    }

    private String mapCapacityStatusToRequestStatus(String capacityStatus) {
        return switch (capacityStatus.toUpperCase()) {
            case "APROBADO" -> "APPROVED";
            case "RECHAZADO" -> "REJECTED";
            default -> {
                logger.error("processValidationEvent: status no reconocido={}", capacityStatus);
                throw new IllegalArgumentException("Unknown capacity status: " + capacityStatus);
            }
        };
    }
}