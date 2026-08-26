# AI Prompt — Task 4.1.3: TicketService (Interface + Impl)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 4.1 Task 4.1.3.

**Sprint/Task:** 4.1 / 4.1.3
**Category:** implementation
**Meaningful:** Yes — business logic hub wiring repositories, state machine, and domain exceptions.

---

## Prompt (verbatim)

> Task 4.1.3 (Sprint 4.1): Implement TicketService (interface + impl) — the business logic
> hub wiring repositories, the state machine, and domain exceptions.
>
> Follow all rules in .cursor/rules/. Read api-contract.md (endpoint behaviors + error codes),
> requirements-analysis.md (FR-1..FR-12, FR-7 terminal edit rule), acceptance-criteria.md,
> the TicketRepository/UserRepository interfaces, TicketStateMachine, TicketStatus enum,
> and the domain exceptions. Target: com.mysite.core.services (interface) and
> com.mysite.core.services.impl.
>
> Create:
>
> 1. Interface com.mysite.core.services.TicketService with methods:
>    - TicketDTO createTicket(TicketDTO ticket)
>    - List<TicketDTO> listTickets(String statusFilter, String query)  // both nullable/optional
>    - TicketDTO getTicket(String id)                                  // throws TicketNotFoundException
>    - TicketDTO updateTicket(String id, TicketDTO changes)            // title/description/priority only
>    - TicketDTO reassignTicket(String id, String assigneeUserId)
>    - TicketDTO changeStatus(String id, String newStatusLabel)        // enforces state machine
>    Javadoc each with behavior + which exceptions it may throw.
>
> 2. Impl com.mysite.core.services.impl.TicketServiceImpl:
>    @Component(service = TicketService.class)
>    @Reference(target = "(impl.type=contentfragment)") private TicketRepository ticketRepository;
>    @Reference(target = "(impl.type=aem)")            private UserRepository userRepository;
>    @Reference private TicketStateMachine stateMachine;
>    // If TicketStateMachine is not an OSGi component, instantiate it directly instead of @Reference —
>    // choose the cleaner option and state which; it is pure logic so a plain 'new' is acceptable.
>
>    Method behavior:
>
>    a) createTicket:
>       - Validate required fields: title non-blank (throw ValidationException), priority is P1..P4.
>       - FORCE status = "Open" (ignore incoming status).
>       - If assignedTo is provided (non-blank), verify it exists via userRepository.getById,
>         else throw UnknownUserException. Blank assignedTo = unassigned (allowed).
>       - Delegate to ticketRepository.create (which sets id + timestamps). Return persisted DTO.
>
>    b) listTickets(statusFilter, query):
>       - If query non-blank -> ticketRepository.searchByTitle(query)
>       - else if statusFilter non-blank -> ticketRepository.findByStatus(statusFilter)
>       - else -> ticketRepository.getAll()
>       - (If both provided, prefer combining: filter search results by status; keep it simple
>         and document the chosen precedence.)
>       - Sort by createdAt DESCENDING (newest first) per api-contract/list behavior.
>
>    c) getTicket(id):
>       - ticketRepository.getById(id).orElseThrow(() -> new TicketNotFoundException(id))
>
>    d) updateTicket(id, changes):
>       - Load existing (getTicket -> 404 if missing).
>       - If existing status is terminal (Closed or Cancelled) -> throw TicketNotEditableException.
>       - Apply only mutable fields from changes: title, description, priority (validate priority enum).
>       - Do NOT change status/assignedTo/createdBy/createdAt here.
>       - Delegate to ticketRepository.update. Return updated DTO.
>
>    e) reassignTicket(id, assigneeUserId):
>       - Load existing (404 if missing).
>       - If terminal status -> TicketNotEditableException.
>       - Verify assigneeUserId exists via userRepository.getById -> else UnknownUserException.
>         (Allow explicit unassign if assigneeUserId is blank? For MVP require a valid user;
>          document the choice.)
>       - Set assignedTo, update via repository. Return DTO.
>
>    f) changeStatus(id, newStatusLabel):
>       - Load existing (404 if missing).
>       - Convert existing.status and newStatusLabel via TicketStatus.fromLabel
>         (IllegalArgumentException -> wrap as ValidationException for unknown status label).
>       - stateMachine.assertCanTransition(from, to)  // throws InvalidTransitionException (-> 409)
>       - Set the new status, update via repository. Return DTO.
>       - IMPORTANT: status changes go ONLY through here — never via updateTicket.
>
> 3. Constraints:
>    - Service works with DTOs only (no ContentFragment/Resource).
>    - All validation throws the appropriate DomainException subclass.
>    - Javadoc + SLF4J logging (info on state changes; warn/error on failures).
>    - Keep methods readable; extract small private helpers (e.g., assertNotTerminal(ticket),
>      validatePriority(p), requireExistingUser(id)).
>
> After generating:
> - Confirm mvn clean install compiles and the component is satisfied in OSGi console
>   (check the @Reference targets resolve: contentfragment + aem).
> - Summarize each method's validation + exceptions so I can verify against api-contract.md.
> Do NOT write service unit tests yet (that is Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `TicketService` interface (six methods with Javadoc listing exceptions) and `TicketServiceImpl` as OSGi `@Component` with `@Reference` to `TicketRepository` (`impl.type=contentfragment`) and `UserRepository` (`impl.type=aem`). `TicketStateMachine` instantiated directly via `new` (pure logic, not OSGi). Implemented create (title/priority validation, force Open, optional assignee check), list (query + status intersection when both set, `createdAt` DESC sort), get, update (terminal guard, mutable fields only), reassign (MVP: assignee required, no unassign), and changeStatus (`assertCanTransition` only path for status changes). Extended `ContentFragmentTicketRepository.update()` to persist `status` element for `changeStatus`. Verified `mvn clean compile -pl core` and `mvn clean test -pl core`. Updated `implementation-plan.md` — 4.1.3 complete; Active Task → 4.1.4.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| `TicketStateMachine` | Direct `new TicketStateMachine()` — not an OSGi component |
| `listTickets` precedence | When both `query` and `statusFilter` set: search then filter by status (AC-40 / FR-12) |
| `reassignTicket` | MVP requires non-blank assignee; unassign deferred (api-contract allows later) |
| `changeStatus` persistence | Repository `update()` extended to write `status` CF element |
| `updateTicket` | Status/assignee never changed; only title/description/priority from non-null `changes` fields |
| Tests | Deferred to Task 4.1.6 per prompt |

---

## Method summary (validation + exceptions)

| Method | Validation / behavior | Exceptions |
|--------|----------------------|------------|
| `createTicket` | Title non-blank; priority P1–P4; force `Open`; optional `assignedTo` verified | `ValidationException`, `UnknownUserException` |
| `listTickets` | Query search; optional status intersect/filter; sort `createdAt` DESC | `ValidationException` (invalid status filter) |
| `getTicket` | Load by id | `TicketNotFoundException`, `ValidationException` (blank id) |
| `updateTicket` | Terminal guard; apply title/description/priority only | `TicketNotFoundException`, `TicketNotEditableException`, `ValidationException` |
| `reassignTicket` | Terminal guard; assignee required and must exist | `TicketNotFoundException`, `TicketNotEditableException`, `ValidationException`, `UnknownUserException` |
| `changeStatus` | Parse labels; `stateMachine.assertCanTransition`; persist status | `TicketNotFoundException`, `ValidationException`, `InvalidTransitionException` |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/services/TicketService.java` | Created |
| `core/src/main/java/com/mysite/core/services/impl/TicketServiceImpl.java` | Created |
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentTicketRepository.java` | Updated — `update()` persists status |
| `implementation-plan.md` | Updated — 4.1.3 complete; Active Task 4.1.4 |
| `ai-prompts/implementation/12-ticket-service.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean compile -pl core` | SUCCESS |
| `mvn clean test -pl core` | SUCCESS |
| OSGi `TicketServiceImpl` Active + references | Verify locally after `mvn clean install -PautoInstallBundle -pl core` |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.3 | Complete |
| FR-1–FR-12 | Create, list, get, update, reassign, changeStatus, search/filter |
| FR-7 | `TicketNotEditableException` on terminal update/reassign |
| FR-8–FR-10 | `changeStatus` delegates to `TicketStateMachine` only |
| AC-40 | Combined `q` + `status` filter via intersection |
| DOD-3 (Sprint 4.1) | `TicketService.changeStatus` delegates to state machine |
| Downstream | 4.1.4 `CommentService`; 5.1 servlets |
