package co.com.bancolombia.r2dbc.requestApplication;

import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface ReqAppReactiveRepository extends ReactiveCrudRepository<RequestApplicationEntity, Long>, ReactiveQueryByExampleExecutor<RequestApplicationEntity> {

}
