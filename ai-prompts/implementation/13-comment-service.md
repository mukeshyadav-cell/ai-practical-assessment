# AI Prompt — Task 4.1.4: CommentService (Interface + Impl)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 4.1 Task 4.1.4.

**Sprint/Task:** 4.1 / 4.1.4
**Category:** implementation
**Meaningful:** Yes — comment business logic with ticket-existence validation and terminal-status allowance.

---

## Prompt (verbatim)

> Task 4.1.4 (Sprint 4.1): Implement CommentService (interface + impl).
>
> Follow all rules in .cursor/rules/. Read api-contract.md (comment endpoints + error codes),
> requirements-analysis.md (FR-13..FR-15), acceptance-criteria.md (comment ACs),
> the CommentRepository and TicketRepository interfaces, CommentDTO, and the domain exceptions.
> Target: com.mysite.core.services (interface) and com.mysite.core.services.impl.
>
> Create:
>
> 1. Interface com.mysite.core.services.CommentService:
>    - CommentDTO addComment(String ticketId, CommentDTO comment)
>        // validates ticket exists + non-empty message; sets nothing the repo already sets
>    - List<CommentDTO> listComments(String ticketId)
>        // ordered by createdAt ascending; verifies ticket exists
>    Javadoc each with behavior + exceptions thrown.
>
> 2. Impl com.mysite.core.services.impl.CommentServiceImpl:
>    @Component(service = CommentService.class)
>    @Reference(target = "(impl.type=contentfragment)") private CommentRepository commentRepository;
>    @Reference(target = "(impl.type=contentfragment)") private TicketRepository ticketRepository;
>
>    Method behavior:
>
>    a) addComment(ticketId, comment):
>       - Verify the ticket exists: ticketRepository.getById(ticketId).orElseThrow ->
>         TicketNotFoundException(ticketId).
>       - Validate message: if comment.message is null/blank -> ValidationException
>         ("Comment message is required").
>       - Ensure the comment's ticketId is set to the path ticketId (authoritative), so the
>         client cannot mismatch body vs path.
>       - createdBy: use comment.createdBy if provided; if blank, decide policy — for MVP,
>         require createdBy non-blank (throw ValidationException) OR default to the current
>         request user later at the servlet layer. Choose "require non-blank createdBy here"
>         and document it.
>       - Comments ARE allowed even when the ticket status is Closed or Cancelled
>         (do NOT block on terminal status).
>       - Delegate to commentRepository.add (repo assigns id + createdAt). Return persisted DTO.
>
>    b) listComments(ticketId):
>       - Verify the ticket exists (404 if not) — for a clean API, listing comments of a
>         non-existent ticket should be TicketNotFoundException.
>       - Return commentRepository.listByTicket(ticketId) (already ordered ascending).
>       - Never return null (empty list if none).
>
> 3. Constraints:
>    - Work with DTOs only (no ContentFragment/Resource).
>    - Validation throws the appropriate DomainException subclass.
>    - Javadoc + SLF4J logging (info on comment added; warn/error on failures).
>    - Small private helpers if useful (e.g., requireTicketExists(ticketId)).
>
> After generating:
> - Confirm mvn clean install compiles and the component is satisfied in OSGi console
>   (both @Reference contentfragment targets resolve).
> - Summarize addComment/listComments validation + exceptions so I can verify against api-contract.
> Do NOT write unit tests yet (Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `CommentService` interface (`addComment`, `listComments` with Javadoc) and `CommentServiceImpl` as OSGi `@Component` with `@Reference` to `CommentRepository` and `TicketRepository` (both `impl.type=contentfragment`). `addComment` verifies ticket exists, validates non-blank message (`"Comment message is required"`) and non-blank `createdBy` (MVP — servlet supplies session user later), overwrites `ticketId` from path, does not block terminal statuses, delegates to `commentRepository.add`. `listComments` verifies ticket exists, returns `listByTicket` (never null). Helpers: `requireTicketExists`, `requireTicketId`. Verified `mvn clean compile test -pl core`. Updated `implementation-plan.md` — 4.1.4 complete; Active Task → 4.1.5.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Terminal ticket comments | Allowed — no status check on add/list (AC-42, AC-43) |
| `ticketId` | Path parameter is authoritative; overwrites body value |
| `createdBy` | Required non-blank at service layer; servlet sets from session in Sprint 5.1 |
| `id` / `createdAt` | Assigned by repository only |
| Tests | Deferred to Task 4.1.6 per prompt |

---

## Method summary (validation + exceptions)

| Method | Validation / behavior | Exceptions |
|--------|----------------------|------------|
| `addComment` | Ticket exists; message non-blank; `createdBy` non-blank; path `ticketId` authoritative | `TicketNotFoundException` (404), `ValidationException` (400) |
| `listComments` | Ticket exists; `listByTicket` ascending; empty list if none | `TicketNotFoundException` (404), `ValidationException` (blank ticketId) |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/services/CommentService.java` | Created |
| `core/src/main/java/com/mysite/core/services/impl/CommentServiceImpl.java` | Created |
| `implementation-plan.md` | Updated — 4.1.4 complete; Active Task 4.1.5 |
| `ai-prompts/implementation/13-comment-service.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean compile test -pl core` | SUCCESS |
| OSGi `CommentServiceImpl` Active + references | Verify locally after bundle deploy |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.4 | Complete |
| FR-13 | Add comment with validation |
| FR-14 | List comments by ticket |
| FR-15 | Empty message rejected (`VALIDATION_ERROR`) |
| AC-41–AC-43, AC-44–AC-46 | Add/list on Open, Closed, Cancelled tickets |
| api-contract.md | POST 201 / GET 200; 400 empty message; 404 ticket not found |
| Downstream | 4.1.5 validation consolidation; 5.1.5 `CommentCollectionServlet` |
