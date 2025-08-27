package co.com.bancolombia.model.requestapplication.gateways;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import reactor.core.publisher.Mono;

public interface RequestApplicationRepository {
    Mono<RequestApplication> save(RequestApplication requestApplication);
}
