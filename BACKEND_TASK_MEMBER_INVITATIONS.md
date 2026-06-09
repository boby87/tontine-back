# 📨 Brief Implémentation — Module "Invitations de membres"

> Tâche à implémenter dans **`tontine-back`** pour permettre au Président :
> 1. d'**inviter de nouveaux membres** après création de la tontine (formulaire UI déjà prêt côté frontend)
> 2. de **suivre** les invitations (PENDING → SENT → ACCEPTED / EXPIRED / CANCELLED)
> 3. de **relancer** ou **annuler** une invitation
>
> ET de brancher l'**auto-invitation des fondateurs** déclarés dans le wizard de création de tontine.
>
> Stack : Spring Boot 3, Java 21, Spring Security, Spring Data JPA, Lombok, Flyway, `SmsSender` + `EmailSender` existants.

---

## 1. Contexte fonctionnel — Flow utilisateur

```
1. User crée un compte           POST /auth/register → /auth/verify-otp
2. User crée une tontine          POST /tontines  (avec founders[])
                                  ↓
                                  Backend doit auto-créer une invitation
                                  pour chaque founder (sauf le créateur)
                                  et envoyer le lien d'inscription
3. Président invite d'autres      POST /president/membership/invite
   membres plus tard              GET  /president/membership/invitations
                                  POST /president/membership/invitations/{id}/resend
                                  POST /president/membership/invitations/{id}/cancel
4. Le candidat clique sur le lien GET  /auth/invitations/{token}/preview   (public)
                                  POST /auth/invitations/{token}/accept    (public)
                                  → crée un compte User + Member.status=ACTIVE
                                    avec le rôle proposé, rattaché à la tontine
```

> **Frontend** : le service Angular `PresidentService` consomme déjà les endpoints `/president/membership/invite|invitations[/...]`. La page `/president/invitations` est en place. Voir §11 pour le détail.

---

## 2. Endpoints à créer

### 2.1 Côté Président (authentifiés, rôle PRESIDENT requis)

| Méthode | Path                                                       | Description                              |
| ------- | ---------------------------------------------------------- | ---------------------------------------- |
| `POST`  | `/president/membership/invite`                             | Créer + envoyer une invitation           |
| `GET`   | `/president/membership/invitations`                        | Lister toutes les invitations            |
| `POST`  | `/president/membership/invitations/{id}/resend`            | Renvoyer le SMS / e-mail                 |
| `POST`  | `/president/membership/invitations/{id}/cancel`            | Annuler                                  |

### 2.2 Endpoints publics (sans auth) pour le candidat

| Méthode | Path                                       | Description                                                      |
| ------- | ------------------------------------------ | ---------------------------------------------------------------- |
| `GET`   | `/auth/invitations/{token}/preview`        | Voir l'invitation (nom tontine, rôle, parrain) avant d'accepter   |
| `POST`  | `/auth/invitations/{token}/accept`         | Accepter → crée le compte User + Member ACTIVE                    |

> Ces endpoints doivent être **whitelistés** dans la `SecurityConfig` au même titre que `/auth/login`, `/auth/register`.

---

## 3. Entité JPA `MembershipInvitation`

Package proposé : **`cm.ftg.tontine.president.membership.invitation`**

```java
package cm.ftg.tontine.president.membership.invitation.entity;

@Entity
@Table(
    name = "membership_invitations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_invitation_token", columnNames = "token")
    },
    indexes = {
        @Index(name = "idx_invitation_tontine", columnList = "tontine_id"),
        @Index(name = "idx_invitation_status", columnList = "status"),
        @Index(name = "idx_invitation_phone", columnList = "candidate_phone")
    }
)
@Getter @Setter @NoArgsConstructor
public class MembershipInvitation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "candidate_full_name", nullable = false, length = 160)
    private String candidateFullName;

    @Column(name = "candidate_phone", nullable = false, length = 20)
    private String candidatePhone;

    @Column(name = "candidate_email", length = 160)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_role", nullable = false, length = 20)
    private UserRole proposedRole;

    /** Concat CSV "SMS,EMAIL,WHATSAPP" — ou table de jonction si tu préfères. */
    @Column(nullable = false, length = 100)
    private String channels;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    /**
     * Token public utilisé dans le lien d'acceptation.
     * Format recommandé : 32 caractères URL-safe (Base64 sans padding) issus de SecureRandom.
     */
    @Column(nullable = false, length = 80)
    private String token;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Column(name = "invited_by_full_name", nullable = false, length = 160)
    private String invitedByFullName;

    @CreationTimestamp
    @Column(name = "invited_at", updatable = false, nullable = false)
    private Instant invitedAt;

    @Column(name = "reminders_sent", nullable = false)
    private int remindersSent = 0;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Version
    private Long version;
}
```

### Enum `InvitationStatus` (existant côté frontend)

```java
package cm.ftg.tontine.president.membership.invitation.enums;

public enum InvitationStatus {
    PENDING,    // créée, pas encore envoyée (entre 2 retries SMS échoués)
    SENT,       // SMS / e-mail envoyés au candidat
    ACCEPTED,   // candidat a accepté → Member créé
    EXPIRED,    // expiresAt < now()
    CANCELLED   // annulée par le Président
}
```

### Migration Flyway `V2__create_membership_invitations.sql`

```sql
CREATE TABLE membership_invitations (
    id                    UUID         NOT NULL,
    tontine_id            UUID         NOT NULL,
    candidate_full_name   VARCHAR(160) NOT NULL,
    candidate_phone       VARCHAR(20)  NOT NULL,
    candidate_email       VARCHAR(160),
    proposed_role         VARCHAR(20)  NOT NULL,
    channels              VARCHAR(100) NOT NULL,
    message               VARCHAR(1000),
    status                VARCHAR(20)  NOT NULL,
    token                 VARCHAR(80)  NOT NULL,
    sent_at               TIMESTAMP,
    accepted_at           TIMESTAMP,
    accepted_user_id      UUID,
    expires_at            TIMESTAMP    NOT NULL,
    invited_by_user_id    UUID         NOT NULL,
    invited_by_full_name  VARCHAR(160) NOT NULL,
    invited_at            TIMESTAMP    NOT NULL,
    reminders_sent        INT          NOT NULL DEFAULT 0,
    cancelled_at          TIMESTAMP,
    cancelled_by_user_id  UUID,
    cancel_reason         VARCHAR(500),
    version               BIGINT,
    CONSTRAINT pk_membership_invitations PRIMARY KEY (id),
    CONSTRAINT uk_invitation_token UNIQUE (token)
);

CREATE INDEX idx_invitation_tontine ON membership_invitations (tontine_id);
CREATE INDEX idx_invitation_status  ON membership_invitations (status);
CREATE INDEX idx_invitation_phone   ON membership_invitations (candidate_phone);
```

---

## 4. DTOs (alignés au frontend Angular)

### 4.1 `InviteMemberRequest`

```java
package cm.ftg.tontine.president.membership.invitation.dto;

public record InviteMemberRequest(
        @NotBlank @Size(max = 160) String candidateFullName,
        @NotBlank @Pattern(regexp = "^\\+237\\d{9}$") String candidatePhone,
        @Email @Size(max = 160) String candidateEmail,
        @NotNull UserRole proposedRole,
        @NotEmpty Set<InvitationChannel> channels,
        @Size(max = 1000) String message
) { }

public enum InvitationChannel { SMS, EMAIL, WHATSAPP }
```

### 4.2 `MembershipInvitationDto`

```java
public record MembershipInvitationDto(
        UUID id,
        UUID tontineId,
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        UserRole proposedRole,
        List<InvitationChannel> channels,
        String message,
        InvitationStatus status,
        Instant sentAt,
        Instant acceptedAt,
        Instant expiresAt,
        UUID invitedByUserId,
        String invitedByFullName,
        Instant invitedAt,
        int remindersSent,
        Instant cancelledAt,
        String cancelReason,
        String acceptUrl   // ⚠ ne renvoyer que pour le Président — masquer pour autres rôles
) {
    public static MembershipInvitationDto from(MembershipInvitation i, String baseAcceptUrl) {
        return new MembershipInvitationDto(
                i.getId(), i.getTontineId(),
                i.getCandidateFullName(), i.getCandidatePhone(), i.getCandidateEmail(),
                i.getProposedRole(),
                Arrays.stream(i.getChannels().split(","))
                      .map(String::trim).filter(s -> !s.isEmpty())
                      .map(InvitationChannel::valueOf).toList(),
                i.getMessage(), i.getStatus(),
                i.getSentAt(), i.getAcceptedAt(), i.getExpiresAt(),
                i.getInvitedByUserId(), i.getInvitedByFullName(), i.getInvitedAt(),
                i.getRemindersSent(),
                i.getCancelledAt(), i.getCancelReason(),
                baseAcceptUrl + "/invitations/" + i.getToken()
        );
    }
}
```

### 4.3 `CancelInvitationRequest`

```java
public record CancelInvitationRequest(@Size(max = 500) String reason) { }
```

### 4.4 `InvitationPreviewDto` (endpoint public)

```java
public record InvitationPreviewDto(
        String tontineName,
        String invitedByFullName,
        UserRole proposedRole,
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        Instant expiresAt,
        boolean expired,
        boolean alreadyAccepted
) { }
```

### 4.5 `AcceptInvitationRequest` (endpoint public)

```java
public record AcceptInvitationRequest(
        @NotBlank @Size(min = 8, max = 100) String password
) { }
```

→ Si `candidateEmail` est fourni au moment de l'invitation, on garde celui-ci ; sinon le candidat fournira lors d'un OTP ultérieur.

### 4.6 `AcceptInvitationResponse`

```java
public record AcceptInvitationResponse(
        AuthSessionDto session,  // tokens + user (mêmes DTOs que /auth/login)
        UUID memberId,
        UUID tontineId,
        String tontineName
) { }
```

---

## 5. Règles métier

### 5.1 Création d'invitation (`POST /president/membership/invite`)

1. **Authorization** : le user doit être `PRESIDENT` de la tontine active (`PresidentAccessChecker.requirePresident(userId, tontineId)`).
2. **Tontine doit être ACTIVE** ou DRAFT (pas COMPLETED / CLOSED) → sinon **422** `TONTINE_INACTIVE`.
3. **Capacité** : `members.count + invitations.SENT.count >= tontine.maxMembers` → **422** `TONTINE_FULL`.
4. **Unicité (téléphone)** : refuser si un `Member` ACTIVE existe déjà avec ce téléphone dans la tontine → **409** `MEMBER_ALREADY_EXISTS`.
5. **Doublon d'invitation** : si une invitation `PENDING|SENT` existe déjà pour ce téléphone dans cette tontine → **409** `INVITATION_ALREADY_PENDING`.
5bis. **Rôle proposé** : `proposedRole` ∈ `{MEMBER, SECRETARY, TREASURER, CENSOR, AUDITOR}`. Si `proposedRole == PRESIDENT` → **422** `INVITATION_PRESIDENT_FORBIDDEN` avec message *« Le créateur de la tontine est l'unique Président. Pour transférer le rôle, utilisez le module Délégations. »*. ⚠ Le frontend utilise déjà le type `InvitableFounderRole = Exclude<FounderRole, 'PRESIDENT'>` côté Angular, mais le backend doit valider à nouveau (defense-in-depth).
6. **Génération du token** : `SecureRandom` → 24 bytes → Base64 URL-safe sans padding (≈ 32 caractères).
7. **Expiration** : `expiresAt = now + 7 jours` (configurable via `app.invitation.expiry-days`, défaut `7`).
8. **Envoi** : voir §6 — selon `channels`, appeler `SmsSender` et/ou `EmailSender`. En cas d'échec d'un canal, ne pas faire échouer la requête, mais logger ; status reste `SENT` si au moins un canal a réussi, sinon `PENDING` (à relancer manuellement).
9. **Audit** : `AuditService.record(userId, "INVITATION_CREATE", "MembershipInvitation", id, tontineId, "{\"phone\":\"...\"}");`
10. **WebSocket** : `realtime.toTontine(tontineId, "invitation.created", dto)` pour rafraîchir le dashboard.

### 5.2 Lister (`GET /president/membership/invitations`)

- Filtre implicite : `tontineId` (résolu via `TontineIdResolver`).
- Tri par `invitedAt DESC`.
- Tâche de fond pour passer les `SENT` à `EXPIRED` (voir §7).

### 5.3 Renvoyer (`POST /.../resend`)

- Refusé si `status != SENT && status != PENDING` → **422** `INVITATION_NOT_RESENDABLE`.
- Réutilise les mêmes canaux que la création.
- Incrémente `remindersSent`.
- Si elle a expiré (`expiresAt < now`), **prolonger** automatiquement `expiresAt = now + 7 days`.
- Audit + realtime.

### 5.4 Annuler (`POST /.../cancel`)

- Refusé si déjà `ACCEPTED` → **422** `INVITATION_ALREADY_ACCEPTED`.
- `status=CANCELLED`, `cancelledAt=now`, `cancelledByUserId=userId`, `cancelReason=req.reason`.
- Audit + realtime.

### 5.5 Preview public (`GET /auth/invitations/{token}/preview`)

- Pas d'auth.
- Si token introuvable → **404** `INVITATION_NOT_FOUND`.
- Toujours retourner **200** avec `expired` / `alreadyAccepted` calculés — c'est au frontend d'afficher le bon message.

### 5.6 Acceptation publique (`POST /auth/invitations/{token}/accept`)

1. Token introuvable → **404**.
2. `status=CANCELLED` → **422** `INVITATION_CANCELLED`.
3. `status=ACCEPTED` → **409** `INVITATION_ALREADY_ACCEPTED`.
4. `expiresAt < now` → **422** `INVITATION_EXPIRED`.
5. Créer `UserEntity` (ou réutiliser si un user avec ce téléphone/email existe déjà) :
   - Champs récupérés de l'invitation : `firstName`/`lastName` (split sur `candidateFullName`), `phone`, `email`.
   - `password` haché avec BCrypt (`PasswordEncoder` existant).
   - **OTP/vérification** : si tu veux garder l'étape OTP, créer le user avec `isPhoneVerified=false` puis envoyer un OTP → mais plus simple : considérer que cliquer sur le lien SMS **vaut vérification du téléphone** → `isPhoneVerified=true`.
6. Créer `Member` :
   - `userId` = nouveau user
   - `tontineId` = invitation.tontineId
   - `status` = ACTIVE
   - `roles` = `EnumSet.of(invitation.proposedRole, UserRole.MEMBER)`
   - `matricule` = format `M-NNN` (compteur sur la tontine)
7. `invitation.status=ACCEPTED`, `acceptedAt=now`, `acceptedUserId=userId`.
8. Incrémenter `tontine.memberCount`.
9. Émettre les `JWT` (access + refresh) — réutiliser le code de `/auth/login`.
10. Audit `INVITATION_ACCEPT`, realtime `invitation.accepted` + `tontine.member-added`.
11. **Si la tontine était `DRAFT` et qu'avec ce nouveau membre `memberCount >= 3`**, ne rien faire d'automatique — c'est au Président de basculer vers `ACTIVE` (via `PATCH /tontines/{id}`).

### 5.7 Auto-invitation des fondateurs (hook dans `TontineService.create()`)

⚠ **Règle stricte sur le rôle du créateur** : le créateur (`creatorUserId`) reçoit **automatiquement** le rôle `PRESIDENT` via `buildCreatorMember()` (déjà en place lignes 186-198). Aucun autre fondateur ne peut être PRESIDENT — c'est la règle "un Président par tontine".

Lorsqu'on parcourt `req.founders()` :
- Si un `FounderInviteDto` arrive avec `role == PRESIDENT` (potentiellement via curl/Postman, le frontend l'empêche par typage) → soit **rejeter** la création avec **422** `FOUNDER_PRESIDENT_FORBIDDEN`, soit **silencieusement remapper** vers `MEMBER` et logger un warning. ✅ Recommandation : **rejeter** pour rendre l'erreur visible.

Modifier `cm.ftg.tontine.tontine.service.TontineService.create()` (lignes 96-111 actuellement) :

```java
// Avant : on créait directement des Member PENDING.
// Après : on crée une MembershipInvitation pour chaque founder ≠ créateur.

InvitationService invitationService;  // injecté

for (FounderInviteDto f : req.founders()) {
    if (isCreator(f, creator)) continue;
    invitationService.inviteFromFounder(
        saved.getId(),
        creatorUserId,
        creator,  // pour invitedByFullName
        f,
        Set.of(InvitationChannel.SMS,
               f.email() != null && !f.email().isBlank() ? InvitationChannel.EMAIL : null)
                .stream().filter(Objects::nonNull).collect(Collectors.toSet())
    );
}
```

Le code actuel qui crée des `Member` PENDING peut être **supprimé** — c'est l'invitation acceptée qui créera le `Member` ACTIVE. **Le `memberCount` reste à 1** (le créateur) jusqu'à ce que des invitations soient acceptées.

> Variante : si on préfère garder les `Member` PENDING en visibilité (pour les afficher dans la liste des membres avec un badge "Invitation envoyée"), on peut **conserver** la création des `Member` PENDING mais aussi créer l'invitation, et la lier via `Member.invitationId`. Au choix — l'option **épurée** (suppression des `Member` PENDING) est recommandée pour éviter la double vérité.

---

## 6. Envoi SMS / e-mail

### Message SMS (140 caractères max idéalement)

```
Bonjour {fullName}, vous êtes invité(e) à rejoindre la tontine "{tontineName}" comme {proposedRoleLabel}. Acceptez : {acceptUrl}
```

### Message e-mail (HTML)

Sujet : `Invitation à rejoindre la tontine "{tontineName}"`

Body (template Thymeleaf ou simple `String.format`) :
- En-tête tontine
- Nom du parrain (invitedByFullName)
- Rôle proposé
- Message libre s'il y en a un
- Bouton « Accepter l'invitation » → `${app.frontend.base-url}/auth/invitations/{token}/accept`
- Date d'expiration

### Variables config (`application.yaml`)

```yaml
app:
  invitation:
    expiry-days: 7
    sms-template: "Bonjour {fullName}, ..."
  frontend:
    base-url: "https://app.tontine-connect.cm"
```

### WhatsApp

Pas d'implémentation actuelle → laisser un `// TODO whatsapp` ou utiliser l'API WhatsApp Business. Pour maintenant, si `WHATSAPP` est dans `channels`, fallback sur le `SmsSender` avec une note dans les logs.

---

## 7. Tâche planifiée — expiration

Dans le package `cm.ftg.tontine.scheduling` :

```java
@Scheduled(cron = "0 0 * * * *")  // toutes les heures
@Transactional
public void expireInvitations() {
    Instant now = Instant.now();
    List<MembershipInvitation> stale = invitationRepository
        .findByStatusInAndExpiresAtBefore(
            Set.of(InvitationStatus.PENDING, InvitationStatus.SENT), now);
    for (MembershipInvitation inv : stale) {
        inv.setStatus(InvitationStatus.EXPIRED);
        invitationRepository.save(inv);
        auditService.record(null, "INVITATION_EXPIRE", "MembershipInvitation",
            inv.getId().toString(), inv.getTontineId(), null);
    }
}
```

---

## 8. Codes d'erreur (via `ApiException`)

| HTTP | Code                              | Message                                                  |
| ---- | --------------------------------- | -------------------------------------------------------- |
| 403  | `FORBIDDEN`                       | Seul le Président peut inviter des membres               |
| 404  | `INVITATION_NOT_FOUND`            | Invitation introuvable                                   |
| 409  | `MEMBER_ALREADY_EXISTS`           | Un membre avec ce numéro existe déjà                     |
| 409  | `INVITATION_ALREADY_PENDING`      | Une invitation est déjà en cours pour ce numéro          |
| 409  | `INVITATION_ALREADY_ACCEPTED`     | Cette invitation a déjà été acceptée                     |
| 422  | `TONTINE_INACTIVE`                | La tontine n'est pas active                              |
| 422  | `TONTINE_FULL`                    | Le nombre maximum de membres est atteint                 |
| 422  | `INVITATION_NOT_RESENDABLE`       | Seules les invitations en attente peuvent être relancées |
| 422  | `INVITATION_CANCELLED`            | Cette invitation a été annulée                           |
| 422  | `INVITATION_EXPIRED`              | Cette invitation a expiré                                |
| 422  | `INVITATION_PRESIDENT_FORBIDDEN`  | Le créateur est l'unique Président — rôle non assignable |
| 422  | `FOUNDER_PRESIDENT_FORBIDDEN`     | Un fondateur ne peut pas être déclaré Président          |

---

## 9. Structure de packages proposée

```
cm.ftg.tontine.president.membership.invitation/
├── controller/
│   ├── PresidentInvitationController.java       (/president/membership/...)
│   └── PublicInvitationController.java          (/auth/invitations/...)
├── dto/
│   ├── InviteMemberRequest.java
│   ├── CancelInvitationRequest.java
│   ├── MembershipInvitationDto.java
│   ├── InvitationPreviewDto.java
│   ├── AcceptInvitationRequest.java
│   └── AcceptInvitationResponse.java
├── entity/
│   └── MembershipInvitation.java
├── enums/
│   ├── InvitationStatus.java
│   └── InvitationChannel.java
├── repository/
│   └── MembershipInvitationRepository.java
└── service/
    ├── InvitationService.java                    (createur/resend/cancel)
    ├── InvitationAcceptService.java              (workflow d'acceptation)
    ├── InvitationDispatcher.java                 (envoie SMS+email selon channels)
    └── InvitationTokenGenerator.java             (SecureRandom helper)
```

---

## 10. Tests d'intégration attendus (`@SpringBootTest`)

Classe : `PresidentInvitationControllerIT`

| # | Scénario | Attendu |
|---|---|---|
| 1 | Invite : succès, SMS + email envoyés, status=SENT | 200 + DB |
| 2 | Invite : sans email → SENT via SMS uniquement | 200 |
| 3 | Invite : téléphone déjà membre → 409 `MEMBER_ALREADY_EXISTS` | 409 |
| 4 | Invite : invitation PENDING déjà ouverte → 409 `INVITATION_ALREADY_PENDING` | 409 |
| 5 | Invite : tontine pleine → 422 `TONTINE_FULL` | 422 |
| 6 | List : retourne en ordre `invitedAt DESC` | 200 |
| 7 | Resend : status=SENT → ok, `remindersSent` +1 | 200 |
| 8 | Resend : status=ACCEPTED → 422 `INVITATION_NOT_RESENDABLE` | 422 |
| 9 | Cancel : status=SENT → ok | 200 |
| 10 | Cancel : status=ACCEPTED → 422 `INVITATION_ALREADY_ACCEPTED` | 422 |
| 11 | Auth: non-président → 403 | 403 |

Classe : `PublicInvitationControllerIT`

| # | Scénario | Attendu |
|---|---|---|
| 12 | Preview : token valide → 200 avec `expired=false, alreadyAccepted=false` | 200 |
| 13 | Preview : token expiré → 200 avec `expired=true` | 200 |
| 14 | Preview : token inexistant → 404 | 404 |
| 15 | Accept : token valide → 200, retourne `AuthSession`, crée user + member ACTIVE | 200 |
| 16 | Accept : token expiré → 422 `INVITATION_EXPIRED` | 422 |
| 17 | Accept : déjà accepté → 409 | 409 |
| 18 | Accept : annulé → 422 `INVITATION_CANCELLED` | 422 |
| 19 | Accept : tontine.memberCount incrémenté | DB assertion |

Classe : `TontineCreateInvitationHookIT`

| # | Scénario | Attendu |
|---|---|---|
| 20 | Création tontine avec 3 founders (1 créateur + 2 autres) → 2 invitations SENT | DB + SMS mock vérifié |
| 21 | Création tontine : founder déclaré avec même téléphone que créateur → ignoré (pas d'invitation) | DB |

---

## 11. Côté frontend (déjà fait — pour référence)

Le frontend Angular `ndah-connect-web` consomme déjà les 4 endpoints `/president/membership/invite|invitations` :

- **Service** : [`src/app/features/president/services/president.service.ts`](../ndah-connect-web/src/app/features/president/services/president.service.ts) — méthodes `inviteMember`, `getInvitations`, `resendInvitation`, `cancelInvitation`
- **Modèle** : [`src/app/shared/models/entities/membership-invitation.model.ts`](../ndah-connect-web/src/app/shared/models/entities/membership-invitation.model.ts) — `MembershipInvitation`, `InvitationStatus`, `InvitationChannel`
- **Page** : [`src/app/features/president/pages/invitations/invitations-page.component.ts`](../ndah-connect-web/src/app/features/president/pages/invitations/invitations-page.component.ts) — route `/president/invitations`
- **Tests** : 5 tests unitaires passants ([`president-invitations.service.spec.ts`](../ndah-connect-web/src/app/features/president/services/president-invitations.service.spec.ts))

**À ajouter côté frontend après livraison backend** :
- Écrans publics `/auth/invitations/:token/preview` et `/auth/invitations/:token/accept` pour le candidat.

---

## 12. Critères d'acceptation

- [ ] Migration `V2__create_membership_invitations.sql` appliquée
- [ ] Entité, repository, enums, DTOs créés
- [ ] 4 endpoints `/president/membership/...` répondent au format `ApiResponse<T>`
- [ ] 2 endpoints publics `/auth/invitations/{token}/...` accessibles sans token JWT (whitelist `SecurityConfig`)
- [ ] Envoi SMS via `SmsSender` + e-mail via `EmailSender` (les 2 implémentations log + Twilio/SMTP fonctionnent)
- [ ] Hook auto-invitation dans `TontineService.create()` actif
- [ ] Tâche planifiée `expireInvitations()` enregistrée (cron horaire)
- [ ] Audit trail (`AuditService`) sur create/resend/cancel/accept/expire
- [ ] Évènements temps réel (`RealtimeEventPublisher`) publiés sur `tontine.{id}` channel
- [ ] Tests d'intégration (21 scénarios) verts
- [ ] Compilation propre : `./mvnw clean verify`

---

## 13. Vérification manuelle (curl)

```bash
# 1. Login Président
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"+237699000001","password":"pass1234"}' \
  | jq -r '.data.tokens.accessToken')

# 2. Inviter un membre
curl -X POST http://localhost:8081/api/president/membership/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "candidateFullName": "Jean Tagne",
    "candidatePhone": "+237699000099",
    "candidateEmail": "jean@example.cm",
    "proposedRole": "MEMBER",
    "channels": ["SMS", "EMAIL"],
    "message": "Bienvenue dans la famille !"
  }'
# → 200 avec MembershipInvitationDto incluant token + acceptUrl

# 3. Lister
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/president/membership/invitations

# 4. Preview public (récupérer le token depuis le résultat ci-dessus)
curl http://localhost:8081/api/auth/invitations/$TOKEN_INV/preview

# 5. Accepter
curl -X POST http://localhost:8081/api/auth/invitations/$TOKEN_INV/accept \
  -H "Content-Type: application/json" \
  -d '{"password":"Welcome123!"}'
# → 200 avec AuthSessionDto + memberId
```

---

## 14. Sécurité

- Token : 24 bytes random → 32 caractères URL-safe. **N'expose jamais** le hash bcrypt du token ; le token brut est stocké tel quel (c'est un secret one-shot court-vivant).
- Le `acceptUrl` ne doit être renvoyé **qu'au Président** dans la liste — pas aux autres rôles (Censeur, Auditeur…). Concrètement, dans `MembershipInvitationDto.from(...)`, on peut ne pas le calculer si le caller n'est pas Président.
- Rate limiting recommandé sur `POST /auth/invitations/{token}/accept` (max 5 tentatives par IP/minute) pour limiter le brute-force de tokens.
- Logs : ne logguer que l'`id` de l'invitation, **jamais le token**.

---

**Référence de patterns** : aligne-toi sur le module existant `cm.ftg.tontine.president.vote.*` (services + controller + AuditService + RealtimeEventPublisher) et `cm.ftg.tontine.president.membership.*` (entité MembershipFile).
