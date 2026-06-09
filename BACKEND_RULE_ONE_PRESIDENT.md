# ⚖️ Règle métier — "Un Président par tontine"

> Note rapide pour le backend `tontine-back` : verrouiller la règle "le créateur de la tontine est l'unique Président".

## Règle

Le **créateur** d'une tontine (`Tontine.createdByUserId`) reçoit automatiquement `EnumSet.of(UserRole.PRESIDENT, UserRole.MEMBER)`. **Aucun autre membre ne peut être déclaré PRESIDENT** lors de la création ni via les invitations post-création. Pour transférer le rôle plus tard, il faut passer par le module `Delegations` (existant) ou par un vote d'assemblée.

## Implémentations à ajouter / vérifier

### 1. `TontineService.create(...)`

Dans `cm.ftg.tontine.tontine.service.TontineService`, avant la boucle sur `req.founders()` :

```java
boolean anyPresidentFounder = req.founders().stream()
        .anyMatch(f -> f.role() == UserRole.PRESIDENT);
if (anyPresidentFounder) {
    throw new ApiException("FOUNDER_PRESIDENT_FORBIDDEN",
            "Un fondateur ne peut pas être déclaré Président : le créateur est l'unique Président. " +
            "Pour transférer ce rôle, utilisez le module Délégations.",
            HttpStatus.UNPROCESSABLE_ENTITY);
}
```

Placement recommandé : juste après la validation `req.founders().size() ...` (s'il y en a une) ou en tête de la méthode `create()`.

### 2. `InvitationService.inviteMember(...)` (du brief `BACKEND_TASK_MEMBER_INVITATIONS.md`)

Même garde au début :

```java
if (req.proposedRole() == UserRole.PRESIDENT) {
    throw new ApiException("INVITATION_PRESIDENT_FORBIDDEN",
            "Le créateur de la tontine est l'unique Président. " +
            "Pour transférer ce rôle, utilisez le module Délégations.",
            HttpStatus.UNPROCESSABLE_ENTITY);
}
```

### 3. `InvitationAcceptService.accept(...)` (idem, defense-in-depth)

Lors de la création du `Member` à partir de l'invitation acceptée :

```java
UserRole role = invitation.getProposedRole();
if (role == UserRole.PRESIDENT) {
    // Ne devrait jamais arriver si les gardes précédentes sont en place.
    // Fail-fast plutôt que de produire silencieusement un second Président.
    throw new IllegalStateException("Invitation invalide : role PRESIDENT non assignable");
}
member.setRoles(EnumSet.of(role, UserRole.MEMBER));
```

### 4. Contrainte SQL (recommandé — bretelles + ceinture)

Ajoute un **index unique partiel** sur la table `members` :

```sql
-- PostgreSQL
CREATE UNIQUE INDEX uk_member_one_president_per_tontine
    ON members (tontine_id)
    WHERE 'PRESIDENT' = ANY (string_to_array(roles, ','));
```

> ⚠ Adapter à la représentation réelle de `Member.roles` dans ton schéma (CSV, table de jointure, JSON). Si c'est une jointure `member_roles(member_id, role)`, l'index unique partiel devient :
> ```sql
> CREATE UNIQUE INDEX uk_one_president_per_tontine
>     ON member_roles (tontine_id, role)
>     WHERE role = 'PRESIDENT';
> ```
> (nécessite que `tontine_id` soit dénormalisé dans `member_roles`, sinon vue matérialisée ou trigger).

L'index garantit qu'**aucun bug ne pourra produire deux Présidents** : la DB rejettera l'insertion.

## Tests à ajouter

| # | Scénario | Attendu |
|---|---|---|
| 1 | `POST /tontines` avec un founder `role=PRESIDENT` | 422 `FOUNDER_PRESIDENT_FORBIDDEN` |
| 2 | `POST /tontines` valide → créateur a `[PRESIDENT, MEMBER]`, aucun autre membre n'a PRESIDENT | DB assertion |
| 3 | `POST /president/membership/invite` avec `proposedRole=PRESIDENT` | 422 `INVITATION_PRESIDENT_FORBIDDEN` |
| 4 | Tentative directe d'`UPDATE members SET roles = roles + 'PRESIDENT'` pour un 2e membre via SQL brut | violation contrainte unique (si index partiel en place) |

## Comment transférer le rôle Président plus tard ?

Pas dans le périmètre de cette tâche, mais à anticiper :

- **Voie 1 — Délégation temporaire** : `POST /president/delegations` avec `powers=['PRESIDE_SESSION', ...]` et un `endsAt` (déjà implémenté).
- **Voie 2 — Vote d'assemblée** : `POST /president/votes` avec `scope=ASSEMBLY` et une motion type *« Désigner X comme nouveau Président »*. Le frontend est prêt côté membre (`/member/votes`). Le hook qui change effectivement le rôle au `close()` de ce vote reste à implémenter — voir `// TODO assembly side-effect dispatcher` dans `VoteService.close()`.

## Frontend (déjà aligné)

Côté `ndah-connect-web` :

- `InvitableFounderRole = Exclude<FounderRole, 'PRESIDENT'>` dans [`shared/models/entities/tontine.model.ts`](../ndah-connect-web/src/app/shared/models/entities/tontine.model.ts)
- Le `<select>` du wizard étape 4 n'expose pas l'option Président
- Bannière `tc-alert kind="success"` au-dessus du formulaire étape 4 : *« {createur} sera automatiquement Président »*
- Carte du créateur affichée en tête de la liste du bureau dans le récap (étape 5) avec badge `Président`

→ Donc l'envoi d'un payload contenant `role: 'PRESIDENT'` est impossible depuis l'UI Angular. Le backend doit néanmoins valider (defense-in-depth) car un client malveillant pourrait contourner via curl.
