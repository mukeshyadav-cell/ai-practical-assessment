# Design Notes — Architecture & Design Decisions

**Date:** 2026-08-27  
**Purpose:** Record the architecture, key design decisions (with trade-offs), package layout, and known limitations for the AEM Support Ticket Management System.

**Sprint/Task:** 8.1 / 8.1.1

---

## Overview

The AEM Support Ticket Management System is a full-stack ticketing application on **AEM as a Cloud Service** (archetype 57). It supports create → triage → resolve → close workflows for support tickets with comments, assignee management, keyword search, and status filtering.

**Persistence:** Ticket and Comment entities are stored as **Content Fragments** under `/content/dam/assessment`. Users are **not** CF-backed — they are seeded AEM authorizables resolved via `UserManager`.

**API:** All data access is exposed through **Sling Servlets** at `/bin/api/v1/*` (JSON via Jackson). The UI is a **single-page (SPA-style)** TypeScript app compiled into clientlib `assessment.ticketing`.

**Layering (strict, top to bottom):**

```
UI (HTL + TypeScript clientlib)
  → Servlet (REST, thin)
    → Service (business logic, validation, state machine)
      → Repository (interface / port)
        → CF adapter (impl.type=contentfragment)
          → Content Fragments (JCR/DAM)
```

Users are read through `UserRepository` → `AemUserRepository` (`impl.type=aem`) → AEM `UserManager`.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Browser: /content/assessment/us/en/tickets.html  (?id= for detail view)    │
│  HTL: ticketapp → ticketlist | ticketdetail | ticketform                    │
│  TS clientlib: assessment.ticketing (ui.frontend → ui.apps)                │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ fetch /bin/api/v1/*  (relative paths)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SERVLET LAYER  com.mysite.core.servlets                                  │
│  TicketCollection, TicketById, TicketAssignee, TicketStatus,                │
│  CommentCollection, UserCollection, UserById, CurrentUser (/me)             │
│  + RoutingFilters for /{id}, /{id}/status, /{id}/assignee, /{id}/comments │
│  ServletResponseUtil: Jackson JSON + DomainException → HTTP status          │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SERVICE LAYER  com.mysite.core.services.impl                               │
│  TicketServiceImpl — create, list, update, reassign, changeStatus, search   │
│  CommentServiceImpl — addComment, listComments                              │
│  TicketStateMachine (pure POJO, new'd in service)                            │
│  TicketValidator / CommentValidator                                           │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ DTOs only (TicketDTO, CommentDTO, UserDTO)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  REPOSITORY PORTS  com.mysite.core.repositories                             │
│  TicketRepository | CommentRepository | UserRepository  (interfaces)        │
└───────────────┬─────────────────────────────┬───────────────────────────────┘
                │                             │
                ▼                             ▼
┌───────────────────────────────┐  ┌──────────────────────────────────────────┐
│  impl.type=contentfragment    │  │  impl.type=aem                           │
│  ContentFragmentTicketRepository│  │  AemUserRepository                       │
│  ContentFragmentCommentRepository│ │  (UserManager + service resolver)        │
└───────────────┬───────────────┘  └──────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  JCR / DAM                                                                   │
│  CFs: /content/dam/assessment/tickets/{ticketId}                            │
│       /content/dam/assessment/comments/{commentId}                          │
│  IDs: /var/assessment/ticket-id-counter, comment-id-counter                 │
│  CF models: /conf/assessment/settings/dam/cfm/models/ticket|comment           │
└─────────────────────────────────────────────────────────────────────────────┘

Future swap (designed, not implemented):
  @Reference(target="(impl.type=database)") TicketRepository dbRepo;
  → JDBC/JPA adapter; UI + servlets unchanged
```

**OSGi wiring:** Services inject repositories with `@Reference(target = "(impl.type=contentfragment)")` (tickets/comments) or `"(impl.type=aem)"` (users). CF repositories use service user `assessment-service` via `ResourceResolverFactory.getServiceResourceResolver()`.

---

## Key Design Decisions (with Trade-offs)

| Decision | Choice | Rationale | Trade-off |
|----------|--------|-----------|-----------|
| **Repository Pattern (Port/Adapter)** | Interface in `.repositories`; CF impl in `.repositories.impl` with `impl.type=contentfragment` | Mandatory-core requirement for future DB migration; services depend on ports only | Extra boilerplate (interfaces, mappers, OSGi targets) vs direct CF access in servlets |
| **Sling Servlets (not GraphQL)** | REST at `/bin/api/v1/*`; **no GraphQL** | Full CRUD + custom business rules (state machine, terminal-ticket edits) need explicit write endpoints; servlets map cleanly to service methods; GraphQL on AEM CF is primarily **read-oriented** and would still need custom mutations for status/assignee | More servlet classes and routing glue than a single GraphQL schema; manual api-contract maintenance |
| **Content Fragments + text-ID FKs** | Tickets/comments as CFs; `ticketId`, `assignedTo`, `createdBy` stored as **plain strings** | Native AEMaaCS headless storage for learning scope; text IDs mirror relational FKs for future DB adapter | CF write API is verbose; no referential integrity at JCR level; search/filter limited vs SQL |
| **State machine as pure domain logic** | `TicketStateMachine` — no AEM/Sling/JCR imports; `new TicketStateMachine()` in `TicketServiceImpl` | Fast, deterministic unit tests (62 tests, no mocks); single source of truth for transitions | UI duplicates transition map in `transitions.ts` (must stay in sync); enforcement only guaranteed server-side |
| **`changeStatus` as sole enforcement path** | Status changes only via `TicketService.changeStatus()` → `stateMachine.assertCanTransition()`; **not** via `PUT /tickets/{id}` body | Prevents bypassing the state machine through field updates; clear audit trail | Three ticket mutation endpoints instead of one generic PATCH |
| **Dedicated `/status` and `/assignee` sub-resources** | `PUT /tickets/{id}/status`, `PUT /tickets/{id}/assignee` separate from `PUT /tickets/{id}` (title/description/priority only) | Matches api-contract; explicit intent per operation; easier servlet authorization/logging later | More endpoints than a consolidated update API |
| **Comments on terminal tickets** | `Closed`/`Cancelled`: reject field update, reassign, status change (**FR-7**); **allow** add comment (**FR-13**, AC-42/AC-43) | Supports post-resolution discussion without reopening or editing closed records | Asymmetric rules — developers must remember comments are the only mutation on terminal tickets |
| **Client-side priority/sort vs server-side status/search** | **Server:** `?status=`, `?q=` (title search) on `GET /tickets`. **Client (6.2):** priority filter, sort (5 options), result count on fetched array | Status/search reduce payload early; sort/priority added as UI polish without backend sprint | Priority filter only applies to **already-fetched** tickets; combined filters can disagree with server if list grows large; no `?priority=` server param yet |
| **`GET /me` for `createdBy`** | `CurrentUserServlet` at `/bin/api/v1/me`; UI calls before create/comment | Fixes hardcoded `agent-1` TODO; uses authenticated AEM session user | Extra round-trip on page load; servlet trusts AEM login (no custom auth layer) |
| **Domain exception hierarchy → HTTP** | Abstract `DomainException` with `errorCode()` + `httpStatus()`; `ServletResponseUtil.handleException()` | Consistent JSON `{"error","code"}` per api-contract; servlets stay thin | Must keep exception catalog in sync with api-contract manually |

### Servlet routing note

The api-contract documents suffix-style paths (`/bin/api/v1/tickets/` + suffix). On the **local AEM SDK**, pure suffix servlet registration **failed** (`suffix=null`, HTML 404). Implementation uses **Sling `RoutingFilter`** classes (`TicketByIdRoutingFilter`, `TicketStatusRoutingFilter`, etc.) to dispatch sub-paths reliably. Trade-off: filters add code but behave consistently on local SDK and author.

### Jackson / `Instant` serialization

`ServletResponseUtil` registers `JavaTimeModule` and disables `WRITE_DATES_AS_TIMESTAMPS` so DTO `Instant` fields serialize as ISO-8601 strings in JSON. This was built into the first servlet task (5.1.1), not retrofitted after a production bug.

---

## Package Structure (`com.mysite.core.*`)

| Package | Responsibility | Key types |
|---------|----------------|-----------|
| `.dto` | Boundary POJOs | `TicketDTO`, `CommentDTO`, `UserDTO` |
| `.repositories` | Persistence ports | `TicketRepository`, `CommentRepository`, `UserRepository` |
| `.repositories.impl` | Adapters | `ContentFragmentTicketRepository`, `ContentFragmentCommentRepository`, `AemUserRepository` |
| `.mappers` | CF element ↔ DTO | `TicketMapper`, `CommentMapper` |
| `.services` / `.services.impl` | Business logic | `TicketService`, `TicketServiceImpl`, `CommentService`, `CommentServiceImpl` |
| `.statemachine` | Lifecycle rules | `TicketStatus`, `TicketStateMachine` |
| `.validation` | Input rules | `TicketValidator`, `CommentValidator` |
| `.exception` | Domain errors | `DomainException`, `InvalidTransitionException`, `ValidationException`, `TicketNotFoundException`, `TicketNotEditableException`, `UnknownUserException` |
| `.servlets` | REST adapters | `*Servlet`, `*RoutingFilter`, `ServletConstants`, `util.ServletResponseUtil` |
| `.util` | Shared helpers | `TimeUtil` (ISO-8601 ↔ `Instant`) |
| `.models` | Sling Models | Archetype `HelloWorldModel` (unused; not removed) |

No `ContentFragment`, `Resource`, or `ResourceResolver` types appear above the repository layer.

---

## Module Map

| Module | In scope? | Holds |
|--------|-----------|-------|
| **core** | Yes | OSGi bundle — all Java above |
| **ui.apps** | Yes | HTL components (`ticketapp`, `ticketlist`, `ticketdetail`, `ticketform`), clientlibs |
| **ui.apps.structure** | Yes | Allowed JCR roots |
| **ui.config** | Yes | Repoinit, service-user mapping, OSGi configs |
| **ui.content** | Yes | CFM definitions, site page `/content/assessment/us/en/tickets`, DAM scaffolding |
| **ui.frontend** | Yes | TypeScript/Webpack → `assessment.ticketing` clientlib |
| **all** | Yes | Container package for local/Cloud Manager deploy |
| **dispatcher** | Yes | AEMaaCS dispatcher config (validated locally; not required for author-only FRs) |
| **it.tests** | **No** | Archetype placeholder — unused |
| **ui.tests** | **No** | Cypress placeholder — unused |

**REST surface (implemented):**

| Method | Path | Servlet |
|--------|------|---------|
| GET, POST | `/bin/api/v1/tickets` | `TicketCollectionServlet` |
| GET, PUT | `/bin/api/v1/tickets/{id}` | `TicketByIdServlet` (+ filter) |
| PUT | `/bin/api/v1/tickets/{id}/assignee` | `TicketAssigneeServlet` (+ filter) |
| PUT | `/bin/api/v1/tickets/{id}/status` | `TicketStatusServlet` (+ filter) |
| GET, POST | `/bin/api/v1/tickets/{id}/comments` | `CommentCollectionServlet` (+ filter) |
| GET | `/bin/api/v1/users` | `UserCollectionServlet` |
| GET | `/bin/api/v1/users/{userId}` | `UserByIdServlet` (+ filter) |
| GET | `/bin/api/v1/me` | `CurrentUserServlet` |

---

## Known Limitations & Future Improvements

| Area | Current state | Improvement |
|------|---------------|-------------|
| **ID generation** | `TKT-{n}` / `CMT-{n}` via JCR property counter at `/var/assessment/*-counter`; **not atomic** under concurrency (documented in repository Javadoc) | Oak counter API or DB sequences with locking |
| **DB adapter** | Designed (`impl.type=database`) but **not implemented** | JDBC/JPA repository impl; same DTOs and service layer |
| **Server-side priority filter** | Priority filtered **client-side** only (Sprint 6.2) | Add `?priority=` query param to `GET /tickets` and repository `findByPriority` |
| **Pagination** | Full ticket list returned (assumption A-2) | Cursor/offset pagination on list endpoint |
| **Ticket delete** | Out of scope | Soft-delete flag if required later |
| **Integration / E2E tests** | `it.tests`, `ui.tests` unused | Cloud Manager custom functional/UI testing |
| **Archetype demo code** | `HelloWorldModel`, `SimpleServlet`, etc. still present | Remove when no longer needed for archetype reference |
| **Suffix servlet vs filter** | api-contract describes suffix registration; runtime uses routing filters on local SDK | Align contract doc with filter implementation or verify suffix works on Cloud Service |
| **Search scope** | Title-only, case-insensitive (`searchByTitle`) | Extend to description or full-text index |
| **AuthZ** | AEM login + CSRF on mutating requests; no per-ticket RBAC | Role-based assignee/transition rules if needed |

---

## Traceability

| Artifact | Sprint |
|----------|--------|
| Architecture baseline | 1.1 (`requirements-analysis.md`, `api-contract.md`, `data-model.md`) |
| Module map (initial) | 2.1.1 |
| Repository + state machine | 3.1, 4.1 |
| REST API | 5.1 |
| UI + `/me` | 6.1, 6.2 |
| Unit tests | 7.1 |
| This document (expanded) | 8.1.1 |
