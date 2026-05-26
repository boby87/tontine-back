package cm.ftg.tontine.tontine.entity;

import cm.ftg.tontine.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FounderInviteEmbeddable {

    @Column(name = "founder_full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "founder_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "founder_email", length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "founder_role", nullable = false, length = 20)
    private UserRole role;
}
