# AI Prompt — Task 3.1.5: CF ↔ DTO Mapper Extraction

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 3.1 Task 3.1.5.

**Sprint/Task:** 3.1 / 3.1.5
**Category:** implementation
**Meaningful:** Yes — refactor to dedicated mappers and TimeUtil; centralizes CF read mapping without behavior change.

---

## Prompt (verbatim)

> Task 3.1.5 (Sprint 3.1): Extract the CF<->DTO mapping into dedicated reusable mapper classes,
> and refactor the ticket and comment repositories to use them.
>
> Follow all rules in .cursor/rules/. Read data-model.md (ticketId->id, commentId->id,
> ISO-8601 text <-> Instant), TicketDTO, CommentDTO, and the existing
> ContentFragmentTicketRepository and ContentFragmentCommentRepository (which currently have
> private toDto helpers to be extracted).
>
> Target package: com.mysite.core.mappers (create it).
>
> This is a REFACTOR — behavior must remain identical. Do not change repository method contracts.
>
> Create:
>
> 1. TicketMapper (final class, or with a private constructor; static methods):
>    - TicketDTO toDto(ContentFragment cf)
>        * element "ticketId" -> id
>        * title, description, priority, status, assignedTo, createdBy -> same-named fields
>        * "createdAt","updatedAt" ISO-8601 text -> Instant (safe parse; null/blank -> null)
>        * null-guard every getElement
>    - Optionally: a helper Map<String,Object> or field-application method used by the
>      repository when WRITING (mapping DTO -> element name/value pairs), if it reduces
>      duplication in create/update. If cleaner to keep write-mapping in the repo, leave it —
>      but at minimum centralize the READ mapping (toDto).
>
> 2. CommentMapper (same style):
>    - CommentDTO toDto(ContentFragment cf)
>        * "commentId" -> id; ticketId, message, createdBy -> same-named
>        * "createdAt" ISO-8601 text -> Instant (safe parse)
>        * null-guard every getElement
>
> 3. A small shared utility for time conversion (e.g., com.mysite.core.util.TimeUtil):
>    - Instant parseInstant(String iso)   // null/blank -> null; log+null on parse error
>    - String formatInstant(Instant t)    // null -> null; ISO-8601 (Instant.toString())
>    Use this in both mappers and (optionally) in repository write paths for consistency.
>
> 4. Refactor ContentFragmentTicketRepository and ContentFragmentCommentRepository:
>    - Replace their private toDto helpers with calls to TicketMapper.toDto / CommentMapper.toDto
>    - Use TimeUtil for any timestamp read/write conversions
>    - Remove now-dead duplicate code
>    - Do NOT change public method signatures or behavior
>
> Requirements:
> - Mappers are framework-light: they may reference com.adobe.cq.dam.cfm.ContentFragment
>   (that is acceptable — mappers are part of the CF adapter concern) and the DTOs,
>   but must NOT reference Sling servlets/services.
> - Javadoc on classes + methods. SLF4J logging in TimeUtil parse failures.
> - Keep mappers null-safe and side-effect free (pure mapping).
>
> After generating:
> - Confirm mvn clean install compiles.
> - Confirm the repositories now delegate to the mappers (no duplicate toDto logic remains).
> - Redeploy and verify read still works (getAll returns your existing test ticket/comment).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `TimeUtil` (`parseInstant` / `formatInstant` with SLF4J on parse errors), `TicketMapper` (`toDto`, `readTicketId`, public element constants), and `CommentMapper` (same pattern) in `com.mysite.core.mappers` and `com.mysite.core.util`. Refactored `ContentFragmentTicketRepository` and `ContentFragmentCommentRepository` to delegate read mapping to mappers and use `TimeUtil` for write-path timestamps; removed private `toDto`, `parseInstant`, `formatInstant`, and duplicate `getElementContent` helpers. Write mapping remains in repositories using mapper element constants. Verified `mvn clean install -pl core -am`. Updated `implementation-plan.md` — 3.1.5 complete; all Sprint 3.1 tasks done; Active Task → Quality Gate Sprint 3.1.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Read mapping | Centralized in `TicketMapper.toDto` / `CommentMapper.toDto` |
| Write mapping | Kept in repositories; uses `TicketMapper.ELEMENT_*` / `CommentMapper.ELEMENT_*` constants |
| Timestamps | Shared `TimeUtil` for parse/format in mappers and repository write paths |
| Lookup helpers | `readTicketId()` on both mappers for find/filter without duplicating element reads |
| `assignedTo` read | `blankToNull` retained inside `TicketMapper` (nullable DTO field) |
| Behavior | Pure refactor — no repository public API or mapping rule changes |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/util/TimeUtil.java` | Created |
| `core/src/main/java/com/mysite/core/mappers/TicketMapper.java` | Created |
| `core/src/main/java/com/mysite/core/mappers/CommentMapper.java` | Created |
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentTicketRepository.java` | Refactored — delegates to mappers |
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentCommentRepository.java` | Refactored — delegates to mappers |
| `implementation-plan.md` | Updated — 3.1.5 complete; QG Sprint 3.1 |
| `ai-prompts/implementation/09-cf-dto-mappers.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core -am` | SUCCESS |
| Duplicate `toDto` in ticket/comment repos | Removed (grep confirms delegation only) |

---

## Smoke test notes

1. Redeploy: `mvn clean install -PautoInstallBundle -pl core`.
2. `getAll()` / `listByTicket()` should return identical DTOs to pre-refactor behavior.
3. Create/update timestamps still use `TimeUtil.formatInstant`.

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 3.1 / Task 3.1.5 | Complete |
| Sprint 3.1 Quality Gate | All tasks 3.1.1–3.1.5 complete |
| [data-model.md](../../data-model.md) | ticketId/commentId → id; ISO-8601 ↔ Instant |
| [01-architecture.mdc](../../.cursor/rules/01-architecture.mdc) | Mapping at repository/mapper layer |
| Downstream | Sprint 4.1 services consume same DTOs via repository ports |
