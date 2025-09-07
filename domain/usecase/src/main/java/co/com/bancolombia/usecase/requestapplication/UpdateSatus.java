package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.dto.UpdateStatusResponse;
import reactor.core.publisher.Mono;

public interface UpdateSatus {
    Mono<UpdateStatusResponse> updateStatus(Long requestId, String newStatus);
}
