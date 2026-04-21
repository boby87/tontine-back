package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TontineInvitationService")
class TontineInvitationServiceTest {

    @Mock private TontineInvitationRepository invitationRepository;
    @Mock private TontineRepository tontineRepository;
    @Mock private UserRepository userRepository;
    @Mock private TontineMemberRepository tontineMemberRepository;

    @InjectMocks private TontineInvitationService invitationService;

    private User defaultUser;
    private Tontine defaultTontine;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId("user-1");
        defaultUser.setEmail("test@test.com");

        defaultTontine = new Tontine();
        defaultTontine.setId(1L);
        defaultTontine.setNom("Tontine Test");
    }

    // ──────────────────────────────────────────────
    //  genererInvitation()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("genererInvitation()")
    class GenererInvitation {

        @Test
        @DisplayName("Doit générer un token alphanumérique de 32 caractères et stocker son hash SHA-256")
        void should_generateAlphanumericTokenAndStoreHash_when_validRequest() {
            // Arrange
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var president = new TontineMember();
            president.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(president));

            when(invitationRepository.save(any(TontineInvitation.class))).thenAnswer(inv -> {
                var invitation = inv.getArgument(0, TontineInvitation.class);
                invitation.setId("inv-1");
                return invitation;
            });

            var request = new GenerateInviteLinkRequest(1L, "user-1", 5);

            // Act
            var result = invitationService.genererInvitation(request);

            // Assert — le token renvoyé est en clair, 32 chars, alphanumérique
            assertThat(result.token()).hasSize(32);
            assertThat(result.token()).matches("[A-Za-z0-9]+");
            assertThat(result.tontineId()).isEqualTo(1L);
            assertThat(result.maxUses()).isEqualTo(5);
            assertThat(result.expiresAt()).isAfter(LocalDateTime.now().plusDays(9));

            // Assert — le hash stocké en base est le SHA-256 du token, pas le token en clair
            verify(invitationRepository).save(argThat(invitation -> {
                String expectedHash = TontineInvitationService.sha256(result.token());
                return invitation.getTokenHash().equals(expectedHash)
                    && invitation.getTokenHash().length() == 64  // SHA-256 hex = 64 chars
                    && !invitation.getTokenHash().equals(result.token());
            }));
        }

        @Test
        @DisplayName("Doit utiliser maxUses=1 par défaut quand non spécifié")
        void should_defaultMaxUsesToOne_when_notSpecified() {
            // Arrange
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var president = new TontineMember();
            president.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(president));

            when(invitationRepository.save(any(TontineInvitation.class))).thenAnswer(inv -> {
                var invitation = inv.getArgument(0, TontineInvitation.class);
                invitation.setId("inv-1");
                return invitation;
            });

            var request = new GenerateInviteLinkRequest(1L, "user-1", null);

            // Act
            var result = invitationService.genererInvitation(request);

            // Assert
            verify(invitationRepository).save(argThat(inv -> inv.getMaxUses() == 1));
        }

        @Test
        @DisplayName("Doit lancer AccesNonAutoriseException quand l'utilisateur n'est pas PRESIDENT")
        void should_throwException_when_userIsNotPresident() {
            // Arrange
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var membre = new TontineMember();
            membre.setRole(TontineRole.MEMBRE);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(membre));

            var request = new GenerateInviteLinkRequest(1L, "user-1", null);

            // Act & Assert
            assertThatThrownBy(() -> invitationService.genererInvitation(request))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("PRESIDENT");

            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lancer TontineIntrouvableException quand la tontine n'existe pas")
        void should_throwException_when_tontineNotFound() {
            // Arrange
            when(tontineRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
            var request = new GenerateInviteLinkRequest(99L, "user-1", null);

            // Act & Assert
            assertThatThrownBy(() -> invitationService.genererInvitation(request))
                .isInstanceOf(TontineIntrouvableException.class);
        }
    }

    // ──────────────────────────────────────────────
    //  validerToken()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("validerToken()")
    class ValiderToken {

        @Test
        @DisplayName("Doit valider un token valide et incrémenter le compteur d'utilisations")
        void should_validateAndIncrementUses_when_tokenIsValid() {
            // Arrange
            String tokenClair = "AbCdEfGhIjKlMnOpQrStUvWxYz012345";
            String tokenHash = TontineInvitationService.sha256(tokenClair);

            var invitation = new TontineInvitation();
            invitation.setId("inv-1");
            invitation.setTontine(defaultTontine);
            invitation.setTokenHash(tokenHash);
            invitation.setExpiresAt(LocalDateTime.now().plusDays(5));
            invitation.setMaxUses(3);
            invitation.setCurrentUses(1);

            when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any(TontineInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = invitationService.validerToken(tokenClair);

            // Assert
            assertThat(result.tontineId()).isEqualTo(1L);
            assertThat(result.tontineNom()).isEqualTo("Tontine Test");
            assertThat(result.currentUses()).isEqualTo(2);
            assertThat(result.maxUses()).isEqualTo(3);

            verify(invitationRepository).save(argThat(inv -> inv.getCurrentUses() == 2));
        }

        @Test
        @DisplayName("Doit lancer InvitationInvalideException quand le token est inconnu")
        void should_throwException_when_tokenUnknown() {
            // Arrange
            String tokenClair = "UnknownToken12345678901234567890";
            String tokenHash = TontineInvitationService.sha256(tokenClair);
            when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> invitationService.validerToken(tokenClair))
                .isInstanceOf(InvitationInvalideException.class)
                .hasMessageContaining("inconnu");
        }

        @Test
        @DisplayName("Doit lancer InvitationInvalideException quand le token est expiré")
        void should_throwException_when_tokenExpired() {
            // Arrange
            String tokenClair = "ExpiredToken1234567890123456789A";
            String tokenHash = TontineInvitationService.sha256(tokenClair);

            var invitation = new TontineInvitation();
            invitation.setId("inv-1");
            invitation.setTontine(defaultTontine);
            invitation.setTokenHash(tokenHash);
            invitation.setExpiresAt(LocalDateTime.now().minusDays(1));  // expiré
            invitation.setMaxUses(5);
            invitation.setCurrentUses(0);

            when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

            // Act & Assert
            assertThatThrownBy(() -> invitationService.validerToken(tokenClair))
                .isInstanceOf(InvitationInvalideException.class)
                .hasMessageContaining("expiré");
        }

        @Test
        @DisplayName("Doit lancer InvitationInvalideException quand le nombre maximal d'utilisations est atteint")
        void should_throwException_when_maxUsesReached() {
            // Arrange
            String tokenClair = "MaxUsesToken123456789012345678AB";
            String tokenHash = TontineInvitationService.sha256(tokenClair);

            var invitation = new TontineInvitation();
            invitation.setId("inv-1");
            invitation.setTontine(defaultTontine);
            invitation.setTokenHash(tokenHash);
            invitation.setExpiresAt(LocalDateTime.now().plusDays(5));
            invitation.setMaxUses(2);
            invitation.setCurrentUses(2);  // épuisé

            when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

            // Act & Assert
            assertThatThrownBy(() -> invitationService.validerToken(tokenClair))
                .isInstanceOf(InvitationInvalideException.class)
                .hasMessageContaining("maximal");
        }
    }

    // ──────────────────────────────────────────────
    //  supprimerInvitationsExpirees()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("supprimerInvitationsExpirees()")
    class SupprimerInvitationsExpirees {

        @Test
        @DisplayName("Doit supprimer les invitations expirées et retourner le nombre")
        void should_deleteExpiredInvitations() {
            // Arrange
            when(invitationRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(5);

            // Act
            int count = invitationService.supprimerInvitationsExpirees();

            // Assert
            assertThat(count).isEqualTo(5);
            verify(invitationRepository).deleteExpiredBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Doit retourner 0 quand aucune invitation n'est expirée")
        void should_returnZero_when_noExpiredInvitations() {
            // Arrange
            when(invitationRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(0);

            // Act
            int count = invitationService.supprimerInvitationsExpirees();

            // Assert
            assertThat(count).isZero();
        }
    }

    // ──────────────────────────────────────────────
    //  Utilitaires (sha256, genererTokenAlphanumerique)
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Utilitaires")
    class Utilitaires {

        @Test
        @DisplayName("sha256 doit retourner un hash hexadécimal de 64 caractères")
        void should_returnHex64_when_sha256() {
            String hash = TontineInvitationService.sha256("hello");
            assertThat(hash).hasSize(64);
            assertThat(hash).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("sha256 doit être déterministe")
        void should_beDeterministic_when_sameInput() {
            String hash1 = TontineInvitationService.sha256("test-token");
            String hash2 = TontineInvitationService.sha256("test-token");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("sha256 doit produire des hashes différents pour des entrées différentes")
        void should_produceDifferentHashes_when_differentInputs() {
            String hash1 = TontineInvitationService.sha256("token-a");
            String hash2 = TontineInvitationService.sha256("token-b");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("genererTokenAlphanumerique doit générer un token de la longueur demandée")
        void should_generateTokenOfCorrectLength() {
            String token = TontineInvitationService.genererTokenAlphanumerique(32);
            assertThat(token).hasSize(32);
            assertThat(token).matches("[A-Za-z0-9]+");
        }

        @Test
        @DisplayName("genererTokenAlphanumerique doit générer des tokens uniques")
        void should_generateUniqueTokens() {
            String token1 = TontineInvitationService.genererTokenAlphanumerique(32);
            String token2 = TontineInvitationService.genererTokenAlphanumerique(32);
            assertThat(token1).isNotEqualTo(token2);
        }
    }
}

