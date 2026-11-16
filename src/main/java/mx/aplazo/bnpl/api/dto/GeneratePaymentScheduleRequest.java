package mx.aplazo.bnpl.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GeneratePaymentScheduleRequest(
        @NotNull(message = "amount is required") BigDecimal amount,
        @NotNull(message = "installments is required") Integer installments,
        @NotNull(message = "commissionRate is required") BigDecimal commissionRate,
        @NotBlank(message = "purchaseDate is required") String purchaseDate
) {
}
