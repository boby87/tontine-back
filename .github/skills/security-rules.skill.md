---
description: "Règles de sécurité obligatoires pour l'application Tontine. Couvre l'authentification JWT stateless, la propagation du contexte de sécurité sur Virtual Threads, la sécurité par méthode avec expressions personnalisées, la protection des données sensibles et l'audit des échecs de sécurité."
---

# Skill : Règles de Sécurité

Ce skill définit les **règles de sécurité obligatoires** à appliquer dans l'application Tontine (Spring Boot 4.0.5 / Java 21). Toute classe touchant à l'authentification, l'autorisation, les données sensibles ou l'audit doit respecter ces règles.

> **Sources** : Cahier des Charges §6.3 (Sécurité, Authentification, Audit) — copilot-instructions.md §1 (Protection des données financières).

---

## Étape 1 — Stateless Auth : JWT

### 1.1 Principe

L'authentification repose sur **JWT stateless**. Aucun token n'est stocké côté serveur — ni en session HTTP, ni en base, ni en cache Redis.

### 1.2 Configuration Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/users/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 1.3 Règles JWT

| Règle | Détail |
|---|---|
| **Algorithme** | RS256 (clé asymétrique) ou HS256 avec secret ≥ 256 bits |
| **Durée access token** | 15 minutes maximum |
| **Refresh token** | Stocké côté client uniquement (HttpOnly cookie ou secure storage mobile) |
| **Claims obligatoires** | `sub` (userId UUID), `roles` (liste de rôles globaux), `iat`, `exp` |
| **Pas de données sensibles** | Jamais de mot de passe, numéro de compte ou montant dans le token |
| **Stockage des secrets** | Vault externe (Vault, AWS Secrets Manager) — jamais dans `application.yaml` ni dans le code |

### 1.4 Filtre d'authentification

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            var authentication = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

---

## Étape 2 — Context Propagation : Virtual Threads

### 2.1 Problème

Avec les Virtual Threads (Java 21), Spring Boot exécute les requêtes HTTP sur des threads virtuels. Le `SecurityContext` de Spring Security est stocké dans un `ThreadLocal` qui **ne se propage pas automatiquement** lors du passage à un autre thread (ex : `ExecutorService`, `@Async`, notification post-commit).

### 2.2 Solution : `DelegatingSecurityContextExecutor`

Encapsuler tout `ExecutorService` utilisé pour des tâches asynchrones avec `DelegatingSecurityContextExecutor` :

```java
@Configuration
public class AsyncSecurityConfig {

    @Bean
    public ExecutorService notificationExecutor() {
        var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        return new DelegatingSecurityContextExecutorService(virtualThreadExecutor);
    }
}
```

### 2.3 Règles de propagation

| Scénario | Solution |
|---|---|
| **Notification post-commit** (Virtual Thread) | `DelegatingSecurityContextExecutorService` sur l'executor |
| **`@Async`** | Configurer `AsyncConfigurer` avec `DelegatingSecurityContextExecutor` |
| **`CompletableFuture.supplyAsync`** | Passer l'executor délégué en paramètre |
| **`ScopedValue` (preview)** | Alternatif à `ThreadLocal` pour les Virtual Threads — à évaluer en Java 25+ |

### 2.4 Vérification en test

```java
@Test
@DisplayName("Le SecurityContext doit être propagé dans le Virtual Thread de notification")
void should_propagateSecurityContext_when_notificationOnVirtualThread() {
    // Arrange
    var auth = new UsernamePasswordAuthenticationToken("user-1", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    var executor = new DelegatingSecurityContextExecutorService(
        Executors.newVirtualThreadPerTaskExecutor());

    // Act
    var future = executor.submit(() ->
        SecurityContextHolder.getContext().getAuthentication());

    // Assert
    assertThat(future.get().getName()).isEqualTo("user-1");
}
```

---

## Étape 3 — Method Security : `@PreAuthorize` avec expressions personnalisées

### 3.1 Activation

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // Active @PreAuthorize, @PostAuthorize, @Secured
}
```

### 3.2 Rôles globaux vs rôles tontine

L'application distingue deux niveaux de rôles :

| Niveau | Enum | Valeurs | Porté par |
|---|---|---|---|
| **Global** (système) | `UserRole` | `ADMIN, PRESIDENT, TREASURER, SECRETARY, CENSOR, MEMBER` | `User.role` |
| **Tontine** (contextuel) | `TontineRole` | `PRESIDENT, TRESORIER, SECRETAIRE, CENSEUR, MEMBRE` | `TontineMember.role` |

Un utilisateur peut être `MEMBRE` au niveau global mais `TRESORIER` dans une tontine spécifique.

### 3.3 Expression personnalisée `hasTontineRole`

Créer un bean `TontineSecurityExpression` exposant la logique de vérification :

```java
@Component("tontineSecurity")
public class TontineSecurityExpression {

    private final TontineMemberRepository tontineMemberRepository;

    public TontineSecurityExpression(TontineMemberRepository tontineMemberRepository) {
        this.tontineMemberRepository = tontineMemberRepository;
    }

    /**
     * Vérifie que l'utilisateur authentifié possède le rôle donné
     * dans la tontine spécifiée.
     */
    public boolean hasTontineRole(String role, String tontineId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        var userId = authentication.getName(); // userId UUID dans le claim sub
        var tontineRole = TontineRole.valueOf(role);
        return tontineMemberRepository.existsByUserIdAndTontineIdAndRole(
            userId, tontineId, tontineRole);
    }

    /**
     * Vérifie que l'utilisateur authentifié est membre actif de la tontine.
     */
    public boolean isMemberOf(String tontineId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        var userId = authentication.getName();
        return tontineMemberRepository.existsByUserIdAndTontineIdAndStatus(
            userId, tontineId, TontineMemberStatus.ACTIF);
    }
}
```

### 3.4 Utilisation dans les services / contrôleurs

```java
// Seul le TRESORIER ou le PRESIDENT de la tontine peut enregistrer une cotisation
@PreAuthorize("@tontineSecurity.hasTontineRole('TRESORIER', #tontineId) " +
              "or @tontineSecurity.hasTontineRole('PRESIDENT', #tontineId)")
public ContributionResult enregistrerCotisation(String tontineId, CotisationRequest request) {
    // ...
}

// Seul le CENSEUR peut consulter le rapport financier complet
@PreAuthorize("@tontineSecurity.hasTontineRole('CENSEUR', #tontineId)")
public RapportFinancier consulterRapport(String tontineId) {
    // ...
}

// Tout membre actif peut voir les séances de sa tontine
@PreAuthorize("@tontineSecurity.isMemberOf(#tontineId)")
public List<SessionResult> listerSeances(String tontineId) {
    // ...
}
```

### 3.5 Méthode repository requise

Ajouter dans `TontineMemberRepository` :

```java
boolean existsByUserIdAndTontineIdAndRole(String userId, String tontineId, TontineRole role);
boolean existsByUserIdAndTontineIdAndStatus(String userId, String tontineId, TontineMemberStatus status);
```

### 3.6 Matrice des permissions par rôle tontine

| Action | PRESIDENT | TRESORIER | SECRETAIRE | CENSEUR | MEMBRE |
|---|---|---|---|---|---|
| Créer une séance | ✅ | ❌ | ✅ | ❌ | ❌ |
| Enregistrer une cotisation | ✅ | ✅ | ❌ | ❌ | ❌ |
| Distribuer le pot | ✅ | ✅ | ❌ | ❌ | ❌ |
| Consulter le rapport financier complet | ✅ | ✅ | ❌ | ✅ | ❌ |
| Voir ses propres cotisations | ✅ | ✅ | ✅ | ✅ | ✅ |
| Exclure un membre | ✅ | ❌ | ❌ | ❌ | ❌ |
| Modifier les paramètres tontine | ✅ | ❌ | ✅ | ❌ | ❌ |

---

## Étape 4 — Sensitive Data : protection des données sensibles

### 4.1 Mot de passe — jamais exposé

Le champ `passwordHash` ne doit **jamais** apparaître dans :
- Les réponses JSON (DTOs / Records de réponse)
- Les logs applicatifs
- Les messages d'erreur

#### Sur l'entité JPA

```java
@Column(nullable = false)
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String passwordHash;
```

#### Sur les DTOs

Les records de requête acceptent le mot de passe en écriture seule :

```java
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password,
    // ... autres champs
) {}
```

Les records de réponse **n'incluent jamais** le mot de passe :

```java
// ✅ Correct — pas de champ password
public record UserResponse(
    String userId,
    String email,
    String phone,
    String firstName,
    String lastName,
    String role,
    String region
) {}
```

### 4.2 Données financières dans les logs

Masquer systématiquement les montants et numéros de compte :

```java
// ✅ Correct — données masquées
log.info("Cotisation enregistrée [membre={}, montant=***, séance={}]",
    membreId, seanceId);

// ❌ Interdit — montant en clair
log.info("Cotisation enregistrée [membre={}, montant={}]",
    membreId, montant);
```

### 4.3 Numéro CNI et Mobile Money

```java
// Masquer les 4 derniers caractères visibles seulement
private String masquerCni(String cni) {
    if (cni == null || cni.length() < 4) return "****";
    return "****" + cni.substring(cni.length() - 4);
}

// Dans les logs
log.info("Utilisateur inscrit [id={}, cni={}]", userId, masquerCni(cniNumber));
```

### 4.4 Checklist données sensibles

| Donnée | En réponse JSON | Dans les logs | En base |
|---|---|---|---|
| Mot de passe | ❌ Jamais | ❌ Jamais | Hash BCrypt (coût ≥ 12) |
| Email | ✅ Pour le propriétaire | Masqué (`a***@example.com`) | Clair |
| Téléphone | ✅ Derniers 4 chiffres | Masqué (`****0000`) | Clair |
| Numéro CNI | ✅ Pour le propriétaire | Masqué (`****1234`) | Chiffré AES-256 |
| Montants | ✅ Pour les rôles autorisés | Masqué (`***`) | `BigDecimal` clair |
| Mobile Money | ❌ Sauf derniers 4 | Masqué | Chiffré AES-256 |

---

## Étape 5 — Audit : journalisation des échecs de sécurité

### 5.1 Principe

Chaque **échec d'authentification** (token invalide, expiré, absent) et chaque **échec d'autorisation** (`AccessDeniedException`) doit générer un log de sécurité structuré contenant :

- L'**adresse IP** du client
- L'**identifiant utilisateur** (si disponible)
- Le **type d'échec**
- Le **timestamp**
- L'**endpoint ciblé**

### 5.2 Listener d'événements Spring Security

```java
@Component
public class SecurityAuditListener {

    private static final Logger securityLog =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        var request = getCurrentHttpRequest();
        var ip = extractClientIp(request);
        var uri = request != null ? request.getRequestURI() : "unknown";
        var principal = event.getAuthentication().getName();

        securityLog.warn("AUTH_FAILURE [user={}, ip={}, uri={}, cause={}]",
            principal, ip, uri, event.getException().getMessage());
    }

    @EventListener
    public void onAuthorizationFailure(AuthorizationDeniedEvent event) {
        var authentication = event.getAuthentication().get();
        var request = getCurrentHttpRequest();
        var ip = extractClientIp(request);
        var uri = request != null ? request.getRequestURI() : "unknown";

        securityLog.warn("ACCESS_DENIED [user={}, ip={}, uri={}]",
            authentication.getName(), ip, uri);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 5.3 Configuration du logger dédié

Dans `application.yaml`, diriger les logs de sécurité vers un fichier séparé :

```yaml
logging:
  level:
    SECURITY_AUDIT: WARN
    cm.ftg.tontine.security: DEBUG

# Logback (logback-spring.xml) — fichier d'audit séparé
# <appender name="SECURITY_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
#   <file>logs/security-audit.log</file>
#   <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
#     <fileNamePattern>logs/security-audit.%d{yyyy-MM-dd}.log</fileNamePattern>
#     <maxHistory>1825</maxHistory>  <!-- 5 ans de rétention (CDC §6.3) -->
#   </rollingPolicy>
# </appender>
# <logger name="SECURITY_AUDIT" level="WARN" additivity="false">
#   <appender-ref ref="SECURITY_FILE" />
# </logger>
```

### 5.4 Entry-point et Access Denied handler personnalisés

```java
@Component
public class SecurityEntryPoint implements AuthenticationEntryPoint {

    private static final Logger securityLog =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        var ip = extractClientIp(request);
        securityLog.warn("UNAUTHENTICATED [ip={}, uri={}, cause={}]",
            ip, request.getRequestURI(), exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
            {"code":"UNAUTHORIZED","message":"Authentification requise"}""");
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

```java
@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger securityLog =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = auth != null ? auth.getName() : "anonymous";
        var ip = extractClientIp(request);

        securityLog.warn("ACCESS_DENIED [user={}, ip={}, uri={}]",
            userId, ip, request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("""
            {"code":"FORBIDDEN","message":"Accès refusé"}""");
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 5.5 Intégration dans `SecurityFilterChain`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
                                        SecurityEntryPoint entryPoint,
                                        SecurityAccessDeniedHandler accessDeniedHandler) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/users/register", "/api/v1/auth/login").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

### 5.6 Matrice des événements audités

| Événement | Niveau | Données loguées |
|---|---|---|
| Token absent / malformé | `WARN` | IP, URI |
| Token expiré | `WARN` | IP, URI, userId (du token expiré) |
| Identifiants incorrects (login) | `WARN` | IP, URI, email tenté |
| `@PreAuthorize` refusé | `WARN` | IP, URI, userId, rôle manquant |
| Tentative d'accès à une tontine sans être membre | `WARN` | IP, URI, userId, tontineId |
| Brute force détecté (> 5 échecs / 5 min) | `ERROR` | IP, userId, nombre d'échecs |

---

## Résumé des classes à générer

| Classe | Package | Responsabilité |
|---|---|---|
| `SecurityConfig` | `cm.ftg.tontine.config` | `SecurityFilterChain` stateless JWT |
| `JwtAuthenticationFilter` | `cm.ftg.tontine.security` | Extraction et validation du token |
| `JwtTokenProvider` | `cm.ftg.tontine.security` | Génération, parsing et validation JWT |
| `AsyncSecurityConfig` | `cm.ftg.tontine.config` | `DelegatingSecurityContextExecutorService` |
| `MethodSecurityConfig` | `cm.ftg.tontine.config` | `@EnableMethodSecurity` |
| `TontineSecurityExpression` | `cm.ftg.tontine.security` | `hasTontineRole()`, `isMemberOf()` |
| `SecurityAuditListener` | `cm.ftg.tontine.security` | Listener `AuthenticationFailureEvent` / `AuthorizationDeniedEvent` |
| `SecurityEntryPoint` | `cm.ftg.tontine.security` | Réponse 401 + log audit |
| `SecurityAccessDeniedHandler` | `cm.ftg.tontine.security` | Réponse 403 + log audit |
