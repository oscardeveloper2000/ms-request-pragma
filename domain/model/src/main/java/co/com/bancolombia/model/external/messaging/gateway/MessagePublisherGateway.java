// domain/model/src/main/java/co/com/bancolombia/model/messaging/gateway/MessagePublisherGateway.java
package co.com.bancolombia.model.external.messaging.gateway;

import reactor.core.publisher.Mono;

public interface MessagePublisherGateway {
    Mono<Void> publishLoanCalculateCapacity(String message);
    <T> Mono<String> publishLoanNotificationEmail(T messageObject);
    <T> Mono<String> publishReportLoan(T messageObject);
    // Futuras colas aquí sin romper implementaciones existentes
}