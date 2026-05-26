---
description: "Procédure de génération de tests unitaires JUnit 5 + Mockito pour l'application Tontine. Couvre le périmètre restreint aux méthodes nouvelles/modifiées, les standards AssertJ, la couverture Happy Path + Edge Cases, le mocking avec @Mock/@InjectMocks, et la vérification non-bloquante pour les Virtual Threads Java 21."
---

# Skill : Générer des tests unitaires

Ce skill décrit la procédure **obligatoire** à suivre pour générer des tests unitaires dans l'application Tontine (Spring Boot 4.0.5 / Java 21 / JUnit 5 / Mockito / AssertJ).

---

## Étape 1 — Périmètre restreint (CRITIQUE)

### 1.1 Règle absolue

L'IA ne génère des tests **QUE** pour :

- les méthodes **nouvellement créées** ;
- les méthodes dont la **logique métier a été modifiée** ;
- les méthodes dont la **signature a changé** (paramètres, type de retour).

### 1.2 Interdictions

| Situation | Action |
|---|---|
| Méthode existante non modifiée | **Ne pas toucher** aux tests existants |
| Refactoring interne sans changement de comportement | **Ne pas régénérer** les tests |
| Ajout d'une annotation (`@Transactional`, `@Cacheable`) sans impact sur les entrées/sorties | **Ne pas modifier** les tests |
| Changement de signature d'une méthode existante | **Adapter** uniquement les tests impactés par la nouvelle signature |

### 1.3 Vérification préalable

Avant de générer un test, vérifier :

1. Le fichier de test correspondant existe-t-il déjà ? → Si oui, **ajouter** les nouveaux tests sans supprimer les existants.
2. Une classe `@Nested` couvre-t-elle déjà la méthode ciblée ? → Si oui, ajouter les cas dans cette classe.
3. La méthode testée a-t-elle changé de signature ? → Si oui, adapter les tests existants de cette méthode uniquement.

---

## Étape 2 — Structure et standards techniques

### 2.1 Squelette de classe de test

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("NomDuService")
class NomDuServiceTest {

    @Mock private DependanceA dependanceA;
    @Mock private DependanceB dependanceB;
    @InjectMocks private NomDuService service;

    @Nested
    @DisplayName("nomDeLaMethode")
    class NomDeLaMethode {

        @Test
        @DisplayName("Doit [résultat attendu] quand [condition]")
        void should_[result]_when_[condition]() {
            // Arrange
            // Act
            // Assert
        }
    }
}
```

### 2.2 Règles structurelles

| Règle | Détail |
|---|---|
| **Extension** | `@ExtendWith(MockitoExtension.class)` — jamais `@SpringBootTest` pour un test unitaire pur |
| **Mocking** | `@Mock` pour chaque dépendance, `@InjectMocks` pour le service sous test |
| **Groupement** | Une classe `@Nested` par méthode publique testée |
| **Nommage** | `should_[résultat]_when_[condition]` en anglais, `@DisplayName` en français |
| **Pattern AAA** | Sections `// Arrange`, `// Act`, `// Assert` explicites dans chaque test |
| **Assertions** | AssertJ exclusivement (`assertThat(...).isEqualTo(...)`) — jamais les assertions JUnit natives |
| **BigDecimal** | Comparer avec `isEqualByComparingTo()` — jamais `isEqualTo()` pour les montants |

### 2.3 Dépendances requises

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

> Ce starter inclut JUnit 5, Mockito et AssertJ. Aucune dépendance supplémentaire n'est nécessaire pour les tests unitaires purs.

---

## Étape 3 — Couverture obligatoire : Happy Path + Edge Cases

### 3.1 Minimum requis

Pour chaque méthode testée, générer **au minimum** :

| Catégorie | Nombre min. | Description |
|---|---|---|
| **Happy Path** | 1 | Le cas nominal où tout fonctionne correctement |
| **Edge Cases** | 2 | Cas d'erreur, valeurs limites, ou scénarios dégradés |

### 3.2 Cas d'erreur typiques à couvrir

Pour les services financiers de l'application Tontine :

```java
// Happy Path
@Test
@DisplayName("Doit enregistrer la cotisation quand toutes les conditions sont remplies")
void should_recordContribution_when_allConditionsMet() { ... }

// Edge Case 1 — Entité introuvable
@Test
@DisplayName("Doit lever MembreIntrouvableException quand le membre n'existe pas")
void should_throwMembreIntrouvable_when_membreDoesNotExist() {
    // Arrange
    when(membreRepository.findByIdAndTontineId(99L, 1L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.recordContribution(request))
        .isInstanceOf(MembreIntrouvableException.class);
}

// Edge Case 2 — Doublon détecté
@Test
@DisplayName("Doit lever DoubleCotisationException quand une cotisation existe déjà")
void should_throwDoubleCotisation_when_alreadyExists() { ... }
```

### 3.3 Grille de couverture par type de méthode

| Type de méthode | Happy Path | Edge Cases obligatoires |
|---|---|---|
| **Création** (enregistrer, ajouter) | Création réussie + retour DTO correct | Entité introuvable, doublon, montant invalide |
| **Calcul** (amende, distribution) | Calcul correct avec valeurs nominales | Zéro jour de retard, plafond atteint, montant zéro |
| **Lecture** (findById, lister) | Retour de l'entité existante | Entité inexistante (`Optional.empty()`) |
| **Validation** (vérifier, contrôler) | Validation passante | Chaque règle de validation violée individuellement |

---

## Étape 4 — Mocking : règles @Mock et @InjectMocks

### 4.1 Configuration

```java
@Mock private MembreRepository membreRepository;
@Mock private SeanceRepository seanceRepository;
@Mock private CotisationRepository cotisationRepository;
@Mock private NotificationService notificationService;
@InjectMocks private ContributionService contributionService;
```

### 4.2 Règles de mocking

| Règle | Détail |
|---|---|
| **Jamais de `@Spy`** sauf si le test porte explicitement sur un appel partiel | Préférer `@Mock` |
| **`when(...).thenReturn(...)`** | Pour les comportements normaux |
| **`when(...).thenThrow(...)`** | Pour simuler les erreurs |
| **`verify(..., times(1))`** | Pour confirmer qu'une écriture ou notification a été appelée |
| **`verify(..., never())`** | Pour confirmer qu'une action n'a PAS eu lieu en cas d'erreur |
| **`ArgumentCaptor`** | Pour inspecter les objets passés aux méthodes appelées (ex : vérifier le montant persisté) |

### 4.3 Vérification post-action

Après le Happy Path, toujours vérifier les effets de bord :

```java
// Vérifier la persistance
var captor = ArgumentCaptor.forClass(Cotisation.class);
verify(cotisationRepository).save(captor.capture());
assertThat(captor.getValue().getMontant()).isEqualByComparingTo(new BigDecimal("5000"));

// Vérifier qu'aucune notification n'a été envoyée en cas d'erreur
verify(notificationService, never()).notifierCotisationRecue(any());
```

---

## Étape 5 — Spécificité Java 21 : Virtual Threads

### 5.1 Quand s'applique cette étape

Cette étape est **obligatoire** si le code testé :

- utilise `Executors.newVirtualThreadPerTaskExecutor()` ;
- délègue une tâche via `executor.submit(...)` ;
- enregistre un callback post-commit exécuté sur un Virtual Thread.

### 5.2 Test de non-blocage

Vérifier que la logique déléguée aux Virtual Threads est non-bloquante et s'exécute correctement :

```java
@Test
@DisplayName("Doit exécuter la notification sur un Virtual Thread sans bloquer le thread principal")
void should_executeNotification_on_virtualThread_without_blocking() {
    // Arrange
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    var latch = new CountDownLatch(1);

    // Act
    executor.submit(() -> {
        assertThat(Thread.currentThread().isVirtual()).isTrue();
        latch.countDown();
    });

    // Assert — le thread principal n'est pas bloqué
    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
}
```

### 5.3 Test d'intégration TransactionSynchronization

Si le service utilise `TransactionSynchronizationManager.registerSynchronization(...)`, activer manuellement le contexte transactionnel dans les tests unitaires :

```java
@BeforeEach
void initTransactionContext() {
    TransactionSynchronizationManager.initSynchronization();
}

@AfterEach
void clearTransactionContext() {
    TransactionSynchronizationManager.clearSynchronization();
}
```

> **Attention** : ne placer le `@BeforeEach`/`@AfterEach` que dans la classe `@Nested` qui teste la méthode utilisant les synchronisations transactionnelles, pas au niveau global.

### 5.4 Vérification du flag Virtual Thread dans les logs

Si le code contient `Thread.currentThread().isVirtual()` dans ses logs, le test doit simplement valider que l'appel ne lève pas d'exception — ne pas mocker `Thread`.

---

## Checklist finale

Avant de soumettre les tests générés, vérifier :

- [ ] Seules les méthodes nouvelles/modifiées sont testées (Étape 1)
- [ ] Chaque classe `@Nested` correspond à une seule méthode publique (Étape 2)
- [ ] Pattern AAA respecté dans chaque test (Étape 2)
- [ ] Assertions AssertJ uniquement, `isEqualByComparingTo()` pour les `BigDecimal` (Étape 2)
- [ ] Au moins 1 Happy Path + 2 Edge Cases par méthode (Étape 3)
- [ ] `@Mock` / `@InjectMocks` sans `@Autowired` (Étape 4)
- [ ] `verify(...)` pour chaque effet de bord (persistance, notification) (Étape 4)
- [ ] Test Virtual Thread si le code utilise un executor virtuel (Étape 5)
- [ ] Le fichier de test compile et tous les tests passent (`mvnw test`)