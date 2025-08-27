package co.com.bancolombia.r2dbc.typeLoan;

import co.com.bancolombia.model.status.Status;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.TypeLoan;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.r2dbc.entity.StatusEntity;
import co.com.bancolombia.r2dbc.entity.TypeLoanEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TypeLoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        TypeLoan/* change for domain model */,
        TypeLoanEntity/* change for adapter model */,
    Long,
        TypeLoanReactiveRepository
> implements TypeLoanRepository {
    public TypeLoanReactiveRepositoryAdapter(TypeLoanReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, TypeLoan.class/* change for domain model */));
    }

}
