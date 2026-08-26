# AI Prompt — Task 3.1.3: Content Fragment CommentRepository Adapter

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 3.1 Task 3.1.3.

**Sprint/Task:** 3.1 / 3.1.3
**Category:** implementation
**Meaningful:** Yes — Comment CF adapter, CMT-{n} ID counter, listByTicket ordering, repository-layer persistence only.

---

## Prompt (verbatim)

> Task 3.1.3 (Sprint 3.1): Implement the Content Fragment CommentRepository adapter
> (add + listByTicket) with CMT-{n} ID generation.
>
> Follow all rules in .cursor/rules/. Read data-model.md (Comment CFM elements: commentId,
> ticketId, message, createdBy, createdAt; CMT-{n} counter at /var/assessment/comment-id-counter;
> ISO-8601 text timestamps), the CommentRepository interface, and CommentDTO.
> Reuse the SAME patterns established in ContentFragmentTicketRepository (service resolver,
> FragmentTemplate create, getElement/setContent, resolver.commit, try-with-resources).
>
> Create class:
>   com.mysite.core.repositories.impl.ContentFragmentCommentRepository
>
> IMPORTANT AEM correctness (do NOT invent APIs; if unsure, say so):
> - Service ResourceResolver via subservice "assessment-service", try-with-resources
> - Create CF: adapt CFM model resource
>   (/conf/assessment/settings/dam/cfm/models/comment) to
>   com.adobe.cq.dam.cfm.FragmentTemplate; createFragment under
>   /content/dam/assessment/comments; set elements; resolver.commit()
> - Read: resource.adaptTo(ContentFragment.class), getElement("<name>").getContent()
>
> Requirements:
>
> 1. OSGi component:
>    @Component(service = CommentRepository.class, property = "impl.type=contentfragment")
>    @Reference private ResourceResolverFactory resolverFactory;
>    Constants: SERVICE_SUBSERVICE = "assessment-service",
>    COMMENTS_PATH = "/content/dam/assessment/comments",
>    COMMENT_MODEL_PATH = "/conf/assessment/settings/dam/cfm/models/comment",
>    COUNTER_PATH = "/var/assessment/comment-id-counter".
>
> 2. CommentDTO add(CommentDTO comment):
>    - Generate id via private generateCommentId() reading+incrementing the counter at
>      COUNTER_PATH (create if absent, start at 1000) -> "CMT-" + n; commit counter update.
>    - Set createdAt = now (Instant -> ISO-8601 text).
>    - Node name = generated id (e.g., CMT-1001).
>    - Map DTO -> CF elements: id -> "commentId", ticketId, message, createdBy, createdAt.
>    - resolver.commit(); return the persisted CommentDTO.
>    - NOTE: do NOT validate ticket existence here (that is service-layer responsibility
>      in Sprint 4.1) — repository just persists.
>
> 3. List<CommentDTO> listByTicket(String ticketId):
>    - Iterate children of COMMENTS_PATH, adapt to ContentFragment, skip non-CFs.
>    - Include only comments whose "ticketId" element equals the given ticketId.
>    - Order results by createdAt ASCENDING (oldest first).
>    - Return empty list if none (never null).
>
> 4. Private helper toDto(ContentFragment cf) mapping:
>    - "commentId" -> dto.id; "ticketId","message","createdBy" -> same-named fields;
>    - "createdAt" ISO-8601 text -> Instant (safe parse; null/blank -> null).
>    Guard every getElement against null.
>
> 5. Concurrency note in Javadoc (counter not atomic; acceptable for learning scope).
>    SLF4J logging on failures. Javadoc on class + methods.
>    Never leak ContentFragment/Resource outside the class.
>
> After generating:
> - Confirm mvn clean install compiles and the component becomes satisfied in OSGi console.
> - Tell me how to smoke test (create a comment CF, verify listByTicket returns it in order,
>   and the counter at /var/assessment/comment-id-counter increments).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `ContentFragmentCommentRepository` in `com.mysite.core.repositories.impl` as OSGi `@Component` with `impl.type=contentfragment`. `add` generates `CMT-{n}` via counter at `/var/assessment/comment-id-counter` (initial 1000 → first id `CMT-1001`), creates CF via `FragmentTemplate.createFragment` under `/content/dam/assessment/comments`, maps DTO fields to CF elements (`id`→`commentId`), sets `createdAt` to now, commits resolver. `listByTicket` filters by `ticketId` element and sorts by `createdAt` ascending. No ticket-existence validation at repository layer. Private `toDto` with null-safe element reads and ISO-8601 parsing. Counter Javadoc documents non-atomic increment. Verified `mvn clean install -pl core -am`. Updated `implementation-plan.md` — 3.1.3 complete; Active Task → 3.1.4.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| CF creation | `FragmentTemplate` from `/conf/assessment/settings/dam/cfm/models/comment` |
| Counter | Property `counter` on `/var/assessment/comment-id-counter`; `INITIAL_COUNTER_VALUE = 1000`; first id `CMT-1001` |
| Counter concurrency | Documented as non-atomic; Oak counter / locking suggested for production |
| Ticket validation | **Not** in repository — deferred to CommentService (Sprint 4.1) |
| `listByTicket` order | `createdAt` ascending (oldest first) |
| Fragment title on create | Comment `message` (fallback to generated id) |
| Repoinit | Reuses `/var/assessment` ACL from Task 3.1.2 (no additional repoinit change) |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentCommentRepository.java` | Created |
| `implementation-plan.md` | Updated — 3.1.3 complete; Active Task 3.1.4 |
| `ai-prompts/implementation/07-content-fragment-comment-repository.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core -am` | SUCCESS |

---

## Smoke test notes

1. Deploy: `mvn clean install -PautoInstallBundle -pl core`.
2. OSGi Console → `ContentFragmentCommentRepository` → Active / Satisfied; service `CommentRepository`.
3. Add comment via repository harness with `ticketId` e.g. `TKT-1001`; expect `CMT-1001`.
4. `listByTicket("TKT-1001")` returns comments in `createdAt` ascending order.
5. CRXDE: `/content/dam/assessment/comments/CMT-1001`; counter at `/var/assessment/comment-id-counter` increments on each add.

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 3.1 / Task 3.1.3 | Complete |
| DOD-2 (Sprint 3.1) | Comment CF path enabled |
| DOD-3 (Sprint 3.1) | No AEM types above repository layer |
| DOD-4 (Sprint 3.1) | Service user resolver |
| [data-model.md](../../data-model.md) | Comment CFM elements, CMT-{n} counter |
| Downstream | Task 3.1.4 (`UserRepository`); Task 4.1.4 (`CommentService` ticket validation) |
