package co.com.bancolombia.model.external.messaging.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityValidationEventListener {
    String eventType;
    String solicitudId;
    String status;
    String reason;
    CalculationDetails calculationDetails;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationDetails {
        BigDecimal monthlyQuota;
        BigDecimal maxCapacity;
        BigDecimal currentDebt;
        BigDecimal availableCapacity;
        BigDecimal totalIncome;
        BigDecimal applicantSalary;
        BigDecimal loanAmount;
    }
}
