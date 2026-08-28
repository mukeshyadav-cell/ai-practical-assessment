# AEM Support Ticket Management System

**Purpose:** Primary project document — setup, architecture overview, API reference, and documentation index for the AEMaaCS Support Ticket Management System (`com.mysite.core` / `assessment`).

---

A full-stack **Support Ticket Management System** on **Adobe Experience Manager as a Cloud Service** (AEM Project Archetype 57). Agents create and triage tickets, add comments, reassign work, and move tickets through a strict lifecycle state machine. Data is persisted in **Content Fragments**, exposed via **Sling Servlets** at `/bin/api/v1/*`, and consumed by a **TypeScript** UI compiled into clientlib `assessment.ticketing`.

---

## Features

### Mandatory core

| Capability | Description |
|------------|-------------|
| Create ticket | Title, description, priority (P1–P4), optional assignee; initial status **Open** |
| List tickets | Newest first (`createdAt` desc) |
| View detail | Single ticket by id |
| Update fields | Title, description, priority (not on Closed/Cancelled) |
| Reassign | Change assignee (not on terminal tickets) |
| Comments | Add and list comments (allowed on **all** statuses including Closed/Cancelled) |
| Keyword search | Case-insensitive **title** search (`?q=`) |
| Status filter | Server-side (`?status=`) |
| State machine | Enforced server-side; invalid transitions → HTTP **409** |

### Stretch enhancements (Sprint 6.2)

Client-side **sort** (5 options), **priority filter**, result count summary, **`GET /me`** for authenticated `createdBy`, terminal transition **confirmations**, and action **toasts**.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Platform | AEM as a Cloud Service SDK (archetype 57; API `2026.8.27673.20260811T193135Z-260700` in parent `pom.xml`) |
| Java | **Java 21** for Cloud Manager / local (`.cloudmanager/java-version`); bytecode `release` **11** in Maven; code kept **Java 17–compatible** (no Java 21 preview features) |
| Build | Maven multi-module |
| Persistence | Content Fragments (`ticket`, `comment` models under `/conf/assessment`) |
| API | Sling Servlets, JSON via Jackson |
| UI | HTL + TypeScript (`ui.frontend` Webpack → `assessment.ticketing` clientlib) |
| Users | AEM `UserManager` (seeded `agent-1`, `agent-2`) — not CF-backed |

---

## Architecture

Strict layering: **UI → Servlet → Service → Repository (interface) → CF adapter → Content Fragments**.

The **Repository Pattern** (`impl.type=contentfragment`) keeps persistence swappable to a future database adapter without changing servlets or UI.

See [design-notes.md](design-notes.md) for diagrams, design decisions, and trade-offs.

---

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| **JDK 21** | Matches Cloud Manager (`.cloudmanager/java-version`). JDK 17+ may work for compile; 21 recommended for local parity. |
| **Apache Maven 3.8+** | Builds all modules; `frontend-maven-plugin` installs **Node v16.17.0** and **npm 8.15.0** automatically during `mvn install` — separate Node install not required if the full Maven build succeeds. |
| **AEM SDK author** | Local Quickstart running at **`http://localhost:4502`** before deploy (`aem.host` / `aem.port` in parent `pom.xml`). |
| **Disk / RAM** | Standard AEM SDK requirements (Adobe documentation). |

Default package-manager credentials used by Maven deploy profile: `admin` / `admin` (`vault.user` / `vault.password` in `pom.xml`). **Local SDK defaults only — not for production.**

---

## Setup & Run

### Step 1 — Start AEM SDK author

Start your local AEM as a Cloud Service SDK author instance and confirm it responds at:

```
http://localhost:4502
```

> **Verify:** Login page loads. AEM must be running **before** Step 2.

### Step 2 — Build and deploy

From the repository root:

```bash
mvn clean install -PautoInstallSinglePackage
```

This command:

1. Compiles the `core` OSGi bundle
2. Runs `npm` / Webpack in `ui.frontend` (via `frontend-maven-plugin`)
3. Builds FileVault packages (`ui.apps`, `ui.config`, `ui.content`, …)
4. Installs the `all` container package to `http://localhost:4502`

**Expected:** `BUILD SUCCESS`. In OSGi console (`/system/console/components`), bundle `ai-practical-assessment.core` is **Active**.

### Step 3 — What gets provisioned automatically

| Artifact | Source | Path / detail |
|----------|--------|----------------|
| DAM folders | Repoinit (`ui.config`) | `/content/dam/assessment/tickets`, `/comments` |
| Service user | Repoinit | `assessment-service` + ACLs on DAM, `/var/assessment`, `/conf/assessment`, `/home/users` (read) |
| Service-user mapping | OSGi (`ui.config`) | `ai-practical-assessment.core:assessment-service=[assessment-service]` |
| Seed users | Repoinit | `agent-1`, `agent-2` with profile email (for assignee picker) |
| CFM models | Content package (`ui.content`) | `/conf/assessment/settings/dam/cfm/models/ticket`, `comment` |
| Ticketing page + template | Content package (`ui.content`) | Page `/content/assessment/us/en/tickets`; template `ticketing-page` with `ticketapp` in **structure** |
| REST servlets | `core` bundle | `/bin/api/v1/*` |

**Demo seed-user passwords** (repoinit placeholder — **not real secrets**):

```
agent-1 / changeme-local-dev-only
agent-2 / changeme-local-dev-only
```

> **Verify:** After deploy, confirm users exist in AEM Security UI and repoinit ran without errors in `/system/console/slinglog`.

### Step 4 — Seed sample tickets (manual — not auto-loaded)

**No sample ticket Content Fragments are shipped** in `ui.content`. The ticket list is empty after a fresh deploy until you create data.

**Option A — curl (fastest for reviewers)**

Uses HTTP Basic auth (`admin:admin` local default). `createdBy` is set server-side from the authenticated user (will be `admin` when using admin credentials).

```bash
# Create first ticket (expect HTTP 201; first id is typically TKT-1001)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"Login issue","description":"User cannot reset password","priority":"P1","assignedTo":"agent-1"}'

# Create second ticket
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"Billing question","description":"Invoice mismatch for March","priority":"P3","assignedTo":"agent-2"}'

# Confirm list
curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets
```

**Option B — UI**

Open the app (Step 5), click **+ New Ticket**, fill the form, and save.

> **Verify:** `GET /bin/api/v1/tickets` returns JSON array; CF assets appear under `/content/dam/assessment/tickets/TKT-*` in CRXDE.

### Step 5 — Open the application

1. Log in to AEM author:
   - **`admin` / `admin`** (local SDK default), or
   - **`agent-1` / `changeme-local-dev-only`** for realistic `createdBy` / assignee UX
2. Open:

```
http://localhost:4502/content/assessment/us/en/tickets.html
```

3. Confirm the ticket list loads (after Step 4), clientlib `assessment.ticketing` is present in DevTools, and `GET /bin/api/v1/tickets` appears in the Network tab.

---

## Running Tests

```bash
mvn test -pl core
```

| Test class | Covers |
|------------|--------|
| `TicketStateMachineTest` | All valid/invalid transitions (AC-22–AC-35) |
| `TicketServiceImplTest` | Create, validation, update, reassign, `changeStatus`, list/search |
| `CommentServiceImplTest` | Add/list comments, terminal-ticket comments, validation |

`it.tests` and `ui.tests` are **not used** for this assessment. See [test-strategy.md](test-strategy.md).

---

## API Reference (summary)

Full contract: [api-contract.md](api-contract.md)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/bin/api/v1/tickets` | List tickets (`?status=`, `?q=`) |
| POST | `/bin/api/v1/tickets` | Create ticket |
| GET | `/bin/api/v1/tickets/{id}` | Ticket detail |
| PUT | `/bin/api/v1/tickets/{id}` | Update title, description, priority |
| PUT | `/bin/api/v1/tickets/{id}/assignee` | Reassign |
| PUT | `/bin/api/v1/tickets/{id}/status` | Change status (state machine) |
| GET | `/bin/api/v1/tickets/{id}/comments` | List comments |
| POST | `/bin/api/v1/tickets/{id}/comments` | Add comment |
| GET | `/bin/api/v1/users` | List assignable users |
| GET | `/bin/api/v1/users/{userId}` | User detail |
| GET | `/bin/api/v1/me` | Current AEM user |

### Example curls

**Create ticket:**

```bash
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"API smoke test","description":"Created from README","priority":"P2"}'
```

**Valid status transition (200)** — ticket must be in **Open**:

```bash
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"In Progress"}'
```

**Invalid transition (409)** — Open → Closed skips workflow:

```bash
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Closed"}'
```

Expected: `{"error":"Invalid transition: Open -> Closed","code":"INVALID_TRANSITION"}`

> **Browser UI note:** POST/PUT from the logged-in UI require an AEM **CSRF token** (`/libs/granite/csrf/token.json`). curl with Basic auth on local author typically does not need CSRF — **verify on your SDK if POST returns 403**.

---

## Project Structure

| Module | Purpose |
|--------|---------|
| **core** | OSGi bundle — DTOs, repositories, services, state machine, servlets |
| **ui.apps** | HTL components, clientlibs (`/apps/assessment`) |
| **ui.apps.structure** | Allowed JCR roots |
| **ui.config** | Repoinit, service-user mapping, OSGi configs |
| **ui.content** | CFM definitions, site page, templates |
| **ui.frontend** | TypeScript/Webpack source → clientlib output |
| **all** | Container package for single-shot deploy |
| **dispatcher** | AEMaaCS dispatcher config |
| **it.tests** | Unused (archetype placeholder) |
| **ui.tests** | Unused (Cypress placeholder) |

Package map and REST servlet list: [design-notes.md](design-notes.md).

---

## State Machine

Allowed transitions only — all others rejected with HTTP **409**.

| From | To | Allowed |
|------|-----|---------|
| Open | In Progress | Yes |
| In Progress | Resolved | Yes |
| Resolved | Closed | Yes |
| Open | Cancelled | Yes |
| In Progress | Cancelled | Yes |
| *All other pairings* | | **No** |

**Terminal states:** Closed, Cancelled — no further status changes. Field updates and reassign are blocked; **comments remain allowed**.

Enforced in `com.mysite.core.statemachine.TicketStateMachine` via `TicketService.changeStatus()` only.

---

## Known Limitations & Out of Scope

| Item | Notes |
|------|-------|
| `it.tests` / `ui.tests` | Not developed; manual curl/browser verification |
| DB adapter | Designed (`impl.type=database`); not implemented |
| ID counter | `TKT-{n}` / `CMT-{n}` via JCR property — **not atomic** under concurrency |
| Auth / RBAC | AEM login only; no per-ticket authorization |
| Pagination | Full list returned |
| Ticket delete | Not in mandatory core |
| Server-side priority filter | Client-side only (Sprint 6.2) |

---

## Documentation Index

### Planning (Sprint 1.1)

| Document | Purpose |
|----------|---------|
| [requirements-analysis.md](requirements-analysis.md) | FR-1–FR-19, scope, state machine |
| [acceptance-criteria.md](acceptance-criteria.md) | AC-1–AC-51 |
| [data-model.md](data-model.md) | CFMs, DTOs, paths |
| [api-contract.md](api-contract.md) | REST contract |
| [implementation-plan.md](implementation-plan.md) | Sprint/task tree |

### Lifecycle (Sprint 8.1)

| Document | Purpose |
|----------|---------|
| [design-notes.md](design-notes.md) | Architecture & design decisions |
| [ui-flow.md](ui-flow.md) | UI screens & journeys |
| [test-strategy.md](test-strategy.md) | Testing approach |
| [debugging-notes.md](debugging-notes.md) | Issues & resolutions |
| [code-review-notes.md](code-review-notes.md) | Self code review |
| [pr-description.md](pr-description.md) | PR description |
| [candidate-info.md](candidate-info.md) | Candidate details |
| [tool-workflow.md](tool-workflow.md) | AI tool workflow |

### Prompt history

| Sprint | File |
|--------|------|
| 1.1–7.1 | [prompt-history/README.md](prompt-history/README.md) |

Categorized prompts: `ai-prompts/` (`planning/`, `implementation/`, `testing/`, `debugging/`, `design/`, `code-review/`, `documentation/`).

---

## Security Note

This README uses **local SDK default credentials** (`admin:admin`) and **demo seed passwords** (`changeme-local-dev-only`) for setup instructions only. Do not use these in production. No API keys or real secrets are stored in application source.

---

## Verify Before Submission

See **README verification checklist** at the end of [tool-workflow.md](tool-workflow.md) or the flagged items in your Task 8.1.3 AI response — run a **clean SDK + fresh deploy** and confirm every setup step.
