# Prompt History — Sprint 5.1: REST API (Servlets)

**Date:** 2026-08-26
**Sprint:** 5.1 — REST API (Servlets)
**Status:** Complete (pending developer review)
**Tasks covered:** 5.1.1 → 5.1.7
**Traceability:** FR-1–FR-19 (REST layer); api-contract.md endpoints + error catalog; AC-1–AC-51 (HTTP mapping); Sprint 5.1 DOD-1–DOD-5

---

> **Recovery notice**
> Prompts in this file are **verbatim** — recovered from the Cursor conversation transcript.
> Typos in original prompts are preserved intentionally.
> AI responses are summarized, not pasted in full.

---

## Goal

Expose the full Support Ticket Management REST API at `/bin/api/v1` via thin Sling servlets: ticket collection/detail/update, assignee and status sub-resources, comment collection, and read-only user endpoints — all delegating to `TicketService`/`CommentService`/`UserRepository`, with consistent Jackson JSON serialization and api-contract error mapping via `ServletResponseUtil`.

## Tasks Completed

| Task ID | Summary | Outcome |
| ------- | ------- | ------- |
| 5.1.1 | TicketCollectionServlet + ServletResponseUtil | `GET`/`POST /tickets`; shared JSON/error helpers |
| 5.1.2 | TicketByIdServlet | `GET`/`PUT /tickets/{id}` via routing filter (suffix servlet unreliable on local SDK) |
| 5.1.3 | TicketAssigneeServlet | `PUT /tickets/{id}/assignee` via filter-dispatch |
| 5.1.4 | TicketStatusServlet | `PUT /tickets/{id}/status`; 409 on `InvalidTransitionException` |
| 5.1.5 | CommentCollectionServlet | `GET`/`POST /tickets/{id}/comments` |
| 5.1.6 | UserCollectionServlet + UserByIdServlet | `GET /users` and `GET /users/{userId}` (read-only) |
| 5.1.7 | Shared error handling | Centralized `execute()`/`handleException()` across all servlets |

## Prompts Log

### Prompt 1 — Sprint 5.1 session start
**Time:** 10:19 PM | **Task:** 5.1 (context)

**Actual prompt:**
> Starting a fresh Cursor session for Sprint 5.1 (REST API — Servlets).
>
> READ before we begin:
> - .cursor/rules/ (ESPECIALLY 04-aem-correctness.mdc for servlet registration + 01-architecture.mdc)
> - implementation-plan.md (Sprint 5.1 task list + current status)
> - api-contract.md (ALL endpoints, JSON shapes, status codes, error catalog)
> - Existing code: com.mysite.core.services (TicketService, CommentService),
>   com.mysite.core.dto, com.mysite.core.exception (DomainException hierarchy),
>   com.mysite.core.statemachine
>
> Summarize back to confirm context:
> 1. Active Sprint / Active Task
> 2. The full Sprint 5.1 servlet list (5.1.1–5.1.7)
> 3. TicketService method signatures
> 4. The api-contract error catalog (code -> HTTP status), especially INVALID_TRANSITION -> 409
> 5. How servlets should be registered (sling.servlet.paths) and JSON library (Jackson)
> 6. Do NOT start coding yet — wait for my Task 5.1.1 prompt.

**AI output:**
- Confirmed Active Sprint 5.1 / Task 5.1.1 pending
- Listed all seven servlet tasks (5.1.1–5.1.7)
- Summarized `TicketService`/`CommentService` method signatures
- Recited error catalog (`VALIDATION_ERROR`→400, `UNKNOWN_USER`→400, `TICKET_NOT_EDITABLE`→400, `NOT_FOUND`→404, `INVALID_TRANSITION`→409, `INTERNAL_ERROR`→500)
- Confirmed `sling.servlet.paths` registration and Jackson for JSON

**What I accepted / changed:** Accepted context summary; proceeded to 5.1.1
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 2 — Task 5.1.1: TicketCollectionServlet + shared helpers
**Time:** 10:20 PM | **Task:** 5.1.1

**Actual prompt:**
> Task 5.1.1 (Sprint 5.1): Implement TicketCollectionServlet handling GET (list) and
> POST (create) at /bin/api/v1/tickets, plus a shared JSON/error-handling helper.
>
> Follow all rules in .cursor/rules/ (04-aem-correctness for servlet registration; use Jackson
> for JSON). Read api-contract.md (list + create shapes, query params ?status= and ?q=,
> status codes, error catalog), TicketService, TicketDTO, and the DomainException hierarchy.
> Target: com.mysite.core.servlets.
>
> [... full task spec: ServletResponseUtil, writeJson/writeError, ObjectMapper JSR-310,
> mapDomainException, TicketCollectionServlet GET/POST, curl tests ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Created `TicketCollectionServlet`, `ServletResponseUtil`, `ErrorResponse`, `ServletConstants`
- `GET` list with `?status=`/`?q=`; `POST` create → 201; `createdBy` from session user
- Malformed JSON → 400 `VALIDATION_ERROR`; `DomainException` mapped via `errorCode()`/`httpStatus()`
- Build/deploy succeeded; `GET /tickets` → 200 `[]`; blank title → 400

**What I accepted / changed:** Accepted; saved to `ai-prompts/implementation/15-ticket-collection-servlet.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 3 — Task 5.1.2: TicketByIdServlet + routing design
**Time:** 10:29 PM | **Task:** 5.1.2

**Actual prompt:**
> Task 5.1.2 (Sprint 5.1): Implement TicketByIdServlet — GET /bin/api/v1/tickets/{id} (detail)
> and PUT /bin/api/v1/tickets/{id} (update title/description/priority).
>
> [... full task spec: suffix routing design decision, 404/400 TICKET_NOT_EDITABLE,
> reject status/assignedTo in PUT body, curl tests ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Investigated Sling suffix servlet registration — **failed on local AEM SDK** (`suffix=null`, HTML 404 from `DefaultGetServlet`)
- Chose **filter-dispatch pattern**: `TicketByIdServlet` handler + `TicketByIdRoutingFilter` (`/bin/api/v1/tickets/[^/]+$`)
- Added `ServletPathUtil.resolveTicketId()`; PUT rejects `status`/`assignedTo` in body
- Live: GET `NOPE` → 404 `NOT_FOUND`; status in body → 400

**What I accepted / changed:** Accepted filter-dispatch over merged servlet; saved to `16-ticket-by-id-servlet.md`
**What I rejected / why:** Rejected pure suffix servlet approach after live SDK failure
**Iteration needed:** Yes — routing debugging required

---

### Prompt 4 — Task 5.1.3: TicketAssigneeServlet
**Time:** 10:40 PM | **Task:** 5.1.3

**Actual prompt:**
> Task 5.1.3 (Sprint 5.1): Implement the reassign endpoint —
> PUT /bin/api/v1/tickets/{id}/assignee.
>
> [... full task spec: routing fit, assignedTo body, UNKNOWN_USER, TICKET_NOT_EDITABLE, curl tests ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- `TicketAssigneeServlet` handler + `TicketAssigneeRoutingFilter` (`/bin/api/v1/tickets/[^/]+/assignee$`)
- Extended `ServletPathUtil` with `resolveTicketIdForSubResource`
- Live: 404 missing ticket; 400 blank/malformed `assignedTo`; collection/by-id regression OK

**What I accepted / changed:** Accepted; saved to `17-ticket-assignee-servlet.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 5 — Task 5.1.4: TicketStatusServlet (409 state machine)
**Time:** 10:43 PM | **Task:** 5.1.4

**Actual prompt:**
> Task 5.1.4 (Sprint 5.1): Implement the status-change endpoint —
> PUT /bin/api/v1/tickets/{id}/status — enforcing the state machine (409 on invalid transition).
>
> [... full task spec: changeStatus delegation, INVALID_TRANSITION, transition curl matrix ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- `TicketStatusServlet` handler + `TicketStatusRoutingFilter` (`/bin/api/v1/tickets/[^/]+/status$`)
- Delegates to `TicketService.changeStatus()` only; `InvalidTransitionException` → 409
- Live: 404 missing ticket; 400 missing status; full transition sequence blocked (no tickets in DAM)

**What I accepted / changed:** Accepted; saved to `18-ticket-status-servlet.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No (HTTP 409 mapping verified in code + Sprint 4.1 unit tests)

---

### Prompt 6 — Task 5.1.5: CommentCollectionServlet
**Time:** 10:45 PM | **Task:** 5.1.5

**Actual prompt:**
> Task 5.1.5 (Sprint 5.1): Implement the comments endpoint —
> GET /bin/api/v1/tickets/{id}/comments (list) and
> POST /bin/api/v1/tickets/{id}/comments (add).
>
> [... full task spec: dedicated servlet recommendation, 201 on create, comments on Closed allowed, CRXDE verify ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- `CommentCollectionServlet` handler + `CommentCollectionRoutingFilter` (`/bin/api/v1/tickets/[^/]+/comments$`)
- `doGet` → list; `doPost` → 201; path `ticketId` authoritative; no terminal-status block
- Live: 404 missing ticket; create/list blocked by no persisted tickets (`POST /tickets` → 500)

**What I accepted / changed:** Accepted; saved to `19-comment-collection-servlet.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 7 — Task 5.1.6: User endpoints
**Time:** 10:49 PM | **Task:** 5.1.6

**Actual prompt:**
> Task 5.1.6 (Sprint 5.1): Implement the user endpoints —
> GET /bin/api/v1/users (list, optional ?q= search) and
> GET /bin/api/v1/users/{userId} (detail).
>
> [... full task spec: routing choice, UserRepository direct injection vs UserService, 404 on unknown ...]
>
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- `UserCollectionServlet` (exact path, `SlingSafeMethodsServlet`) + `UserByIdServlet` handler + `UserByIdRoutingFilter`
- Direct `UserRepository` injection `(impl.type=aem)` — documented trade-off in Javadoc
- Live: `GET /users` → 200 `[]`; `GET /users/nobody` → 404 (seed users not visible to repository on instance)

**What I accepted / changed:** Accepted architecture choice (a); saved to `20-user-servlets.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 8 — Task 5.1.7: Shared error handling
**Time:** 10:52 PM | **Task:** 5.1.7

**Actual prompt:**
> Task 5.1.7 (Sprint 5.1): Audit and finalize consistent error handling across ALL servlets,
> aligned with the api-contract.md error catalog.
>
> Part A — Audit (report BEFORE changing code):
> Produce a table auditing every servlet + method + error path [...]
>
> Part B — Harden ServletResponseUtil (single source of truth) [...]
>
> Part C — Refactor servlets for consistency [...]
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Provide the FINAL error-handling coverage table (Part A re-run) showing all paths now match.
> - Provide 2-3 curl commands demonstrating a 400, a 404, and the 409 with correct JSON bodies.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Audited all servlet error paths; found duplicate catches and per-servlet malformed-JSON handling
- Hardened `ServletResponseUtil` with `execute()`, `writeValidationError()`, `writeNotFoundError()`, centralized `handleException()`
- Refactored all eight servlet/handler classes to use `execute()` exclusively
- Build/deploy succeeded; curl: 400 malformed JSON, 404 ticket/user, 400 validation; 409 mapping confirmed in code

**What I accepted / changed:** Accepted centralized error handling; saved to `21-shared-error-handling.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 9 — Sprint 5.1 Quality Gate
**Time:** 10:58 PM | **Task:** 5.1 QG

**Actual prompt:**
> Sprint 5.1 Quality Gate: All servlet tasks (5.1.1–5.1.7) complete and committed.
>
> Step 1 — Verify Sprint 5.1 Definition of Done:
> - All api-contract endpoints respond correctly (list/detail/create/update/assignee/status/
>   comments/users)
> - Create ticket -> 201; create comment -> 201
> - Invalid transition -> 409 INVALID_TRANSITION; unknown user -> 400 UNKNOWN_USER
> - Terminal update/reassign -> 400 TICKET_NOT_EDITABLE
> - All responses Content-Type application/json; consistent {error, code} on failures
> - No servlet routing conflicts (servlet resolver clean)
> Provide a short endpoint-by-endpoint verification table (endpoint | method | tested result).
> State any DOD item not satisfied; if all pass, state "All DOD satisfied".
>
> Step 2 — Generate the sprint log:
> Following the prompt-history-sprint-log rule in .cursor/rules/:
> 1. Read THIS session's Cursor transcript.
> 2. Filter meaningful user prompts.
> 3. Generate prompt-history/sprint-5.1.md using the EXACT rule structure (verbatim prompts,
>    summarized AI outputs, what I accepted/changed — including any servlet routing/CF-write
>    debugging, files changed, requirements traced, QG result).
> 4. Update prompt-history/README.md Session Index with the Sprint 5.1 row.
>
> Sprint title: "REST API (Servlets)".
> Do NOT start Sprint 6.1 yet — stop after generating the sprint log for my review.

**AI output:**
- Ran full curl verification suite against local AEM (admin:admin)
- Produced endpoint verification table and DOD assessment (partial pass — see QG result)
- Generated this sprint log; updated `prompt-history/README.md`

**What I accepted / changed:** Pending developer review
**What I rejected / why:** N/A
**Iteration needed:** TBD — CF persistence and seed-user environment gaps block full happy-path DOD

---

## What I did without AI assistance

- Ran `mvn clean install` / `-PautoInstallSinglePackage` locally
- Reviewed servlet resolver behavior and AEM request logs during routing debugging (Task 5.1.2)
- Saved task prompt logs to `ai-prompts/implementation/15-` through `21-`

## Prompt engineering notes

| Observation | What it shows |
|----|---|
| "FIRST, resolve the routing design" before coding | Forces explicit AEM SDK constraint documentation (suffix servlet failure) |
| Task specs with exact curl expected status/body | Enables immediate live verification per endpoint |
| Part A audit-before-refactor (5.1.7) | Surfaces duplicate error-handling before consolidation |
| Filter-dispatch pattern reused 5.1.2→5.1.6 | Consistent sub-resource routing once suffix servlets proved unreliable |

## Files changed

| File | Change |
|---|-----|
| `core/.../servlets/TicketCollectionServlet.java` | Created |
| `core/.../servlets/TicketByIdServlet.java` | Created |
| `core/.../servlets/TicketByIdRoutingFilter.java` | Created |
| `core/.../servlets/TicketAssigneeServlet.java` | Created |
| `core/.../servlets/TicketAssigneeRoutingFilter.java` | Created |
| `core/.../servlets/TicketStatusServlet.java` | Created |
| `core/.../servlets/TicketStatusRoutingFilter.java` | Created |
| `core/.../servlets/CommentCollectionServlet.java` | Created |
| `core/.../servlets/CommentCollectionRoutingFilter.java` | Created |
| `core/.../servlets/UserCollectionServlet.java` | Created |
| `core/.../servlets/UserByIdServlet.java` | Created |
| `core/.../servlets/UserByIdRoutingFilter.java` | Created |
| `core/.../servlets/ServletConstants.java` | Created |
| `core/.../servlets/util/ServletResponseUtil.java` | Created / Updated (5.1.7 hardening) |
| `core/.../servlets/util/ErrorResponse.java` | Created |
| `core/.../servlets/util/ServletPathUtil.java` | Created |
| `implementation-plan.md` | Updated — 5.1.1–5.1.7 complete; QG pending review |
| `ai-prompts/implementation/15-ticket-collection-servlet.md` | Created |
| `ai-prompts/implementation/16-ticket-by-id-servlet.md` | Created |
| `ai-prompts/implementation/17-ticket-assignee-servlet.md` | Created |
| `ai-prompts/implementation/18-ticket-status-servlet.md` | Created |
| `ai-prompts/implementation/19-comment-collection-servlet.md` | Created |
| `ai-prompts/implementation/20-user-servlets.md` | Created |
| `ai-prompts/implementation/21-shared-error-handling.md` | Created |
| `prompt-history/sprint-5.1.md` | Created |
| `prompt-history/README.md` | Updated |

## Requirements traced

| ID | Coverage |
|----|----------|
| FR-1 | `POST /tickets` — create (servlet layer; persistence blocked on test instance) |
| FR-2, FR-4, FR-11, FR-12 | `GET /tickets` — list, filter, search |
| FR-3 | `GET /tickets/{id}` — detail |
| FR-5, FR-7 | `PUT /tickets/{id}` — update fields; terminal rejection via service |
| FR-6, FR-7 | `PUT /tickets/{id}/assignee` — reassign |
| FR-8–FR-10 | `PUT /tickets/{id}/status` — state machine via `changeStatus` |
| FR-13–FR-15 | `POST /tickets/{id}/comments` — add comment |
| FR-14 | `GET /tickets/{id}/comments` — list comments |
| FR-16 | `GET /users` — list/search |
| FR-17 | `GET /users/{userId}` — detail |
| FR-18 | All endpoints JSON via `ServletResponseUtil` |
| FR-19 | Relative `/bin/api/v1` paths (no hostnames) |
| NFR-AC-1 | `VALIDATION_ERROR` → 400 JSON |
| NFR-AC-2 | `INVALID_TRANSITION` → 409 JSON (code path verified; live blocked) |
| NFR-AC-5 | `NOT_FOUND` → 404 JSON |
| NFR-AC-6 | `INTERNAL_ERROR` → 500 JSON (no stack leak) |
| Sprint 5.1 DOD-1–DOD-5 | See Quality Gate result |

## Quality Gate result

### Step 1 — Endpoint verification (curl, local AEM `localhost:4502`, `admin:admin`)

| Endpoint | Method | Tested result |
|----------|--------|---------------|
| `/bin/api/v1/tickets` | GET | 200 `[]` — `application/json` |
| `/bin/api/v1/tickets?status=Open` | GET | 200 `[]` — `application/json` |
| `/bin/api/v1/tickets?status=Bad` | GET | 400 `VALIDATION_ERROR` |
| `/bin/api/v1/tickets` | POST (valid body) | **500 `INTERNAL_ERROR`** — CF persistence failure (repository layer) |
| `/bin/api/v1/tickets` | POST (malformed JSON) | 400 `VALIDATION_ERROR` `"Malformed request body"` |
| `/bin/api/v1/tickets` | POST (`assignedTo:nobody`) | 400 `UNKNOWN_USER` |
| `/bin/api/v1/tickets/{id}` | GET | 404 `NOT_FOUND` (TKT-9999) |
| `/bin/api/v1/tickets/{id}` | PUT | 404 `NOT_FOUND`; 400 if `status` in body |
| `/bin/api/v1/tickets/{id}/assignee` | PUT | 404 `NOT_FOUND` (no ticket) |
| `/bin/api/v1/tickets/{id}/status` | PUT | 404 `NOT_FOUND` (no ticket) |
| `/bin/api/v1/tickets/{id}/comments` | GET | 404 `NOT_FOUND` |
| `/bin/api/v1/tickets/{id}/comments` | POST | 404 `NOT_FOUND` |
| `/bin/api/v1/users` | GET | 200 `[]` — `application/json` |
| `/bin/api/v1/users?q=agent` | GET | 200 `[]` |
| `/bin/api/v1/users/{userId}` | GET | 404 `NOT_FOUND` (nobody, agent-1) |

**Routing:** No conflicts observed — collection paths return 200; sub-paths return JSON 404 (not HTML `DefaultGetServlet` 404). Distinct filter patterns for by-id, assignee, status, comments, users.

### Step 1 — Sprint 5.1 Definition of Done

| Check | Criterion | Result | Notes |
|-------|-----------|--------|-------|
| DOD-1 | All ten api-contract endpoints respond via curl | **Partial** | All routes respond with JSON; **POST create returns 500** (CF write), blocking happy-path flows |
| DOD-2 | Create ticket → 201; create comment → 201 | **Failed** | `POST /tickets` → 500 `INTERNAL_ERROR`; comment 201 not testable without persisted ticket |
| DOD-3 | 409 `INVALID_TRANSITION`; 400 `UNKNOWN_USER` | **Partial** | `UNKNOWN_USER` verified live on create; **409 not live-tested** (no tickets); mapping verified in `InvalidTransitionException` + Sprint 4.1 unit tests |
| DOD-4 | Terminal update/reassign → 400 `TICKET_NOT_EDITABLE` | **Not verified live** | Service layer implements; blocked without persisted tickets |
| DOD-5 | `Content-Type: application/json` on all responses | **Passed** | All tested success and error responses: `application/json;charset=utf-8` |
| Routing | No servlet resolver conflicts | **Passed** | Filter-dispatch pattern; distinct HTTP responses per sub-resource |
| Error shape | `{error, code}` on failures | **Passed** | Consistent across all tested error paths |
| Build | `mvn clean install -PautoInstallSinglePackage` | **Passed** | Deployed to local SDK |

**Environment blockers (pre-existing, not servlet-layer):**
1. **CF ticket persistence** — `POST /tickets` with valid body returns 500 (repository/CF adapter issue from Sprint 3.1 layer).
2. **Seed users not returned** — `GET /users` → `[]`; `AemUserRepository` does not surface repoinit `agent-1`/`agent-2` on test instance.

**Overall DOD:** **NOT all DOD satisfied** — servlet layer and error handling complete; happy-path 201/409/TICKET_NOT_EDITABLE blocked by persistence/seed-user environment gaps. Recommend fixing CF write + user seed visibility before Sprint 6.1 UI work.

| Build | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | **Passed** |
| Live curl (error paths) | **Passed** |
| Live curl (201 create, 409 transition, TICKET_NOT_EDITABLE) | **Blocked** — no persisted tickets |

**Sprint exit:** Pending developer review. **Do not start Sprint 6.1** until approved and environment blockers addressed.

## Developer review

**Status:** Pending review
**Approved by:** Developer — _pending_
**Notes:** Prompts verbatim from Cursor transcript. Typos preserved. Servlet routing uses filter-dispatch due to local AEM SDK suffix servlet limitation.
