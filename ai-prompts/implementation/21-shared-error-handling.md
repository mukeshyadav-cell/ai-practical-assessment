# AI Prompt — Task 5.1.7: Shared Error Handling Across Servlets

**Date:** 2026-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.7.

**Sprint/Task:** 5.1 / 5.1.7
**Category:** implementation
**Meaningful:** Yes — audit, harden, and refactor error handling across all REST servlets.

---

## Prompt (verbatim)

> Task 5.1.7 (Sprint 5.1): Audit and finalize consistent error handling across ALL servlets,
> aligned with the api-contract.md error catalog.
>
> Follow all rules in .cursor/rules/. Read api-contract.md (full error catalog: code -> HTTP
> status -> meaning), the DomainException hierarchy, ServletResponseUtil, and all servlets
> created in 5.1.1–5.1.6 (ticket collection/by-id/assignee/status, comment collection, users).
>
> Part A — Audit (report BEFORE changing code):
> Produce a table auditing every servlet + method + error path:
> | Servlet.method | Error scenario | Expected (api-contract) | Currently returns | Match? |
> Cover at minimum:
> - Malformed/empty JSON body on POST/PUT -> 400 VALIDATION_ERROR (all write endpoints)
> - Missing ticket -> 404 NOT_FOUND (detail, update, status, assignee, comments)
> - Invalid transition -> 409 INVALID_TRANSITION (status)
> - Unknown user -> 400 UNKNOWN_USER (assignee)
> - Terminal ticket edit/reassign -> 400 TICKET_NOT_EDITABLE
> - Blank required fields -> 400 VALIDATION_ERROR (create ticket, create comment)
> - Unknown status label -> 400 VALIDATION_ERROR (status)
> - Unexpected exception -> 500 INTERNAL_ERROR (all endpoints)
> - Unknown user id (GET /users/{id}) -> 404
> List any INCONSISTENCIES or gaps found.
>
> Part B — Harden ServletResponseUtil (single source of truth):
> - Ensure ONE place maps DomainException -> (httpStatus, code, message):
>     ValidationException      -> 400 VALIDATION_ERROR
>     UnknownUserException     -> 400 UNKNOWN_USER
>     TicketNotEditableException -> 400 TICKET_NOT_EDITABLE
>     TicketNotFoundException  -> 404 NOT_FOUND
>     InvalidTransitionException -> 409 INVALID_TRANSITION
>     any other DomainException -> use its errorCode()/httpStatus()
>     any non-DomainException   -> 500 INTERNAL_ERROR (log full stack server-side,
>                                  return generic message to client — NO stack trace leak)
> - Provide a single method every servlet uses in its catch block, e.g.:
>     handleException(SlingHttpServletResponse resp, Exception e)
> - Ensure the error JSON shape is EXACTLY { "error": <message>, "code": <code> } everywhere.
> - Ensure malformed JSON parsing is caught and returned as 400 VALIDATION_ERROR (not 500).
>
> Part C — Refactor servlets for consistency:
> - Every servlet doGet/doPost/doPut wraps its logic and delegates ALL exceptions to
>   ServletResponseUtil.handleException (or equivalent). Remove ad-hoc/duplicated catch logic.
> - Confirm every write endpoint guards malformed JSON.
> - Do NOT change success behavior (200/201 responses stay identical).
> - Close any gaps identified in Part A.
>
> Constraints:
> - No stack traces or internal messages leaked to clients.
> - Consistent Content-Type: application/json on ALL responses (success and error).
> - SLF4J: warn for 4xx client errors, error for 5xx.
> - Javadoc the exception-mapping method with the full code->status table.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Provide the FINAL error-handling coverage table (Part A re-run) showing all paths now match.
> - Provide 2-3 curl commands demonstrating a 400, a 404, and the 409 with correct JSON bodies.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Completed Parts A–C: audited all servlet error paths (found duplicate catch blocks, per-servlet malformed-JSON handling, and `UserByIdServlet` using local constants instead of shared helpers). Hardened `ServletResponseUtil` with `execute()`, `writeValidationError()`, `writeNotFoundError()`, and centralized `handleException()` — malformed JSON → 400 `VALIDATION_ERROR`; `DomainException` → `errorCode()`/`httpStatus()`; all other exceptions → 500 `INTERNAL_ERROR` with stack logged server-side only. Refactored all eight servlet/handler classes (TicketCollection, TicketById, TicketAssignee, TicketStatus, CommentCollection, UserCollection, UserById) to delegate exclusively through `ServletResponseUtil.execute()`. Build/deploy `mvn clean install -PautoInstallSinglePackage` succeeded. Live curl verification: 400 malformed JSON, 404 ticket/user not found, 400 validation errors; 409 `INVALID_TRANSITION` mapping confirmed via exception contract (blocked live demo — no tickets in DAM on test instance). Updated `implementation-plan.md` — 5.1.7 complete; Active Task → 5.1 Quality Gate.

---

## Part A — Pre-refactor audit (gaps found)

| Servlet.method | Error scenario | Expected (api-contract) | Previously returned | Match? |
|---|---|---|---|---|
| All write endpoints | Malformed/empty JSON | 400 `VALIDATION_ERROR` | Duplicated per-servlet `isMalformedJson` catches | Partial |
| All endpoints | Unexpected exception | 500 `INTERNAL_ERROR` | Mixed `catch (DomainException)` + `catch (Exception)` | Partial |
| TicketById/Assignee/Status/Comment | Bad path | 400 `VALIDATION_ERROR` | Ad-hoc `writeError` calls | Yes |
| UserByIdServlet.doGet | Unknown user id | 404 `NOT_FOUND` | Local constant + direct `writeError` | Partial |
| Service-thrown errors | Ticket not found, invalid transition, etc. | Per catalog | Via `handleException` when reached | Yes |

**Inconsistencies:** duplicate catch blocks, duplicated malformed-JSON handling, `UserByIdServlet` not using shared helpers.

---

## Part B — ServletResponseUtil changes

| Method | Purpose |
|---|---|
| `execute(response, log, action)` | Wraps servlet logic; catches all exceptions |
| `handleException(response, log, e)` | Single mapping: malformed JSON → 400; `DomainException` → catalog; else → 500 |
| `writeValidationError(response, log, msg)` | 400 `VALIDATION_ERROR` + warn log |
| `writeNotFoundError(response, log, msg)` | 404 `NOT_FOUND` + warn log |
| `mapDomainException(exception)` | Returns `(httpStatus, code, message)` tuple |

**Error catalog (Javadoc + api-contract.md):**

| code | HTTP | Source |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `ValidationException`; malformed JSON; servlet field checks |
| `UNKNOWN_USER` | 400 | `UnknownUserException` |
| `TICKET_NOT_EDITABLE` | 400 | `TicketNotEditableException` |
| `NOT_FOUND` | 404 | `TicketNotFoundException`; unknown user id |
| `INVALID_TRANSITION` | 409 | `InvalidTransitionException` |
| `INTERNAL_ERROR` | 500 | Any other non-domain exception |

---

## Part C — Final error-handling coverage table

| Servlet.method | Error scenario | Expected | Now returns | Match? |
|---|---|---|---|---|
| TicketCollectionServlet.doGet | Invalid `?status=` | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketCollectionServlet.doGet | Unexpected error | 500 `INTERNAL_ERROR` | 500 `INTERNAL_ERROR` | ✅ |
| TicketCollectionServlet.doPost | Malformed JSON | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketCollectionServlet.doPost | Blank title/description | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketCollectionServlet.doPost | Unknown assignee | 400 `UNKNOWN_USER` | 400 `UNKNOWN_USER` | ✅ |
| TicketCollectionServlet.doPost | Unexpected error | 500 `INTERNAL_ERROR` | 500 `INTERNAL_ERROR` | ✅ |
| TicketByIdServlet.doGet | Missing/bad ticket id | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketByIdServlet.doGet | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| TicketByIdServlet.doPut | Malformed JSON | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketByIdServlet.doPut | `status` in body | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketByIdServlet.doPut | `assignedTo` in body | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketByIdServlet.doPut | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| TicketByIdServlet.doPut | Terminal ticket edit | 400 `TICKET_NOT_EDITABLE` | 400 `TICKET_NOT_EDITABLE` | ✅ |
| TicketAssigneeServlet.doPut | Malformed JSON | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketAssigneeServlet.doPut | Missing `assignedTo` | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketAssigneeServlet.doPut | Unknown user | 400 `UNKNOWN_USER` | 400 `UNKNOWN_USER` | ✅ |
| TicketAssigneeServlet.doPut | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| TicketAssigneeServlet.doPut | Terminal ticket reassign | 400 `TICKET_NOT_EDITABLE` | 400 `TICKET_NOT_EDITABLE` | ✅ |
| TicketStatusServlet.doPut | Malformed JSON | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketStatusServlet.doPut | Missing/blank status | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketStatusServlet.doPut | Unknown status label | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| TicketStatusServlet.doPut | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| TicketStatusServlet.doPut | Invalid transition | 409 `INVALID_TRANSITION` | 409 `INVALID_TRANSITION` | ✅ |
| CommentCollectionServlet.doGet | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| CommentCollectionServlet.doPost | Malformed JSON | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| CommentCollectionServlet.doPost | Blank message | 400 `VALIDATION_ERROR` | 400 `VALIDATION_ERROR` | ✅ |
| CommentCollectionServlet.doPost | Ticket not found | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| UserCollectionServlet.doGet | Unexpected error | 500 `INTERNAL_ERROR` | 500 `INTERNAL_ERROR` | ✅ |
| UserByIdServlet.doGet | Unknown user id | 404 `NOT_FOUND` | 404 `NOT_FOUND` | ✅ |
| All endpoints | Unexpected exception | 500 `INTERNAL_ERROR` (no stack leak) | 500 `INTERNAL_ERROR` | ✅ |

---

## curl verification (live)

Use `-u admin:admin` for AEM auth.

**400 — malformed JSON:**
```bash
curl -s -u admin:admin -X POST http://localhost:4502/bin/api/v1/tickets \
  -H "Content-Type: application/json" -d '{bad json'
# {"error":"Malformed request body","code":"VALIDATION_ERROR"}  HTTP 400
```

**404 — ticket not found:**
```bash
curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets/TKT-9999
# {"error":"Ticket not found: TKT-9999","code":"NOT_FOUND"}  HTTP 404
```

**409 — invalid transition** (requires existing Open ticket; CF persistence blocked ticket creation on test instance):
```bash
curl -s -u admin:admin -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -H "Content-Type: application/json" -d '{"status":"Closed"}'
# Expected: {"error":"Invalid transition: Open -> Closed","code":"INVALID_TRANSITION"}  HTTP 409
```

---

## Files changed

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/util/ServletResponseUtil.java` | Centralized exception mapping, `execute()` wrapper, Javadoc error table |
| `core/src/main/java/com/mysite/core/servlets/TicketCollectionServlet.java` | Refactored to `execute()` |
| `core/src/main/java/com/mysite/core/servlets/TicketByIdServlet.java` | Refactored to `execute()` + `writeValidationError` |
| `core/src/main/java/com/mysite/core/servlets/TicketAssigneeServlet.java` | Refactored to `execute()` |
| `core/src/main/java/com/mysite/core/servlets/TicketStatusServlet.java` | Refactored to `execute()` |
| `core/src/main/java/com/mysite/core/servlets/CommentCollectionServlet.java` | Refactored to `execute()` |
| `core/src/main/java/com/mysite/core/servlets/UserCollectionServlet.java` | Refactored to `execute()` |
| `core/src/main/java/com/mysite/core/servlets/UserByIdServlet.java` | Refactored to `execute()` + `writeNotFoundError` |
| `implementation-plan.md` | 5.1.7 marked complete; Active Task → 5.1 Quality Gate |

---

## What I accepted / changed

Accepted the centralized `ServletResponseUtil.execute()` pattern over per-servlet `catch` blocks. Kept servlet-level path validation using `writeValidationError`/`writeNotFoundError` (not thrown as exceptions) for thin-servlet path checks.

## What I rejected / why

N/A — verified at task completion.

## Iteration needed

No — build passed; curl 400/404 verified live; 409 mapping confirmed via `InvalidTransitionException` contract.
