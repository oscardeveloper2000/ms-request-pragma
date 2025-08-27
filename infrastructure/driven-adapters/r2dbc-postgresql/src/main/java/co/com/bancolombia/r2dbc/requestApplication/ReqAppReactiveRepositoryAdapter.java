package co.com.bancolombia.r2dbc.requestApplication;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.mapper.RequestApplicationEntityMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

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


}
