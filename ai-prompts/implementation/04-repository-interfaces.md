# AI Prompt — Task 2.1.6: Repository Interfaces (Ports)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 2.1 Task 2.1.6.

**Sprint/Task:** 2.1 / 2.1.6
**Category:** implementation
**Meaningful:** Yes — repository port definitions, method signatures, and architecture constraints for swappable persistence.

---

## Prompt (verbatim)

> Task 2.1.6 (Sprint 2.1): Create the repository interfaces (Ports) in the core module.
>
> Follow all rules in .cursor/rules/. Read data-model.md, api-contract.md, and
> implementation-plan.md (Sprint 3.1 tasks) for the exact operations needed.
> Target package: com.mysite.core.repositories.
>
> These are PURE interfaces (Ports) — method signatures only, no implementation.
> They define the persistence contract so a Content Fragment adapter now (and a DB adapter
> later) can implement them without changing services/servlets. Interfaces reference ONLY
> DTOs (com.mysite.core.dto) and java.util/java.time types — NEVER AEM/JCR/Sling types.
>
> Create three interfaces in core/src/main/java/com/mysite/core/repositories/:
>
> 1. TicketRepository
>    - List<TicketDTO> getAll()
>    - Optional<TicketDTO> getById(String id)
>    - List<TicketDTO> findByStatus(String status)
>    - List<TicketDTO> searchByTitle(String query)   // case-insensitive keyword search
>    - TicketDTO create(TicketDTO ticket)             // assigns id, sets timestamps in impl
>    - TicketDTO update(TicketDTO ticket)             // updates updatedAt in impl
>    // NOTE: no delete — ticket deletion is OUT OF SCOPE per requirements-analysis.md
>
> 2. CommentRepository
>    - CommentDTO add(CommentDTO comment)             // assigns id + createdAt in impl
>    - List<CommentDTO> listByTicket(String ticketId) // ordered by createdAt ascending
>
> 3. UserRepository
>    - Optional<UserDTO> getById(String userId)
>    - List<UserDTO> getAll()                          // seeded, non-system users
>    - List<UserDTO> search(String query)              // by displayName/email
>
> Requirements:
> - Use Optional<T> for single-item lookups that may not exist (getById)
> - Use List<T> for collections (never null — impls return empty list)
> - Javadoc on the interface (its role as a Port) and on EACH method
>   (params, return, and what "not found" / empty means)
> - Do NOT add OSGi annotations to interfaces (impls will be @Component in Sprint 3.1)
> - Do NOT reference ContentFragment, Resource, ResourceResolver, or any javax.jcr type
> - Document in each interface's Javadoc that implementations are selected via the OSGi
>   property "impl.type" (contentfragment now, database later) per the architecture rule
>
> After generating, confirm compile (mvn clean install) and that interfaces import ONLY
> com.mysite.core.dto + java.util/java.time.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `TicketRepository`, `CommentRepository`, and `UserRepository` in `com.mysite.core.repositories` as pure port interfaces with method signatures only. Each interface documents OSGi `impl.type` selection (`contentfragment` / `database`). Javadoc on every method covers parameters, return values, and empty/not-found semantics. `TicketRepository` includes six methods (no delete); `CommentRepository` has `add` and `listByTicket`; `UserRepository` is read-only with `getById`, `getAll`, and `search`. Imports are limited to `com.mysite.core.dto` and `java.util` (`Optional`, `List`). Verified `mvn -pl core clean compile` succeeds. Updated `implementation-plan.md` — 2.1.6 complete; all Sprint 2.1 tasks done; Active Task → Quality Gate Sprint 2.1.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Ticket delete | **Not on port** — out of scope per requirements-analysis.md |
| Single lookups | `Optional<T>` for `getById` on Ticket and User |
| Collections | `List<T>` — implementations return empty list, never null |
| OSGi | No annotations on interfaces; `@Component` + `impl.type` on adapters in Sprint 3.1 |
| UserRepository | Read-only (no create/update/delete) — users seeded via repoinit |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/repositories/TicketRepository.java` | Created |
| `core/src/main/java/com/mysite/core/repositories/CommentRepository.java` | Created |
| `core/src/main/java/com/mysite/core/repositories/UserRepository.java` | Created |
| `implementation-plan.md` | Updated — 2.1.6 complete; QG Sprint 2.1 |
| `ai-prompts/implementation/04-repository-interfaces.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn -pl core clean compile` | SUCCESS |
| Imports in `repositories` package | `com.mysite.core.dto.*`, `java.util.List`, `java.util.Optional` only |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 2.1 / Task 2.1.6 | Complete |
| DOD-5 (Sprint 2.1) | DTO + repository interfaces compile in `core` |
| Sprint 2.1 Quality Gate | All tasks 2.1.1–2.1.6 complete |
| [01-architecture.mdc](../../.cursor/rules/01-architecture.mdc) | Repository pattern ports |
| Downstream | CF adapters Sprint 3.1 (`impl.type=contentfragment`) |
