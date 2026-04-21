---
description: "Procédure de synchronisation backend Spring Boot 4 / frontend Angular via contrat OpenAPI. Couvre la génération du fichier openapi.json, l'export du contrat, la configuration CORS, et la génération automatique des interfaces TypeScript."
---

# Skill : API Bridge — Synchronisation Backend ↔ Frontend

Ce skill définit la procédure **obligatoire** pour maintenir le contrat d'API synchronisé entre le backend Spring Boot 4.0.5 (Java 21) et le frontend Angular. Il garantit que toute modification d'API backend se reflète automatiquement dans les modèles et services TypeScript du frontend.

> **Principe clé** : Le backend est la **source de vérité unique** du contrat OpenAPI. Le frontend ne définit jamais manuellement ses interfaces de modèles — elles sont **générées** depuis le contrat.

---

## Étape 1 — Génération OpenAPI : configuration SpringDoc

### 1.1 Dépendance Maven

La dépendance SpringDoc est déjà présente dans le projet :

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

### 1.2 Configuration dans `application.yaml`

Ajouter la section SpringDoc dans `src/main/resources/application.yaml` :

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs                    # JSON par défaut
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  info:
    title: Tontine API
    description: API de gestion de tontines — Spring Boot 4 / Java 21
    version: 1.0.0
  show-actuator: false
  packages-to-scan: cm.ftg.tontine.controller
  default-produces-media-type: application/json
```

### 1.3 Bean de configuration OpenAPI

Créer une classe de configuration dédiée dans `cm.ftg.tontine.config` :

```java
package cm.ftg.tontine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tontineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tontine API")
                .description("API de gestion de tontines communautaires — Spring Boot 4 / Java 21")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Équipe Tontine")
                    .email("contact@ftg.cm"))
                .license(new License()
                    .name("Propriétaire")
                    .url("https://ftg.cm/licence")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Développement"),
                new Server().url("https://api.ftg.cm").description("Production")
            ));
    }
}
```

### 1.4 Endpoints exposés après démarrage

| URL | Format | Usage |
|---|---|---|
| `http://localhost:8080/v3/api-docs` | JSON | Contrat machine-readable (pour génération) |
| `http://localhost:8080/v3/api-docs.yaml` | YAML | Contrat lisible pour revue humaine |
| `http://localhost:8080/swagger-ui.html` | HTML | Interface interactive Swagger UI |

### 1.5 Rappel : documentation séparée du contrôleur

Conformément au skill `controller-layer`, les annotations OpenAPI (`@Operation`, `@ApiResponse`, `@Tag`) sont définies dans des **interfaces** du package `cm.ftg.tontine.controller.doc` :

```
controller/
├── doc/
│   ├── ContributionControllerDoc.java
│   ├── SessionControllerDoc.java
│   ├── TontineControllerDoc.java
│   └── UserControllerDoc.java
├── ContributionController.java   ← implements ContributionControllerDoc
├── SessionController.java        ← implements SessionControllerDoc
├── TontineController.java        ← implements TontineControllerDoc
└── UserController.java           ← implements UserControllerDoc
```

Le contrôleur lui-même ne contient **aucune** annotation Swagger.

---

## Étape 2 — Export du contrat OpenAPI

### 2.1 Export automatique au build Maven

Ajouter le plugin `springdoc-openapi-maven-plugin` dans le `pom.xml` pour générer le fichier de contrat à chaque build :

```xml
<build>
    <plugins>
        <!-- ... plugins existants ... -->

        <!-- Génération du contrat OpenAPI au build -->
        <plugin>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-maven-plugin</artifactId>
            <version>1.4</version>
            <executions>
                <execution>
                    <id>generate-openapi</id>
                    <phase>integration-test</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <apiDocsUrl>http://localhost:8080/v3/api-docs</apiDocsUrl>
                <outputFileName>openapi.json</outputFileName>
                <outputDir>${project.basedir}/contract</outputDir>
            </configuration>
        </plugin>
    </plugins>
</build>
```

> **Prérequis** : Le plugin démarre l'application pendant la phase `integration-test`. S'assurer que le `spring-boot-maven-plugin` est configuré avec les goals `start` / `stop` :

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>pre-integration-test</id>
            <goals><goal>start</goal></goals>
        </execution>
        <execution>
            <id>post-integration-test</id>
            <goals><goal>stop</goal></goals>
        </execution>
    </executions>
</plugin>
```

### 2.2 Export manuel (développement rapide)

Pour un export rapide sans build complet, utiliser `curl` après démarrage de l'application :

```bash
# JSON (recommandé pour la génération TypeScript)
curl -o contract/openapi.json http://localhost:8080/v3/api-docs

# YAML (pour revue manuelle / Git diff lisible)
curl -o contract/openapi.yaml http://localhost:8080/v3/api-docs.yaml
```

### 2.3 Structure du dossier de contrat

Le contrat est exporté à la racine du projet backend dans un dossier `contract/` :

```
tontine/
├── contract/
│   ├── openapi.json          ← Source de vérité pour la génération frontend
│   └── openapi.yaml          ← Optionnel, pour revue humaine
├── src/
│   └── ...
└── pom.xml
```

### 2.4 Versionnement du contrat

| Règle | Détail |
|---|---|
| **Commiter le contrat** | `contract/openapi.json` est versionné dans Git — chaque PR modifiant une API doit inclure le contrat mis à jour |
| **Revue de diff** | Le YAML (`openapi.yaml`) est plus lisible en diff Git — le commiter aussi pour faciliter la revue |
| **Numéro de version** | Incrémenter `info.version` dans `OpenApiConfig` à chaque changement d'API : patch (fix), minor (nouvel endpoint), major (breaking change) |
| **Breaking changes** | Tout retrait de champ ou changement de type nécessite un bump de version majeure et la création d'un endpoint `v2` |

### 2.5 Copie vers le frontend (monorepo ou multi-repo)

#### Option A — Monorepo (backend + frontend dans le même dépôt)

```bash
# Script post-build : copier le contrat dans le frontend
cp contract/openapi.json ../frontend/src/api/openapi.json
```

#### Option B — Multi-repo (dépôts séparés)

Publier le contrat comme artefact CI ou le copier via un script dans le pipeline :

```bash
# Dans le pipeline CI du backend
cp contract/openapi.json $SHARED_ARTIFACTS_DIR/openapi.json

# Dans le pipeline CI du frontend
cp $SHARED_ARTIFACTS_DIR/openapi.json src/api/openapi.json
npm run generate:api
```

---

## Étape 3 — Configuration CORS pour le frontend

### 3.1 Classe de configuration

Créer `CorsConfig.java` dans `cm.ftg.tontine.config` :

```java
package cm.ftg.tontine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Idempotency-Key",
            "Accept"
        ));
        config.setExposedHeaders(List.of(
            "X-Virtual-Thread",
            "X-Thread-Name",
            "Location"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);  // Pré-vol en cache 1h

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### 3.2 Configuration externalisée dans `application.yaml`

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:4200    # Angular dev server
      - http://localhost:4300    # Storybook / tests E2E
```

Pour la production, surcharger avec un profil `application-prod.yaml` :

```yaml
app:
  cors:
    allowed-origins:
      - https://app.ftg.cm
```

### 3.3 Intégration avec Spring Security

Dans `SecurityConfig`, activer CORS en référençant le bean `CorsConfigurationSource` :

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
                                        CorsConfigurationSource corsSource) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsSource))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/users/register", "/api/v1/auth/login").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated()
        )
        // ... autres configurations
        .build();
}
```

### 3.4 Règles de sécurité CORS

| Règle | Détail |
|---|---|
| **Pas de wildcard `*`** | Ne jamais utiliser `allowedOrigins("*")` — lister explicitement les origines |
| **`allowCredentials(true)`** | Nécessaire pour le JWT en header `Authorization` |
| **Endpoints Swagger** | En développement uniquement — désactiver en production via profil |
| **Méthodes autorisées** | Uniquement celles réellement utilisées par l'API — pas de `*` |
| **Headers exposés** | Limiter aux headers custom nécessaires (`X-Virtual-Thread`, `Location`) |

### 3.5 Protection de Swagger UI en production

```yaml
# application-prod.yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## Étape 4 — Génération TypeScript depuis le contrat OpenAPI

### 4.1 Outil recommandé : `openapi-typescript`

Installer le générateur dans le projet frontend Angular :

```bash
npm install --save-dev openapi-typescript
```

### 4.2 Script de génération

Ajouter le script dans le `package.json` du frontend :

```json
{
  "scripts": {
    "generate:api": "openapi-typescript src/api/openapi.json -o src/app/generated/api-types.ts",
    "generate:api:remote": "openapi-typescript http://localhost:8080/v3/api-docs -o src/app/generated/api-types.ts"
  }
}
```

| Script | Usage |
|---|---|
| `generate:api` | Génère depuis le fichier local `openapi.json` (CI, offline) |
| `generate:api:remote` | Génère directement depuis le backend en cours d'exécution (développement) |

### 4.3 Structure des fichiers générés

```
frontend/
├── src/
│   ├── api/
│   │   └── openapi.json             ← Contrat copié depuis le backend
│   └── app/
│       └── generated/
│           └── api-types.ts          ← Interfaces TypeScript auto-générées
├── package.json
└── ...
```

### 4.4 Exemple de sortie générée

Pour le record Java `ContributionResponse` :

```java
// Backend — Java record
public record ContributionResponse(
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

Le générateur produit automatiquement :

```typescript
// Frontend — TypeScript généré (api-types.ts)
export interface components {
  schemas: {
    ContributionResponse: {
      id?: number;
      cleIdempotence?: string;
      membreId?: number;
      seanceId?: number;
      montant?: number;
      statut?: string;
      montantAmende?: number;
      joursRetard?: number;
      dateOperation?: string;  // ISO 8601
    };
    // ... autres schémas
  };
}
```

### 4.5 Alternative avancée : `@openapitools/openapi-generator-cli`

Pour générer non seulement les **interfaces** mais aussi les **services HTTP** Angular complets :

```bash
npm install --save-dev @openapitools/openapi-generator-cli
```

Script dans `package.json` :

```json
{
  "scripts": {
    "generate:api:full": "openapi-generator-cli generate -i src/api/openapi.json -g typescript-angular -o src/app/generated/api --additional-properties=ngVersion=18"
  }
}
```

Cela génère :

```
src/app/generated/api/
├── model/
│   ├── contributionResponse.ts
│   ├── contributionRequest.ts
│   ├── tontineResponse.ts
│   ├── sessionResponse.ts
│   ├── userResponse.ts
│   └── ...
├── api/
│   ├── contributions.service.ts     ← HttpClient Angular prêt à l'emploi
│   ├── tontines.service.ts
│   ├── sessions.service.ts
│   ├── users.service.ts
│   └── ...
├── configuration.ts
└── index.ts
```

### 4.6 Règles sur les fichiers générés

| Règle | Détail |
|---|---|
| **Ne jamais modifier manuellement** | Les fichiers dans `generated/` sont écrasés à chaque génération |
| **Ajouter au `.gitignore`** | Optionnel : certaines équipes commitent les fichiers générés pour la traçabilité ; d'autres les régénèrent en CI |
| **Wrapper personnalisé** | Créer des services wrapper autour des services générés pour ajouter la logique métier frontend (retry, cache, error handling) |
| **BigDecimal → number** | Attention : Java `BigDecimal` devient `number` en TypeScript. Pour la précision financière côté frontend, envisager l'utilisation de bibliothèques comme `decimal.js` |

---

## Étape 5 — Workflow complet de synchronisation

### 5.1 Procédure développeur (quotidienne)

```
┌──────────────────────────────────────────────────────────────────────┐
│                         WORKFLOW API BRIDGE                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Modifier l'API backend                                           │
│     └── Controller, Request/Response records, Doc interface          │
│                                                                      │
│  2. Démarrer le backend                                              │
│     └── mvnw spring-boot:run                                         │
│                                                                      │
│  3. Vérifier sur Swagger UI                                          │
│     └── http://localhost:8080/swagger-ui.html                        │
│                                                                      │
│  4. Exporter le contrat                                              │
│     └── curl -o contract/openapi.json http://localhost:8080/v3/api-docs │
│                                                                      │
│  5. Copier vers le frontend                                          │
│     └── cp contract/openapi.json ../frontend/src/api/openapi.json    │
│                                                                      │
│  6. Régénérer les types TypeScript                                   │
│     └── cd ../frontend && npm run generate:api                       │
│                                                                      │
│  7. Commiter le contrat + les types générés                          │
│     └── git add contract/ src/app/generated/                         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.2 Automatisation CI/CD

```yaml
# Exemple GitHub Actions — job de vérification du contrat
api-contract-check:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4

    - name: Build & Start backend
      run: |
        cd tontine
        ./mvnw spring-boot:start -DskipTests

    - name: Export OpenAPI contract
      run: |
        curl -f -o contract/openapi.json http://localhost:8080/v3/api-docs

    - name: Stop backend
      run: |
        cd tontine
        ./mvnw spring-boot:stop

    - name: Check contract diff
      run: |
        git diff --exit-code contract/openapi.json || \
          (echo "❌ Le contrat OpenAPI a changé mais n'a pas été commité !" && exit 1)

    - name: Generate TypeScript types
      run: |
        cd frontend
        npm ci
        npm run generate:api

    - name: Verify no uncommitted generated changes
      run: |
        cd frontend
        git diff --exit-code src/app/generated/ || \
          (echo "❌ Les types TypeScript générés ne sont pas à jour !" && exit 1)
```

---

## Checklist API Bridge

- [ ] `springdoc-openapi-starter-webmvc-ui` version `2.8.6` dans le `pom.xml`
- [ ] `OpenApiConfig` bean configuré avec titre, version et serveurs
- [ ] `springdoc` configuré dans `application.yaml` (path, packages-to-scan)
- [ ] Swagger UI désactivé en production (`application-prod.yaml`)
- [ ] `contract/openapi.json` exporté et versionné dans Git
- [ ] `CorsConfig` avec origines explicites (jamais `*`) — externalisé dans `application.yaml`
- [ ] CORS intégré dans `SecurityFilterChain` via `.cors(cors -> cors.configurationSource(...))`
- [ ] Endpoints Swagger (`/v3/api-docs/**`, `/swagger-ui/**`) autorisés dans Spring Security
- [ ] Script `generate:api` dans le `package.json` du frontend
- [ ] Fichiers `generated/` jamais modifiés manuellement
- [ ] Pipeline CI vérifie la cohérence contrat ↔ types générés
- [ ] `BigDecimal` Java documenté comme `number` en TypeScript — utiliser `decimal.js` côté frontend si précision requise

