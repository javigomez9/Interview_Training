package mx.aplazo.bnpl.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stateless amortization engine for Aplazo BNPL loans.
 * No I/O, no persistence, no caching — pure in-memory calculation.
 */
public final class PaymentScheduleGenerator {

    private static final int MIN_INSTALLMENTS = 2;
    private static final int MAX_INSTALLMENTS = 12;
    private static final int INSTALLMENT_INTERVAL_DAYS = 15;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    // "uuuu" (proleptic year), not "yyyy" (year-of-era): STRICT resolution needs an
    // explicit era to resolve year-of-era, which a plain ISO date never supplies.
    private static final DateTimeFormatter STRICT_ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private PaymentScheduleGenerator() {
    }

    public static PaymentSchedule generatePaymentSchedule(
            BigDecimal amount,
            int installments,
            BigDecimal commissionRate,
            String purchaseDate
    ) {
        BigDecimal normalizedAmount = validateAmount(amount);
        validateInstallments(installments);
        BigDecimal normalizedCommissionRate = validateCommissionRate(commissionRate);
        LocalDate firstDueDate = validateAndParsePurchaseDate(purchaseDate);

        BigDecimal totalCommission = normalizedAmount
                .multiply(normalizedCommissionRate)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal totalAmount = normalizedAmount.add(totalCommission);

        List<Installment> schedule = buildSchedule(totalAmount, installments, firstDueDate);

        return new PaymentSchedule(totalAmount, totalCommission, installments, schedule);
    }

    private static List<Installment> buildSchedule(BigDecimal totalAmount, int installments, LocalDate firstDueDate) {
        long totalCents = totalAmount.movePointRight(MONEY_SCALE).longValueExact();
        long baseCents = totalCents / installments;
        long remainderCents = totalCents % installments;

        List<Installment> schedule = new ArrayList<>(installments);
        LocalDate dueDate = firstDueDate;
        for (int number = 1; number <= installments; number++) {
            // The remainder from the integer division always lands on installment 1,
            // so installments 2..N stay identical and the sum matches totalAmount exactly.
            long cents = (number == 1) ? baseCents + remainderCents : baseCents;
            BigDecimal installmentAmount = BigDecimal.valueOf(cents, MONEY_SCALE);
            InstallmentStatus status = (number == 1) ? InstallmentStatus.PAID : InstallmentStatus.PENDING;

            schedule.add(new Installment(number, dueDate, installmentAmount, status));
            dueDate = dueDate.plusDays(INSTALLMENT_INTERVAL_DAYS);
        }
        return schedule;
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ScheduleValidationException(
                    ScheduleValidationException.ErrorCode.INVALID_AMOUNT,
                    "amount must be a finite number strictly greater than 0");
        }
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static void validateInstallments(int installments) {
        if (installments < MIN_INSTALLMENTS || installments > MAX_INSTALLMENTS) {
            throw new ScheduleValidationException(
                    ScheduleValidationException.ErrorCode.INVALID_INSTALLMENTS,
                    "installments must be an integer between " + MIN_INSTALLMENTS + " and " + MAX_INSTALLMENTS);
        }
    }

    private static BigDecimal validateCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ScheduleValidationException(
                    ScheduleValidationException.ErrorCode.INVALID_COMMISSION_RATE,
                    "commissionRate must be a finite number greater than or equal to 0");
        }
        return commissionRate;
    }

    private static LocalDate validateAndParsePurchaseDate(String purchaseDate) {
        if (purchaseDate == null || !ISO_DATE_PATTERN.matcher(purchaseDate).matches()) {
            throw new ScheduleValidationException(
                    ScheduleValidationException.ErrorCode.INVALID_PURCHASE_DATE,
                    "purchaseDate must follow strict YYYY-MM-DD format");
        }
        try {
            return LocalDate.parse(purchaseDate, STRICT_ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new ScheduleValidationException(
                    ScheduleValidationException.ErrorCode.INVALID_PURCHASE_DATE,
                    "purchaseDate is not a valid calendar date: " + purchaseDate);
        }
    }
}
