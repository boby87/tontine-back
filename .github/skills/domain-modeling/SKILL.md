---
description: "Procédure de modélisation des entités JPA pour l'application Tontine. Impose la fidélité aux champs de la Section 7 du cahier des charges, les relations @ManyToOne/@OneToMany du diagramme ER, l'héritage BaseEntity pour l'audit (Module 10), et les contraintes de validation @Column."
---

# Skill : Modéliser une entité du domaine

Ce skill décrit la procédure **obligatoire** à suivre pour créer ou modifier toute entité JPA dans l'application Tontine. Il garantit la conformité avec le cahier des charges version 1.0, en particulier la Section 7 (Modèle de Données) et le Module 10 (Audit).

---

## Étape 1 — Classe de base obligatoire : `BaseEntity`

### 1.1 Définition

Toute entité **doit** hériter de `BaseEntity`. Cette classe fournit les champs d'audit requis par le Module 10 du cahier des charges (traçabilité de toutes les actions).

```java
package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### 1.2 Entités concernées par le soft delete

Les entités `User` et `Tontine` possèdent un champ `deletedAt` supplémentaire pour le **soft delete** exigé Section 7. Ne jamais supprimer physiquement ces enregistrements.

```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

---

## Étape 2 — Fidélité aux champs de la Section 7

### 2.1 Règle

Chaque entité générée doit contenir **au minimum** les champs décrits dans la Section 7.1 du cahier des charges. Ne jamais omettre un champ obligatoire. Les champs optionnels (marqués `- optionnel` dans le CDC) doivent être `nullable = true`.

### 2.2 Référence des entités et leurs champs obligatoires

#### `User` — Utilisateur

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String phone;                        // Format camerounais (+237), unique

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;                 // BCrypt coût ≥ 12

    @Column(name = "first_name", nullable = false)
    private String firstName;                    // Min 2 caractères (valider en service)

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;               // Âge ≥ 18 ans (valider en service)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;                       // 10 régions du Cameroun

    @Column(name = "cni_number", nullable = false)
    private String cniNumber;

    @Column(name = "cni_photo_url")
    private String cniPhotoUrl;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "mobile_money_number")
    private String mobileMoneyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_money_provider")
    private MobileMoneyProvider mobileMoneyProvider;  // MTN / ORANGE

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2faEnabled = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;             // Soft delete
}
```

#### `Tontine` — Tontine

```java
@Entity
@Table(name = "tontine")
public class Tontine extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "currency", nullable = false)
    private String currency = "XAF";

    @Column(name = "contribution_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_frequency", nullable = false)
    private ContributionFrequency contributionFrequency;  // WEEKLY / BIWEEKLY / MONTHLY

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode", nullable = false)
    private DistributionMode distributionMode;            // ROTATION / AUCTION / LOTTERY

    @Column(name = "cycle_sessions_count", nullable = false)
    private int cycleSessionsCount;

    @Column(name = "loan_enabled", nullable = false)
    private boolean loanEnabled = false;

    @Column(name = "loan_interest_rate", precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    @Column(name = "loan_max_duration_months")
    private Integer loanMaxDurationMonths;

    @Column(name = "loan_max_amount", precision = 15, scale = 2)
    private BigDecimal loanMaxAmount;

    @Column(name = "loan_guarantors_required")
    private Integer loanGuarantorsRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "bureau_designation_mode", nullable = false)
    private DesignationMode bureauDesignationMode;        // NOMINATION / ELECTION

    @Column(name = "bureau_mandate_duration_months")
    private Integer bureauMandateDurationMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;             // Soft delete
}
```

#### `TontineMember` — Membre de tontine

```java
@Entity
@Table(name = "tontine_member",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tontine_id", "user_id"}))
public class TontineMember extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TontineRole role;                    // PRESIDENT / VICE_PRESIDENT / SECRETARY / ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;                 // PENDING / ACTIVE / SUSPENDED / LEFT

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "rotation_order")
    private Integer rotationOrder;               // Optionnel : pour le mode tour de rôle
}
```

#### `Session` — Séance

```java
@Entity
@Table(name = "session")
public class Session extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false)
    private String location;

    @Column(length = 2000)
    private String agenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private TontineMember beneficiary;           // Optionnel : mode rotation

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;                // SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED

    @Column(name = "minutes_url")
    private String minutesUrl;                   // PV de réunion

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
```

#### `Attendance` — Présence

```java
@Entity
@Table(name = "attendance",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "member_id"}))
public class Attendance extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TontineMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;             // PRESENT / LATE / ABSENT / EXCUSED

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "justification_document_url")
    private String justificationDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "justification_status")
    private JustificationStatus justificationStatus;  // PENDING / APPROVED / REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;
}
```

#### `Payment` — Paiement

```java
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TontineMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;                     // Optionnel

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;                    // CONTRIBUTION / SANCTION / LOAN_REPAYMENT / ...

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;         // MTN_MOMO / ORANGE_MONEY / CASH / BANK_TRANSFER

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;                // PENDING / COMPLETED / FAILED / REFUNDED

    @Column(name = "receipt_url")
    private String receiptUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
```

#### `Sanction` — Sanction

```java
@Entity
@Table(name = "sanction")
public class Sanction extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TontineMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanctionType type;                   // LATE / ABSENT / DISTURBANCE / NON_PAYMENT / OTHER

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanctionStatus status;               // PENDING / PAID / WAIVED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by", nullable = false)
    private User appliedBy;                      // Censeur

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;                     // Optionnel : lié quand soldé
}
```

#### `Loan` — Prêt

```java
@Entity
@Table(name = "loan")
public class Loan extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private TontineMember borrower;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "total_due", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalDue;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;                   // PENDING / APPROVED / REJECTED / DISBURSED / REPAID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by")
    private User disbursedBy;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "repaid_at")
    private LocalDateTime repaidAt;
}
```

#### `LoanGuarantor` — Garant

```java
@Entity
@Table(name = "loan_guarantor",
       uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "guarantor_id"}))
public class LoanGuarantor extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id", nullable = false)
    private TontineMember guarantor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuarantorStatus status;              // PENDING / ACCEPTED / REJECTED

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
```

#### `ExtraordinaryContribution` — Cotisation extraordinaire

```java
@Entity
@Table(name = "extraordinary_contribution")
public class ExtraordinaryContribution extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;                 // DEATH_MEMBER / DEATH_RELATIVE / MARRIAGE / ...

    @Column(name = "event_description", length = 500)
    private String eventDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private TontineMember beneficiary;

    @Column(name = "amount_per_member", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountPerMember;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(name = "total_collected", precision = 15, scale = 2)
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtraContribStatus status;           // OPEN / CLOSED / DISTRIBUTED

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
```

#### `AuditLog` — Journal d'audit (Module 10)

```java
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;                     // Optionnel

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;                  // CREATE / UPDATE / DELETE / LOGIN / ...

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AuditEntityType entityType;          // USER / TONTINE / PAYMENT / ...

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;                    // JSON

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;                    // JSON

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    // Note : AuditLog n'étend PAS BaseEntity (immuable, pas d'updatedAt)
}
```

---

## Étape 3 — Relations JPA conformes au diagramme ER

### 3.1 Règles générales

| Règle | Détail |
|---|---|
| **`@ManyToOne`** | Toujours `fetch = FetchType.LAZY` — jamais EAGER sauf besoin justifié |
| **`@OneToMany`** | Côté inverse (`mappedBy`) toujours défini sur l'entité propriétaire |
| **`@JoinColumn`** | Toujours explicite avec `name = "colonne_fk"` |
| **Cascade** | Utiliser `CascadeType.ALL` uniquement pour les agrégats forts (ex: `Loan` → `LoanGuarantor`) |
| **Contrainte unicité** | Déclarer `@UniqueConstraint` sur `@Table` pour les paires de FK |

### 3.2 Relations du diagramme ER

```
User ←─── TontineMember ───► Tontine
                │
         ┌──────┴──────┐
         ▼             ▼
    Attendance      Payment
         │             │
         └──────┬───────┘
                ▼
            Session
                │
         ┌──────┴──────┐
         ▼             ▼
      Sanction        Loan ──► LoanGuarantor
                       │
               ExtraordinaryContribution
```

### 3.3 Navigation bidirectionnelle (si nécessaire)

Ne déclarer `@OneToMany` que lorsque la navigation depuis le parent est réellement utilisée en code (éviter les collections fantômes chargées inutilement) :

```java
// Dans Loan — uniquement si on doit naviguer depuis le prêt vers ses garants
@OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
private List<LoanGuarantor> guarantors = new ArrayList<>();
```

---

## Étape 4 — Contraintes de validation

### 4.1 Contraintes de base de données (`@Column`)

| Champ | Contrainte | Raison (CDC Section 7) |
|---|---|---|
| `User.email` | `unique = true, nullable = false` | Email unique par utilisateur |
| `User.phone` | `unique = true, nullable = false` | Téléphone unique (+237) |
| `User.cniNumber` | `nullable = false` | CNI camerounaise obligatoire |
| `Tontine.currency` | `nullable = false` | Toujours XAF |
| `Payment.amount` | `precision = 15, scale = 2` | BigDecimal financier |
| `TontineMember` `(tontine_id, user_id)` | `@UniqueConstraint` | Un membre une seule fois par tontine |

### 4.2 Enums à déclarer

Créer un enum Java pour chaque valeur contrainte du CDC :

```java
public enum TontineRole      { PRESIDENT, VICE_PRESIDENT, SECRETARY, SECRETARY_DEPUTY,
                               TREASURER, TREASURER_DEPUTY, AUDITOR, CENSOR, MEMBER }
public enum MemberStatus     { PENDING, ACTIVE, SUSPENDED, LEFT }
public enum ContributionFrequency { WEEKLY, BIWEEKLY, MONTHLY, CUSTOM }
public enum DistributionMode { ROTATION, AUCTION, LOTTERY }
public enum DesignationMode  { NOMINATION, ELECTION }
public enum SessionStatus    { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
public enum AttendanceStatus { PRESENT, LATE, ABSENT, EXCUSED }
public enum PaymentType      { CONTRIBUTION, SANCTION, LOAN_REPAYMENT, EXTRAORDINARY,
                               MEMBERSHIP_FEE, EMERGENCY_FUND }
public enum PaymentMethod    { MTN_MOMO, ORANGE_MONEY, CASH, BANK_TRANSFER }
public enum PaymentStatus    { PENDING, COMPLETED, FAILED, REFUNDED }
public enum SanctionType     { LATE, ABSENT, DISTURBANCE, NON_PAYMENT, OTHER }
public enum SanctionStatus   { PENDING, PAID, WAIVED }
public enum LoanStatus       { PENDING, APPROVED, REJECTED, DISBURSED, REPAID }
public enum GuarantorStatus  { PENDING, ACCEPTED, REJECTED }
public enum EventType        { DEATH_MEMBER, DEATH_RELATIVE, MARRIAGE, BIRTH, ILLNESS, OTHER }
public enum ExtraContribStatus { OPEN, CLOSED, DISTRIBUTED }
public enum MobileMoneyProvider { MTN, ORANGE }
public enum AuditAction      { CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PAYMENT, DISBURSEMENT }
public enum AuditEntityType  { USER, TONTINE, TONTINE_MEMBER, SESSION, PAYMENT,
                               SANCTION, LOAN, EXTRAORDINARY_CONTRIBUTION }
public enum Region           { ADAMAOUA, CENTRE, EST, EXTREME_NORD, LITTORAL,
                               NORD, NORD_OUEST, OUEST, SUD, SUD_OUEST }
```

---

## Checklist finale

Avant de valider une entité, vérifier :

- [ ] L'entité hérite de `BaseEntity` (sauf `AuditLog` qui est immuable) (Étape 1)
- [ ] Les entités `User` et `Tontine` ont un champ `deletedAt` pour le soft delete (Étape 1)
- [ ] Tous les champs de la Section 7.1 du CDC sont présents (Étape 2)
- [ ] Tous les `@ManyToOne` sont en `FetchType.LAZY` (Étape 3)
- [ ] Les `@JoinColumn` sont explicites avec `name` (Étape 3)
- [ ] Les `@UniqueConstraint` sont déclarés sur `@Table` pour les paires de FK (Étape 4)
- [ ] `User.email` et `User.phone` ont `unique = true` (Étape 4)
- [ ] Tous les montants financiers sont `BigDecimal` avec `precision = 15, scale = 2` (Étape 4)
- [ ] Tous les statuts et types sont des enums Java avec `@Enumerated(EnumType.STRING)` (Étape 4)
