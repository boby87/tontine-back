package cm.ftg.tontine.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User")
class UserTest {

    private User creerUtilisateurValide() {
        var user = new User();
        user.setId("550e8400-e29b-41d4-a716-446655440000");
        user.setEmail("jean.dupont@test.cm");
        user.setPhone("+237690000000");
        user.setPasswordHash("$2a$12$hashedvalue");
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setDateOfBirth(LocalDate.of(1990, 5, 15));
        user.setRole(UserRole.MEMBER);
        user.setRegion(Region.CENTRE);
        user.setCniNumber("123456789");
        return user;
    }

    // ──────────────────────────────────────────────
    //  Valeurs par défaut
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Valeurs par défaut")
    class ValeursParDefaut {

        @Test
        @DisplayName("Doit initialiser le rôle à MEMBER par défaut")
        void should_defaultRoleToMember_when_newUser() {
            var user = new User();
            assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("Doit initialiser verified à false par défaut")
        void should_defaultVerifiedToFalse_when_newUser() {
            var user = new User();
            assertThat(user.isVerified()).isFalse();
        }

        @Test
        @DisplayName("Doit initialiser twoFactorEnabled à false par défaut")
        void should_defaultTwoFactorToFalse_when_newUser() {
            var user = new User();
            assertThat(user.isTwoFactorEnabled()).isFalse();
        }

        @Test
        @DisplayName("Doit initialiser la liste memberships vide par défaut")
        void should_defaultMembershipsToEmptyList_when_newUser() {
            var user = new User();
            assertThat(user.getMemberships()).isNotNull().isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    //  Getters / Setters (métier uniquement)
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Champs métier")
    class ChampsMetier {

        @Test
        @DisplayName("Doit stocker et retourner l'ID UUID")
        void should_storeAndReturnId_when_setId() {
            var user = creerUtilisateurValide();
            assertThat(user.getId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }

        @Test
        @DisplayName("Doit stocker et retourner le rôle plateforme")
        void should_storeAndReturnRole_when_setRole() {
            var user = creerUtilisateurValide();
            user.setRole(UserRole.ADMIN);
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Doit stocker le téléphone au format camerounais")
        void should_storePhone_when_cameroonFormat() {
            var user = creerUtilisateurValide();
            assertThat(user.getPhone()).startsWith("+237");
        }
    }

    // ──────────────────────────────────────────────
    //  Soft delete
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Soft delete")
    class SoftDelete {

        @Test
        @DisplayName("Doit positionner deletedAt pour un soft delete")
        void should_setDeletedAt_when_softDelete() {
            var user = creerUtilisateurValide();
            var now = LocalDateTime.now();
            user.setDeletedAt(now);
            assertThat(user.getDeletedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Doit avoir deletedAt null par défaut")
        void should_haveNullDeletedAt_when_newUser() {
            var user = new User();
            assertThat(user.getDeletedAt()).isNull();
        }
    }

    // ──────────────────────────────────────────────
    //  Memberships
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Memberships")
    class Memberships {

        @Test
        @DisplayName("Doit pouvoir ajouter un TontineMember à la liste")
        void should_addMember_when_addToMemberships() {
            var user = creerUtilisateurValide();
            var member = new TontineMember();
            member.setUser(user);
            member.setRole(TontineRole.PRESIDENT);

            user.getMemberships().add(member);

            assertThat(user.getMemberships()).hasSize(1);
            assertThat(user.getMemberships().getFirst().getRole()).isEqualTo(TontineRole.PRESIDENT);
        }
    }
}
