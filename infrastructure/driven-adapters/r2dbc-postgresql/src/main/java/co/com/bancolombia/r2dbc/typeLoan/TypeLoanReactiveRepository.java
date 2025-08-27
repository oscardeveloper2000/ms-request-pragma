package co.com.bancolombia.r2dbc.typeLoan;

import co.com.bancolombia.r2dbc.entity.StatusEntity;
import co.com.bancolombia.r2dbc.entity.TypeLoanEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface TypeLoanReactiveRepository extends ReactiveCrudRepository<TypeLoanEntity, Long>, ReactiveQueryByExampleExecutor<TypeLoanEntity> {

}
