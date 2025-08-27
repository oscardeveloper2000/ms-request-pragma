package co.com.bancolombia.r2dbc.requestApplication;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReqAppReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        RequestApplication/* change for domain model */,
        RequestApplicationEntity/* change for adapter model */,
    Long,
        ReqAppReactiveRepository
> implements RequestApplicationRepository {
    public ReqAppReactiveRepositoryAdapter(ReqAppReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, RequestApplication.class/* change for domain model */));
    }

}
