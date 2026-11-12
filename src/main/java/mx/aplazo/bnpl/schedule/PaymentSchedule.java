package mx.aplazo.bnpl.schedule;

import java.math.BigDecimal;
import java.util.List;

public record PaymentSchedule(
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        int installmentsCount,
        List<Installment> schedule
) {
}
