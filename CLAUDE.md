# Copilot Instructions — Projet Tontine

## Décisions d'architecture (actées — ne pas modifier sans discussion)

| Sujet | Decision | Raison |
| --- | --- | --- |
| Package racine | `cm.ftg.tontine` (ne pas renommer) | Cout migration trop eleve, coherence codebase |
| Type montants | `BigDecimal` (precision 19, scale 2) | Securite financiere, compatibilite codebase |
| Tests | Groovy + Spock Framework (pas JUnit 5) | Decision actee — tous les tests en Groovy uniquement |
| UI frontend | Tailwind CSS (pas Angular Material) | Deja en place, CLAUDE.md projet |

## Contexte du projet

Application de gestion de tontines développée avec **Spring Boot 4.0.5** et **Java 21**.
Package racine : `cm.ftg.tontine`. Build : Maven.

---

## 1. Sécurité — Protection des données financières

### Principes généraux

- Toute donnée financière (montants, soldes, transactions) est considérée **sensible**. Ne jamais l'exposer dans les logs, messages d'erreur ou réponses d'API non filtrées.
- Appliquer le principe du **moindre privilège** : chaque endpoint n'expose que les données strictement nécessaires.
- Toute opération financière (cotisation, retrait, distribution) doit être **idempotente** et protégée contre le double-traitement.

### Authentification & Autorisation

- Utiliser **Spring Security** avec JWT (stateless). Ne jamais stocker de tokens côté serveur.
- Tout endpoint manipulant des données financières doit exiger un rôle explicite (`@PreAuthorize`).
- Séparer les rôles : `MEMBRE`, `TRESORIER`, `ADMIN`. Un membre ne peut consulter que ses propres transactions.

### Protection des données

- Chiffrer les données sensibles au repos (numéros de compte, montants critiques) avec **AES-256**. Stocker les clés dans un vault externe, jamais dans le code source ni dans `application.yaml`.
- Utiliser **BCrypt** (coût ≥ 12) pour le hachage des mots de passe.
- Appliquer `@JsonIgnore` ou des DTOs de projection pour ne jamais sérialiser les champs sensibles vers le client.
- Masquer les données financières dans les logs (`amount=***`, `accountNumber=****1234`).

### Validation & Intégrité

- Valider systématiquement les entrées avec **Jakarta Validation** (`@NotNull`, `@Positive`, `@DecimalMin`).
- Utiliser `BigDecimal` pour tous les montants financiers — jamais `double` ni `float`.
- Protéger les opérations concurrentes (double cotisation) avec du **verrouillage optimiste** (`@Version`) ou pessimiste selon le cas.
- Empêcher les injections SQL en utilisant exclusivement des requêtes paramétrées (Spring Data JPA / named parameters).

### Audit

- Journaliser chaque opération financière (création, modification, annulation) dans une table d'audit immuable avec : utilisateur, timestamp, IP, action, ancien/nouveau état.
- Ne jamais supprimer physiquement une transaction financière — utiliser le **soft delete**.

---

## 2. Performance — Virtual Threads (Java 21)

### Activation

- Activer les Virtual Threads dans `application.yaml` :
  ```yaml
  spring:
    threads:
      virtual:
        enabled: true
  ```
- Avec cette configuration, Spring Boot exécute automatiquement les requêtes HTTP sur des Virtual Threads (Tomcat / Netty).

### Bonnes pratiques

- Privilégier le modèle **thread-per-request** classique (code bloquant simple et lisible) plutôt que la programmation réactive (`WebFlux`). Les Virtual Threads éliminent le besoin de code réactif pour la scalabilité I/O.
- Utiliser `ExecutorService` avec `Executors.newVirtualThreadPerTaskExecutor()` pour les tâches parallèles internes (calcul de distributions, envoi de notifications en lot).
- Ne pas utiliser de pool de threads borné (`FixedThreadPool`) pour les Virtual Threads — cela annule leur avantage.

### Points de vigilance

- Éviter les blocs `synchronized` prolongés et les opérations CPU-intensives longues dans les Virtual Threads (elles **pin** le carrier thread). Préférer `ReentrantLock` pour les sections critiques.
- Ne pas stocker d'état dans des `ThreadLocal` de longue durée — les Virtual Threads rendent les `ThreadLocal` coûteux en mémoire. Utiliser `ScopedValue` (Java 21 preview) quand possible.
- Surveiller les métriques de pinning avec `-Djdk.tracePinnedThreads=short` en développement.



## 3. Tests unitaires — Spock Framework (Groovy)

> Tous les tests sont écrits en **Groovy uniquement**. Pas de fichiers `.java` dans `src/test/`.
> Répertoire : `src/test/groovy/cm/ftg/tontine/`

### Convention de nommage

- Fichier : `{ClasseTestée}Spec.groovy` (ex : `CotisationServiceSpec.groovy`).
- Labels Spock : `given:` / `when:` / `then:` / `and:` / `expect:` / `where:`.

### Structure d'une spec (pattern given/when/then)

```groovy
class CotisationServiceSpec extends Specification {

    CotisationRepository repository = Mock()
    CotisationService service = new CotisationService(repository)

    def "doit rejeter une cotisation avec un montant negatif"() {
        given:
        def request = new CotisationRequest(membreId, BigDecimal.valueOf(-100))

        when:
        service.enregistrer(request)

        then:
        thrown(MontantInvalideException)
    }
}
```

### Mocking

- Utiliser les mocks natifs Spock : `Mock()`, `Stub()`, `Spy()` — pas Mockito.
- Vérifier les interactions avec `1 * repository.save(_)` dans le bloc `then:`.
- Capturer les arguments avec `1 * repository.save({ it.montant == expected })`.

### Tests paramétrés

```groovy
def "doit valider le montant"() {
    expect:
    service.isValid(montant) == resultat

    where:
    montant                    || resultat
    BigDecimal.ZERO            || false
    BigDecimal.valueOf(100)    || true
    BigDecimal.valueOf(-1)     || false
    BigDecimal.valueOf(999999) || true
}
```

### Montants BigDecimal

- Comparer avec `==` en Groovy (appelle `equals`) ou `montant.compareTo(expected) == 0` pour ignorer la scale.

### Couverture

- Viser **≥ 80 %** sur les services métier (calcul de parts, distributions, cotisations).
- Les contrôleurs se testent avec `@WebMvcTest` + `MockMvc` via Spock (`def mockMvc = MockMvcBuilders...`).
- Ne pas tester les getters/setters, les entités JPA sans logique, ni le code auto-généré.
