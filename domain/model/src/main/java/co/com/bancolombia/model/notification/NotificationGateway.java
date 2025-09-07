package co.com.bancolombia.model.notification;

import reactor.core.publisher.Mono;

public interface NotificationGateway {
    Mono<String> sendNotification(String message);
}
