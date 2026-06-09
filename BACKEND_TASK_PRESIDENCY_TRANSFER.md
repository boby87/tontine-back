# 👑 Brief Implémentation — Module "Transfert de Présidence"

> Tâche à implémenter dans **`tontine-back`** pour permettre au Président actuel de transférer définitivement son rôle à un autre membre actif, avec **acceptation bilatérale** obligatoire.
>
> Stack : Spring Boot 3, Java 21, Spring Security, Spring Data JPA, Lombok, Flyway.
>
> Pré-requis : la règle "un seul Président par tontine" est déjà documentée dans [`BACKEND_RULE_ONE_PRESIDENT.md`](BACKEND_RULE_ONE_PRESIDENT.md).

---

## 1. Flow utilisateur

```
PRÉSIDENT actuel                          MEMBRE désigné
─────────────────                         ───────────────
1. Saisit motif + sélectionne membre
2. POST /president/presidency-transfer
   { targetMemberId, reason }
   → status=PENDING, expiresAt = now+72h
   → notification push au membre cible
                                          3. Voit la proposition sur
                                             /member/presidency-transfer
                                          4. Soit accepte :
                                             POST /members/me/presidency-transfer/{id}/accept
                                             → SWAP atomique des rôles :
                                                - ancien : roles.remove(PRESIDENT)
                                                - nouveau : roles.add(PRESIDENT)
                                             → status=ACCEPTED
                                             → notification à tous
                                             → ancien Président redirigé en /dashboard membre
                                          5. Soit refuse :
                                             POST /.../decline { reason }
                                             → status=DECLINED
                                             → ancien Président notifié

Annulation possible côté Président tant que status=PENDING :
   POST /president/presidency-transfers/{id}/cancel { reason? }

Expiration automatique (job @Scheduled) :
   status=PENDING && expiresAt < now → status=EXPIRED
```

---

## 2. Endpoints à créer

### 2.1 Côté Président

| Méthode | Path                                                      | Description                            |
| ------- | --------------------------------------------------------- | -------------------------------------- |
| `POST`  | `/president/presidency-transfer`                          | Initier un transfert (1 seul PENDING à la fois) |
| `GET`   | `/president/presidency-transfers`                         | Historique des transferts de la tontine|
| `POST`  | `/president/presidency-transfers/{id}/cancel`             | Annuler (tant que PENDING)             |

### 2.2 Côté Membre destinataire

| Méthode | Path                                                | Description                            |
| ------- | --------------------------------------------------- | -------------------------------------- |
| `GET`   | `/members/me/presidency-transfer/pending`           | Transfert PENDING qui m'est adressé    |
| `POST`  | `/members/me/presidency-transfer/{id}/accept`       | Accepter — déclenche le swap atomique  |
| `POST`  | `/members/me/presidency-transfer/{id}/decline`      | Refuser avec motif                     |

---

## 3. Entité JPA `PresidencyTransfer`

Package proposé : **`cm.ftg.tontine.president.presidencytransfer`**

```java
package cm.ftg.tontine.president.presidencytransfer.entity;

@Entity
@Table(
    name = "presidency_transfers",
    indexes = {
        @Index(name = "idx_pt_tontine", columnList = "tontine_id"),
        @Index(name = "idx_pt_status",  columnList = "status"),
        @Index(name = "idx_pt_target",  columnList = "target_member_id")
    }
)
@Getter @Setter @NoArgsConstructor
public class PresidencyTransfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "initiated_by_user_id", nullable = false)
    private UUID initiatedByUserId;

    @Column(name = "initiated_by_full_name", nullable = false, length = 160)
    private String initiatedByFullName;

    @Column(name = "target_member_id", nullable = false)
    private UUID targetMemberId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_member_full_name", nullable = false, length = 160)
    private String targetMemberFullName;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresidencyTransferStatus status = PresidencyTransferStatus.PENDING;

    @CreationTimestamp
    @Column(name = "initiated_at", updatable = false, nullable = false)
    private Instant initiatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Version
    private Long version;
}
```

### Enum `PresidencyTransferStatus`

```java
package cm.ftg.tontine.president.presidencytransfer.enums;

public enum PresidencyTransferStatus {
    PENDING, ACCEPTED, DECLINED, CANCELLED, EXPIRED
}
```

### Migration `V3__create_presidency_transfers.sql`

```sql
CREATE TABLE presidency_transfers (
    id                       UUID         NOT NULL,
    tontine_id               UUID         NOT NULL,
    initiated_by_user_id     UUID         NOT NULL,
    initiated_by_full_name   VARCHAR(160) NOT NULL,
    target_member_id         UUID         NOT NULL,
    target_user_id           UUID,
    target_member_full_name  VARCHAR(160) NOT NULL,
    reason                   VARCHAR(2000) NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    initiated_at             TIMESTAMP    NOT NULL,
    expires_at               TIMESTAMP    NOT NULL,
    accepted_at              TIMESTAMP,
    declined_at              TIMESTAMP,
    decline_reason           VARCHAR(1000),
    cancelled_at             TIMESTAMP,
    cancelled_by_user_id     UUID,
    cancel_reason            VARCHAR(1000),
    version                  BIGINT,
    CONSTRAINT pk_presidency_transfers PRIMARY KEY (id)
);

CREATE INDEX idx_pt_tontine ON presidency_transfers (tontine_id);
CREATE INDEX idx_pt_status  ON presidency_transfers (status);
CREATE INDEX idx_pt_target  ON presidency_transfers (target_member_id);

-- Garantit qu'au plus UN transfert est PENDING par tontine à un instant T
CREATE UNIQUE INDEX uk_pt_one_pending_per_tontine
    ON presidency_transfers (tontine_id)
    WHERE status = 'PENDING';
```

---

## 4. DTOs (alignés au frontend)

### 4.1 `InitiatePresidencyTransferRequest`

```java
public record InitiatePresidencyTransferRequest(
        @NotNull UUID targetMemberId,
        @NotBlank @Size(min = 10, max = 2000) String reason
) { }
```

### 4.2 `DeclinePresidencyTransferRequest`

```java
public record DeclinePresidencyTransferRequest(
        @NotBlank @Size(min = 10, max = 1000) String reason
) { }
```

### 4.3 `CancelPresidencyTransferRequest`

```java
public record CancelPresidencyTransferRequest(
        @Size(max = 1000) String reason
) { }
```

### 4.4 `PresidencyTransferDto`

```java
public record PresidencyTransferDto(
        UUID id,
        UUID tontineId,
        UUID initiatedByUserId,
        String initiatedByFullName,
        UUID targetMemberId,
        String targetMemberFullName,
        UUID targetUserId,
        String reason,
        PresidencyTransferStatus status,
        Instant initiatedAt,
        Instant expiresAt,
        Instant acceptedAt,
        Instant declinedAt,
        String declineReason,
        Instant cancelledAt,
        String cancelReason
) {
    public static PresidencyTransferDto from(PresidencyTransfer t) {
        return new PresidencyTransferDto(
                t.getId(), t.getTontineId(),
                t.getInitiatedByUserId(), t.getInitiatedByFullName(),
                t.getTargetMemberId(), t.getTargetMemberFullName(), t.getTargetUserId(),
                t.getReason(), t.getStatus(),
                t.getInitiatedAt(), t.getExpiresAt(),
                t.getAcceptedAt(), t.getDeclinedAt(), t.getDeclineReason(),
                t.getCancelledAt(), t.getCancelReason()
        );
    }
}
```

> ⚠ Le `GET /pending` peut renvoyer `null` (pas d'enveloppe vide ; l'API renverra `{ "data": null, ... }`) — le frontend gère.

---

## 5. Règles métier

### 5.1 Initier (`POST /president/presidency-transfer`)

1. **Autorization** : `PresidentAccessChecker.requirePresident(userId, tontineId)`.
2. **Au plus un PENDING par tontine** : query `findFirstByTontineIdAndStatus(t, PENDING)`. Si trouvé → **409** `PRESIDENCY_TRANSFER_ALREADY_PENDING`.
3. **Cible existe et est ACTIVE** : `memberRepository.findByIdAndTontineId(targetMemberId, tontineId)`. Status doit être `ACTIVE` → sinon **422** `TARGET_MEMBER_NOT_ELIGIBLE`.
4. **Cible ≠ initiateur** : `target.userId != userId` → sinon **422** `TARGET_CANNOT_BE_SELF`.
5. **Cible n'est pas déjà Président** : si `target.roles.contains(PRESIDENT)` → **422** `TARGET_ALREADY_PRESIDENT`. (Garde-fou — devrait être impossible si la règle "un seul Président" tient.)
6. **expiresAt = now + app.presidency-transfer.expiry-hours** (défaut `72`, configurable).
7. **Snapshot** des noms : `initiatedByFullName` + `targetMemberFullName` figés à la création (au cas où le membre est renommé après).
8. **Notification** : push + e-mail + SMS au `targetUserId` (s'il existe). Template :
   *"Le Président {initiatedByFullName} de la tontine {tontineName} vous propose de prendre sa succession. Vous avez jusqu'au {expiresAt} pour décider. Lien : {frontend}/member/presidency-transfer"*
9. **Audit** : `auditService.record(userId, "PRESIDENCY_TRANSFER_INIT", "PresidencyTransfer", id, tontineId, "{\"target\":\"" + targetMemberId + "\"}")`.
10. **Realtime** : `realtime.toMember(tontineId, target.getUserId(), "presidency-transfer.proposed", dto)`.

### 5.2 Liste (`GET /president/presidency-transfers`)

- Filtre par `tontineId`.
- Tri `initiatedAt DESC`.

### 5.3 Annuler (`POST /.../cancel`)

- Refusé si `status != PENDING` → **422** `TRANSFER_NOT_PENDING`.
- `status=CANCELLED`, `cancelledByUserId=userId`, `cancelReason=req.reason`, `cancelledAt=now`.
- Audit + notification au membre cible.

### 5.4 Voir l'attente (`GET /members/me/presidency-transfer/pending`)

- Cherche `findByTargetUserIdAndStatus(userId, PENDING)`.
- Si rien → renvoyer `{ "data": null }` (HTTP 200, pas 404).
- Si trouvé mais `expiresAt < now` → bascule en `EXPIRED` au passage et renvoie `null`.

### 5.5 Accepter (`POST /members/me/presidency-transfer/{id}/accept`)

**Opération critique — doit être dans une seule `@Transactional` avec niveau d'isolation `REPEATABLE_READ` ou plus, pour éviter qu'un autre transfert ne s'intercale.**

1. Charger le transfert. Vérifier `targetUserId == currentUserId` (sinon **403**).
2. Vérifier `status == PENDING` (sinon **422** `TRANSFER_NOT_PENDING`).
3. Vérifier `expiresAt > now` (sinon **422** `TRANSFER_EXPIRED` + bascule status=EXPIRED).
4. Charger l'**ancien Président** :
   ```java
   Member oldPres = memberRepository.findByTontineIdAndRoleContaining(tontineId, PRESIDENT)
       .orElseThrow(() -> new IllegalStateException("Aucun Président actuel"));
   ```
5. Charger le **nouveau Président** (cible) :
   ```java
   Member newPres = memberRepository.findById(transfer.getTargetMemberId())
       .orElseThrow(ResourceNotFoundException);
   ```
6. **Swap atomique** :
   ```java
   oldPres.getRoles().remove(UserRole.PRESIDENT);
   // garantir qu'il reste au moins MEMBER
   if (!oldPres.getRoles().contains(UserRole.MEMBER)) oldPres.getRoles().add(UserRole.MEMBER);
   newPres.getRoles().add(UserRole.PRESIDENT);
   memberRepository.save(oldPres);
   memberRepository.save(newPres);
   ```
7. `transfer.setStatus(ACCEPTED); transfer.setAcceptedAt(now);`
8. **Audit critique** :
   ```java
   auditService.record(currentUserId, "PRESIDENCY_TRANSFER_ACCEPT",
       "PresidencyTransfer", transfer.getId().toString(), tontineId,
       String.format("{\"from\":\"%s\",\"to\":\"%s\"}", oldPres.getUserId(), newPres.getUserId()));
   ```
9. **Realtime à toute la tontine** :
   `realtime.toTontine(tontineId, "presidency-transfer.accepted", dto)`
10. **Notifications** : in-app pour tous les membres, SMS+email pour l'ancien Président.
11. **JWT refresh** : si tu embarques les rôles dans le JWT, l'ancien et le nouveau doivent appeler `/auth/refresh` pour récupérer un token avec les rôles à jour. Mentionne-le clairement dans la doc Swagger. Côté frontend, `AuthService.loadCurrentUser()` est appelé après `accept()` — voir [`member/pages/presidency-transfer/presidency-transfer-page.component.ts`](../ndah-connect-web/src/app/features/member/pages/presidency-transfer/presidency-transfer-page.component.ts).

### 5.6 Refuser (`POST /members/me/presidency-transfer/{id}/decline`)

- `status=DECLINED`, `declineReason=req.reason`, `declinedAt=now`.
- Notification + e-mail à l'ancien Président.
- Audit `PRESIDENCY_TRANSFER_DECLINE`.

### 5.7 Tâche planifiée — expiration

```java
@Scheduled(fixedDelay = 300_000)  // toutes les 5 minutes
@Transactional
public void expirePendingTransfers() {
    Instant now = Instant.now();
    List<PresidencyTransfer> stale = transferRepository
        .findByStatusAndExpiresAtBefore(PresidencyTransferStatus.PENDING, now);
    for (PresidencyTransfer t : stale) {
        t.setStatus(PresidencyTransferStatus.EXPIRED);
        transferRepository.save(t);
        auditService.record(null, "PRESIDENCY_TRANSFER_EXPIRE",
            "PresidencyTransfer", t.getId().toString(), t.getTontineId(), null);
        realtime.toUser(t.getInitiatedByUserId(),
            "presidency-transfer.expired", PresidencyTransferDto.from(t));
    }
}
```

---

## 6. Codes d'erreur (via `ApiException`)

| HTTP | Code                                  | Message                                                            |
| ---- | ------------------------------------- | ------------------------------------------------------------------ |
| 403  | `FORBIDDEN`                           | Seul le Président peut initier un transfert                        |
| 403  | `FORBIDDEN`                           | Vous n'êtes pas le destinataire de cette proposition               |
| 404  | `PRESIDENCY_TRANSFER_NOT_FOUND`       | Transfert introuvable                                              |
| 409  | `PRESIDENCY_TRANSFER_ALREADY_PENDING` | Un transfert est déjà en attente. Annulez-le avant d'en créer un nouveau |
| 422  | `TARGET_MEMBER_NOT_ELIGIBLE`          | Le membre désigné doit être actif                                  |
| 422  | `TARGET_CANNOT_BE_SELF`               | Vous ne pouvez pas vous désigner vous-même                         |
| 422  | `TARGET_ALREADY_PRESIDENT`            | Le membre désigné est déjà Président                               |
| 422  | `TRANSFER_NOT_PENDING`                | Ce transfert n'est plus en attente                                 |
| 422  | `TRANSFER_EXPIRED`                    | Cette proposition a expiré                                         |

---

## 7. Structure de packages

```
cm.ftg.tontine.president.presidencytransfer/
├── controller/
│   ├── PresidentPresidencyTransferController.java   (/president/presidency-transfer[s])
│   └── MemberPresidencyTransferController.java      (/members/me/presidency-transfer)
├── dto/
│   ├── InitiatePresidencyTransferRequest.java
│   ├── DeclinePresidencyTransferRequest.java
│   ├── CancelPresidencyTransferRequest.java
│   └── PresidencyTransferDto.java
├── entity/
│   └── PresidencyTransfer.java
├── enums/
│   └── PresidencyTransferStatus.java
├── repository/
│   └── PresidencyTransferRepository.java
└── service/
    ├── PresidencyTransferService.java               (initiate/list/cancel/getPending/decline)
    └── PresidencyAcceptService.java                 (swap atomique — séparé pour clarifier l'isolation)
```

---

## 8. Tests d'intégration attendus

Classe : `PresidencyTransferControllerIT` (`@SpringBootTest` + `MockMvc`)

| # | Scénario | Attendu |
|---|---|---|
| 1 | Président initie un transfert vers un membre ACTIVE → 200 + DB | OK |
| 2 | Cible = soi-même → 422 `TARGET_CANNOT_BE_SELF` | 422 |
| 3 | Cible = membre SUSPENDED → 422 `TARGET_MEMBER_NOT_ELIGIBLE` | 422 |
| 4 | Deux transferts simultanés sur la même tontine → 409 `..._ALREADY_PENDING` (testé via index unique partiel) | 409 |
| 5 | Membre cible appelle GET /pending → renvoie le transfert | 200 |
| 6 | Autre membre appelle GET /pending → renvoie `null` | 200 + data=null |
| 7 | **Accept atomique** : ancien perd PRESIDENT, nouveau l'a, et un seul Président existe au final | DB assertion + count |
| 8 | Accept par un autre user que le destinataire → 403 | 403 |
| 9 | Accept expiré → 422 `TRANSFER_EXPIRED` + status bascule EXPIRED | DB |
| 10 | Decline avec motif → status=DECLINED, notification à l'ancien | OK |
| 11 | Cancel PENDING → status=CANCELLED | OK |
| 12 | Cancel ACCEPTED → 422 `TRANSFER_NOT_PENDING` | 422 |
| 13 | Job d'expiration : transfert vieux passe en EXPIRED | DB |
| 14 | Test concurrentiel : 2 accepts simultanés → un seul réussit (optimistic locking sur `@Version`) | un succès, un échec |

---

## 9. Sécurité / Audit / Réversibilité

- **Concurrence** : utilise `@Version` (déjà sur l'entité) + `@Transactional(isolation = Isolation.REPEATABLE_READ)` pour l'accept. En cas de conflit `OptimisticLockingFailureException` → renvoie **409** `TRANSFER_CONFLICT`, le client peut retry.
- **Audit non altérable** : les entrées `AuditService` doivent être `INSERT ONLY`, jamais updatées. C'est la trace en cas de litige.
- **Réversibilité** : aucune. Une fois ACCEPTED, seul un nouveau transfert (initié par le nouveau Président) peut rétablir la situation. C'est documenté dans l'UI (bandeau d'avertissement orange dans le formulaire — cf. frontend).
- **Pas de batch** : un Président, un transfert à la fois. Verrouillé par l'index unique partiel.

---

## 10. Configuration `application.yaml`

```yaml
app:
  presidency-transfer:
    expiry-hours: 72                       # durée de validité d'une proposition
    max-pending-per-tontine: 1             # informatif (verrouillé par DB)
    notification-channels: [IN_APP, SMS, EMAIL]
```

---

## 11. Vérification manuelle (curl)

```bash
# Login Président
PRES_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"+237699000001","password":"pass1234"}' \
  | jq -r '.data.tokens.accessToken')

# 1. Initier
TRANSFER_ID=$(curl -s -X POST http://localhost:8081/api/president/presidency-transfer \
  -H "Authorization: Bearer $PRES_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetMemberId":"<UUID>","reason":"Je passe la main pour raisons personnelles"}' \
  | jq -r '.data.id')

# 2. Membre cible se connecte, voit la proposition
MEMBER_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"+237699000002","password":"pass1234"}' \
  | jq -r '.data.tokens.accessToken')

curl -H "Authorization: Bearer $MEMBER_TOKEN" \
  http://localhost:8081/api/members/me/presidency-transfer/pending

# 3. Accepter
curl -X POST http://localhost:8081/api/members/me/presidency-transfer/$TRANSFER_ID/accept \
  -H "Authorization: Bearer $MEMBER_TOKEN"

# 4. Refresh JWT pour récupérer le nouveau rôle
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Authorization: Bearer $MEMBER_TOKEN"

# 5. Vérifier (ancien Président devient simple membre)
curl -H "Authorization: Bearer $PRES_TOKEN" http://localhost:8081/api/auth/me
# → roles ne contient plus PRESIDENT
```

---

## 12. Frontend (déjà livré — pour référence)

Côté `ndah-connect-web` :

- **Modèle** : [`shared/models/entities/presidency-transfer.model.ts`](../ndah-connect-web/src/app/shared/models/entities/presidency-transfer.model.ts)
- **Service Président** : méthodes `initiatePresidencyTransfer`, `getPresidencyTransfers`, `cancelPresidencyTransfer` dans [`features/president/services/president.service.ts`](../ndah-connect-web/src/app/features/president/services/president.service.ts)
- **Service Membre** : [`features/member/services/presidency.service.ts`](../ndah-connect-web/src/app/features/member/services/presidency.service.ts) — `getPending`, `accept`, `decline`
- **Page Président** : `/president/presidency-transfer` ([`features/president/pages/presidency-transfer/`](../ndah-connect-web/src/app/features/president/pages/presidency-transfer/))
- **Page Membre** : `/member/presidency-transfer` ([`features/member/pages/presidency-transfer/`](../ndah-connect-web/src/app/features/member/pages/presidency-transfer/))
- **Tests** : 8 tests unitaires passants (4 président + 4 membre)

Le frontend appelle déjà `AuthService.loadCurrentUser()` après acceptation pour rafraîchir les rôles côté UI — vérifier que `/auth/me` retourne bien les rôles à jour après le swap.

---

## 13. Critères d'acceptation

- [ ] Migration `V3__create_presidency_transfers.sql` + index unique partiel
- [ ] Entité, repository, enum, DTOs
- [ ] 6 endpoints répondant en `ApiResponse<T>`
- [ ] Swap atomique transactionnel + `@Version` testé en concurrence
- [ ] Tâche planifiée d'expiration (5 min)
- [ ] Notifications in-app + SMS + e-mail (templates fournis)
- [ ] Audit trail complet (INIT / ACCEPT / DECLINE / CANCEL / EXPIRE)
- [ ] Realtime events via `RealtimeEventPublisher`
- [ ] 14 tests d'intégration verts
- [ ] `./mvnw clean verify` propre

---

**Référence projet** : aligne-toi sur `cm.ftg.tontine.president.delegation.*` (le pattern d'un workflow Président avec acceptation est similaire) et sur le module `vote.*` pour le `RealtimeEventPublisher`.
