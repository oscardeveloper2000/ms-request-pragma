package co.com.bancolombia.model.domains.typeloan.gateways;

import co.com.bancolombia.model.domains.typeloan.TypeLoan;
import reactor.core.publisher.Mono;

public interface TypeLoanRepository {
    Mono<TypeLoan> findById(Long id);
}
