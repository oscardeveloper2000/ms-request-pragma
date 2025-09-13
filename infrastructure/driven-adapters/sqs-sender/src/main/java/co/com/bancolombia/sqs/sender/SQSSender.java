package co.com.bancolombia.sqs.sender;


import co.com.bancolombia.model.external.messaging.gateway.MessagePublisherGateway;
import co.com.bancolombia.sqs.sender.config.SQSSenderProperties;
import co.com.bancolombia.sqs.sender.enums.QueueType;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements MessagePublisherGateway /*implements SomeGateway*/ {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private static final Gson gson = new Gson();
    @Override
    public Mono<Void> publishLoanCalculateCapacity(String message) {
        log.info("Enviando evento a loan-calculate-capacity-queue: {}", message);
        return sendToQueue(message, QueueType.LOAN_CALCULATE_CAPACITY).then();
    }

    @Override
    public <T> Mono<String> publishLoanNotificationEmail(T messageObject) {
        log.info("Enviando notificación a loan-notification-email-queue: {}", messageObject.toString());
        String jsonMessage = gson.toJson(messageObject);
        log.debug("JSON message: {}", jsonMessage);
        return sendToQueue(jsonMessage, QueueType.LOAN_NOTIFICATION_EMAIL);
    }

    private Mono<String> sendToQueue(String message, QueueType queueType) {
        String queueUrl = getQueueUrl(queueType);
        return sendToQueueUrl(message, queueUrl);
    }

    private String getQueueUrl(QueueType queueType) {
        String queueUrl = properties.queues().get(queueType.getQueueKey());
        if (queueUrl == null || queueUrl.isEmpty()) {
            throw new IllegalArgumentException("Queue URL not found for: " + queueType);
        }
        return queueUrl;
    }

    private Mono<String> sendToQueueUrl(String message, String queueUrl) {
        return Mono.fromCallable(() -> buildRequest(message, queueUrl))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent to queue {} with messageId: {}", queueUrl, response.messageId()))
                .map(SendMessageResponse::messageId)
                .doOnError(e -> log.error("Error sending message to queue {}: {}", queueUrl, e.getMessage()));
    }

    private SendMessageRequest buildRequest(String message, String queueUrl) {
        return SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .build();
    }
}
