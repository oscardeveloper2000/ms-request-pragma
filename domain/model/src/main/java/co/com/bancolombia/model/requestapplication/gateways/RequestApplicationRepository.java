package co.com.bancolombia.model.requestapplication.gateways;

import co.com.bancolombia.model.requestapplication.PageRequest;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



public interface RequestApplicationRepository {
    Mono<RequestApplication> save(RequestApplication requestApplication);
    Flux<RequestApplication> findAllByStatusIdWithPageable (Long statusId, PageRequest pageable);
    Flux<RequestApplication> findAllByStatusId (Long statusId);
    Mono<Long> countByStatusId (Long statusId);
    Mono<RequestApplication> findById(Long id);

}
