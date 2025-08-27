package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import reactor.core.publisher.Mono;

public interface RequestApplicationEvents {
    Mono<RequestApplication> applySave(RequestApplication requestApplication);
}
