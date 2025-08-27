package co.com.bancolombia.api.request.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record RequestApplicationRecord(
        @NotBlank(message = "Document number is mandatory")
        String documentNumber,
        @NotNull(message = "Amount is mandatory")
        @Positive(message = "Amount must be a positive number")
        BigDecimal amount,
        @NotNull(message = "Term is mandatory")
        @Positive(message = "Term must be a positive number")
        Integer term,
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email should be valid")
        String email,
        @NotNull(message = "Loan type ID is mandatory")
        Long loanTypeId
) {
}

