package co.com.bancolombia.model.domains.status.gateways;

import co.com.bancolombia.model.domains.status.Status;
import reactor.core.publisher.Mono;

public interface StatusRepository {
    Mono<Status> findById(Long id);
}
