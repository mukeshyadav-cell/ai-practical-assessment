# AI Prompt — Task 2.1.5: DTO Classes

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 2.1 Task 2.1.5.

**Sprint/Task:** 2.1 / 2.1.5
**Category:** implementation
**Meaningful:** Yes — DTO creation task with field specs, id mapping rules, and framework-free constraints.

---

## Prompt (verbatim)

> Task 2.1.5 (Sprint 2.1): Create the DTO classes in the core module.
>
> Follow all rules in .cursor/rules/. Read data-model.md and api-contract.md for exact
> field names, types, and the ticketId->id mapping. Target package: com.mysite.core.dto.
>
> These are plain POJOs (source-agnostic). They MUST NOT import or reference any AEM/JCR/Sling
> types (no ContentFragment, Resource, ValueMap). Timestamps use java.time.Instant.
>
> Create three classes in core/src/main/java/com/mysite/core/dto/:
>
> 1. TicketDTO
>    Fields (match api-contract JSON shape):
>    - String id                (JSON "id"; maps from CF element ticketId)
>    - String title
>    - String description
>    - String priority          (P1..P4 as String)
>    - String status            (Open, In Progress, Resolved, Closed, Cancelled as String)
>    - String assignedTo        (userId; nullable = unassigned)
>    - String createdBy         (userId)
>    - Instant createdAt
>    - Instant updatedAt
>
> 2. CommentDTO
>    - String id                (JSON "id"; maps from CF element commentId)
>    - String ticketId          (FK)
>    - String message
>    - String createdBy         (userId)
>    - Instant createdAt
>
> 3. UserDTO
>    - String userId
>    - String displayName
>    - String email
>
> Requirements:
> - Standard POJOs: private fields, public getters/setters, no-arg constructor
> - Add an all-args constructor for convenience
> - Javadoc on the class describing its purpose (source-agnostic DTO)
> - equals()/hashCode() based on id (userId for UserDTO) — keep simple
> - toString() for debugging (avoid logging sensitive data)
> - Do NOT add Jackson annotations unless needed; if the JSON field name differs from the
>   Java field, prefer matching the Java field name to the JSON already (id, ticketId, etc.)
> - Keep them framework-free (no OSGi annotations — these are plain data holders)
>
> After generating, confirm the classes compile (mvn clean install) and that NONE of them
> import AEM/JCR/Sling packages.
>
> Note on id vs ticketId
> Your plan flagged this mapping: DTO id ← CF ticketId. Make sure Cursor uses id in TicketDTO (not ticketId), because that's your JSON/API field name. The mapper (Task 3.1.5) handles the translation ticketId (CF) ↔ id (DTO/JSON). Same pattern for CommentDTO.id ← commentId
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `TicketDTO`, `CommentDTO`, and `UserDTO` in `com.mysite.core.dto` as plain POJOs with private fields, no-arg and all-args constructors, getters/setters, class-level Javadoc, `equals()`/`hashCode()` on `id` (`userId` for `UserDTO`), and `toString()` omitting sensitive/content fields (`description`, `message`, `email`). Used `java.time.Instant` for timestamps; `assignedTo` nullable on `TicketDTO`. No Jackson or OSGi annotations; Java field names align with `api-contract.md` JSON (`id`, not `ticketId`, on ticket/comment DTOs). Verified `mvn -pl core clean compile` succeeds with no AEM/JCR/Sling imports in the dto package. Updated `implementation-plan.md` — 2.1.5 complete; Active Task 2.1.6.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Ticket/Comment ID field | **`id`** on DTO (JSON/API); CF element `ticketId`/`commentId` mapped in Sprint 3.1 |
| Timestamps | `java.time.Instant` (not `String` on DTO) |
| Annotations | None — Jackson serializes by getter name when added in servlets (Sprint 5.1) |
| `toString()` | Omits `description`, `message`, `email` to avoid logging user content/sensitive data |
| `equals`/`hashCode` | Identity by `id` / `userId` only |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/dto/TicketDTO.java` | Created |
| `core/src/main/java/com/mysite/core/dto/CommentDTO.java` | Created |
| `core/src/main/java/com/mysite/core/dto/UserDTO.java` | Created |
| `implementation-plan.md` | Updated — 2.1.5 complete; Active Task 2.1.6 |
| `ai-prompts/implementation/03-dto-classes.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn -pl core clean compile` | SUCCESS |
| AEM/JCR/Sling imports in `dto` package | None |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 2.1 / Task 2.1.5 | Complete |
| DOD-5 (Sprint 2.1) | DTO classes compile in `core` |
| [data-model.md](../../data-model.md) §6 | DTO field definitions |
| [api-contract.md](../../api-contract.md) §6 | JSON shapes |
| Downstream | Repository interfaces (2.1.6), CF mappers `ticketId`↔`id` (3.1.5) |
