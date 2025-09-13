package co.com.bancolombia.model.domains.requestapplication.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class NotificationStatusMessage {
    private long requestId;
        private String status;
        private String userClient;
        private String emailClient;
}
