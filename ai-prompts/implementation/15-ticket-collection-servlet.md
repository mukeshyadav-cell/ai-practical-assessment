# AI Prompt — Task 5.1.1: TicketCollectionServlet + Shared JSON/Error Helper

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.1.

**Sprint/Task:** 5.1 / 5.1.1
**Category:** implementation
**Meaningful:** Yes — first REST servlet exposing ticket list/create with shared JSON error handling.

---

## Prompt (verbatim)

> Task 5.1.1 (Sprint 5.1): Implement TicketCollectionServlet handling GET (list) and
> POST (create) at /bin/api/v1/tickets, plus a shared JSON/error-handling helper.
>
> Follow all rules in .cursor/rules/ (04-aem-correctness for servlet registration; use Jackson
> for JSON). Read api-contract.md (list + create shapes, query params ?status= and ?q=,
> status codes, error catalog), TicketService, TicketDTO, and the DomainException hierarchy.
> Target: com.mysite.core.servlets.
>
> IMPORTANT AEM correctness (do NOT invent APIs; if unsure, say so):
> - Register via @Component(service = Servlet.class) with property
>   "sling.servlet.paths=/bin/api/v1/tickets" and "sling.servlet.methods={GET,POST}"
> - Extend org.apache.sling.api.servlets.SlingAllMethodsServlet (supports GET + POST)
> - Read request body via request.getReader(); parse JSON with Jackson ObjectMapper
> - Set response.setContentType("application/json"); response.setCharacterEncoding("UTF-8")
>
> Create:
>
> 1. A shared helper: com.mysite.core.servlets.util.ServletResponseUtil (or similar)
>    - writeJson(SlingHttpServletResponse resp, int status, Object body)
>        // sets content type, status, writes Jackson JSON
>    - writeError(SlingHttpServletResponse resp, int status, String code, String message)
>        // writes { "error": message, "code": code }
>    - A single ObjectMapper configured to serialize java.time.Instant as ISO-8601
>      (register JavaTimeModule / JSR-310; disable WRITE_DATES_AS_TIMESTAMPS)
>    - A helper mapDomainException(DomainException e) -> (status, code, message) OR a
>      handle(resp, exception) method that maps:
>        ValidationException/UnknownUser/TicketNotEditable -> 400 (their codes)
>        TicketNotFoundException -> 404
>        InvalidTransitionException -> 409
>        any other Exception -> 500 INTERNAL_ERROR
>      Use e.errorCode() and e.httpStatus() if available.
>
> 2. TicketCollectionServlet:
>    doGet:
>      - Read query params: status (optional), q (optional)
>      - Call ticketService.listTickets(status, q)
>      - Return 200 with JSON array of TicketDTO
>    doPost:
>      - Parse request body JSON -> TicketDTO
>      - Call ticketService.createTicket(dto)
>      - Return 201 with the created TicketDTO in the body
>      - On DomainException -> mapped status + {error, code} via the helper
>      - On malformed JSON -> 400 VALIDATION_ERROR ("Malformed request body")
>
> 3. Wiring:
>    @Reference private TicketService ticketService;
>    Constants for the path and any header names. SLF4J logging (warn on client errors,
>    error on 500s). Javadoc on the servlet + methods.
>
> Constraints:
> - Servlet is THIN: parse -> call service -> serialize. No business logic in the servlet.
> - Never leak stack traces to the client; log them server-side, return clean JSON errors.
> - Work only with DTOs (services already return DTOs).
>
> After generating:
> - Confirm mvn clean install compiles and deploy: mvn clean install -PautoInstallSinglePackage
> - Provide EXACT curl commands to test:
>     a) POST create a ticket (JSON body) -> expect 201 + body
>     b) GET list -> expect 200 + array
>     c) GET list?status=Open and ?q=<keyword>
>     d) POST with blank title -> expect 400 VALIDATION_ERROR
> - Confirm the servlet is registered (Sling > Servlet Resolver or by hitting the URL).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Implemented `TicketCollectionServlet` (`GET`/`POST` at `/bin/api/v1/tickets`) and shared servlet utilities: `ServletResponseUtil` (Jackson `ObjectMapper` with `JavaTimeModule`, `writeJson`, `writeError`, `mapDomainException`, `handleException`), `ErrorResponse` POJO, and `ServletConstants`. Servlet is thin — parses query/body, sets `createdBy` from session user, delegates to `TicketService`, maps `DomainException` via `errorCode()`/`httpStatus()`, and returns `400 VALIDATION_ERROR` for malformed JSON. `mvn clean install -pl core -am` and `mvn clean install -PautoInstallSinglePackage` both succeeded; live `GET /bin/api/v1/tickets` returned `200 []`; blank-title POST returned `400 VALIDATION_ERROR`. Updated `implementation-plan.md` — 5.1.1 complete; Active Task → 5.1.2.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Servlet registration | `@Component(service = Servlet.class)` with `sling.servlet.paths` + separate `sling.servlet.methods=GET` / `POST` |
| Base class | `SlingAllMethodsServlet` (GET + POST on same path) |
| JSON library | Jackson `ObjectMapper` with `JavaTimeModule`; `WRITE_DATES_AS_TIMESTAMPS` disabled |
| `createdBy` | Resolved from `request.getRemoteUser()` / `getUserPrincipal()` per api-contract |
| Malformed JSON | `400` / `VALIDATION_ERROR` / `"Malformed request body"` |
| Domain errors | `ServletResponseUtil.handleException` uses `DomainException.httpStatus()` + `errorCode()` |
| Java compatibility | Avoided `record` and pattern-matching `instanceof` (compiler `release` 11) |
| Shared error helper | Partially addresses Task 5.1.7; reusable by subsequent servlets |

---

## Servlet behavior summary

| Method | Path | Behavior | Success | Error mapping |
|--------|------|----------|---------|---------------|
| `GET` | `/bin/api/v1/tickets` | `listTickets(status, q)` | `200` + `TicketDTO[]` | `DomainException` → mapped code/status; other → `500 INTERNAL_ERROR` |
| `POST` | `/bin/api/v1/tickets` | Parse body → set `createdBy` → `createTicket` | `201` + `TicketDTO` | Malformed JSON → `400 VALIDATION_ERROR`; `DomainException` → mapped; other → `500` |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Created |
| `core/src/main/java/com/mysite/core/servlets/util/ErrorResponse.java` | Created |
| `core/src/main/java/com/mysite/core/servlets/util/ServletResponseUtil.java` | Created |
| `core/src/main/java/com/mysite/core/servlets/TicketCollectionServlet.java` | Created |
| `implementation-plan.md` | Updated — 5.1.1 complete; Active Task 5.1.2 |
| `ai-prompts/implementation/15-ticket-collection-servlet.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core -am` | SUCCESS (59 tests pass) |
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| `GET /bin/api/v1/tickets` (live) | `200` + `[]` |
| `POST` blank title (live) | `400` + `VALIDATION_ERROR` |
| `POST` with `assignedTo: agent-1` (live) | `400` + `UNKNOWN_USER` (seeded users not present on instance) |
| `POST` without assignee (live) | `500 INTERNAL_ERROR` (CF/DAM persistence — check AEM error.log) |

---

## curl test commands

```bash
# a) POST — create ticket (expect 201)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"Login page broken","description":"Users cannot sign in","priority":"P2","assignedTo":"agent-1"}'

# b) GET — list all (expect 200 + array)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/tickets

# c) GET — filtered list
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  "http://localhost:4502/bin/api/v1/tickets?status=Open"

curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  "http://localhost:4502/bin/api/v1/tickets?q=login"

# d) POST — blank title (expect 400 VALIDATION_ERROR)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"","description":"Users cannot sign in","priority":"P2"}'
```

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.1 | Complete |
| FR-1, FR-2, FR-4, FR-11, FR-12, FR-18 | List + create ticket REST endpoints |
| AC-1–AC-8, AC-37–AC-40 | Create validation; list/search/filter |
| DOD-1, DOD-2 (Sprint 5.1) | First two of ten api-contract endpoints |
| Downstream | 5.1.2 `TicketByIdServlet`; 5.1.7 shared error handling (helper started here) |
