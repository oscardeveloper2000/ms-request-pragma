package co.com.bancolombia.model.domains.requestapplication.dto;

import java.math.BigDecimal;

public record PaymentPlan(
        Integer month,
        String principalForMonth,
        BigDecimal principalForMonthNumber,
        String interestToPay,
        String capitalPayment,
        String obligationBalance
) {}
