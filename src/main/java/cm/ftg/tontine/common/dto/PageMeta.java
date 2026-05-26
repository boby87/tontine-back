package cm.ftg.tontine.common.dto;

import org.springframework.data.domain.Page;

public record PageMeta(
        int page,
        int pageSize,
        long total,
        int totalPages,
        boolean hasNext,
        boolean hasPrev) {

    public static PageMeta from(Page<?> page) {
        return new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
