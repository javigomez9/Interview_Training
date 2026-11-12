package mx.aplazo.bnpl.schedule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentScheduleGeneratorTest {

    @Test
    void exactDivisionWithoutCommission() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("1000"), 5, BigDecimal.ZERO, "2026-01-01");

        assertEquals("1000.00", result.totalAmount().toPlainString());
        assertEquals("0.00", result.totalCommission().toPlainString());
        assertEquals(5, result.installmentsCount());

        for (Installment installment : result.schedule()) {
            assertEquals("200.00", installment.amount().toPlainString());
        }
        assertEquals(InstallmentStatus.PAID, result.schedule().get(0).status());
        for (int i = 1; i < result.schedule().size(); i++) {
            assertEquals(InstallmentStatus.PENDING, result.schedule().get(i).status());
        }
        assertSumMatchesTotal(result);
    }

    @Test
    void inexactDivisionAdjustsCentsOnFirstInstallment() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("100"), 3, BigDecimal.ZERO, "2026-01-01");

        List<Installment> schedule = result.schedule();
        assertEquals("33.34", schedule.get(0).amount().toPlainString());
        assertEquals("33.33", schedule.get(1).amount().toPlainString());
        assertEquals("33.33", schedule.get(2).amount().toPlainString());
        assertSumMatchesTotal(result);
    }

    @Test
    void commissionItselfRequiresRoundingBeforeSplitting() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("99.99"), 4, new BigDecimal("0.15"), "2026-01-01");

        // 99.99 * 0.15 = 14.9985 -> HALF_UP -> 15.00
        assertEquals("15.00", result.totalCommission().toPlainString());
        assertEquals("114.99", result.totalAmount().toPlainString());

        List<Installment> schedule = result.schedule();
        assertEquals("28.77", schedule.get(0).amount().toPlainString());
        assertEquals("28.74", schedule.get(1).amount().toPlainString());
        assertEquals("28.74", schedule.get(2).amount().toPlainString());
        assertEquals("28.74", schedule.get(3).amount().toPlainString());
        assertSumMatchesTotal(result);
    }

    @Test
    void endOfMonthAndLeapYearDateRollover() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("500"), 2, BigDecimal.ZERO, "2026-02-20");

        assertEquals(LocalDate.of(2026, 2, 20), result.schedule().get(0).dueDate());
        assertEquals(LocalDate.of(2026, 3, 7), result.schedule().get(1).dueDate());
    }

    @Test
    void yearBoundaryDateRollover() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("500"), 2, BigDecimal.ZERO, "2026-12-25");

        assertEquals(LocalDate.of(2026, 12, 25), result.schedule().get(0).dueDate());
        assertEquals(LocalDate.of(2027, 1, 9), result.schedule().get(1).dueDate());
    }

    @Test
    void firstInstallmentDueDateMatchesPurchaseDateAndIsPaid() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("300"), 2, BigDecimal.ZERO, "2026-05-10");

        Installment first = result.schedule().get(0);
        assertEquals(LocalDate.of(2026, 5, 10), first.dueDate());
        assertEquals(InstallmentStatus.PAID, first.status());
    }

    @Test
    void amountIsSilentlyRoundedToTwoDecimalsHalfUp() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("99.999"), 2, BigDecimal.ZERO, "2026-01-01");

        assertEquals("100.00", result.totalAmount().toPlainString());
    }

    @Test
    void negativeAmountIsRejected() {
        ScheduleValidationException exception = assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("-10"), 5, BigDecimal.ZERO, "2026-01-01"));
        assertEquals(ScheduleValidationException.ErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    @Test
    void zeroAmountIsRejected() {
        assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        BigDecimal.ZERO, 5, BigDecimal.ZERO, "2026-01-01"));
    }

    @Test
    void tooFewInstallmentsIsRejected() {
        ScheduleValidationException exception = assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 1, BigDecimal.ZERO, "2026-01-01"));
        assertEquals(ScheduleValidationException.ErrorCode.INVALID_INSTALLMENTS, exception.getErrorCode());
    }

    @Test
    void tooManyInstallmentsIsRejected() {
        assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 13, BigDecimal.ZERO, "2026-01-01"));
    }

    @Test
    void negativeCommissionRateIsRejected() {
        ScheduleValidationException exception = assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 5, new BigDecimal("-0.01"), "2026-01-01"));
        assertEquals(ScheduleValidationException.ErrorCode.INVALID_COMMISSION_RATE, exception.getErrorCode());
    }

    @Test
    void invalidCalendarDateIsRejected() {
        ScheduleValidationException exception = assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 5, BigDecimal.ZERO, "2026-02-30"));
        assertEquals(ScheduleValidationException.ErrorCode.INVALID_PURCHASE_DATE, exception.getErrorCode());
    }

    @Test
    void invalidMonthIsRejected() {
        assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 5, BigDecimal.ZERO, "2026-13-01"));
    }

    @Test
    void malformedDateFormatIsRejected() {
        assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 5, BigDecimal.ZERO, "2026-2-5"));
    }

    @Test
    void realLeapYearFebruary29IsAccepted() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("100"), 2, BigDecimal.ZERO, "2028-02-29");

        assertEquals(LocalDate.of(2028, 2, 29), result.schedule().get(0).dueDate());
    }

    @Test
    void nonLeapYearFebruary29IsRejected() {
        assertThrows(ScheduleValidationException.class, () ->
                PaymentScheduleGenerator.generatePaymentSchedule(
                        new BigDecimal("100"), 5, BigDecimal.ZERO, "2026-02-29"));
    }

    @Test
    void amountsAlwaysExposeExactlyTwoDecimalsRegardlessOfTrailingZeros() {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                new BigDecimal("300"), 3, BigDecimal.ZERO, "2026-01-01");

        assertEquals(2, result.totalAmount().scale());
        assertEquals(2, result.totalCommission().scale());
        for (Installment installment : result.schedule()) {
            assertEquals(2, installment.amount().scale());
        }
        assertEquals("100.00", result.schedule().get(0).amount().toPlainString());
    }

    @Test
    void invariantHoldsWithPeriodicCommissionRateAcrossAllInstallmentCounts() {
        for (int installments = 2; installments <= 12; installments++) {
            PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                    new BigDecimal("100"), installments, new BigDecimal("0.0333"), "2026-01-01");
            assertSumMatchesTotal(result);
        }
    }

    private static void assertSumMatchesTotal(PaymentSchedule result) {
        BigDecimal sum = result.schedule().stream()
                .map(Installment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(result.totalAmount(), sum);
        assertTrue(result.schedule().stream().filter(i -> i.installmentNumber() == 1).count() == 1);
    }
}
