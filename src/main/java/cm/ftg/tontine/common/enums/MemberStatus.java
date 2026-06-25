package cm.ftg.tontine.common.enums;

public enum MemberStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    LEFT,
    /** @deprecated use LEFT */
    @Deprecated
    RESIGNED,
    EXCLUDED
}
