package cm.ftg.tontine.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.ALWAYS) Map<String, java.util.List<String>> details,
        Instant timestamp,
        String path,
        int status) {

    public static ApiError of(String code, String message, int status, String path) {
        return new ApiError(code, message, null, Instant.now().truncatedTo(ChronoUnit.MILLIS), path, status);
    }

    public static ApiError of(String code, String message, int status, String path,
                              Map<String, java.util.List<String>> details) {
        return new ApiError(code, message, details, Instant.now().truncatedTo(ChronoUnit.MILLIS), path, status);
    }
}
