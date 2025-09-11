// domain/model/src/main/java/co/com/bancolombia/model/messaging/gateway/MessagePublisherGateway.java
package co.com.bancolombia.model.external.messaging.gateway;

import reactor.core.publisher.Mono;

public interface MessagePublisherGateway {
    Mono<Void> publishLoanCalculateCapacity(String message);
    Mono<String> publishLoanNotificationEmail(String message);
    // Futuras colas aquí sin romper implementaciones existentes
}