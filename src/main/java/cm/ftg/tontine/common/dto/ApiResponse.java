package cm.ftg.tontine.common.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
