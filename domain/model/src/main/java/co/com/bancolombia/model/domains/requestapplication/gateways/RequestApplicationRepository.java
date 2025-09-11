package co.com.bancolombia.model.domains.requestapplication.gateways;

import co.com.bancolombia.model.common.paginators.PageableDomain;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



public interface RequestApplicationRepository {
    Mono<RequestApplication> save(RequestApplication requestApplication);
    Flux<RequestApplication> findAllByStatusIdWithPageable (Long statusId, PageableDomain pageable);
    Flux<RequestApplication> findAllByStatusId (Long statusId);
    Mono<Long> countByStatusId (Long statusId);
    Mono<RequestApplication> findById(Long id);
    Flux<RequestApplication> findByEmailAndStatusId(String email, Long statusId);

}
