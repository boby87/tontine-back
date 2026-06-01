# API Tontine - Endpoints

Base URL : http://localhost:8081/api
Spec OpenAPI : http://localhost:8081/api/v3/api-docs
Swagger UI    : http://localhost:8081/api/swagger-ui.html

**Total : 173 endpoints repartis en 11 groupes.**

## Auth (8)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/auth/forgot-password` |  |
| `POST` | `/auth/login` |  |
| `POST` | `/auth/logout` |  |
| `GET` | `/auth/me` |  |
| `POST` | `/auth/refresh` |  |
| `POST` | `/auth/register` |  |
| `POST` | `/auth/reset-password` |  |
| `POST` | `/auth/verify-otp` |  |

## Tontines (6)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/tontines` |  |
| `POST` | `/tontines` |  |
| `GET` | `/tontines/{id}` |  |
| `PATCH` | `/tontines/{id}` |  |
| `GET` | `/tontines/{id}/cycles` |  |
| `GET` | `/tontines/mine` |  |

## Members (4)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/members/me/contributions` |  |
| `GET` | `/members/me/loans` |  |
| `GET` | `/members/me/planning` |  |
| `GET` | `/members/me/summary` |  |

## Notifications (5)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/notifications` |  |
| `DELETE` | `/notifications/{id}` |  |
| `POST` | `/notifications/{id}/read` |  |
| `POST` | `/notifications/read-all` |  |
| `GET` | `/notifications/unread-count` |  |

## Files / Storage (4)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/files` |  |
| `DELETE` | `/files/{id}` |  |
| `GET` | `/files/{id}` |  |
| `GET` | `/files/{id}/download` |  |

## Webhooks (1)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/webhooks/mobile-money/{provider}` |  |

## Président (41)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/president/announcements` |  |
| `POST` | `/president/announcements` |  |
| `GET` | `/president/conflicts` |  |
| `GET` | `/president/conflicts/{id}` |  |
| `POST` | `/president/conflicts/{id}/decide` |  |
| `POST` | `/president/conflicts/{id}/schedule-mediation` |  |
| `GET` | `/president/cycle-close` |  |
| `POST` | `/president/cycle-close/check/{key}` |  |
| `POST` | `/president/cycle-close/sign` |  |
| `GET` | `/president/dashboard` |  |
| `GET` | `/president/delegations` |  |
| `POST` | `/president/delegations` |  |
| `POST` | `/president/delegations/{id}/revoke` |  |
| `GET` | `/president/emergency-blocks` |  |
| `POST` | `/president/emergency-blocks` |  |
| `POST` | `/president/emergency-blocks/{id}/lift` |  |
| `GET` | `/president/extraordinary-contributions` |  |
| `POST` | `/president/extraordinary-contributions` |  |
| `POST` | `/president/extraordinary-contributions/{id}/close` |  |
| `POST` | `/president/extraordinary-contributions/{id}/distribute` |  |
| `GET` | `/president/membership` |  |
| `GET` | `/president/membership/{id}` |  |
| `POST` | `/president/membership/{id}/decide` |  |
| `GET` | `/president/reports` |  |
| `GET` | `/president/reports/{id}` |  |
| `GET` | `/president/sanctions` |  |
| `POST` | `/president/sanctions/{id}/confirm` |  |
| `POST` | `/president/sanctions/{id}/waive` |  |
| `GET` | `/president/sessions` |  |
| `GET` | `/president/sessions/{id}` |  |
| `POST` | `/president/sessions/{id}/agenda/{agendaId}/advance` |  |
| `POST` | `/president/sessions/{id}/close` |  |
| `POST` | `/president/sessions/{id}/open` |  |
| `POST` | `/president/sessions/{id}/sign-cagnotte` |  |
| `GET` | `/president/validations` |  |
| `GET` | `/president/validations/{id}` |  |
| `POST` | `/president/validations/{id}/decide` |  |
| `GET` | `/president/votes` |  |
| `POST` | `/president/votes` |  |
| `GET` | `/president/votes/{id}` |  |
| `POST` | `/president/votes/{id}/close` |  |

## Secrétaire (26)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/secretary/agendas` |  |
| `POST` | `/secretary/agendas` |  |
| `GET` | `/secretary/agendas/{id}` |  |
| `POST` | `/secretary/agendas/{id}/submit` |  |
| `GET` | `/secretary/announcements` |  |
| `POST` | `/secretary/announcements` |  |
| `GET` | `/secretary/archives` |  |
| `POST` | `/secretary/archives` |  |
| `GET` | `/secretary/convocations` |  |
| `POST` | `/secretary/convocations` |  |
| `GET` | `/secretary/dashboard` |  |
| `GET` | `/secretary/members` |  |
| `PATCH` | `/secretary/members/{id}` |  |
| `GET` | `/secretary/membership` |  |
| `POST` | `/secretary/membership/{id}/review` |  |
| `GET` | `/secretary/minutes` |  |
| `GET` | `/secretary/minutes/{id}` |  |
| `PUT` | `/secretary/minutes/{id}` |  |
| `POST` | `/secretary/minutes/{id}/sign` |  |
| `GET` | `/secretary/reports` |  |
| `POST` | `/secretary/reports/generate` |  |
| `POST` | `/secretary/sessions/{sessionId}/attendance/{memberId}` |  |
| `POST` | `/secretary/sessions/{sessionId}/attendance/finalize` |  |
| `GET` | `/secretary/sessions/{sessionId}/rsvps` |  |
| `POST` | `/secretary/sessions/{sessionId}/rsvps/{memberId}` |  |
| `POST` | `/secretary/sessions/{sessionId}/rsvps/remind` |  |

## Trésorier (29)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/treasurer/cashboxes` |  |
| `GET` | `/treasurer/cashboxes/{id}/movements` |  |
| `GET` | `/treasurer/contributions` |  |
| `POST` | `/treasurer/contributions/{id}/pay` |  |
| `POST` | `/treasurer/contributions/advance` |  |
| `GET` | `/treasurer/dashboard` |  |
| `GET` | `/treasurer/distributions` |  |
| `POST` | `/treasurer/distributions` |  |
| `GET` | `/treasurer/expenses` |  |
| `POST` | `/treasurer/expenses` |  |
| `GET` | `/treasurer/extra-contributions` |  |
| `POST` | `/treasurer/extra-contributions/{id}/collect` |  |
| `GET` | `/treasurer/loans` |  |
| `POST` | `/treasurer/loans/{id}/disburse` |  |
| `POST` | `/treasurer/loans/{id}/repay` |  |
| `GET` | `/treasurer/mobile-money` |  |
| `POST` | `/treasurer/mobile-money/{id}/approve` |  |
| `POST` | `/treasurer/mobile-money/{id}/reject` |  |
| `GET` | `/treasurer/mobile-money/reconciliation` |  |
| `POST` | `/treasurer/mobile-money/send` |  |
| `GET` | `/treasurer/reports` |  |
| `POST` | `/treasurer/reports/generate` |  |
| `GET` | `/treasurer/sanctions` |  |
| `POST` | `/treasurer/sanctions/{id}/collect` |  |
| `POST` | `/treasurer/sanctions/{id}/refund` |  |
| `GET` | `/treasurer/sessions` |  |
| `GET` | `/treasurer/sessions/{id}/bilan` |  |
| `GET` | `/treasurer/transfers` |  |
| `POST` | `/treasurer/transfers` |  |

## Censeur (21)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/censor/attendance-modifications` |  |
| `POST` | `/censor/attendance-modifications/{id}/decide` |  |
| `GET` | `/censor/communications` |  |
| `POST` | `/censor/communications` |  |
| `GET` | `/censor/contestations` |  |
| `POST` | `/censor/contestations/{id}/decide` |  |
| `GET` | `/censor/dashboard` |  |
| `GET` | `/censor/justifications` |  |
| `POST` | `/censor/justifications/{id}/decide` |  |
| `POST` | `/censor/justifications/{id}/president-approve` |  |
| `GET` | `/censor/members` |  |
| `GET` | `/censor/reports` |  |
| `POST` | `/censor/reports/{id}/observations` |  |
| `POST` | `/censor/reports/generate` |  |
| `GET` | `/censor/sanctions` |  |
| `POST` | `/censor/sanctions` |  |
| `POST` | `/censor/sanctions/{id}/cancel` |  |
| `GET` | `/censor/sanctions/auto-detected` |  |
| `POST` | `/censor/sanctions/batch` |  |
| `POST` | `/censor/sanctions/confirm-batch` |  |
| `GET` | `/censor/unpaid-sanctions` |  |

## Auditeur (28)

| Methode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/auditor/anomalies` |  |
| `POST` | `/auditor/anomalies` |  |
| `POST` | `/auditor/anomalies/{id}/close` |  |
| `POST` | `/auditor/anomalies/{id}/reopen` |  |
| `GET` | `/auditor/audits` |  |
| `POST` | `/auditor/audits` |  |
| `GET` | `/auditor/balance-reviews` |  |
| `POST` | `/auditor/balance-reviews` |  |
| `GET` | `/auditor/certifications` |  |
| `POST` | `/auditor/certifications` |  |
| `GET` | `/auditor/clarifications` |  |
| `POST` | `/auditor/clarifications` |  |
| `POST` | `/auditor/clarifications/{id}/evaluate` |  |
| `POST` | `/auditor/clarifications/{id}/simulate-response` |  |
| `GET` | `/auditor/controls` |  |
| `POST` | `/auditor/controls` |  |
| `POST` | `/auditor/controls/{id}/complete` |  |
| `GET` | `/auditor/dashboard` |  |
| `GET` | `/auditor/export` |  |
| `GET` | `/auditor/financial-data` |  |
| `GET` | `/auditor/recommendations` |  |
| `POST` | `/auditor/recommendations` |  |
| `POST` | `/auditor/recommendations/{id}/status` |  |
| `GET` | `/auditor/reports` |  |
| `POST` | `/auditor/reports/generate` |  |
| `GET` | `/auditor/sessions` |  |
| `GET` | `/auditor/validations` |  |
| `POST` | `/auditor/validations/{id}/opinion` |  |

