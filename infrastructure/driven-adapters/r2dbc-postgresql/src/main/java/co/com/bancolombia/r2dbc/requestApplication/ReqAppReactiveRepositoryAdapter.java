package co.com.bancolombia.r2dbc.requestApplication;

import co.com.bancolombia.model.common.paginators.PageableDomain;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.mapper.RequestApplicationEntityMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public class ReqAppReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        RequestApplication/* change for domain model */,
        RequestApplicationEntity/* change for adapter model */,
    Long,
        ReqAppReactiveRepository
> implements RequestApplicationRepository {
    private final RequestApplicationEntityMapper entityMapper;

    public ReqAppReactiveRepositoryAdapter(ReqAppReactiveRepository repository, ObjectMapper mapper, RequestApplicationEntityMapper entityMapper
    ) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, entityMapper::toModel);
        this.entityMapper = entityMapper;

    }

    @Override
    protected RequestApplicationEntity toData(RequestApplication domain) {
        return entityMapper.toEntity(domain);
    }


    @Override
    public Flux<RequestApplication> findAllByStatusIdWithPageable(Long statusId, PageableDomain pageable) {
        Pageable pageable1 =  org.springframework.data.domain.PageRequest.of(pageable.getPage(), pageable.getSize());
        return repository.findByStatusId(statusId, pageable1)
                .map(entityMapper::toModel);
    }

    @Override
    public Flux<RequestApplication> findAllByStatusId(Long statusId) {
        return repository.findByStatusId(statusId)
                .map(entityMapper::toModel);
    }

    @Override
    public Mono<Long> countByStatusId(Long statusId) {
        return repository.countByStatusId(statusId);
    }

    @Override
    public Flux<RequestApplication> findByEmailAndStatusId(String email, Long statusId) {
        return repository.findByEmailAndStatusId(email, statusId)
                .map(entityMapper::toModel);
    }
}
