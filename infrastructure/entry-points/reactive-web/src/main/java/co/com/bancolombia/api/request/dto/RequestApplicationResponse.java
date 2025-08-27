package co.com.bancolombia.api.request.dto;

import java.math.BigDecimal;

public record RequestApplicationResponse(
        Long id,
        String documentNumber,
        BigDecimal amount,
        Integer term,
        String email,
        Long loanTypeId,
        Long statusId
) { }