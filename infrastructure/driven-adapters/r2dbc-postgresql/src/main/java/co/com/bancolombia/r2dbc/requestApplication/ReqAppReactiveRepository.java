package co.com.bancolombia.r2dbc.requestApplication;

import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// TODO: This file is just an example, you should delete or modify it
public interface ReqAppReactiveRepository extends ReactiveCrudRepository<RequestApplicationEntity, Long>, ReactiveQueryByExampleExecutor<RequestApplicationEntity> {
    Flux<RequestApplicationEntity> findByStatusId(Long statusId, Pageable pageable);
    Flux<RequestApplicationEntity> findByStatusId(Long statusId);
    Mono<Long> countByStatusId(Long statusId);
    Flux<RequestApplicationEntity> findByEmailAndStatusId(String email, Long statusId);
}
