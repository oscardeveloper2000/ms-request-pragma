package co.com.bancolombia.model.external.messaging.dto;

import co.com.bancolombia.model.domains.requestapplication.dto.PaymentPlan;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageBody<T> {
        private final String email;
        private final String subject;
        private final T message;
        private final List<PaymentPlan> paymentPlan;
    }