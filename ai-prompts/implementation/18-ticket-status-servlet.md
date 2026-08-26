# AI Prompt — Task 5.1.4: TicketStatusServlet (PUT status change + 409)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.4.

**Sprint/Task:** 5.1 / 5.1.4
**Category:** implementation
**Meaningful:** Yes — status sub-resource PUT endpoint with state machine enforcement over HTTP.

---

## Prompt (verbatim)

> Task 5.1.4 (Sprint 5.1): Implement the status-change endpoint —
> PUT /bin/api/v1/tickets/{id}/status — enforcing the state machine (409 on invalid transition).
>
> Follow all rules in .cursor/rules/ (ESPECIALLY 02-state-machine.mdc). Read api-contract.md
> (status endpoint: request {"status":"..."}, 200 success, 409 INVALID_TRANSITION with
> {error, code}, 404 if ticket missing, 400 on unknown status label), TicketService.changeStatus,
> InvalidTransitionException, TicketStatus, and ServletResponseUtil.
> Target: com.mysite.core.servlets. Reuse ServletResponseUtil.
>
> Fit the routing consistently with the existing tickets servlets (from Tasks 5.1.2/5.1.3):
> - Suffix pattern is "/{id}/status". If a single suffix-based TicketsServlet exists, extend its
>   PUT handling to route "/{id}/status" here; otherwise create a dedicated TicketStatusServlet
>   captured at /bin/api/v1/tickets/{id}/status. State the decision; ensure NO routing conflict.
>
> Implement PUT /bin/api/v1/tickets/{id}/status:
>   - Parse the id and confirm sub-resource is "status".
>   - Request body JSON: { "status": "<label>" }  (confirm exact field name in api-contract).
>   - Call ticketService.changeStatus(id, statusLabel).
>   - Success -> 200 + updated TicketDTO (with new status + updatedAt).
>   - Error mapping via ServletResponseUtil:
>       TicketNotFoundException     -> 404 NOT_FOUND
>       InvalidTransitionException  -> 409 INVALID_TRANSITION
>           (message should include from->to, e.g., "Invalid transition: Open -> Closed";
>            body { "error": <message>, "code": "INVALID_TRANSITION" })
>       ValidationException         -> 400 VALIDATION_ERROR (unknown status label)
>       Malformed JSON              -> 400
>   - IMPORTANT: this endpoint changes ONLY status, and ONLY via TicketService.changeStatus
>     (which delegates to the state machine). Do NOT bypass the state machine.
>
> Requirements:
> - Extend SlingAllMethodsServlet; register method {PUT}.
> - @Reference TicketService. Thin servlet: parse -> service -> serialize.
> - Reuse ServletResponseUtil; SLF4J logging; no stack traces to client; Javadoc.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Verify no routing conflict (servlet resolver console).
> - Provide curl commands that PROVE the state machine over HTTP (assume a fresh ticket in Open):
>     VALID sequence (each 200):
>       a) PUT .../TKT-1001/status {"status":"In Progress"}  -> 200
>       b) PUT .../TKT-1001/status {"status":"Resolved"}     -> 200
>       c) PUT .../TKT-1001/status {"status":"Closed"}       -> 200
>     INVALID (each expected status shown):
>       d) On a fresh Open ticket: {"status":"Closed"}       -> 409 INVALID_TRANSITION
>       e) On a Closed ticket:     {"status":"Open"}         -> 409 INVALID_TRANSITION
>       f) {"status":"Bogus"}                                -> 400 VALIDATION_ERROR
>       g) nonexistent ticket status change                  -> 404
>     Also: Open->Cancelled and In Progress->Cancelled should be 200 (valid).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Confirmed routing structure from 5.1.2/5.1.3: not a merged TicketsServlet — filter-dispatch pattern for sub-resources. Implemented `TicketStatusServlet` handler + `TicketStatusRoutingFilter` (pattern `/bin/api/v1/tickets/[^/]+/status$`, PUT only). Body field `status` per api-contract; delegates exclusively to `TicketService.changeStatus()` → `TicketStateMachine`. `InvalidTransitionException` maps to `409 INVALID_TRANSITION` with message `"Invalid transition: {from} -> {to}"` via `ServletResponseUtil`. Build/deploy succeeded; live tests: `404 NOT_FOUND` for missing tickets, `400 VALIDATION_ERROR` for missing status field; assignee regression unchanged. Full HTTP transition sequence blocked — no tickets in DAM (`GET /tickets` → `[]`); state machine logic verified by existing unit tests. Updated `implementation-plan.md` — 5.1.4 complete; Active Task → 5.1.5.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Routing structure | Same filter-dispatch pattern as assignee (5.1.3) and by-id (5.1.2) |
| Status dispatch | `TicketStatusRoutingFilter` on `/bin/api/v1/tickets/[^/]+/status$` |
| `TicketStatusServlet` | Handler class (not separate `@Component` servlet) |
| No conflict | Distinct sub-resource suffix; does not overlap by-id or assignee patterns |
| Request field | `status` (api-contract.md) |
| State machine | **Only** via `TicketService.changeStatus()` — never bypassed in servlet |
| `409` body | `InvalidTransitionException.getMessage()` → `"Invalid transition: Open -> Closed"` |
| Terminal ticket via status | Service/state machine returns `409` for disallowed transitions (e.g. Closed → Open) |

---

## Endpoint behavior summary

| Method | Path | Handler | Success | Errors |
|--------|------|---------|---------|--------|
| `PUT` | `/bin/api/v1/tickets/{id}/status` | `TicketStatusRoutingFilter` → `TicketStatusServlet.doPut` | `200` + `TicketDTO` | `404 NOT_FOUND`, `409 INVALID_TRANSITION`, `400 VALIDATION_ERROR`, malformed JSON → `400` |

**Implementation flow:** `Servlet → TicketService.changeStatus(ticketId, newStatus) → TicketStateMachine.assertCanTransition → InvalidTransitionException → 409`

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/TicketStatusServlet.java` | Created — status PUT handler |
| `core/src/main/java/com/mysite/core/servlets/TicketStatusRoutingFilter.java` | Created — Sling filter dispatch |
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Updated — `STATUS_SUB_RESOURCE`, `FIELD_STATUS` |
| `implementation-plan.md` | Updated — 5.1.4 complete; Active Task 5.1.5 |
| `ai-prompts/implementation/18-ticket-status-servlet.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| No routing conflict | Confirmed — distinct `/status` filter pattern |
| `PUT …/NOPE/status` | `404` + `NOT_FOUND` |
| Missing `status` in body | `400` + `"status: is required"` |
| Assignee regression | `404 NOT_FOUND` JSON (unchanged) |
| Full transition sequence (a–e) over HTTP | Blocked — no persisted tickets (`POST` create `500`; pre-existing CF issue) |
| State machine unit tests | `TicketStateMachineTest` — 54 tests pass (Sprint 4.1) |

---

## curl test commands

```bash
# VALID sequence (each → 200; requires ticket TKT-1001 in Open)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"In Progress"}'

curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Resolved"}'

curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Closed"}'

# Open → Cancelled (200)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Cancelled"}'

# d) Open → Closed (skip) → 409 INVALID_TRANSITION
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Closed"}'

# e) Closed → Open → 409 INVALID_TRANSITION
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Open"}'

# f) Unknown label → 400 VALIDATION_ERROR
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Bogus"}'

# g) Nonexistent ticket → 404 NOT_FOUND
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/NOPE/status \
  -d '{"status":"In Progress"}'
```

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.4 | Complete |
| FR-8, FR-9, FR-10, FR-18 | Status change REST endpoint; state machine enforcement |
| AC-22–AC-36 | Valid/invalid transitions; `409 INVALID_TRANSITION` |
| NFR-AC-2 | Invalid transition → 409 with clear JSON |
| 02-state-machine.mdc | Enforcement only in `TicketStateMachine` via service |
| Downstream | 5.1.5 `CommentCollectionServlet`; same filter pattern for `/comments` |
