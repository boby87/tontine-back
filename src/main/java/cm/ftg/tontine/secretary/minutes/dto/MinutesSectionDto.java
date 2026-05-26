package cm.ftg.tontine.secretary.minutes.dto;

import cm.ftg.tontine.secretary.minutes.entity.MinutesSectionEntity;

public record MinutesSectionDto(
        String key,
        String title,
        String content,
        boolean required
) {

    public static MinutesSectionDto from(MinutesSectionEntity s) {
        return new MinutesSectionDto(s.getSectionKey(), s.getTitle(), s.getContent(), s.isRequired());
    }
}
