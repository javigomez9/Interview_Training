package mx.aplazo.bnpl.schedule;

public class ScheduleValidationException extends RuntimeException {

    public enum ErrorCode {
        INVALID_AMOUNT,
        INVALID_INSTALLMENTS,
        INVALID_COMMISSION_RATE,
        INVALID_PURCHASE_DATE
    }

    private final ErrorCode errorCode;

    public ScheduleValidationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
