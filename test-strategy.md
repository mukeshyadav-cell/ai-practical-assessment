# Test Strategy — AEM Support Ticket Management System

**Date:** 2026-08-27  
**Purpose:** Document what is tested, how to run tests, manual verification approach, and explicit out-of-scope items for this assessment.

**Sprint/Task:** 8.1 / 8.1.1

---

## Scope Decision

| Layer | Location | In scope? | Rationale |
|-------|----------|-----------|-----------|
| **Java unit tests** | `core/src/test/java` | **Yes** | Fast feedback on state machine and service logic; no running AEM instance required for these tests |
| **Integration tests** | `it.tests` | **No** | Archetype module present for Cloud Manager compatibility; not developed for this learning/assessment project |
| **UI E2E (Cypress)** | `ui.tests` | **No** | Same — placeholder only; manual browser verification used instead |

**Honest rationale:** The assessment prioritizes **correct business rules** (especially the ticket state machine) and **service orchestration** over full-stack automation. Unit tests prove FR-8/FR-9 and validation paths cheaply. REST and UI were verified manually via curl and browser during Sprints 5.1 and 6.1/6.2 Quality Gates. Adding `it.tests`/Cypress would increase CI time and AEM SDK setup burden without changing the mandatory-core feature set.

**Policy:** `mvn test` (core module) is the automated test gate. Archetype sample tests (`HelloWorldModelTest`, `SimpleServletTest`, etc.) may still run in the reactor but are **not** part of the assessment test strategy.

---

## What Is Unit-Tested

| Component | Approach | Framework |
|-----------|----------|-----------|
| `TicketStateMachine` | Pure POJO — no mocks, no AEM | JUnit 5 (`@Test`, `@ParameterizedTest`) |
| `TicketServiceImpl` | Mockito mocks for `TicketRepository`, `UserRepository`; **real** `TicketStateMachine` inside service | JUnit 5 + Mockito |
| `CommentServiceImpl` | Mockito mocks for `CommentRepository`, `TicketRepository` | JUnit 5 + Mockito |

**Not unit-tested (by design):**

- Servlets (thin delegation; covered by manual HTTP + service tests)
- Repository CF adapters (would need AEM/Sling mocks or running instance)
- UI TypeScript (manual browser verification)
- Routing filters

---

## Coverage Table

| Component | Key scenarios | Test class | Test count (approx.) |
|-----------|---------------|------------|----------------------|
| `TicketStateMachine` | All 5 valid transitions; full invalid matrix (parameterized); terminal `allowedNextStatuses` empty; same-state rejected; null/unknown labels; `InvalidTransitionException` fields | `TicketStateMachineTest` | **62** |
| `TicketServiceImpl` | Create forces Open; validation (title, priority, assignee); update mutable fields only; terminal ticket not editable; reassign; `changeStatus` valid/invalid; list/search/filter/sort | `TicketServiceImplTest` | **23** |
| `CommentServiceImpl` | Add with valid message; reject empty/whitespace; ticket not found; **comments on Closed/Cancelled allowed**; list comments; blank `createdBy` rejected | `CommentServiceImplTest` | **11** |
| **Assessment total** | | | **~96** |

---

## State Machine Coverage (AC-22–AC-35)

| AC | Transition / rule | Test coverage |
|----|-------------------|---------------|
| AC-22 | Open → In Progress | `shouldAllowOpenToInProgress`, parameterized valid pairs |
| AC-23 | In Progress → Resolved | `shouldAllowInProgressToResolved` |
| AC-24 | Resolved → Closed | `shouldAllowResolvedToClosed` |
| AC-25 | Open → Cancelled | `shouldAllowOpenToCancelled` |
| AC-26 | In Progress → Cancelled | `shouldAllowInProgressToCancelled` |
| AC-27 | Open → Resolved (reject) | `shouldRejectOpenToResolved` |
| AC-28 | Open → Closed (reject) | `shouldRejectOpenToClosed` |
| AC-29 | In Progress → Closed (reject) | `shouldRejectInProgressToClosed` |
| AC-30 | In Progress → Open (reject) | `shouldRejectInProgressToOpen` |
| AC-31 | Resolved → Open (reject) | `shouldRejectResolvedToOpen` |
| AC-32 | Closed → Open (reject) | `shouldRejectClosedToOpen` |
| AC-33 | Cancelled → Open (reject) | `shouldRejectCancelledToOpen` |
| AC-34 | Resolved → In Progress (reject) | `shouldRejectResolvedToInProgress` |
| AC-35 | Resolved → Cancelled (reject) | `shouldRejectResolvedToCancelled` |
| — | Full invalid matrix | `shouldRejectAllInvalidTransitions` (parameterized) |
| — | Same-state transitions | `shouldRejectSameStateTransition` |
| — | Terminal next statuses | `shouldReturnEmptyNextStatusesForClosed/Cancelled` |

Service-layer tests additionally verify `changeStatus` delegates to the state machine (`shouldAllowValidTransition`, `shouldRejectInvalidTransition`).

---

## How to Run

```bash
# All core unit tests (assessment scope)
mvn test -pl core

# Full reactor build (includes archetype tests in other modules)
mvn clean install
```

**Expected:** All `TicketStateMachineTest`, `TicketServiceImplTest`, and `CommentServiceImplTest` methods pass. Sprint 7.1 Quality Gate verified `mvn test` green.

---

## Manual Verification (REST + UI)

Automated integration tests were **not** implemented. The following were verified manually during development:

| Area | Method | When |
|------|--------|------|
| All 10 REST endpoints | curl / Postman | Sprint 5.1 QG |
| Error JSON shapes (400/404/409) | curl | Sprint 5.1.7 |
| Full UI flow (list → create → detail → comment → status → close) | Browser on local SDK | Sprint 6.1 QG |
| Sort, filters, `/me`, toasts | Browser | Sprint 6.2 QG |

### Representative curl — state machine over HTTP

Prerequisites: local AEM author at `localhost:4502`, ticket `TKT-1001` in **Open** status (create via UI or POST first).

**Valid transition (200):** Open → In Progress

```bash
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"In Progress"}'
```

Expected: HTTP `200`, JSON body with `"status": "In Progress"`.

**Invalid transition (409):** Open → Closed (skips workflow)

```bash
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Closed"}'
```

Expected: HTTP `409`, body like:

```json
{"error":"Invalid transition: Open -> Closed","code":"INVALID_TRANSITION"}
```

**Additional curls used in Sprint 5.1:**

```bash
# List tickets
curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets

# Unknown ticket → 404
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/NOPE/status \
  -d '{"status":"In Progress"}'

# Current user (Sprint 6.2)
curl -s -u admin:admin http://localhost:4502/bin/api/v1/me
```

> **Note:** During initial servlet development (Task 5.1.4), some full HTTP transition sequences were blocked when no tickets existed in DAM (`GET /tickets` returned `[]`). State machine behavior was confirmed via unit tests; live 409/200 curls were run once tickets were persisted.

---

## What Is NOT Tested (and Why)

| Item | Why not |
|------|---------|
| `ContentFragmentTicketRepository` / `ContentFragmentCommentRepository` | Requires AEM runtime or heavy Sling mocks; validated via manual create + CRXDE inspection |
| `AemUserRepository` | Depends on repoinit seed users and JCR ACLs; fixed via debugging (see `debugging-notes.md`) and manual `GET /users` |
| Servlets / routing filters | Thin wrappers; error mapping spot-checked with curl, not JUnit |
| HTL / TypeScript UI | No Cypress suite; end-to-end verified manually in browser |
| Dispatcher rules | Validated with Dispatcher SDK script, not ticket-specific |
| Concurrency on ID counters | Known non-atomic limitation; not stress-tested |
| Cloud Manager pipeline | Out of local assessment scope |

---

## Traceability

| Requirement | Verification |
|-------------|--------------|
| FR-8, FR-9 (state machine) | `TicketStateMachineTest` + manual `PUT …/status` curl |
| FR-7 (terminal edits) | `TicketServiceImplTest` + manual API |
| FR-13–FR-15 (comments) | `CommentServiceImplTest` + manual UI |
| AC-22–AC-35 | State machine test `@DisplayName` annotations |
| NFR-AC (unit test policy) | This document; `core/src/test` only |
