# 🗳️ Brief Implémentation — Module "Vote Membre"

> Tâche à implémenter dans le backend Spring Boot **`tontine-back`** pour permettre aux membres de **voter** sur les motions créées par le Président. Aujourd'hui seul le Président peut **créer** et **clôturer** un vote — aucun mécanisme côté membre n'existe, donc les compteurs (`option.count`, `totalVoted`) ne peuvent jamais bouger.

- **Stack** : Spring Boot 3, Java 21, Spring Security, Spring Data JPA, Lombok
- **Base path** : `http://localhost:8081/api`
- **Module concerné** : nouveau package `cm.ftg.tontine.member.vote` (à créer)
- **Module existant à réutiliser** : `cm.ftg.tontine.president.vote.*` (entités `Vote`, `VoteOption` + `VoteRepository`, `VoteOptionRepository` déjà en place)

---

## 1. Contexte fonctionnel

Le **Président** crée déjà des votes via :

```
POST /president/votes            (existe)
GET  /president/votes            (existe)
GET  /president/votes/{id}       (existe)
POST /president/votes/{id}/close (existe)
```

Le frontend Angular consomme déjà 4 nouveaux endpoints **côté membre** (cf. `MemberVoteService` dans `ndah-connect-web` → `src/app/features/member/services/vote.service.ts`). Il faut donc **les implémenter dans le backend**.

---

## 2. Endpoints à créer

Tous sous le préfixe **`/members/me/votes`**, authentifiés (`@AuthenticationPrincipal AuthenticatedUser user`), avec résolution de `tontineId` via le header `X-Tontine-Id` (pattern `TontineIdResolver` déjà utilisé dans `VoteController`).

| Méthode  | Path                                       | Description                                            |
| -------- | ------------------------------------------ | ------------------------------------------------------ |
| `GET`    | `/members/me/votes`                        | Liste des votes auxquels le membre est éligible        |
| `GET`    | `/members/me/votes/{id}`                   | Détail d'un vote                                        |
| `POST`   | `/members/me/votes/{id}/cast`              | Soumettre son choix                                     |
| `GET`    | `/members/me/votes/{id}/ballot-status`     | A-t-il déjà voté ?                                      |

### 2.1 `GET /members/me/votes?status=OPEN`

- Query optionnelle : `status` (`DRAFT|OPEN|CLOSED|CANCELLED`)
- Retourne uniquement les votes pour lesquels le membre est **éligible** (cf. §3.1)
- `Vote.status = DRAFT` est **toujours masqué** au membre, même si `status=DRAFT` est explicitement demandé

**Response** :
```json
{
  "data": [VoteDto, ...],
  "timestamp": "..."
}
```
On peut **réutiliser** `VoteDto` existant + ajouter le champ calculé `hasVoted` (cf. §4).

### 2.2 `GET /members/me/votes/{id}`

- 404 si vote inexistant
- 403 si tontine ne match pas
- 403 si membre **non éligible** (audience exclut son rôle)

### 2.3 `POST /members/me/votes/{id}/cast`

**Request body** :
```json
{ "optionId": "uuid" }
```

**Response** :
```json
{
  "data": {
    "voteId": "uuid",
    "optionId": "uuid",
    "optionLabel": "Alice",
    "castAt": "2026-06-07T10:30:00Z"
  },
  "message": "Vote enregistré",
  "timestamp": "..."
}
```

### 2.4 `GET /members/me/votes/{id}/ballot-status`

**Response** :
```json
{ "data": { "hasVoted": true }, "timestamp": "..." }
```

---

## 3. Règles métier

### 3.1 Éligibilité (audience)

Calculer si le membre peut voir/voter un `Vote` selon le champ `Vote.audience` :

| `audience`         | Critère d'éligibilité (à appliquer côté backend)                                                  |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| `ALL`              | Tout membre rattaché à la tontine, peu importe son statut                                          |
| `BUREAU`           | Membre dont **au moins un rôle** ∈ `{PRESIDENT, SECRETARY, TREASURER, CENSOR, AUDITOR}`            |
| `MEMBERS_ACTIVE`   | Membre dont `Member.status = ACTIVE` (table `members` ou équivalent)                              |

→ Crée un service `VoterEligibilityChecker` (dans `cm.ftg.tontine.member.vote.service`) avec une méthode `isEligible(UUID userId, UUID tontineId, Vote vote) : boolean`.

Si non éligible : **403 FORBIDDEN** avec code `VOTE_NOT_ELIGIBLE`, message `"Vous n'êtes pas éligible à participer à ce vote"`.

### 3.2 Unicité du vote

- Un `(memberId, voteId)` ne peut exister **qu'une seule fois**.
- Contrainte d'unicité SQL : `UNIQUE (vote_id, member_id_hash)` (cf. §5).
- Si tentative de re-cast : **409 CONFLICT** avec code `VOTE_ALREADY_CAST`, message `"Vous avez déjà voté pour cette motion"`.

### 3.3 Fenêtre temporelle

Avant d'accepter un `cast()` :
- `Vote.status` doit être `OPEN` → sinon **422** code `VOTE_NOT_OPEN`
- `now() ∈ [opensAt, closesAt]` → sinon **422** code `VOTE_OUTSIDE_WINDOW`
- Au cas où `status=DRAFT` mais `opensAt <= now()`, **basculer automatiquement** en `OPEN` (déjà fait au create — à reproduire dans un job planifié, cf. §6)

### 3.4 Anonymat (`Vote.isAnonymous = true`)

- **Ne pas stocker `memberId` en clair** dans la table `vote_ballots` lorsque `isAnonymous=true`.
- À la place, stocker un **hash déterministe** : `SHA-256(voteId + memberId + serverSecret)`.
- Le hash permet :
  - de bloquer le re-vote (déduplication)
  - mais empêche tout audit ultérieur de qui a voté quoi
- `serverSecret` : injecté via `@Value("${app.vote.anonymous-secret}")` dans `application.yml`. **Doit être une valeur stable** (sinon les anciens hash deviendront invalides) — recommandation : générer un UUID au premier déploiement et le placer dans la config serveur (jamais dans Git).
- Pour les votes **non-anonymes** (`isAnonymous=false`), stocker `memberId` directement → permet à un censeur ou audit d'avoir la liste nominative.

### 3.5 Compteurs

À chaque `cast()` réussi, dans la **même transaction** :
1. `INSERT INTO vote_ballots (...)` (avec `member_id` ou `member_hash` selon §3.4)
2. `UPDATE vote_options SET count = count + 1 WHERE id = :optionId AND vote_id = :voteId` (vérifier `rowsAffected = 1`)
3. `UPDATE votes SET total_voted = total_voted + 1 WHERE id = :voteId`
4. Publier un évènement temps réel : `realtime.toVote(tontineId, voteId, "vote.ballot-cast", {voteId, totalVoted, optionCounts})`
5. Auditer : `auditService.record(userId, "VOTE_CAST", "Vote", voteId.toString(), tontineId, "{\"optionId\":\"" + optionId + "\"}")`

### 3.6 Calcul de `totalVoters`

Le code actuel de `VoteService.create()` initialise `totalVoters=0` ⚠️ **bug à corriger** : il doit refléter le nombre de **membres éligibles** à `opensAt`.

- À la création (ou au passage `DRAFT → OPEN`), calculer `totalVoters = COUNT(members éligibles selon audience)`.
- C'est cette valeur qui sert au calcul du **quorum** (`totalVoted / totalVoters >= quorumPercent`).

### 3.7 Calcul de `passed` à la clôture

Le code actuel de `VoteService.computePassed()` ne tient compte **que de la majorité simple sur la première option**, et n'utilise **pas** `quorumPercent` ⚠️ **bug à corriger** :

```java
private Boolean computePassed(Vote v, List<VoteOption> options) {
    if (options == null || options.isEmpty()) return Boolean.FALSE;

    // Quorum
    if (v.getTotalVoters() == 0) return Boolean.FALSE;
    BigDecimal participation = BigDecimal.valueOf(v.getTotalVoted())
        .divide(BigDecimal.valueOf(v.getTotalVoters()), 4, RoundingMode.HALF_UP);
    if (participation.compareTo(v.getQuorumPercent()) < 0) return Boolean.FALSE;

    // Majorité (option la plus votée doit être strictement supérieure aux autres)
    VoteOption top = options.stream().max(Comparator.comparingLong(VoteOption::getCount)).orElseThrow();
    long others = options.stream().filter(o -> !o.getId().equals(top.getId())).mapToLong(VoteOption::getCount).max().orElse(0);
    return top.getCount() > others ? Boolean.TRUE : Boolean.FALSE;
}
```

---

## 4. DTOs à créer

### 4.1 `MemberVoteDto` (extension de `VoteDto`)

Pour éviter de polluer `VoteDto` (utilisé par le Président qui n'a pas la notion de "j'ai voté"), créer un nouveau record côté membre :

```java
package cm.ftg.tontine.member.vote.dto;

public record MemberVoteDto(
        UUID id,
        UUID tontineId,
        String question,
        String description,
        List<VoteOptionDto> options,
        boolean isAnonymous,
        boolean hideResultsUntilClose,
        VoteScope scope,
        VoteAudience audience,
        VoteStatus status,
        Instant opensAt,
        Instant closesAt,
        Instant createdAt,
        int totalVoters,
        int totalVoted,
        BigDecimal quorumPercent,
        Boolean passed,
        boolean hasVoted  // ← spécifique au membre
) { }
```

> Si `hideResultsUntilClose = true` ET `status != CLOSED`, **masquer les compteurs** : retourner `option.count = 0` pour toutes les options et `totalVoted = 0`. Le membre ne doit pas pouvoir déduire les tendances en faisant la différence entre deux appels.

### 4.2 `CastVoteRequest`

```java
package cm.ftg.tontine.member.vote.dto;

public record CastVoteRequest(
        @NotNull UUID optionId
) { }
```

### 4.3 `VoteBallotDto`

```java
package cm.ftg.tontine.member.vote.dto;

public record VoteBallotDto(
        UUID voteId,
        UUID optionId,
        String optionLabel,
        Instant castAt
) { }
```

---

## 5. Entité JPA `VoteBallot`

```java
package cm.ftg.tontine.member.vote.entity;

@Entity
@Table(
    name = "vote_ballots",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vote_ballot_member",
        columnNames = {"vote_id", "voter_key"}
    ),
    indexes = {
        @Index(name = "idx_vote_ballot_vote", columnList = "vote_id"),
        @Index(name = "idx_vote_ballot_option", columnList = "option_id")
    }
)
@Getter @Setter @NoArgsConstructor
public class VoteBallot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vote_id", nullable = false)
    private UUID voteId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    /**
     * Clé d'identification du votant :
     *  - non-anonyme : memberId.toString()
     *  - anonyme : SHA-256(voteId + memberId + serverSecret) en hex
     * La contrainte UNIQUE (vote_id, voter_key) garantit l'unicité du vote.
     */
    @Column(name = "voter_key", nullable = false, length = 80)
    private String voterKey;

    /** Renseigné uniquement si Vote.isAnonymous = false, sinon NULL. */
    @Column(name = "member_id")
    private UUID memberId;

    @CreationTimestamp
    @Column(name = "cast_at", updatable = false, nullable = false)
    private Instant castAt;
}
```

Migration Flyway (ou Liquibase selon ce que tu utilises) : `V{n}__create_vote_ballots.sql`.

---

## 6. Job planifié (optionnel mais recommandé)

Tu as déjà un package `cm.ftg.tontine.scheduling` — ajoute-y une tâche :

```java
@Scheduled(fixedDelay = 60_000)
public void transitionVotes() {
    Instant now = Instant.now();
    voteRepository.findByStatusAndOpensAtBefore(VoteStatus.DRAFT, now)
        .forEach(v -> {
            v.setStatus(VoteStatus.OPEN);
            // recalculer totalVoters au passage
            v.setTotalVoters(voterEligibilityChecker.countEligible(v));
            voteRepository.save(v);
            realtime.toVote(v.getTontineId(), v.getId(), "vote.opened", VoteDto.from(v, ...));
        });

    voteRepository.findByStatusAndClosesAtBefore(VoteStatus.OPEN, now)
        .forEach(v -> voteService.close(v.getId(), v.getTontineId(), SYSTEM_USER_ID));
}
```

---

## 7. Codes d'erreur attendus

Tous via la classe `ApiException` existante (conforme à l'enveloppe `ApiError` du frontend) :

| HTTP | Code                  | Message FR                                                        |
| ---- | --------------------- | ----------------------------------------------------------------- |
| 403  | `VOTE_NOT_ELIGIBLE`   | Vous n'êtes pas éligible à participer à ce vote                  |
| 403  | `FORBIDDEN`           | Tontine non concordante                                           |
| 404  | `VOTE_NOT_FOUND`      | Vote introuvable                                                  |
| 404  | `OPTION_NOT_FOUND`    | Option introuvable                                                |
| 409  | `VOTE_ALREADY_CAST`   | Vous avez déjà voté pour cette motion                            |
| 422  | `VOTE_NOT_OPEN`       | Ce vote n'est pas ouvert aux votants                              |
| 422  | `VOTE_OUTSIDE_WINDOW` | La période de vote n'est pas active                               |
| 422  | `VOTE_OPTION_MISMATCH`| L'option ne correspond pas à ce vote                              |

---

## 8. Structure de package proposée

```
cm.ftg.tontine.member.vote/
├── controller/
│   └── MemberVoteController.java
├── dto/
│   ├── MemberVoteDto.java
│   ├── CastVoteRequest.java
│   └── VoteBallotDto.java
├── entity/
│   └── VoteBallot.java
├── repository/
│   └── VoteBallotRepository.java
└── service/
    ├── MemberVoteService.java
    └── VoterEligibilityChecker.java
```

Le `MemberVoteController` doit suivre le **même pattern** que `MemberController` existant (`@RequestMapping("/members/me/votes")`).

---

## 9. Exemple de squelette `MemberVoteController`

```java
package cm.ftg.tontine.member.vote.controller;

@RestController
@RequestMapping("/members/me/votes")
public class MemberVoteController {

    private final MemberVoteService service;
    private final TontineIdResolver tontineIdResolver;

    public MemberVoteController(MemberVoteService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<MemberVoteDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) VoteStatus status,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listForMember(t, user.id(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberVoteDto> getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.findForMember(id, t, user.id()));
    }

    @PostMapping("/{id}/cast")
    public ApiResponse<VoteBallotDto> cast(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CastVoteRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.cast(id, t, user.id(), req.optionId()), "Vote enregistré");
    }

    @GetMapping("/{id}/ballot-status")
    public ApiResponse<Map<String, Boolean>> ballotStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(Map.of("hasVoted", service.hasVoted(id, t, user.id())));
    }
}
```

---

## 10. Tests d'intégration attendus (`@SpringBootTest` + `MockMvc`)

Classe : `MemberVoteControllerIT`

| # | Scénario                                                                                | Attendu                           |
| - | --------------------------------------------------------------------------------------- | --------------------------------- |
| 1 | Liste vide quand aucun vote                                                              | 200, `data: []`                   |
| 2 | Liste filtrée par audience : un vote `BUREAU` n'apparaît pas pour un MEMBER simple      | 200, vote absent                  |
| 3 | `cast` succès : insère ballot, incrémente `option.count` et `totalVoted`                | 200, ballot retourné              |
| 4 | `cast` deux fois → 2e refusé                                                             | 409 `VOTE_ALREADY_CAST`           |
| 5 | `cast` sur vote `CLOSED`                                                                 | 422 `VOTE_NOT_OPEN`               |
| 6 | `cast` avant `opensAt`                                                                   | 422 `VOTE_OUTSIDE_WINDOW`         |
| 7 | `cast` avec `optionId` d'un autre vote                                                   | 422 `VOTE_OPTION_MISMATCH`        |
| 8 | `cast` non-éligible (mauvaise audience)                                                  | 403 `VOTE_NOT_ELIGIBLE`           |
| 9 | Vote anonyme : `vote_ballots.member_id` est NULL, `voter_key` est un hash               | DB assertion                       |
| 10 | `hideResultsUntilClose=true` + status `OPEN` → `option.count` masqués (0) côté GET     | 200, tous compteurs à 0           |
| 11 | `hideResultsUntilClose=true` + status `CLOSED` → vrais compteurs                       | 200, compteurs réels              |
| 12 | Quorum non atteint à la clôture → `passed=false`                                        | DB assertion après `/close`       |

---

## 11. Critères d'acceptation

- [ ] Migration SQL créée et versionnée (table `vote_ballots`)
- [ ] 4 endpoints `/members/me/votes/...` répondent avec l'enveloppe `ApiResponse<T>`
- [ ] Documentation Swagger générée automatiquement (`springdoc-openapi`)
- [ ] Test d'intégration `MemberVoteControllerIT` (12 scénarios)
- [ ] Bug fixés au passage : `totalVoters` initialisé correctement, `computePassed()` intègre le quorum
- [ ] Évènements temps réel publiés via `RealtimeEventPublisher.toVote(...)` (`vote.opened`, `vote.ballot-cast`, `vote.closed`)
- [ ] Audit trail : chaque `cast()` produit une entrée `AuditService.record(..., "VOTE_CAST", ...)`
- [ ] Job planifié `transitionVotes()` actif (toutes les 60s)
- [ ] Secret d'anonymat dans `application.yml` (avec valeur par défaut en dev) + variable d'env en prod
- [ ] Le backend compile sans warning : `./mvnw clean verify`

---

## 12. Vérification manuelle (curl)

Une fois implémenté, ces commandes doivent fonctionner (remplacer `$TOKEN` par un JWT membre valide) :

```bash
# Liste mes votes
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/members/me/votes

# Détail
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/members/me/votes/<voteId>

# Vérifier si j'ai voté
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/api/members/me/votes/<voteId>/ballot-status

# Voter
curl -X POST \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"optionId":"<optionUuid>"}' \
     http://localhost:8081/api/members/me/votes/<voteId>/cast

# Re-voter → doit renvoyer 409
curl -X POST \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"optionId":"<optionUuid>"}' \
     http://localhost:8081/api/members/me/votes/<voteId>/cast
```

---

## 13. Côté frontend (déjà fait — pour référence)

Le frontend Angular `ndah-connect-web` a déjà :

- **Service** : `src/app/features/member/services/vote.service.ts` (`MemberVoteService`) avec `list()`, `listOpen()`, `getDetail()`, `cast(voteId, optionId)`, `hasVoted(voteId)`
- **Page** : `src/app/features/member/pages/votes/my-votes.component.ts` accessible via la route `/member/votes`
- **Tests unitaires** : `vote.service.spec.ts` (5 tests passants)

Donc dès que les 4 endpoints backend sont en place, l'écran membre fonctionne sans rien changer côté front.

---

## 14. Effet d'aboutissement (hors-scope mais à anticiper)

Quand un vote `scope=ASSEMBLY` se clôture avec `passed=true`, il peut avoir un **effet métier réel** (ex: élection d'un président, exclusion d'un membre, modification des règles). Ce mécanisme **n'est pas demandé maintenant** — laisse un `// TODO assembly side-effect dispatcher` dans `VoteService.close()` pour qu'on l'implémente plus tard via un événement `vote.assembly.passed` + handlers dédiés.

---

**Référence projet** : tous les patterns à suivre (controller, service, exception, audit, realtime) sont déjà appliqués dans `cm.ftg.tontine.president.vote.*`. Aligne-toi sur ce module existant pour la cohérence.
