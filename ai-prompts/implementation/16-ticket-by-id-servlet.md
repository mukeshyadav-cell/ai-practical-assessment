# AI Prompt — Task 5.1.2: TicketByIdServlet (GET detail + PUT update)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.2.

**Sprint/Task:** 5.1 / 5.1.2
**Category:** implementation
**Meaningful:** Yes — by-id REST endpoints with Sling routing design decision and filter-based dispatch.

---

## Prompt (verbatim)

> Task 5.1.2 (Sprint 5.1): Implement TicketByIdServlet — GET /bin/api/v1/tickets/{id} (detail)
> and PUT /bin/api/v1/tickets/{id} (update title/description/priority).
>
> Follow all rules in .cursor/rules/. Read api-contract.md (detail + update shapes, 404 on
> missing, 400 TICKET_NOT_EDITABLE on terminal, allowed update fields), TicketService,
> TicketDTO, DomainException hierarchy, and the ServletResponseUtil from Task 5.1.1.
> Target: com.mysite.core.servlets. Reuse ServletResponseUtil for JSON + error mapping.
>
> FIRST, resolve the routing design:
> - The api-contract uses /bin/api/v1/tickets/{id}. Decide how to capture {id} with Sling:
>   Preferred: register this servlet on sling.servlet.paths = /bin/api/v1/tickets and read the
>   id from request.getRequestPathInfo().getSuffix() (strip leading '/'), while
>   TicketCollectionServlet handles the case with NO suffix.
>   IF that causes a conflict with the existing TicketCollectionServlet on the same path,
>   explain the conflict and choose the cleanest resolution:
>     (a) Merge collection + by-id into ONE servlet on /bin/api/v1/tickets that branches on
>         suffix presence (no suffix = collection GET/POST; suffix = by-id GET/PUT), OR
>     (b) Use a distinct path/selector for by-id.
>   Recommend option (a) if the suffix approach conflicts — a single TicketsServlet is often
>   cleanest. State your decision clearly before coding.
>
> Then implement (in whichever servlet structure you chose):
>
> GET /bin/api/v1/tickets/{id}:
>   - Extract id from suffix. If id blank -> this is the collection case (or 400 if by-id servlet).
>   - ticketService.getTicket(id) -> 200 + TicketDTO
>   - TicketNotFoundException -> 404 NOT_FOUND (via ServletResponseUtil)
>
> PUT /bin/api/v1/tickets/{id}:
>   - Extract id from suffix (required; if blank -> 400).
>   - Parse body JSON -> TicketDTO (only title, description, priority are honored).
>   - ticketService.updateTicket(id, changes) -> 200 + updated TicketDTO
>   - TicketNotFoundException -> 404
>   - TicketNotEditableException (terminal status) -> 400 TICKET_NOT_EDITABLE
>   - ValidationException (bad priority etc.) -> 400 VALIDATION_ERROR
>   - Malformed JSON -> 400
>   - IMPORTANT: PUT here must NOT change status or assignedTo (those have dedicated endpoints
>     in Tasks 5.1.3 and 5.1.4). Ignore/reject those fields if present per api-contract.
>
> Requirements:
> - Extend SlingAllMethodsServlet (GET + PUT). Register methods {GET, PUT} (+ POST if merged).
> - @Reference TicketService. Thin servlet: parse -> service -> serialize.
> - Reuse ServletResponseUtil (do not duplicate JSON/error logic).
> - SLF4J logging; no stack traces to client; Javadoc.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Confirm no servlet registration CONFLICT with TicketCollectionServlet
>   (check Sling Servlet Resolver console).
> - Provide curl commands:
>     a) GET /bin/api/v1/tickets/TKT-1001 -> 200 detail
>     b) GET /bin/api/v1/tickets/NOPE -> 404 NOT_FOUND
>     c) PUT /bin/api/v1/tickets/TKT-1001 with {title,description,priority} -> 200 updated
>     d) PUT with invalid priority -> 400 VALIDATION_ERROR
>     e) (After a ticket is Closed later) PUT -> 400 TICKET_NOT_EDITABLE
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Investigated Sling suffix routing per api-contract (`/bin/api/v1/tickets/` vs exact `/bin/api/v1/tickets`). Suffix servlet registration did not route `/bin/api/v1/tickets/{id}` on local AEM SDK (servlet resolver logged `no servlet found`; HTML 404 from `DefaultGetServlet`). Chose not to merge collection + by-id into one servlet; instead implemented `TicketByIdServlet` as a handler class with static `doGet`/`doPut`, plus `TicketByIdRoutingFilter` (Sling filter pattern `/bin/api/v1/tickets/[^/]+$`) to dispatch by-id GET/PUT without conflicting with `TicketCollectionServlet`. Added `ServletPathUtil.resolveTicketId()` (suffix + URI fallback). PUT rejects `status`/`assignedTo` in body with `400 VALIDATION_ERROR`. Build/deploy succeeded; live tests: GET `NOPE` → `404 NOT_FOUND`, invalid priority → `400 VALIDATION_ERROR`, status in body → rejected. Updated `implementation-plan.md` — 5.1.2 complete; Active Task → 5.1.3.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Routing (preferred) | api-contract suffix path `/bin/api/v1/tickets/` + `getSuffix()` |
| Routing (actual) | Suffix servlet not invoked on local AEM SDK for `/bin/{id}` sub-paths |
| Conflict resolution | **Not option (a) merge** — kept `TicketCollectionServlet` on exact path; added `TicketByIdRoutingFilter` for by-id |
| `TicketByIdServlet` | Handler class (not separate `@Component` servlet) — logic separated from collection servlet |
| Filter pattern | `/bin/api/v1/tickets/[^/]+$` — excludes `/assignee`, `/status`, `/comments` sub-resources |
| PUT body | Rejects `status` and `assignedTo` fields with `400 VALIDATION_ERROR` before calling service |
| ID resolution | `ServletPathUtil.resolveTicketId()` — suffix first, URI path fallback |

---

## Servlet / filter behavior summary

| Method | Path | Handler | Success | Errors |
|--------|------|---------|---------|--------|
| `GET` | `/bin/api/v1/tickets/{id}` | `TicketByIdRoutingFilter` → `TicketByIdServlet.doGet` | `200` + `TicketDTO` | `404 NOT_FOUND`, `500` |
| `PUT` | `/bin/api/v1/tickets/{id}` | `TicketByIdRoutingFilter` → `TicketByIdServlet.doPut` | `200` + `TicketDTO` | `400 VALIDATION_ERROR`, `400 TICKET_NOT_EDITABLE`, `404 NOT_FOUND`, malformed JSON → `400` |

`TicketCollectionServlet` unchanged: `GET`/`POST` on exact `/bin/api/v1/tickets` only.

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/TicketByIdServlet.java` | Created — by-id GET/PUT handler methods |
| `core/src/main/java/com/mysite/core/servlets/TicketByIdRoutingFilter.java` | Created — Sling filter dispatch |
| `core/src/main/java/com/mysite/core/servlets/util/ServletPathUtil.java` | Updated — `resolveTicketId()`, `isTicketIdOnly()` |
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Updated — `TICKETS_SUFFIX_PATH` |
| `core/src/main/java/com/mysite/core/servlets/TicketCollectionServlet.java` | Updated — Javadoc notes filter routing; reverted to collection-only registration |
| `implementation-plan.md` | Updated — 5.1.2 complete; Active Task 5.1.3 |
| `ai-prompts/implementation/16-ticket-by-id-servlet.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| No servlet conflict with `TicketCollectionServlet` | Confirmed — exact path only; filter handles sub-paths |
| `GET /bin/api/v1/tickets/NOPE` | `404` + `NOT_FOUND` JSON |
| `PUT` invalid priority | `400` + `VALIDATION_ERROR` |
| `PUT` with `status` in body | `400` + `"status cannot be updated on this endpoint"` |
| `GET /bin/api/v1/tickets` (collection) | `200` + `[]` (unchanged) |
| `POST` create ticket | `500 INTERNAL_ERROR` — CF/DAM persistence (pre-existing; servlet layer correct) |

---

## curl test commands

```bash
# a) GET detail (replace TKT-1001 with a real id)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/tickets/TKT-1001

# b) GET missing ticket → 404 NOT_FOUND
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/tickets/NOPE

# c) PUT update fields → 200
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001 \
  -d '{"title":"Login page broken — updated","description":"OAuth redirect fails","priority":"P1"}'

# d) PUT invalid priority → 400 VALIDATION_ERROR
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001 \
  -d '{"title":"x","description":"y","priority":"INVALID"}'

# e) PUT on Closed ticket → 400 TICKET_NOT_EDITABLE (after 5.1.4 status endpoint)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001 \
  -d '{"title":"Too late","description":"Cannot edit","priority":"P2"}'
```

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.2 | Complete |
| FR-3, FR-5, FR-7, FR-18 | Ticket detail GET; field update PUT; terminal edit guard |
| AC-9, AC-10, AC-13–AC-17 | Detail 404; update validation; terminal ticket rejection |
| api-contract | PUT rejects `status`/`assignedTo`; dedicated sub-resources deferred to 5.1.3–5.1.4 |
| Downstream | 5.1.3 `TicketAssigneeServlet`; extend filter pattern for `/assignee` suffix |
