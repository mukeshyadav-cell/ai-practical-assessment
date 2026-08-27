# Implementation Plan — Sprint & Task Tree

**Project:** AEM Support Ticket Management System
**Java base package:** com.mysite.core
**Content namespace:** assessment (/apps/assessment, /conf/assessment, /content/dam/assessment)
**Archetype:** AEM Project Archetype 57 (AEMaaCS)
**Frontend:** TypeScript via ui.frontend Webpack pipeline (plain JS permitted where simpler)
**REST base:** `/bin/api/v1`

---

## Current Status

- **Active Sprint:** 6.1
- **Active Task:** 6.1 — Quality Gate
- **Last Completed:** 6.1.6 — Reassign UI
- **Next:** Sprint 6.1 Quality Gate — full E2E browser verification

---

## Planning Artifacts Index (Sprint 1.1)

| Doc | Purpose |
|-----|---------|
| [requirements-analysis.md](requirements-analysis.md) | Mandatory-core scope, entities, FR-1–FR-19, state machine, assumptions |
| [acceptance-criteria.md](acceptance-criteria.md) | Testable AC-1–AC-51 + NFR-AC-1–6 in Given/When/Then format |
| [data-model.md](data-model.md) | CFM fields, DTOs, JCR paths, TKT/CMT ID strategy, DB mapping |
| [api-contract.md](api-contract.md) | REST endpoints, JSON shapes, error codes, servlet map |

---

## Cross-check reconciliation (1.1.5)

Planning docs were reconciled against this task tree. **Fixes applied:**

| Issue | Resolution |
|-------|------------|
| `3.1.2` mentioned ticket **delete** | Removed — ticket delete is out of scope ([requirements-analysis.md](requirements-analysis.md)) |
| `5.1.3` separate search/filter **endpoint** | Removed — `?status=` and `?q=` are query params on `GET /bin/api/v1/tickets` ([api-contract.md](api-contract.md)) |
| `5.1.1` lumped assignee/status into one servlet task | Split into `TicketAssigneeServlet`, `TicketStatusServlet` per api-contract sub-resources |
| User REST endpoints missing from Sprint 5.1 | Added `UserCollectionServlet`, `UserByIdServlet` (FR-16, FR-17) |
| CFM paths generic | Specified `/conf/assessment/settings/dam/cfm/models/ticket` and `comment` |
| CF data paths | `/content/dam/assessment/tickets`, `/content/dam/assessment/comments` |
| ID format | `TKT-{n}`, `CMT-{n}` counters under `/var/assessment/` ([data-model.md](data-model.md)) |
| Priority enum | `P1`–`P4` (not Low/Medium/High/Critical) |
| DTO JSON `id` vs CF `ticketId` | DTO field `id` maps from CF element `ticketId` |

---

## Sprint 1.1 — Planning & Analysis

- [x] 1.1.1 — [requirements-analysis.md](requirements-analysis.md) (FR-1–FR-19, state machine, scope)
- [x] 1.1.2 — [acceptance-criteria.md](acceptance-criteria.md) (AC-1–AC-51, NFR-AC-1–6)
- [x] 1.1.3 — [data-model.md](data-model.md) (CFMs, DTOs, persistence paths, validation)
- [x] 1.1.4 — [api-contract.md](api-contract.md) (REST contract, error catalog, servlet map)
- [x] 1.1.5 — [implementation-plan.md](implementation-plan.md) (finalize this file; traceability)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | All four planning docs exist at repo root and cross-reference consistently |
| DOD-2 | Every FR-1–FR-19 has ≥1 AC in acceptance-criteria.md |
| DOD-3 | data-model.md and api-contract.md agree on enums, paths, DTO fields |
| DOD-4 | implementation-plan.md task tree matches api-contract endpoints and data-model CFMs |
| DOD-5 | Prompts saved under `ai-prompts/planning/` for tasks 1.1.1–1.1.5 |
| DOD-6 | No application code, CFMs, or integration tests written in Sprint 1.1 |

**Quality Gate:** All planning docs complete, reviewed, mutually consistent → generate `prompt-history/sprint-1.1.md`.

---

## Sprint 2.1 — Project Scaffold & Foundation

- [x] 2.1.1 — Verify `mvn clean install` succeeds; document module map in `core/README.md` or design-notes stub
- [x] 2.1.2 — CFM **ticket** at `/conf/assessment/settings/dam/cfm/models/ticket` (elements: ticketId, title, description, priority, status, assignedTo, createdBy, createdAt, updatedAt)
- [x] 2.1.3 — CFM **comment** at `/conf/assessment/settings/dam/cfm/models/comment` (elements: commentId, ticketId, message, createdBy, createdAt)
- [x] 2.1.4 — Repoinit + OSGi: DAM folders `/content/dam/assessment/tickets`, `/comments`; service user `assessment-service`; seed users `agent-1`, `agent-2` (ui.config)
- [x] 2.1.5 — DTOs: `com.mysite.core.dto.TicketDTO`, `CommentDTO`, `UserDTO` (`Instant` timestamps, nullable `assignedTo`)
- [x] 2.1.6 — Repository interfaces: `com.mysite.core.repositories.TicketRepository`, `CommentRepository`, `UserRepository` (CRUD/query method signatures from data-model)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | `mvn clean install` passes; package deploys to local AEM SDK |
| DOD-2 | Ticket + Comment CFMs visible in AEM CF model console |
| DOD-3 | DAM folders exist; `cq:conf` points to `/conf/assessment` |
| DOD-4 | Service user mapping active; bundle `core` is **Active** in OSGi console |
| DOD-5 | DTO + repository interface classes compile in `core` module |

**Quality Gate:** Project builds, deploys to local SDK, bundle is Active.

---

## Sprint 3.1 — Repository Layer (Content Fragment adapters)

- [x] 3.1.1 — `TicketRepository` CF impl (`impl.type=contentfragment`): `getAll`, `getById`, `findByStatus`, `searchByTitle` (case-insensitive)
- [x] 3.1.2 — `TicketRepository` CF write: `create`, `update` + `TKT-{n}` ID generation (`/var/assessment/ticket-id-counter`); **no delete**
- [x] 3.1.3 — `CommentRepository` CF impl: `add`, `listByTicket` + `CMT-{n}` ID generation (`/var/assessment/comment-id-counter`)
- [x] 3.1.4 — `UserRepository` AEM impl: `getById`, `getAll`, `search` via `UserManager` (skip system users)
- [x] 3.1.5 — Mappers: `com.mysite.core.util` (or `.mappers`) CF element ↔ DTO (`ticketId`→`id`, ISO-8601 text↔`Instant`)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | Create ticket CF at `/content/dam/assessment/tickets/TKT-1001` via repository harness or unit test |
| DOD-2 | Read back ticket + add comment CF at `/content/dam/assessment/comments/CMT-1001` |
| DOD-3 | No `ContentFragment`/`Resource` types leak above repository layer |
| DOD-4 | Service user resolver used (not administrative resolver) |

**Quality Gate:** Can create and read a Ticket + Comment via a test/harness.

---

## Sprint 4.1 — State Machine + Services

- [x] 4.1.1 — `com.mysite.core.statemachine.TicketStateMachine` + `com.mysite.core.exception.InvalidTransitionException`
- [x] 4.1.2 — `com.mysite.core.exception` domain exceptions: validation, not-found, ticket-not-editable (maps to api-contract error codes)
- [x] 4.1.3 — `TicketService`: create, list (`createdAt` desc), get, update fields, reassign, `changeStatus`, search/filter (`q` + `status`)
- [x] 4.1.4 — `CommentService`: add comment, list by ticket (`createdAt` asc); allow on Closed/Cancelled
- [x] 4.1.5 — Validation: required fields, P1–P4 / status enums, known assignee, terminal-ticket edit rules (FR-7)
- [x] 4.1.6 — Unit tests: `TicketStateMachineTest` — all valid transitions + representative invalid (AC-22–AC-35)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | Every valid transition has a passing unit test |
| DOD-2 | Representative invalid transitions rejected in unit tests |
| DOD-3 | `TicketService.changeStatus` delegates to `TicketStateMachine` only |
| DOD-4 | Validation runs before persistence for create/update/reassign/status |

**Quality Gate:** State-machine unit tests pass (valid + invalid transitions).

---

## Sprint 5.1 — REST API (Servlets)

All servlets in `com.mysite.core.servlets`; register via `sling.servlet.paths`; JSON via Jackson.

- [x] 5.1.1 — `TicketCollectionServlet` — `GET`/`POST` `/bin/api/v1/tickets` (list with `?status=`, `?q=`; create → 201)
- [x] 5.1.2 — `TicketByIdServlet` — `GET`/`PUT` `/bin/api/v1/tickets/{id}` (detail; update title/description/priority only)
- [x] 5.1.3 — `TicketAssigneeServlet` — `PUT` `/bin/api/v1/tickets/{id}/assignee`
- [x] 5.1.4 — `TicketStatusServlet` — `PUT` `/bin/api/v1/tickets/{id}/status` (409 on invalid transition)
- [x] 5.1.5 — `CommentCollectionServlet` — `GET`/`POST` `/bin/api/v1/tickets/{id}/comments`
- [x] 5.1.6 — `UserCollectionServlet` — `GET` `/bin/api/v1/users` (`?q=` optional); `UserByIdServlet` — `GET` `/bin/api/v1/users/{userId}`
- [x] 5.1.7 — Shared error handling: JSON `{ "error", "code" }`; map 400/404/409/500 per [api-contract.md](api-contract.md)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | All ten api-contract endpoints respond via curl/Postman |
| DOD-2 | Create ticket → 201; create comment → 201 |
| DOD-3 | Invalid transition → 409 `INVALID_TRANSITION`; unknown user → 400 `UNKNOWN_USER` |
| DOD-4 | Terminal ticket update/reassign → 400 `TICKET_NOT_EDITABLE` |
| DOD-5 | Every servlet sets `Content-Type: application/json` |

**Quality Gate:** All endpoints verified via Postman/curl.

---

## Sprint 6.1 — UI (HTL + TypeScript)

Single-page (SPA-style) app on a static template. Clientlib category: `assessment.ticketing`.
Components under `/apps/assessment/components`. TypeScript source in `ui.frontend`, compiled
into the clientlib. App URL: `/content/assessment/us/en/tickets.html`. UI consumes the
existing REST API only — NO backend changes.

- [x] 6.1.0 — UI scaffold: static template + tickets page + clientlib (`assessment.ticketing`)
              + component skeletons (`ticketapp`, `ticketlist`, `ticketdetail`, `ticketform`)
              + TS entry that proves API wiring (logs ticket count) + view-switching stub (`?id=`)
- [x] 6.1.1 — `ticketlist` component + TS: `GET /bin/api/v1/tickets` with search (`q`) and status filter
- [x] 6.1.2 — `ticketdetail` component + TS: `GET /bin/api/v1/tickets/{id}` + comment list
- [x] 6.1.3 — `ticketform` component + TS: create/edit; assignee from `GET /bin/api/v1/users`
- [x] 6.1.4 — add-comment UI: `POST /bin/api/v1/tickets/{id}/comments`
- [x] 6.1.5 — Status change UI: `PUT /bin/api/v1/tickets/{id}/status` — only valid next statuses shown (FR-10)
- [x] 6.1.6 — Reassign UI: `PUT /bin/api/v1/tickets/{id}/assignee`; relative API paths only (FR-19)

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-0 | Tickets page renders on the static template; clientlib loads; TS reaches the API (console shows ticket count); `?id=` view-switching stub works |
| DOD-1 | End-to-end flow: list → create → detail → comment → status change → close |
| DOD-2 | Loading, empty, and error states handled in TS fetch calls |
| DOD-3 | User content escaped in DOM (XSS) |
| DOD-4 | No hardcoded hostnames or secrets in clientlibs |

**Quality Gate:** Full user flow works end-to-end in the browser.

---

## Sprint 7.1 — Unit Tests (Java, core module)
- [ ] 7.1.1 — State machine unit tests: all VALID transitions succeed
- [ ] 7.1.2 — State machine unit tests: INVALID transitions rejected (InvalidTransitionException)
- [ ] 7.1.3 — TicketService unit tests (create defaults Open, validation, reassign, changeStatus)
- [ ] 7.1.4 — CommentService unit tests (empty message rejected, non-existent ticket rejected)
**Quality Gate:** All unit tests green via `mvn test`. it.tests/ui.tests remain unused.

---

## Sprint 8.1 — Documentation & Reflection

- [ ] 8.1.1 — [design-notes.md](design-notes.md), [ui-flow.md](ui-flow.md), [test-strategy.md](test-strategy.md), [debugging-notes.md](debugging-notes.md)
- [ ] 8.1.2 — [code-review-notes.md](code-review-notes.md), [pr-description.md](pr-description.md)
- [ ] 8.1.3 — [README.md](README.md) (project setup), [candidate-info.md](candidate-info.md), [tool-workflow.md](tool-workflow.md)
- [ ] 8.1.4 — [reflection.md](reflection.md), [final-ai-usage-summary.md](final-ai-usage-summary.md)
- [ ] 8.1.5 — Consolidate `ai-prompts/` folders; verify prompt-history index

**Definition of Done**

| Check | Criterion |
|-------|-----------|
| DOD-1 | All lifecycle artifacts listed above exist with date + purpose header |
| DOD-2 | README setup verified from scratch on clean SDK install |
| DOD-3 | `prompt-history/README.md` index updated for all sprints |

**Quality Gate:** All lifecycle artifacts complete; README setup verified from scratch.

---

## Traceability — Sprint → FR / AC

| Sprint | Delivers (FR) | Key AC / NFR coverage |
|--------|---------------|------------------------|
| **1.1** | Planning baseline for FR-1–FR-19 | All AC-1–AC-51 defined; NFR-AC-1–6 |
| **2.1** | Foundation for FR-18 (structure) | CFMs/DTOs enable all FRs |
| **3.1** | Persistence for FR-1–FR-17 data | NFR-AC-4 (CF persistence path) |
| **4.1** | FR-1–FR-17 business logic; FR-8–FR-10 state machine | AC-22–AC-36 (unit level); validation AC-3–AC-6 |
| **5.1** | FR-1–FR-18 REST exposure | AC-1–AC-50; NFR-AC-1, NFR-AC-2, NFR-AC-5 |
| **6.1** | FR-10 UI transitions; FR-19 UI API usage | AC-36, AC-51 |
| **7.1** | Verification of all FRs via HTTP | AC-1–AC-51; NFR-AC-1–6 |
| **8.1** | Documentation & process traceability | Sprint logs; lifecycle artifacts |

### FR delivery by implementation sprint

| FR | Primary sprint(s) | Verification |
|----|-------------------|--------------|
| FR-1 | 4.1, 5.1 | AC-1–AC-6; 7.1.3 |
| FR-2 | 4.1, 5.1 | AC-7, AC-8; 7.1.3 |
| FR-3 | 4.1, 5.1 | AC-9, AC-10; 7.1.3 |
| FR-4 | 4.1, 5.1 | AC-11, AC-12; 7.1.5 |
| FR-5 | 4.1, 5.1 | AC-13–AC-15; 7.1.3 |
| FR-6 | 4.1, 5.1 | AC-18, AC-19; 7.1.3 |
| FR-7 | 4.1, 5.1 | AC-16, AC-17, AC-20, AC-21; 7.1.3 |
| FR-8 | 4.1, 5.1 | AC-22–AC-26; 7.1.1 |
| FR-9 | 4.1, 5.1 | AC-27–AC-35; 7.1.2 |
| FR-10 | 4.1, 5.1, 6.1 | AC-36; 6.1.5 |
| FR-11 | 4.1, 5.1 | AC-37–AC-39; 7.1.5 |
| FR-12 | 4.1, 5.1 | AC-40; 7.1.5 |
| FR-13 | 4.1, 5.1 | AC-41–AC-43; 7.1.4 |
| FR-14 | 4.1, 5.1 | AC-44; 7.1.4 |
| FR-15 | 4.1, 5.1 | AC-45, AC-46; 7.1.4 |
| FR-16 | 3.1, 4.1, 5.1 | AC-47; 7.1.6 |
| FR-17 | 3.1, 4.1, 5.1 | AC-48, AC-49; 7.1.6 |
| FR-18 | 5.1 | AC-50; all `*IT.java` |
| FR-19 | 6.1 | AC-51; code review 8.1.2 |

---

## Sprint Numbering

- Use X.1 per phase (1.1, 2.1, …). Split to X.2 only if a phase genuinely grows.
- Each sprint ends with a Quality Gate + generated `prompt-history/sprint-X.X.md`.

## Namespace Reminder

| Layer | Location |
|-------|----------|
| Java code | `com.mysite.core.*` (`core` module) |
| Servlets | `com.mysite.core.servlets` |
| DTOs | `com.mysite.core.dto` |
| Repositories | `com.mysite.core.repositories` / `.repositories.impl` |
| CFM models | `/conf/assessment/settings/dam/cfm/models` |
| Ticket CFs | `/content/dam/assessment/tickets/{ticketId}` |
| Comment CFs | `/content/dam/assessment/comments/{commentId}` |
| HTL / clientlibs | `/apps/assessment` |
| OSGi config | `ui.config` → `/apps/assessment/osgiconfig` |

Never mix Java package names with JCR content paths.
