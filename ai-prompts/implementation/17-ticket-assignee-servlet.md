# AI Prompt — Task 5.1.3: TicketAssigneeServlet (PUT reassign)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.3.

**Sprint/Task:** 5.1 / 5.1.3
**Category:** implementation
**Meaningful:** Yes — assignee sub-resource PUT endpoint with filter-dispatch routing consistent with Task 5.1.2.

---

## Prompt (verbatim)

> Task 5.1.3 (Sprint 5.1): Implement the reassign endpoint —
> PUT /bin/api/v1/tickets/{id}/assignee.
>
> Follow all rules in .cursor/rules/. Read api-contract.md (assignee endpoint: request shape,
> 200 success, 404 if ticket missing, 400 UNKNOWN_USER, 400 TICKET_NOT_EDITABLE on terminal),
> TicketService.reassignTicket, TicketDTO, DomainException hierarchy, and ServletResponseUtil.
> Target: com.mysite.core.servlets. Reuse ServletResponseUtil (no duplicate JSON/error logic).
>
> FIRST, fit the routing to the existing structure:
> - Tell me how tickets routing currently works (from Task 5.1.2: merged TicketsServlet with
>   suffix handling, or separate servlets). Choose the cleanest option consistent with it:
>     (a) If a single suffix-based TicketsServlet exists, extend its PUT handling to detect the
>         suffix pattern "/{id}/assignee" and route to reassign.
>     (b) Otherwise create a dedicated TicketAssigneeServlet registered so it captures
>         /bin/api/v1/tickets/{id}/assignee (via suffix on /bin/api/v1/tickets or a specific path).
>   State the decision and ensure NO conflict with the collection/by-id routing.
>
> Implement PUT /bin/api/v1/tickets/{id}/assignee:
>   - Parse the id and confirm the sub-resource is "assignee".
>   - Request body JSON: { "assignedTo": "<userId>" }  (per api-contract — confirm the exact
>     field name in api-contract.md and match it).
>   - Call ticketService.reassignTicket(id, assignedTo).
>   - Success -> 200 + updated TicketDTO.
>   - Error mapping via ServletResponseUtil:
>       TicketNotFoundException      -> 404 NOT_FOUND
>       UnknownUserException         -> 400 UNKNOWN_USER
>       TicketNotEditableException   -> 400 TICKET_NOT_EDITABLE (terminal ticket)
>       ValidationException          -> 400 VALIDATION_ERROR (e.g., blank assignedTo if required)
>       Malformed JSON               -> 400
>   - IMPORTANT: this endpoint changes ONLY assignedTo — never status or other fields.
>
> Requirements:
> - Extend SlingAllMethodsServlet; register method {PUT} appropriately.
> - @Reference TicketService. Thin servlet: parse -> service -> serialize.
> - Reuse ServletResponseUtil; SLF4J logging; no stack traces to client; Javadoc.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Verify no servlet routing conflict (Sling Servlet Resolver console) with collection/by-id.
> - Provide curl commands:
>     a) PUT /bin/api/v1/tickets/TKT-1001/assignee  body {"assignedTo":"agent-1"} -> 200 updated
>     b) PUT .../assignee body {"assignedTo":"ghost"} -> 400 UNKNOWN_USER
>     c) PUT nonexistent ticket .../assignee -> 404
>     d) (after ticket Closed) PUT .../assignee -> 400 TICKET_NOT_EDITABLE
>     e) Regression: collection GET/POST and by-id GET/PUT still work
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Confirmed Task 5.1.2 routing: not a merged TicketsServlet — `TicketCollectionServlet` on exact `/bin/api/v1/tickets`; by-id via `TicketByIdServlet` handler + `TicketByIdRoutingFilter` (filter pattern `/bin/api/v1/tickets/[^/]+$`) because Sling suffix servlet registration fails on local AEM SDK. Chose consistent pattern for assignee: `TicketAssigneeServlet` handler + `TicketAssigneeRoutingFilter` (pattern `/bin/api/v1/tickets/[^/]+/assignee$`, PUT only). Extended `ServletPathUtil` with `resolveRemainderAfterTickets` and `resolveTicketIdForSubResource`. Body field `assignedTo` per api-contract; calls `ticketService.reassignTicket(id, assignedTo)`. Build/deploy succeeded; live tests: `404 NOT_FOUND` for missing tickets, `400 VALIDATION_ERROR` for blank/missing/malformed body; collection and by-id regression unchanged. Updated `implementation-plan.md` — 5.1.3 complete; Active Task → 5.1.4.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Routing structure | **Not** merged TicketsServlet — same filter-dispatch pattern as 5.1.2 |
| Assignee dispatch | `TicketAssigneeRoutingFilter` on `/bin/api/v1/tickets/[^/]+/assignee$` |
| `TicketAssigneeServlet` | Handler class (not separate `@Component` servlet) — mirrors `TicketByIdServlet` |
| No conflict | By-id filter ends with `$` (single segment); assignee filter requires `/assignee` suffix |
| Request field | `assignedTo` (api-contract.md) |
| Blank/null `assignedTo` | Rejected at servlet layer with `400 VALIDATION_ERROR` before service call |
| Unassign (null/empty) | api-contract allows; MVP service requires non-blank — deferred to service validation |

---

## Endpoint behavior summary

| Method | Path | Handler | Success | Errors |
|--------|------|---------|---------|--------|
| `PUT` | `/bin/api/v1/tickets/{id}/assignee` | `TicketAssigneeRoutingFilter` → `TicketAssigneeServlet.doPut` | `200` + `TicketDTO` | `404 NOT_FOUND`, `400 UNKNOWN_USER`, `400 TICKET_NOT_EDITABLE`, `400 VALIDATION_ERROR`, malformed JSON → `400` |

Changes **only** `assignedTo` via `TicketService.reassignTicket` — never status or other fields.

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/TicketAssigneeServlet.java` | Created — assignee PUT handler |
| `core/src/main/java/com/mysite/core/servlets/TicketAssigneeRoutingFilter.java` | Created — Sling filter dispatch |
| `core/src/main/java/com/mysite/core/servlets/util/ServletPathUtil.java` | Updated — sub-resource path parsing |
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Updated — `ASSIGNEE_SUB_RESOURCE`, `FIELD_ASSIGNED_TO` |
| `core/src/main/java/com/mysite/core/servlets/TicketByIdServlet.java` | Updated — Javadoc fix |
| `implementation-plan.md` | Updated — 5.1.3 complete; Active Task 5.1.4 |
| `ai-prompts/implementation/17-ticket-assignee-servlet.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| No routing conflict with collection/by-id | Confirmed — distinct filter patterns |
| `PUT …/NOPE/assignee` | `404` + `NOT_FOUND` |
| Blank/missing `assignedTo` | `400` + `VALIDATION_ERROR` |
| Malformed JSON | `400` + `Malformed request body` |
| Regression `GET /tickets` | `200` + `[]` |
| Regression `GET /tickets/NOPE` | `404` + `NOT_FOUND` JSON |
| `PUT …/TKT-1001/assignee` with `ghost` | `404` (ticket does not exist — user check runs after ticket load) |
| `(a)`/`(b)` full success path | Blocked — `POST` create still `500` (CF persistence; pre-existing) |

---

## curl test commands

```bash
# a) Reassign (needs existing ticket TKT-1001)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/assignee \
  -d '{"assignedTo":"agent-1"}'

# b) Unknown user → 400 UNKNOWN_USER (on existing ticket)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/assignee \
  -d '{"assignedTo":"ghost"}'

# c) Nonexistent ticket → 404 NOT_FOUND
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/NOPE/assignee \
  -d '{"assignedTo":"agent-1"}'

# d) Closed ticket → 400 TICKET_NOT_EDITABLE (after 5.1.4 status endpoint)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/assignee \
  -d '{"assignedTo":"agent-2"}'

# e) Regression — collection + by-id
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin http://localhost:4502/bin/api/v1/tickets
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin http://localhost:4502/bin/api/v1/tickets/NOPE
```

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.3 | Complete |
| FR-6, FR-7, FR-18 | Reassign endpoint; terminal ticket guard |
| AC-18–AC-21 | Assignee update; unknown user; terminal rejection |
| api-contract | `PUT /tickets/{id}/assignee`; body `assignedTo` |
| Downstream | 5.1.4 `TicketStatusServlet`; same filter pattern for `/status` |
