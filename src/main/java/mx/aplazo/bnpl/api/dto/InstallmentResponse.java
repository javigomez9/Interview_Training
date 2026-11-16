package mx.aplazo.bnpl.api.dto;

import mx.aplazo.bnpl.schedule.Installment;
import mx.aplazo.bnpl.schedule.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        InstallmentStatus status
) {
    public static InstallmentResponse from(Installment installment) {
        return new InstallmentResponse(
                installment.installmentNumber(),
                installment.dueDate(),
                installment.amount(),
                installment.status());
    }
}
