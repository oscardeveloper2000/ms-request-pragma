package co.com.bancolombia.r2dbc.status;

import co.com.bancolombia.model.domains.status.Status;
import co.com.bancolombia.model.domains.status.gateways.StatusRepository;
import co.com.bancolombia.r2dbc.entity.StatusEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Status/* change for domain model */,
        StatusEntity/* change for adapter model */,
    Long,
        StatusReactiveRepository
> implements StatusRepository {
    public StatusReactiveRepositoryAdapter(StatusReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Status.class/* change for domain model */));
    }

}
