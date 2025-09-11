package co.com.bancolombia.sqs.listener;

import co.com.bancolombia.model.external.messaging.dto.CapacityValidationEventListener;
import co.com.bancolombia.usecase.processcapacityvalidation.ProcessCapacityValidationUseCase;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
     private final ProcessCapacityValidationUseCase processCapacityValidationUseCase;
    private static final Gson gson = new Gson();
    @Override
    public Mono<Void> apply(Message message) {
        System.out.println("Message received: " + message.body());
        CapacityValidationEventListener event = gson.fromJson(message.body(), CapacityValidationEventListener.class);
        return Mono.just(event)
                .flatMap(processCapacityValidationUseCase::processValidationEvent)
                .then();
    }
}
