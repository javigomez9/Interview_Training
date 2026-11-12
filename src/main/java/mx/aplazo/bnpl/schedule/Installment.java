package mx.aplazo.bnpl.schedule;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Installment(
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        InstallmentStatus status
) {
}
