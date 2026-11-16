package mx.aplazo.bnpl.api.dto;

import mx.aplazo.bnpl.schedule.PaymentSchedule;

import java.math.BigDecimal;
import java.util.List;

public record PaymentScheduleResponse(
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        int installmentsCount,
        List<InstallmentResponse> schedule
) {
    public static PaymentScheduleResponse from(PaymentSchedule domain) {
        return new PaymentScheduleResponse(
                domain.totalAmount(),
                domain.totalCommission(),
                domain.installmentsCount(),
                domain.schedule().stream().map(InstallmentResponse::from).toList());
    }
}
