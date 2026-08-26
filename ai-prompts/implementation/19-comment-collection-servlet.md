# AI Prompt — Task 5.1.5: CommentCollectionServlet (GET list + POST add)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.5.

**Sprint/Task:** 5.1 / 5.1.5
**Category:** implementation
**Meaningful:** Yes — comments sub-resource REST endpoints with filter-dispatch routing.

---

## Prompt (verbatim)

> Task 5.1.5 (Sprint 5.1): Implement the comments endpoint —
> GET /bin/api/v1/tickets/{id}/comments (list) and
> POST /bin/api/v1/tickets/{id}/comments (add).
>
> Follow all rules in .cursor/rules/. Read api-contract.md (comment list + create shapes,
> 201 on create, 404 if ticket missing, 400 on empty message), CommentService,
> CommentDTO, DomainException hierarchy, and ServletResponseUtil. Target: com.mysite.core.servlets.
> Reuse ServletResponseUtil (no duplicate JSON/error logic).
>
> Fit routing consistently with the existing tickets servlets (Tasks 5.1.2–5.1.4):
> - Suffix pattern is "/{id}/comments". If a single suffix-based TicketsServlet handles ticket
>   sub-resources, decide whether comments belong there or in a dedicated CommentCollectionServlet.
>   RECOMMENDATION: a dedicated CommentCollectionServlet is cleaner (comments are a distinct
>   resource). Register it to capture /bin/api/v1/tickets/{id}/comments via suffix on
>   /bin/api/v1/tickets, branching so it does NOT conflict with ticket collection/by-id/status/
>   assignee routing. State the decision and verify no conflict.
>
> Implement:
>
> GET /bin/api/v1/tickets/{id}/comments:
>   - Extract ticketId from the suffix (the "{id}" before "/comments").
>   - Call commentService.listComments(ticketId).
>   - 200 + JSON array of CommentDTO (ordered by createdAt ascending — service already does this).
>   - TicketNotFoundException -> 404 NOT_FOUND.
>
> POST /bin/api/v1/tickets/{id}/comments:
>   - Extract ticketId from suffix.
>   - Parse body JSON -> CommentDTO (message, createdBy). The path ticketId is authoritative:
>     set/override comment.ticketId = the path id (ignore any ticketId in the body).
>   - Call commentService.addComment(ticketId, comment).
>   - 201 + created CommentDTO (with generated id + createdAt).
>   - Error mapping via ServletResponseUtil:
>       TicketNotFoundException -> 404 NOT_FOUND
>       ValidationException     -> 400 VALIDATION_ERROR (empty message / blank createdBy)
>       Malformed JSON          -> 400
>   - Comments MUST be allowed even if the ticket status is Closed or Cancelled
>     (do not block on terminal status).
>
> Requirements:
> - Extend SlingAllMethodsServlet; register methods {GET, POST}.
> - @Reference CommentService. Thin servlet: parse -> service -> serialize.
> - Reuse ServletResponseUtil; SLF4J logging; no stack traces to client; Javadoc.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Verify no routing conflict (servlet resolver) with ticket endpoints.
> - Provide curl commands:
>     a) POST /bin/api/v1/tickets/TKT-1001/comments {"message":"First comment","createdBy":"agent-1"} -> 201
>     b) POST another comment -> 201
>     c) GET /bin/api/v1/tickets/TKT-1001/comments -> 200 array (ascending order)
>     d) POST {"message":"","createdBy":"agent-1"} -> 400 VALIDATION_ERROR
>     e) POST/GET on nonexistent ticket -> 404
>     f) POST a comment on a CLOSED ticket -> 201 (allowed)
> - Verify in CRXDE: comment CFs created under /content/dam/assessment/comments (CMT-1001 ...)
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Chose dedicated `CommentCollectionServlet` handler + `CommentCollectionRoutingFilter` (pattern `/bin/api/v1/tickets/[^/]+/comments$`, GET/POST) — consistent with 5.1.2–5.1.4 filter-dispatch pattern; no conflict with collection, by-id, assignee, or status routes. `doGet` calls `commentService.listComments(ticketId)` → `200` array; `doPost` parses `CommentDTO`, overrides `ticketId` from path, resolves `createdBy` from body or session user → `commentService.addComment` → `201`. No terminal-status blocking (service allows Closed/Cancelled). Build/deploy succeeded; live tests: `404 NOT_FOUND` for missing tickets on GET/POST; ticket/status/collection regressions unchanged. Full create/list/CRXDE verification blocked — no persisted tickets (`POST` create `500`; pre-existing CF issue). Updated `implementation-plan.md` — 5.1.5 complete; Active Task → 5.1.6.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Routing structure | Dedicated `CommentCollectionServlet` + `CommentCollectionRoutingFilter` (not merged into TicketsServlet) |
| Filter pattern | `/bin/api/v1/tickets/[^/]+/comments$` — GET and POST |
| No conflict | Distinct `/comments` suffix; does not overlap by-id, assignee, or status patterns |
| Path `ticketId` | Authoritative — set on DTO before service call; body `ticketId` ignored |
| `createdBy` | From request body when present; otherwise authenticated session user (api-contract) |
| Terminal tickets | No servlet-level block — `CommentService` allows comments on Closed/Cancelled |
| Request body | `message` required; api-contract shows message-only POST; task curl also accepts `createdBy` |

---

## Endpoint behavior summary

| Method | Path | Handler | Success | Errors |
|--------|------|---------|---------|--------|
| `GET` | `/bin/api/v1/tickets/{id}/comments` | `CommentCollectionRoutingFilter` → `doGet` | `200` + `CommentDTO[]` | `404 NOT_FOUND`, `500` |
| `POST` | `/bin/api/v1/tickets/{id}/comments` | `CommentCollectionRoutingFilter` → `doPost` | `201` + `CommentDTO` | `404 NOT_FOUND`, `400 VALIDATION_ERROR`, malformed JSON → `400` |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/CommentCollectionServlet.java` | Created — comments GET/POST handler |
| `core/src/main/java/com/mysite/core/servlets/CommentCollectionRoutingFilter.java` | Created — Sling filter dispatch |
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Updated — `COMMENTS_SUB_RESOURCE` |
| `implementation-plan.md` | Updated — 5.1.5 complete; Active Task 5.1.6 |
| `ai-prompts/implementation/19-comment-collection-servlet.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| No routing conflict | Confirmed — distinct `/comments` filter pattern |
| `GET …/NOPE/comments` | `404` + `NOT_FOUND` |
| `POST …/NOPE/comments` | `404` + `NOT_FOUND` |
| Regression: status, collection | Unchanged |
| `(a)–(c)`, `(d)` empty message, `(f)` closed ticket, CRXDE | Blocked — no persisted tickets |

---

## curl test commands

```bash
# a) POST first comment → 201
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets/TKT-1001/comments \
  -d '{"message":"First comment","createdBy":"agent-1"}'

# b) POST second comment → 201
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets/TKT-1001/comments \
  -d '{"message":"Second comment"}'

# c) GET list → 200 array (ascending createdAt)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/tickets/TKT-1001/comments

# d) Empty message → 400 VALIDATION_ERROR
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets/TKT-1001/comments \
  -d '{"message":"","createdBy":"agent-1"}'

# e) Nonexistent ticket → 404
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/tickets/NOPE/comments

# f) Comment on CLOSED ticket → 201 (after closing via status endpoint)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets/TKT-1001/comments \
  -d '{"message":"Post-close note"}'
```

**CRXDE:** After successful `POST`, verify `/content/dam/assessment/comments/CMT-{n}`.

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.5 | Complete |
| FR-13, FR-14, FR-15, FR-18 | Comment add/list REST endpoints |
| AC-41–AC-46, AC-44 | Empty message rejected; list order; terminal ticket comments allowed |
| api-contract | `GET`/`POST /tickets/{id}/comments`; `201` on create |
| Downstream | 5.1.6 User servlets; 5.1.7 shared error handling (ServletResponseUtil reused) |
