# Copilot Instructions — Projet Tontine

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

### Base de données

- Configurer HikariCP avec un pool de connexions suffisant pour éviter l'engorgement sous charge virtuelle :
  ```yaml
  spring:
    datasource:
      hikari:
        maximum-pool-size: 50
        minimum-idle: 10
  ```
- Les Virtual Threads multiplient le parallélisme : s'assurer que le pool de connexions DB ne devient pas le goulot d'étranglement.

---

## 3. Tests unitaires — Structure JUnit 5

### Convention de nommage

- Classe de test : `{ClasseTestée}Test.java` (ex : `CotisationServiceTest.java`).
- Méthode de test : `should_<résultat attendu>_when_<condition>` (ex : `should_throwException_when_amountIsNegative`).

### Structure d'un test (pattern AAA)

```java
@Test
@DisplayName("Doit rejeter une cotisation avec un montant négatif")
void should_throwException_when_amountIsNegative() {
    // Arrange
    var request = new CotisationRequest(membreId, BigDecimal.valueOf(-100));

    // Act & Assert
    assertThatThrownBy(() -> cotisationService.enregistrer(request))
        .isInstanceOf(MontantInvalideException.class)
        .hasMessageContaining("montant");
}
```

### Organisation

- Utiliser `@Nested` pour regrouper les tests par scénario ou méthode testée :
  ```java
  class CotisationServiceTest {

      @Nested
      @DisplayName("enregistrer()")
      class Enregistrer {

          @Test void should_persist_when_validRequest() { ... }
          @Test void should_throwException_when_amountIsNegative() { ... }
      }

      @Nested
      @DisplayName("calculerTotal()")
      class CalculerTotal { ... }
  }
  ```

### Mocking & Assertions

- Utiliser **Mockito** (`@ExtendWith(MockitoExtension.class)`) pour isoler la couche service des dépendances (repositories, clients externes).
- Ne jamais mocker la classe sous test.
- Préférer **AssertJ** (`assertThat(...)`) aux assertions JUnit natives pour la lisibilité.
- Pour les montants financiers, comparer avec `isEqualByComparingTo()` (ignore la scale de `BigDecimal`).

### Tests paramétrés

- Utiliser `@ParameterizedTest` avec `@CsvSource` ou `@MethodSource` pour les cas limites financiers :
  ```java
  @ParameterizedTest
  @CsvSource({"0, false", "100, true", "-1, false", "999999999, true"})
  void should_validateAmount(BigDecimal amount, boolean expected) {
      assertThat(validator.isValid(amount)).isEqualTo(expected);
  }
  ```

### Couverture

- Viser **≥ 80 %** de couverture sur les services métier (calcul de parts, distributions, cotisations).
- Les contrôleurs REST se testent avec `@WebMvcTest` et `MockMvc` — pas de tests unitaires purs.
- Ne pas tester les getters/setters, les entités JPA sans logique, ni le code auto-généré.
