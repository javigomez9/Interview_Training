package mx.aplazo.bnpl.api.dto;

import java.time.Instant;

public record ApiErrorResponse(
        String errorCode,
        String message,
        String timestamp,
        String path
) {
    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode, message, Instant.now().toString(), path);
    }
}
