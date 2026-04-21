---
description: "Procédure de revue de code pour l'application Tontine. Couvre l'audit Java 21 (Virtual Threads / pinning), la sécurité (entités JPA jamais exposées, Records en controllers), la logique métier financière (BigDecimal, rôles Censeur/Trésorier) et le Clean Code (nommage, lisibilité, commentaires). La réponse est obligatoirement formatée en tableau."
---

# Skill : Revue de Code — Tontine

Ce skill définit la **procédure obligatoire** de revue de code (code review) pour l'application Tontine (Spring Boot 4.0.5 / Java 21). Il s'applique à toute demande de type : *"Fais une code review de ce fichier"*, *"Audite ce service"*, *"Vérifie ce controller"*.

> **Sources** : `copilot-instructions.md` (§1 Sécurité, §2 Virtual Threads, §3 Tests), `Cahier_des_Charges.md` (§3 Rôles, §4 Spécifications fonctionnelles), `security-rules.skill.md`.

---

## Format de réponse obligatoire

Toute revue de code doit être rendue sous la forme d'un tableau Markdown. **Aucune autre structure n'est acceptée.**

```
| Fichier | Ligne | Sévérité | Suggestion de correction |
|---------|-------|----------|--------------------------|
| `NomDuFichier.java` | 42 | 🔴 Haute | Description précise de la correction à apporter |
| `AutreFichier.java` | 17 | 🟡 Basse | Description de l'amélioration recommandée |
```

### Légende des sévérités

| Niveau | Icône | Critère |
|--------|-------|---------|
| **Haute** | 🔴 | Erreur bloquante : sécurité compromise, comportement incorrect, risque financier, violation d'une règle obligatoire du projet |
| **Basse** | 🟡 | Amélioration non bloquante : lisibilité, nommage, commentaire manquant, déviation stylistique |

> Si aucun problème n'est détecté dans une catégorie, l'indiquer explicitement dans le tableau avec `✅ Aucun problème détecté`.

---

## Étape 1 — Audit Java 21 : Virtual Threads

### 1.1 Règles à vérifier

Analyser chaque fichier à la recherche des patterns suivants qui **pinent** le carrier thread et annulent le bénéfice des Virtual Threads :

| Pattern interdit | Sévérité | Raison |
|------------------|----------|--------|
| `Thread.sleep(...)` dans un service ou controller | 🔴 Haute | Bloque le Virtual Thread et pin le carrier |
| Bloc `synchronized(...)` ou méthode `synchronized` sur un objet partagé de longue durée | 🔴 Haute | Pin garanti du carrier thread — remplacer par `ReentrantLock` |
| `synchronized` sur `this` dans un singleton Spring | 🔴 Haute | Le bean Spring est un singleton, le synchronized pin le carrier sur toute la durée du lock |
| `ThreadLocal` utilisé pour stocker de l'état entre requêtes | 🔴 Haute | Les Virtual Threads rendent les `ThreadLocal` coûteux en mémoire |
| `Executors.newFixedThreadPool(...)` pour des tâches I/O | 🟡 Basse | Annule l'avantage des Virtual Threads — utiliser `Executors.newVirtualThreadPerTaskExecutor()` |
| Opération CPU-intensive (calcul lourd) non déléguée à un pool dédié | 🟡 Basse | Les opérations longues CPU doivent être isolées des Virtual Threads |

### 1.2 Pattern correct pour les sections critiques

```java
// ❌ Interdit
public synchronized void enregistrerCotisation(ContributionRequest request) { ... }

// ✅ Correct
private final ReentrantLock lock = new ReentrantLock();

public void enregistrerCotisation(ContributionRequest request) {
    lock.lock();
    try { ... }
    finally { lock.unlock(); }
}
```

### 1.3 Pattern correct pour les tâches parallèles

```java
// ❌ Interdit
ExecutorService pool = Executors.newFixedThreadPool(10);

// ✅ Correct
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
```

---

## Étape 2 — Audit Sécurité

### 2.1 Entités JPA jamais exposées dans les controllers

**Règle absolue** (§6.3 Cahier des Charges + `security-rules.skill.md`) : aucune entité JPA (`@Entity`) ne doit apparaître dans la signature d'une méthode de controller (paramètre `@RequestBody`, type de retour, `@ResponseBody`).

| Violation | Sévérité | Correction |
|-----------|----------|------------|
| Entité JPA en `@RequestBody` | 🔴 Haute | Créer un `record` Request dédié avec validation Jakarta (`@NotNull`, `@Positive`) |
| Entité JPA en type de retour `ResponseEntity<MonEntite>` | 🔴 Haute | Créer un `record` Response dédié — utiliser `@JsonIgnore` sur les champs sensibles |
| Entité exposée via un `List<MonEntite>` dans la réponse | 🔴 Haute | Mapper vers `List<MonEntiteResponse>` via un Mapper dédié |

**Patterns corrects à valoriser :**

```java
// ❌ Interdit
@PostMapping
public ResponseEntity<Membre> creer(@RequestBody Membre membre) { ... }

// ✅ Correct
@PostMapping
public ResponseEntity<MembreResponse> creer(@Valid @RequestBody CreateMembreRequest request) { ... }
```

### 2.2 Autres contrôles sécurité

| Point de contrôle | Sévérité | Détail |
|-------------------|----------|--------|
| Endpoint sans `@PreAuthorize` sur une opération sensible | 🔴 Haute | Tout endpoint financier ou de gestion doit déclarer un rôle explicite |
| Données financières (montants, soldes) dans les logs | 🔴 Haute | Masquer : `log.debug("amount=***")` — jamais la valeur réelle |
| Mot de passe ou token dans les logs ou messages d'erreur | 🔴 Haute | Supprimer immédiatement |
| `double` ou `float` pour un montant | 🔴 Haute | Remplacer par `BigDecimal` (voir Étape 3) |
| Requête JPQL construite par concaténation de chaînes | 🔴 Haute | Injection SQL — utiliser exclusivement des paramètres nommés `:param` |
| Exception propagée avec `ex.getMessage()` dans une réponse HTTP publique | 🟡 Basse | Risque de fuite d'informations internes — utiliser un message générique |
| Champ sensible sans `@JsonIgnore` dans une entité sérialisée | 🔴 Haute | Ajouter `@JsonIgnore` ou utiliser un DTO de projection |

### 2.3 Vérification des Records de request/response

Chaque `record` Request doit déclarer des contraintes Jakarta Validation :

```java
// ✅ Correct
public record CreateCotisationRequest(
    @NotNull Long membreId,
    @NotNull Long seanceId,
    @Positive @DecimalMin("0.01") BigDecimal montant
) {}
```

---

## Étape 3 — Audit Métier

### 3.1 Calculs financiers : BigDecimal obligatoire

**Source** : `copilot-instructions.md` §1 (Validation & Intégrité).

| Violation | Sévérité | Correction |
|-----------|----------|------------|
| `double` ou `float` pour un montant | 🔴 Haute | Remplacer par `BigDecimal` |
| `BigDecimal` créé avec `new BigDecimal(0.1)` (constructeur `double`) | 🔴 Haute | Utiliser `BigDecimal.valueOf(0.1)` ou `new BigDecimal("0.1")` |
| Comparaison `montant.equals(autreMotant)` | 🔴 Haute | Utiliser `montant.compareTo(autreMontant) == 0` (la scale peut différer) |
| Arrondi non explicite sur un résultat de division | 🟡 Basse | Spécifier `RoundingMode.HALF_UP` et la précision souhaitée |
| Opération financière sans gestion de l'idempotence | 🔴 Haute | Protéger contre le double-traitement (verrou optimiste `@Version` ou clé d'idempotence) |

### 3.2 Respect des rôles métier (Cahier des Charges §3)

Vérifier que les opérations sont contraintes par les bons rôles conformément à la matrice des permissions :

| Opération | Rôle(s) autorisé(s) | Annotation attendue |
|-----------|---------------------|---------------------|
| Enregistrer une cotisation | `TRESORIER`, `PRESIDENT`, `ADMIN` | `@PreAuthorize("hasAnyRole('TRESORIER','PRESIDENT','ADMIN')")` |
| Appliquer une sanction | `CENSEUR`, `ADMIN` | `@PreAuthorize("hasAnyRole('CENSEUR','ADMIN')")` |
| Distribuer la cagnotte | `TRESORIER`, `PRESIDENT`, `ADMIN` | `@PreAuthorize("hasAnyRole('TRESORIER','PRESIDENT','ADMIN')")` |
| Planifier une séance | `SECRETAIRE`, `PRESIDENT`, `VICE_PRESIDENT`, `ADMIN` | `@PreAuthorize("hasAnyRole('SECRETAIRE','PRESIDENT','VICE_PRESIDENT','ADMIN')")` |
| Créer une tontine | `PRESIDENT`, `ADMIN` | `@PreAuthorize("hasAnyRole('PRESIDENT','ADMIN')")` |
| Auditer les comptes | `COMMISSAIRE`, `PRESIDENT`, `ADMIN` | `@PreAuthorize("hasAnyRole('COMMISSAIRE','PRESIDENT','ADMIN')")` |
| Consulter son propre historique | Tout rôle authentifié | `@PreAuthorize("isAuthenticated()")` + vérification `membreId == principal.id` |

**Violations à signaler :**

| Violation | Sévérité |
|-----------|----------|
| Opération de distribution sans vérification du rôle `TRESORIER` | 🔴 Haute |
| Application d'une sanction sans vérification du rôle `CENSEUR` | 🔴 Haute |
| Membre accédant aux transactions d'un autre membre | 🔴 Haute |
| Opération financière sans entrée dans la table d'audit | 🔴 Haute |
| Suppression physique d'une transaction (pas de soft delete) | 🔴 Haute |

### 3.3 Cohérence des règles de tontine

| Point de contrôle | Sévérité | Détail |
|-------------------|----------|--------|
| Cotisation enregistrée pour une séance déjà clôturée | 🔴 Haute | Vérifier le statut `OUVERTE` avant tout enregistrement |
| Double cotisation pour un même membre sur une même séance | 🔴 Haute | Lever `DoubleCotisationException` — vérification en base avant insertion |
| Sanction appliquée deux fois sur la même infraction | 🔴 Haute | Lever `DoubleSanctionException` |
| Calcul de part utilisant la division entière Java | 🔴 Haute | Utiliser `BigDecimal.divide(diviseur, 2, RoundingMode.HALF_UP)` |

---

## Étape 4 — Clean Code

### 4.1 Nommage (standards du projet)

| Règle | Sévérité | Exemple |
|-------|----------|---------|
| Nom de méthode en **français** pour la logique métier dans les services | 🟡 Basse | `enregistrerCotisation()`, `planifierSession()`, `appliquerSanction()` |
| Nom de champ/variable en **anglais** pour les classes techniques (config, mapper, DTO) | 🟡 Basse | `contributionMapper`, `sessionService`, `errorResponse` |
| Abréviation cryptique (`tmp`, `obj`, `x`, `d`) dans un service métier | 🟡 Basse | Renommer avec un nom explicite : `cotisationDuJour`, `membreTrouve` |
| Classe de test sans `@DisplayName` sur la classe et les méthodes | 🟡 Basse | Ajouter `@DisplayName("Description lisible en français")` |
| Méthode de test ne respectant pas `should_[résultat]_when_[condition]` | 🟡 Basse | Renommer selon la convention du projet |

### 4.2 Lisibilité et commentaires

| Point de contrôle | Sévérité | Détail |
|-------------------|----------|--------|
| Logique de calcul financier complexe (répartition de cagnotte, calcul de pénalités) sans commentaire explicatif | 🟡 Basse | Ajouter un commentaire décrivant la formule ou la règle métier appliquée |
| Méthode dépassant 30 lignes sans décomposition en sous-méthodes privées | 🟡 Basse | Extraire en méthodes privées nommées explicitement |
| Constante magique (ex: `0.05`, `30`, `"ACTIF"`) sans `static final` | 🟡 Basse | Définir une constante nommée : `static final BigDecimal TAUX_PENALITE = BigDecimal.valueOf(0.05)` |
| Import générique avec `.*` | 🟡 Basse | Utiliser des imports explicites |
| Bloc `catch` vide ou avec seul `e.printStackTrace()` | 🔴 Haute | Loguer via `log.error(...)` et relancer ou traiter l'exception correctement |

### 4.3 Architecture et structure

| Point de contrôle | Sévérité | Détail |
|-------------------|----------|--------|
| Logique métier dans un controller | 🔴 Haute | Déplacer dans le service correspondant — le controller ne doit que déléguer et mapper |
| Accès direct au repository depuis un controller | 🔴 Haute | Toujours passer par la couche service |
| `@Transactional` absent sur une méthode modifiant plusieurs entités | 🔴 Haute | Ajouter `@Transactional` pour garantir l'atomicité |
| Mapper implémenté avec des setters dans le controller | 🟡 Basse | Utiliser une classe `*Mapper` dédiée (pattern du projet) |

---

## Exemple de réponse attendue

Voici un exemple de sortie conforme au format attendu pour une revue du fichier `CotisationService.java` :

| Fichier | Ligne | Sévérité | Suggestion de correction |
|---------|-------|----------|--------------------------|
| `CotisationService.java` | 34 | 🔴 Haute | `double montant` doit être remplacé par `BigDecimal montant` — risque de perte de précision sur les calculs financiers |
| `CotisationService.java` | 58 | 🔴 Haute | Bloc `synchronized(this)` détecté — remplacer par `ReentrantLock` pour éviter le pinning des Virtual Threads |
| `CotisationService.java` | 72 | 🔴 Haute | Aucune vérification du rôle `TRESORIER` avant l'enregistrement — ajouter `@PreAuthorize("hasRole('TRESORIER')")` |
| `CotisationService.java` | 91 | 🔴 Haute | L'opération de cotisation n'est pas protégée contre le double-traitement — ajouter un verrou optimiste `@Version` sur l'entité |
| `CotisationService.java` | 105 | 🟡 Basse | Constante magique `30` (délai en jours) — extraire en `static final int DELAI_COTISATION_JOURS = 30` |
| `CotisationController.java` | 22 | 🔴 Haute | `Cotisation` (entité JPA) retournée directement en `ResponseEntity<Cotisation>` — créer et retourner un `CotisationResponse` record |
| `CotisationController.java` | 45 | 🟡 Basse | Méthode de 42 lignes — décomposer en méthodes privées pour améliorer la lisibilité |
