// domain/model/src/main/java/co/com/bancolombia/model/capacityvalidation/CapacityValidationEvent.java
package co.com.bancolombia.model.external.messaging.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CapacityValidationEventPublish {
    private Long solicitudId;
    private BigDecimal totalIncome;
    private BigDecimal amount;
    private Integer termMonths;
    private Double interestRate;
    private BigDecimal applicantSalary;
    private List<ActiveLoan> activeLoans;

    @Data
    @Builder
    public static class ActiveLoan {
        private String id;
        private BigDecimal amount;
        private Double interestRate;
        private Integer termMonths;
    }
}