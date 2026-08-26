# Design Notes — AEM Support Ticket Management System

**Date:** 2025-08-26  
**Purpose:** Architecture decisions, module layout, and implementation guidance for the assessment project.

**Sprint/Task:** 2.1 / 2.1.1

---

## Build Verification (Task 2.1.1)

| Check | Result |
|-------|--------|
| Command | `mvn clean install` |
| Status | **SUCCESS** (verified locally by developer) |
| Archetype | AEM Project Archetype 57 (AEMaaCS) |
| AEM SDK API | `2026.8.27673.20260811T193135Z-260700` (parent `pom.xml`) |

Full reactor build includes `core`, `ui.frontend`, `ui.apps`, `ui.apps.structure`, `ui.config`, `ui.content`, `all`, `dispatcher`, `it.tests`, and `ui.tests` modules. Only in-scope modules contribute application artifacts; see Testing Scope below.

---

## Module Map

Maven multi-module layout for **com.mysite** / **assessment** namespace. Artifact paths align with [data-model.md](data-model.md) and [api-contract.md](api-contract.md).

| Module | In scope? | Purpose (this project) | Where artifacts live |
|--------|-----------|------------------------|----------------------|
| **all** | Yes | Container / deployment package — bundles `core` OSGi jar and all FileVault content packages (`ui.apps`, `ui.config`, `ui.content`, etc.) into a single installable package for local SDK and Cloud Manager. | `all/target/*.zip` → installs to AEM author/publish |
| **core** | Yes | Java OSGi bundle: `com.mysite.core` — DTOs, repository interfaces + CF adapters, services, state machine, domain exceptions, REST servlets, Sling Models, mappers. | See **core package map** below |
| **ui.apps** | Yes | HTL components, dialogs, and clientlibs under `/apps/assessment`. | `/apps/assessment/components/*` (e.g. `ticket-list`, `ticket-detail`, `ticket-form`, `comment-list` — Sprint 6.1); `/apps/assessment/clientlibs/*` (category `assessment.ticketing`) |
| **ui.apps.structure** | Yes | Repository structure package — declares allowed JCR roots before code packages deploy (empty content, filter definitions only). | Filter roots: `/apps/assessment`, `/content/dam/assessment`, overlay roots — `ui.apps.structure/pom.xml` |
| **ui.config** | Yes | Runmode OSGi configurations (repoinit, service-user mapping, logging, CORS). | `/apps/assessment/osgiconfig/config*` — e.g. `RepositoryInitializer~assessment`, `ServiceUserMapper` (Sprint 2.1.4) |
| **ui.content** | Yes | Mutable content: CFM definitions, DAM folder scaffolding, site/templates. | CFMs: `/conf/assessment/settings/dam/cfm/models/ticket`, `comment` (Sprint 2.1.2–2.1.3); DAM data folders: `/content/dam/assessment/tickets`, `/content/dam/assessment/comments`; site: `/content/assessment` |
| **ui.frontend** | Yes | TypeScript/Webpack build pipeline; compiles TS/SCSS and copies output into `ui.apps` clientlibs via `clientlib.config.js`. | Source: `ui.frontend/src/main/webpack/`; output → `ui.apps/.../apps/assessment/clientlibs/` |
| **dispatcher** | Yes | AEM as a Cloud Service dispatcher configuration (caching, filters, farms). Validated locally with Dispatcher SDK. | `dispatcher/src/` — not required for mandatory-core FRs on local author-only dev |
| **it.tests** | **No** | AEM integration tests (HTTP clients against running author). Present from archetype; **unused** for this assessment. | `it.tests/src/main/java` — do not implement |
| **ui.tests** | **No** | Cypress E2E UI tests. Present from archetype; **unused** for this assessment. | `ui.tests/` — do not implement |

### core package map (`core/src/main/java/com/mysite/core/`)

| Package | Planned artifacts | JCR / API tie-in |
|---------|-------------------|------------------|
| `.dto` | `TicketDTO`, `CommentDTO`, `UserDTO` | JSON shapes in [api-contract.md](api-contract.md); `id` maps from CF `ticketId` / `commentId` |
| `.repositories` | `TicketRepository`, `CommentRepository`, `UserRepository` (interfaces) | Persistence contract per [data-model.md](data-model.md) |
| `.repositories.impl` | CF adapters (`impl.type=contentfragment`) | Read/write CFs at `/content/dam/assessment/tickets/{ticketId}`, `/content/dam/assessment/comments/{commentId}` |
| `.services` / `.services.impl` | `TicketService`, `CommentService` | Business logic, validation, state-machine orchestration |
| `.statemachine` | `TicketStateMachine` | Enforces transitions per [requirements-analysis.md](requirements-analysis.md) §4 |
| `.exception` | `InvalidTransitionException`, validation/not-found exceptions | Maps to HTTP 400/404/409 per api-contract error catalog |
| `.servlets` | `TicketCollectionServlet`, `TicketByIdServlet`, `TicketAssigneeServlet`, `TicketStatusServlet`, `CommentCollectionServlet`, `UserCollectionServlet`, `UserByIdServlet` | `sling.servlet.paths` under `/bin/api/v1/*` |
| `.util` (or `.mappers`) | CF element ↔ DTO mappers | ISO-8601 text ↔ `java.time.Instant` |
| `.models` | Sling Models for HTL (if needed) | Component data binding in Sprint 6.1 |

**Application metadata (repository layer):** ID counters at `/var/assessment/ticket-id-counter`, `/var/assessment/comment-id-counter` (Sprint 3.1).

**Users:** Not CF-backed — resolved via `UserManager`; seeded users `agent-1`, `agent-2` via repoinit in `ui.config` (Sprint 2.1.4).

### REST surface (servlets → services)

All browser/integration access via `/bin/api/v1` only ([api-contract.md](api-contract.md)):

| Path | Servlet (planned) |
|------|-------------------|
| `GET/POST /bin/api/v1/tickets` | `TicketCollectionServlet` |
| `GET/PUT /bin/api/v1/tickets/{id}` | `TicketByIdServlet` |
| `PUT /bin/api/v1/tickets/{id}/assignee` | `TicketAssigneeServlet` |
| `PUT /bin/api/v1/tickets/{id}/status` | `TicketStatusServlet` |
| `GET/POST /bin/api/v1/tickets/{id}/comments` | `CommentCollectionServlet` |
| `GET /bin/api/v1/users` | `UserCollectionServlet` |
| `GET /bin/api/v1/users/{userId}` | `UserByIdServlet` |

---

## Testing Scope

| Layer | Module / path | Used? | Notes |
|-------|---------------|-------|-------|
| Java unit tests | `core/src/test/java` | **Yes** | State machine (`TicketStateMachineTest`), service validation, servlet unit tests where appropriate |
| Integration tests | `it.tests` | **No** | Out of scope — archetype placeholder only; Sprint 7.1 tasks in implementation plan are **not** executed |
| UI E2E (Cypress) | `ui.tests` | **No** | Out of scope — Cypress not used |

**Policy:** Only `mvn test` (core module unit tests) is part of the assessment test strategy. `it.tests` and `ui.tests` remain in the reactor for archetype compatibility but are not developed or relied upon.

---

## Build & Run

| Action | Command |
|--------|---------|
| Full build | `mvn clean install` |
| Build + deploy to local SDK author | `mvn clean install -PautoInstallSinglePackage` |
| Run unit tests only | `mvn test` |
| Build frontend only | `cd ui.frontend && npm run build` |
| Validate dispatcher config | `cd dispatcher && ./bin/validate.sh src` |

### Java version

| Topic | Value |
|-------|-------|
| Cloud Manager / local target | **Java 21** (`.cloudmanager/java-version`) |
| Source compatibility | **Java 17** — code must not use Java 21-only preview features ([requirements-analysis.md](requirements-analysis.md)) |
| Maven compiler `release` | `11` in parent `pom.xml` (archetype default; aligns with Java 17+ bytecode) |

Local AEM SDK author defaults: `localhost:4502` (`aem.host` / `aem.port` in parent POM).

---

## Archetype Sample Code — Keep / Remove

Archetype 57 ships demo classes and matching tests/components. **Do not delete in Task 2.1.1** — track for replacement in later sprints.

| Class / artifact | Location | Action | Reason |
|------------------|----------|--------|--------|
| `HelloWorldModel` | `core/.../models/HelloWorldModel.java` | **Remove** (later) | Archetype demo Sling Model; replaced by ticketing Sling Models if needed (Sprint 6.1) |
| `HelloWorldModelTest` | `core/src/test/.../HelloWorldModelTest.java` | **Remove** (later) | Test for removed demo model |
| `SimpleServlet` | `core/.../servlets/SimpleServlet.java` | **Remove** (later) | Demo servlet (`resourceTypes` binding); replaced by `/bin/api/v1/*` servlets per [api-contract.md](api-contract.md) (Sprint 5.1) |
| `SimpleServletTest` | `core/src/test/.../SimpleServletTest.java` | **Remove** (later) | Test for removed demo servlet |
| `SimpleScheduledTask` | `core/.../schedulers/SimpleScheduledTask.java` | **Remove** (later) | Archetype scheduler sample; no scheduled jobs in mandatory core |
| `SimpleScheduledTaskTest` | `core/src/test/.../SimpleScheduledTaskTest.java` | **Remove** (later) | Test for removed scheduler |
| `LoggingFilter` | `core/.../filters/LoggingFilter.java` | **Remove** (later) | Archetype request filter demo; not required for ticketing API |
| `LoggingFilterTest` | `core/src/test/.../LoggingFilterTest.java` | **Remove** (later) | Test for removed filter |
| `SimpleResourceListener` | `core/.../listeners/SimpleResourceListener.java` | **Remove** (later) | Archetype JCR listener demo; persistence via repository layer, not listeners |
| `SimpleResourceListenerTest` | `core/src/test/.../SimpleResourceListenerTest.java` | **Remove** (later) | Test for removed listener |
| `helloworld` component | `ui.apps/.../components/helloworld/` | **Remove** (later) | Archetype HTL demo; replaced by `ticket-list`, `ticket-detail`, etc. |
| `_helloworld.js` / `_helloworld.scss` | `ui.frontend/src/main/webpack/components/` | **Remove** (later) | Frontend assets for demo component |

**Keep (for now):** `AppAemContext` test utility (`core/src/test/.../testcontext/AppAemContext.java`) — reuse for unit tests until replaced.

---

## Traceability

| Artifact | Sprint |
|----------|--------|
| Module map (this section) | 2.1.1 |
| CFM definitions | 2.1.2, 2.1.3 |
| Repoinit + OSGi | 2.1.4 |
| DTOs | 2.1.5 |
| Repository interfaces | 2.1.6 |
