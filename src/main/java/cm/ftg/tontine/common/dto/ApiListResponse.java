package cm.ftg.tontine.common.dto;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record ApiListResponse<T>(List<T> data, PageMeta meta, String message, Instant timestamp) {

    public static <T> ApiListResponse<T> of(Page<T> page) {
        return new ApiListResponse<>(page.getContent(), PageMeta.from(page), null, Instant.now());
    }

    public static <T> ApiListResponse<T> of(List<T> data, PageMeta meta) {
        return new ApiListResponse<>(data, meta, null, Instant.now());
    }
}
