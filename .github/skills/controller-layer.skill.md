---
description: "Procédure de création d'un contrôleur REST Spring Boot 4 dans l'application Tontine. Couvre le versioning d'API, l'injection par constructeur, les Records Java 21 pour les payloads, la traçabilité Virtual Thread et la documentation OpenAPI séparée."
---

# Skill : Créer un contrôleur REST

Ce skill décrit la procédure **obligatoire** à suivre pour créer tout nouveau contrôleur REST dans l'application Tontine (Spring Boot 4.0.5 / Java 21). Il garantit la cohérence sur le versioning, l'injection, les payloads, la traçabilité Virtual Threads et la documentation OpenAPI.

---

## Étape 1 — Annotations et versioning d'API

### 1.1 Structure de base

Tout contrôleur REST utilise `@RestController` et un `@RequestMapping` versionné :

```java
@RestController
@RequestMapping("/api/v1/cotisations")
@RequiredArgsConstructor
@Tag(name = "Cotisations", description = "Gestion des cotisations des membres")
public class CotisationController {
    // ...
}
```

### 1.2 Convention de versioning

| Règle | Détail |
|---|---|
| **Préfixe** | `/api/v{n}/` — toujours en minuscules |
| **Version courante** | `v1` |
| **Nommage ressource** | Pluriel, kebab-case pour les composés : `/api/v1/fonds-social` |
| **Pas de verbe dans l'URL** | Utiliser les méthodes HTTP : `POST /api/v1/cotisations` et non `POST /api/v1/creerCotisation` |
| **Sous-ressources** | `/api/v1/tontines/{tontineId}/seances/{seanceId}/cotisations` |

### 1.3 Annotations de mapping

Utiliser les annotations spécifiques, jamais `@RequestMapping(method=...)` :

```java
@GetMapping("/{id}")
@PostMapping
@PutMapping("/{id}")
@DeleteMapping("/{id}")
```

---

## Étape 2 — Injection par constructeur avec Lombok

### 2.1 Règle

L'injection se fait **exclusivement par constructeur** via `@RequiredArgsConstructor` de Lombok. Ne jamais utiliser `@Autowired` sur un champ.

```java
@RestController
@RequestMapping("/api/v1/cotisations")
@RequiredArgsConstructor
public class CotisationController {

    private final ContributionService contributionService;
    private final CotisationMapper cotisationMapper;
}
```

### 2.2 Pourquoi

- Les dépendances sont `final` → immutables après construction.
- Le contrôleur est testable sans Spring : on injecte des mocks via le constructeur.
- Pas de magie de réflexion à l'exécution.

### 2.3 Dépendance Maven requise

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## Étape 3 — Payloads : Records Java 21

### 3.1 Request body

Chaque request body est un **record** avec des annotations Jakarta Validation :

```java
public record CotisationRequest(
    @NotNull Long membreId,
    @NotNull Long tontineId,
    @NotNull Long seanceId,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal montant
) {}
```

### 3.2 Response body

Chaque response body est un **record** séparé — ne jamais exposer une entité JPA :

```java
public record CotisationResponse(
    Long id,
    String cleIdempotence,
    Long membreId,
    Long seanceId,
    BigDecimal montant,
    String statut,
    BigDecimal montantAmende,
    long joursRetard,
    LocalDateTime dateOperation
) {}
```

### 3.3 Règles sur les records

| Règle | Détail |
|---|---|
| **Pas d'entité JPA dans la réponse** | Toujours mapper vers un record de réponse dédié |
| **Montants** | `BigDecimal` exclusivement, jamais `double` |
| **Champs sensibles** | Ne jamais inclure : mot de passe, numéro de compte complet, clé de chiffrement |
| **Nommage** | `{Ressource}Request`, `{Ressource}Response` |
| **Validation** | Sur le request record uniquement, avec `@Valid` dans la signature du contrôleur |

### 3.4 Utilisation dans le contrôleur

```java
@PostMapping
public ResponseEntity<CotisationResponse> enregistrer(@Valid @RequestBody CotisationRequest request) {
    var result = contributionService.recordContribution(request);
    var response = cotisationMapper.toResponse(result);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

## Étape 4 — Traçabilité Virtual Threads

### 4.1 Header de debug

Chaque réponse HTTP doit inclure un header `X-Virtual-Thread` indiquant si la requête a été traitée par un Virtual Thread. Cela se fait via un **filtre global**, pas dans chaque contrôleur :

```java
@Component
public class VirtualThreadHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Virtual-Thread", String.valueOf(Thread.currentThread().isVirtual()));
        response.setHeader("X-Thread-Name", Thread.currentThread().getName());
        filterChain.doFilter(request, response);
    }
}
```

### 4.2 Log dans le contrôleur

En plus du filtre, chaque méthode du contrôleur logue l'information au niveau `DEBUG` :

```java
@PostMapping
public ResponseEntity<CotisationResponse> enregistrer(@Valid @RequestBody CotisationRequest request) {
    log.debug("POST /api/v1/cotisations — VirtualThread={}, thread={}",
        Thread.currentThread().isVirtual(), Thread.currentThread().getName());

    var result = contributionService.recordContribution(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(cotisationMapper.toResponse(result));
}
```

### 4.3 Configuration logging

Activer le log DEBUG pour les contrôleurs en développement :

```yaml
logging:
  level:
    cm.ftg.tontine.controller: DEBUG
```

> **En production** : passer à `INFO` et désactiver le header `X-Thread-Name` (fuite d'information interne). Conserver `X-Virtual-Thread` pour le monitoring.

---

## Étape 5 — Documentation OpenAPI (séparée du contrôleur)

### 5.1 Principe

La documentation Swagger/OpenAPI est **séparée** du code du contrôleur. Le contrôleur reste propre ; la documentation est définie dans une **interface** que le contrôleur implémente.

### 5.2 Interface de documentation

Créer une interface dans le package `cm.ftg.tontine.controller.doc` :

```java
package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.service.CotisationRequest;
import cm.ftg.tontine.service.CotisationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Cotisations", description = "Gestion des cotisations des membres d'une tontine")
public interface CotisationControllerDoc {

    @Operation(
        summary = "Enregistrer une cotisation",
        description = "Enregistre la cotisation d'un membre pour une séance donnée. "
                    + "Calcule automatiquement une amende si la date limite est dépassée.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Cotisation enregistrée",
                content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Membre ou séance introuvable"),
            @ApiResponse(responseCode = "409", description = "Double cotisation détectée")
        }
    )
    ResponseEntity<CotisationResponse> enregistrer(CotisationRequest request);

    @Operation(
        summary = "Consulter une cotisation",
        description = "Retourne le détail d'une cotisation par son identifiant.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cotisation trouvée"),
            @ApiResponse(responseCode = "404", description = "Cotisation introuvable")
        }
    )
    ResponseEntity<CotisationResponse> consulter(
        @Parameter(description = "Identifiant de la cotisation", required = true) Long id
    );
}
```

### 5.3 Implémentation dans le contrôleur

Le contrôleur implémente l'interface — aucune annotation Swagger dans le contrôleur lui-même :

```java
@RestController
@RequestMapping("/api/v1/cotisations")
@RequiredArgsConstructor
public class CotisationController implements CotisationControllerDoc {

    private static final Logger log = LoggerFactory.getLogger(CotisationController.class);
    private final ContributionService contributionService;
    private final CotisationMapper cotisationMapper;

    @Override
    @PostMapping
    public ResponseEntity<CotisationResponse> enregistrer(@Valid @RequestBody CotisationRequest request) {
        log.debug("POST /api/v1/cotisations — VirtualThread={}", Thread.currentThread().isVirtual());
        var result = contributionService.recordContribution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cotisationMapper.toResponse(result));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CotisationResponse> consulter(@PathVariable Long id) {
        log.debug("GET /api/v1/cotisations/{} — VirtualThread={}", id, Thread.currentThread().isVirtual());
        var result = contributionService.findById(id);
        return ResponseEntity.ok(cotisationMapper.toResponse(result));
    }
}
```

### 5.4 Dépendance Maven

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

### 5.5 Avantages de la séparation

- Le contrôleur est **lisible** — pas de bruit d'annotations Swagger.
- La documentation est **testable** indépendamment (vérification des métadonnées).
- Réutilisable si on génère un client à partir du contrat OpenAPI.

---

## Étape 6 — Gestion des erreurs

### 6.1 `@RestControllerAdvice` global

Ne jamais gérer les exceptions dans le contrôleur. Utiliser un handler centralisé :

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MembreIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleMembreIntrouvable(MembreIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("MEMBRE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DoubleCotisationException.class)
    public ResponseEntity<ErrorResponse> handleDoubleCotisation(DoubleCotisationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_CONTRIBUTION", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
}
```

### 6.2 Record d'erreur standard

```java
public record ErrorResponse(String code, String message) {}
```

### 6.3 Règles

- Ne **jamais** exposer de stacktrace ni de message d'exception interne au client.
- Masquer les montants dans les messages d'erreur renvoyés au client.
- Chaque code d'erreur est une constante lisible : `MEMBRE_NOT_FOUND`, `DUPLICATE_CONTRIBUTION`, `INVALID_AMOUNT`.

---

## Checklist de création d'un contrôleur REST

- [ ] `@RestController` + `@RequestMapping("/api/v1/{ressource}")` en pluriel kebab-case
- [ ] Injection par constructeur via `@RequiredArgsConstructor` — champs `private final`
- [ ] Request body = record Java 21 avec Jakarta Validation (`@Valid` dans la signature)
- [ ] Response body = record Java 21 — jamais une entité JPA
- [ ] `log.debug(...)` avec `Thread.currentThread().isVirtual()` dans chaque méthode
- [ ] Le filtre `VirtualThreadHeaderFilter` est présent dans le projet (header `X-Virtual-Thread`)
- [ ] Documentation OpenAPI dans une interface `{Controller}Doc` séparée (package `controller.doc`)
- [ ] Le contrôleur `implements {Controller}Doc` — aucune annotation Swagger dedans
- [ ] Gestion des erreurs déléguée au `@RestControllerAdvice` global
- [ ] Pas d'entité JPA, de stacktrace ni de montant en clair dans les réponses d'erreur
- [ ] Test avec `@WebMvcTest` + `MockMvc` (pas de test unitaire pur sur le contrôleur)
