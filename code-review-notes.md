# Code Review Notes — Self Review

**Date:** 2026-08-27  
**Purpose:** Honest self-assessment of the Support Ticket Management System implementation — strengths, improvement areas, security observations, and checklist.

**Sprint/Task:** 8.1 / 8.1.2

---

## Review Scope

This review covers the **mandatory-core** implementation delivered across Sprints 2.1–7.1:

| Area | Paths reviewed |
|------|----------------|
| State machine | `core/.../statemachine/TicketStateMachine.java`, `TicketStatus.java` |
| Domain exceptions | `core/.../exception/*` |
| Validation | `core/.../validation/TicketValidator.java`, `CommentValidator.java` |
| Services | `core/.../services/impl/TicketServiceImpl.java`, `CommentServiceImpl.java` |
| Repositories | `core/.../repositories/impl/ContentFragment*Repository.java`, `AemUserRepository.java` |
| Mappers | `core/.../mappers/TicketMapper.java`, `CommentMapper.java` |
| REST layer | `core/.../servlets/*Servlet.java`, `*RoutingFilter.java`, `ServletResponseUtil.java` |
| Unit tests | `core/src/test/.../TicketStateMachineTest.java`, `*ServiceImplTest.java` |
| UI | `ui.frontend/.../ticketing/*.ts`, HTL under `ui.apps/.../ticket*` |

**Not in scope for this review:** `it.tests`, `ui.tests`, dispatcher farm tuning, Cloud Manager pipeline config.

---

## Strengths

### 1. Clean layering and Repository Pattern (swappability)

Services depend on repository **interfaces** with OSGi target filters (`impl.type=contentfragment`, `impl.type=aem`). Example from `TicketServiceImpl`:

```java
@Reference(target = "(impl.type=contentfragment)")
private TicketRepository ticketRepository;

@Reference(target = "(impl.type=aem)")
private UserRepository userRepository;
```

Servlets inject only services — no CF or JCR types in the servlet layer. A future `impl.type=database` adapter can be swapped without UI or servlet changes. This matches the architecture rules and `design-notes.md`.

### 2. State machine as pure, well-tested domain logic

`TicketStateMachine` has **no AEM/Sling/JCR imports** — only `EnumMap`/`EnumSet` and `InvalidTransitionException`. Instantiated with `new` inside `TicketServiceImpl` (not OSGi), which keeps tests simple.

`TicketStateMachineTest` provides **62 tests** including a parameterized full invalid-transition matrix mapped to AC-22–AC-35. `TicketServiceImpl.changeStatus()` is the only service path that mutates status and delegates exclusively to `stateMachine.assertCanTransition()`.

### 3. DTO boundary — no leaks above repository

`TicketMapper.toDto(ContentFragment)` and repository impls confine `ContentFragment`, `Resource`, and `ResourceResolver` to `.repositories.impl`. Services and servlets work only with `TicketDTO`, `CommentDTO`, and `UserDTO`. Field naming is consistent (`id` in DTO ↔ `ticketId`/`commentId` CF elements).

### 4. Consistent error handling

`DomainException` subclasses expose `errorCode()` and `httpStatus()`. All servlets route through `ServletResponseUtil.execute()` → `handleException()`:

- Malformed JSON → `400 VALIDATION_ERROR`
- Domain errors → catalog codes (`INVALID_TRANSITION` → 409, `TICKET_NOT_EDITABLE` → 400, etc.)
- Unexpected errors → `500 INTERNAL_ERROR` with generic message; stack trace logged server-side only

This gives predictable JSON for the TypeScript `ApiRequestError` parser in `api.ts`.

### 5. Reusable UI layer

- **`api.ts`** — single module for all REST calls, typed interfaces, shared `parseApiResponse`, `fetchWithCsrf` for mutating requests
- **`list.ts`** — unified `ListState` (`q`, `status`, `priority`, `sort`) with one refresh pipeline: fetch → client priority filter → sort → render + summary
- **`dom.ts`** — `createElement` uses `textContent` for safe text insertion
- **`transitions.ts`** — documents that server is authoritative; UI restricts dropdown choices

---

## Observations / Potential Improvements

### Concurrency — ID counter not atomic

`ContentFragmentTicketRepository.generateTicketId()` reads/increments a JCR property at `/var/assessment/ticket-id-counter`. Javadoc explicitly notes this is **not atomic** under concurrent creates. Acceptable for assessment scope; production would need Oak counter API or DB sequences.

### In-memory repository queries at scale

`findByStatus()` and `searchByTitle()` call `getAll()` then filter in Java:

```java
for (TicketDTO ticket : getAll()) {
    if (status.equals(ticket.getStatus())) {
        matches.add(ticket);
    }
}
```

`getById()` also scans DAM children (`findContentFragmentByTicketId` iterates folder children). Fine for demo volumes; at scale, use JCR Query Builder / Oak index on CF elements or move to SQL in a DB adapter.

### Client vs server filtering asymmetry

- **Server:** `?status=`, `?q=` (title) on `GET /tickets`
- **Client:** priority filter and sort (Sprint 6.2) on the fetched array

Trade-off documented in `design-notes.md`. Risk: priority filter only applies to tickets already returned; large datasets would need `?priority=` server-side.

### Authorship and authorization gaps (scope)

- **`GET /me`** + UI `userContext.ts` supply `createdBy` for create/comment
- **`TicketCollectionServlet`** correctly **overwrites** `createdBy` from the AEM session on POST
- **`CommentCollectionServlet.resolveCreatedBy()`** prefers the **request body** `createdBy` when non-blank — a client could spoof authorship unless the servlet always overwrites from session (ticket create does; comments do not consistently)
- No per-ticket RBAC, no custom auth layer — any logged-in AEM user can call all endpoints (in scope for assessment)

### Duplication and magic strings

| Duplication | Location | Suggestion |
|-------------|----------|------------|
| Status labels (`"Open"`, `"In Progress"`, …) | `TicketStatus` enum, `transitions.ts`, `list.ts` STATUS_OPTIONS, repository `STATUS_OPEN` constant | Generate UI options from API or share a constants module; keep TS map in sync with Java enum |
| Transition rules | `TicketStateMachine` + `transitions.ts` | Acceptable for UX; document sync requirement (already in `transitions.ts` comment) |
| Service subservice name | Repeated `assessment-service` in three repository impls | Could centralize in one constants class |

### Silent failure on resolver login errors

`ContentFragmentTicketRepository.getAll()` catches `LoginException` and returns `Collections.emptyList()` instead of propagating an error. Callers see an empty list, not `500` — can mask misconfigured service-user mapping during development (was a real debugging issue; see `debugging-notes.md`).

### Servlet routing complexity

Suffix servlet registration failed on local SDK; **routing filters** dispatch sub-paths (`TicketByIdRoutingFilter`, etc.). Works reliably but adds classes and diverges slightly from `api-contract.md` suffix documentation.

### Archetype sample code remains

`HelloWorldModel`, `SimpleServlet`, `LoggingFilter`, `SimpleScheduledTask`, `SimpleResourceListener` and their tests are still in `core` (tracked for removal in `design-notes.md`). Increases noise in builds and test runs.

### Test coverage gaps (by design)

| Gap | Reason |
|-----|--------|
| Repository CF adapters | Need AEM runtime or heavy mocks |
| Servlets / routing filters | Thin delegation; manually verified via curl |
| UI TypeScript | No Cypress suite; browser E2E manual |
| Concurrency on ID counter | Out of scope |

~96 focused unit tests on state machine + services; appropriate for assessment goals but not full-stack regression safety.

---

## Security Review

| Topic | Finding |
|-------|---------|
| **Secrets in repo** | No API keys or real passwords in application code. Archetype `ui.tests/pom.xml` has default `admin` placeholders (archetype default, not used). Repoinit uses seeded users without committed secrets. |
| **Service user least-privilege** | `assessment-service` has explicit ACLs: DAM read/write, `/var/assessment`, `/conf/assessment`, `/home/users` read — not admin. Mapped via OSGi `ServiceUserMapperImpl`. |
| **XSS in UI** | User-generated content (titles, descriptions, comments) rendered via `textContent` in `dom.ts`, `detail.ts`, `form.ts`, `toast.ts` — no `innerHTML` for ticket data. |
| **CSRF** | Mutating UI calls use `fetchWithCsrf` (`csrf.ts` → Granite token). Required for AEM author POST/PUT. |
| **Stack trace leakage** | `ServletResponseUtil` returns generic `INTERNAL_ERROR` message; logs full stack server-side. |
| **createdBy trust** | Ticket create: session wins. Comment create: body can override — **watch** if spoofing matters. |
| **Input validation** | Server-side validators for title, description, priority, status labels; state machine enforces transitions. |
| **CORS / hostnames** | UI uses relative `/bin/api/v1` paths only (FR-19). |

---

## Code Review Checklist

| Item | Status | Comment |
|------|--------|---------|
| Layering UI → Servlet → Service → Repository | **Pass** | Strict; no CF types above repo |
| Repository Pattern / swappability | **Pass** | OSGi `impl.type` targets |
| State machine centralized | **Pass** | `TicketStateMachine` only; 409 on invalid |
| DTO boundary | **Pass** | Mappers isolate CF API |
| Domain exception → HTTP mapping | **Pass** | `ServletResponseUtil` + catalog |
| Servlets thin | **Pass** | Parse, delegate, serialize |
| Validation before persistence | **Pass** | `TicketValidator` / service checks |
| Unit tests for business rules | **Pass** | 62 + 23 + 11 tests |
| No secrets committed | **Pass** | Verified grep; archetype test POM only |
| XSS-safe UI | **Pass** | `textContent` pattern |
| CSRF on mutations | **Pass** | `fetchWithCsrf` in `api.ts` |
| Service user ACLs | **Pass** | Repoinit + mapping documented |
| ID generation concurrency | **Note** | Non-atomic; documented risk |
| Repository query efficiency | **Note** | In-memory filter/scan OK at demo scale |
| Comment `createdBy` from body | **Note** | Consider always using session user |
| api-contract vs routing filters | **Note** | Doc/runtime mismatch |
| Archetype demo code cleanup | **Note** | Still present |
| Integration / E2E tests | **Note** | Out of scope by design |
| Pagination | **Note** | Full list returned (assumption A-2) |

---

## Overall Assessment

The codebase delivers a **coherent, maintainable mandatory-core** implementation for an AEM learning assessment. The strongest qualities are **architectural discipline** (layering, repository ports, pure state machine) and **consistent API error contracts** that the UI can rely on. Business rules are testable without a running AEM instance.

The main technical debt is **scale-oriented** (in-memory JCR scans, non-atomic counters) and **documentation drift** (suffix vs filter routing, duplicated status strings in TS/Java). Security is appropriate for a local author-only demo, with the main watch item being **comment authorship trust** if the API were exposed beyond trusted authors.

**Verdict:** **Good quality for scope** — production-ready patterns in structure and domain logic, with honest limitations documented. A reviewer merging this PR would approve the architecture and test strategy while filing follow-up tickets for counter atomicity, repository query optimization, and archetype cleanup.

---

## Related Documents

- [design-notes.md](design-notes.md) — architecture and design decisions
- [test-strategy.md](test-strategy.md) — what is and is not tested
- [debugging-notes.md](debugging-notes.md) — issues encountered during build
