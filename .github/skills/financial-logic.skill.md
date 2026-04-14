---
description: "Procédure de création d'un service financier dans l'application Tontine. Couvre la validation de solvabilité, l'isolation transactionnelle, la délégation asynchrone des notifications via Virtual Threads, et la manipulation stricte des montants en BigDecimal."
---

# Skill : Créer un service financier

Ce skill décrit la procédure **obligatoire** à suivre pour créer tout nouveau service qui manipule de la logique financière dans l'application Tontine. Il s'applique aux cotisations, distributions de pot, amendes, mouvements du fonds social, et toute opération impliquant un transfert ou un calcul de montant.

---

## Étape 1 — Pré-validation : solvabilité et existence du membre

Avant toute opération financière, le service **doit** exécuter deux vérifications préalables, dans cet ordre :

### 1.1 Vérifier l'existence du membre

Confirmer que le membre existe, est actif et appartient à la tontine concernée. Lever une exception métier explicite en cas d'échec.

```java
var membre = membreRepository.findByIdAndTontineId(membreId, tontineId)
    .orElseThrow(() -> new MembreIntrouvableException(membreId, tontineId));

if (membre.getStatut() != StatutMembre.ACTIF) {
    throw new MembreInactifException(membreId);
}
```

### 1.2 Vérifier la solvabilité

La solvabilité dépend du contexte de l'opération :

| Opération | Contrôle de solvabilité |
|---|---|
| **Cotisation** | Le membre n'a pas de cotisation déjà payée pour cette séance (`unicité membreId + seanceId`) |
| **Distribution du pot** | Toutes les cotisations de la séance sont collectées ; le bénéficiaire n'a pas d'amendes impayées |
| **Aide fonds social** | Le solde du fonds social couvre le montant de l'aide (`solde - montant >= 0`) |
| **Amende** | Le retard est confirmé (date du jour > date limite séance) et aucune amende n'existe déjà pour ce couple `(membreId, seanceId)` |

```java
private void verifierSolvabilite(Membre membre, Seance seance) {
    boolean cotisationExiste = cotisationRepository
        .existsByMembreIdAndSeanceId(membre.getId(), seance.getId());
    if (cotisationExiste) {
        throw new DoubleCotisationException(membre.getId(), seance.getId());
    }

    boolean amendesImpayees = amendeRepository
        .existsByMembreIdAndStatut(membre.getId(), StatutAmende.IMPAYEE);
    if (amendesImpayees && seance.estTourDe(membre)) {
        throw new AmendesImpayeesException(membre.getId());
    }
}
```

> **Règle absolue** : ne jamais exécuter de logique d'écriture (persist, update) avant que ces deux validations ne soient passées avec succès.

---

## Étape 2 — Sécurité transactionnelle : isolation SERIALIZABLE

Toute méthode de service qui effectue un calcul financier suivi d'une écriture **doit** utiliser le niveau d'isolation `SERIALIZABLE` pour prévenir les lectures fantômes et les conditions de course (double cotisation, double distribution).

### 2.1 Annotation du service

```java
@Service
public class CotisationService {

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public Cotisation enregistrer(CotisationRequest request) {
        // 1. Pré-validation (étape 1)
        var membre = verifierExistence(request.membreId(), request.tontineId());
        var seance = verifierSeanceOuverte(request.seanceId());
        verifierSolvabilite(membre, seance);

        // 2. Logique métier
        var cotisation = Cotisation.creer(membre, seance, request.montant());

        // 3. Persistance
        return cotisationRepository.save(cotisation);
    }
}
```

### 2.2 Règles complémentaires

- **Jamais de `@Transactional` au niveau de la classe** — l'annoter uniquement sur les méthodes qui le requièrent, pour ne pas imposer SERIALIZABLE aux lectures simples.
- Les méthodes en lecture seule utilisent `@Transactional(readOnly = true)`.
- En cas de `CannotAcquireLockException` (contention SERIALIZABLE), laisser Spring retenter via `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100))` sur la couche service. Ajouter `spring-retry` en dépendance.
- Toute exception non rattrapée provoque un **rollback complet** (`rollbackFor = Exception.class`).

### 2.3 Protection contre le double-traitement

Chaque opération financière doit inclure une **clé d'idempotence** :

```java
@Column(unique = true)
private String cleIdempotence; // ex: "COT-{membreId}-{seanceId}" ou UUID fourni par le client
```

---

## Étape 3 — Performance : notifications asynchrones via Virtual Threads

Les notifications post-transaction (SMS, e-mail, push) ne doivent **jamais** bloquer la transaction financière. Elles sont déléguées à des Virtual Threads après le commit.

### 3.1 Configuration de l'executor

Déclarer un `ExecutorService` dédié dans la configuration :

```java
@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### 3.2 Délégation post-commit

Utiliser `TransactionSynchronizationManager` pour déclencher la notification **uniquement après le commit réussi** de la transaction :

```java
@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
public Cotisation enregistrer(CotisationRequest request) {
    // ... validation + persistance ...
    var cotisation = cotisationRepository.save(nouvelleCotisation);

    TransactionSynchronization.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            notificationExecutor.submit(() ->
                notificationService.notifierCotisationRecue(cotisation)
            );
        }
    });

    return cotisation;
}
```

### 3.3 Règles de notification

- Ne jamais passer l'entité JPA au thread de notification (la session Hibernate est fermée). Passer un **DTO ou record** immuable.
- Les échecs de notification sont loggués mais ne provoquent **jamais** de rollback de la transaction financière.
- Pour les envois en lot (ex. rappel de cotisation à tous les membres), utiliser le pattern Structured Concurrency de Java 21 :

```java
public void envoyerRappelsSeance(Seance seance, List<Membre> retardataires) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        retardataires.forEach(membre ->
            scope.fork(() -> {
                notificationService.envoyerRappel(membre, seance);
                return null;
            })
        );
        scope.join();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Envoi des rappels interrompu pour la séance {}", seance.getId());
    }
}
```

---

## Étape 4 — Format des montants : BigDecimal exclusif

### 4.1 Règle non négociable

Tout montant financier dans l'application est de type `BigDecimal`. L'utilisation de `double`, `float` ou `long` (en centimes) est **interdite**.

### 4.2 Création

```java
// ✅ Correct
BigDecimal montant = new BigDecimal("5000");
BigDecimal taux = new BigDecimal("0.05");

// ❌ Interdit — perte de précision
BigDecimal montant = BigDecimal.valueOf(5000.50);
BigDecimal montant = new BigDecimal(5000.50);
```

### 4.3 Opérations arithmétiques

Toujours spécifier le `MathContext` ou la `scale` + `RoundingMode` :

```java
BigDecimal prelevement = pot.multiply(tauxFondsSocial)
    .setScale(0, RoundingMode.HALF_UP);

BigDecimal montantParMembre = total.divide(
    new BigDecimal(nombreMembres), 0, RoundingMode.HALF_UP
);
```

### 4.4 Comparaison

Ne **jamais** utiliser `equals()` pour comparer des `BigDecimal` (sensible à la scale). Utiliser `compareTo()` :

```java
// ✅ Correct
if (solde.compareTo(BigDecimal.ZERO) < 0) {
    throw new SoldeInsuffisantException();
}

// ❌ Faux — new BigDecimal("0").equals(new BigDecimal("0.00")) retourne false
if (solde.equals(BigDecimal.ZERO)) { ... }
```

### 4.5 Persistance JPA

```java
@Column(precision = 15, scale = 2, nullable = false)
private BigDecimal montant;
```

### 4.6 Validation d'entrée

```java
public record CotisationRequest(
    @NotNull Long membreId,
    @NotNull Long seanceId,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal montant
) {}
```

---

## Checklist de création d'un service financier

Avant de soumettre un nouveau service financier, vérifier chaque point :

- [ ] Le membre est vérifié : existence, statut actif, appartenance à la tontine
- [ ] La solvabilité est contrôlée selon le type d'opération
- [ ] La méthode porte `@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)`
- [ ] Une clé d'idempotence empêche le double-traitement
- [ ] `@Retryable` est configuré pour gérer la contention SERIALIZABLE
- [ ] Les notifications sont déléguées en post-commit sur Virtual Threads
- [ ] Les notifications reçoivent un DTO, pas une entité JPA
- [ ] Tous les montants sont en `BigDecimal` avec `scale` et `RoundingMode` explicites
- [ ] Les comparaisons de montants utilisent `compareTo()`, jamais `equals()`
- [ ] Les colonnes JPA financières déclarent `precision` et `scale`
- [ ] L'opération est journalisée dans la table d'audit
- [ ] Les tests unitaires couvrent : cas nominal, montant zéro, montant négatif, double-traitement, solvabilité insuffisante
