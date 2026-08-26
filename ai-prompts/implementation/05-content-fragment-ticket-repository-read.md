# AI Prompt — Task 3.1.1: Content Fragment TicketRepository Read Adapter

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 3.1 Task 3.1.1.

**Sprint/Task:** 3.1 / 3.1.1
**Category:** implementation
**Meaningful:** Yes — CF adapter read operations, AEM API constraints, service-user resolver, and DTO mapping rules.

---

## Prompt (verbatim)

> Task 3.1.1 (Sprint 3.1): Implement the READ operations of the Content Fragment
> TicketRepository adapter.
>
> Follow all rules in .cursor/rules/. Read data-model.md (CFM element names, ticketId->id
> mapping, timestamps stored as ISO-8601 text), the TicketRepository interface, and TicketDTO.
> Target: com.mysite.core.repositories.impl.
>
> IMPORTANT AEM correctness (do NOT invent APIs; if unsure, say so and give closest-correct):
> - Adapt resources via resource.adaptTo(com.adobe.cq.dam.cfm.ContentFragment.class)
> - Read element values via contentFragment.getElement("<name>").getContent()
> - Obtain ResourceResolver via ResourceResolverFactory.getServiceResourceResolver with a
>   Map authInfo containing SUBSERVICE = "assessment-service" (mapped in Sprint 2.1.4).
>   Use try-with-resources so the resolver is always closed.
> - Tickets are stored under /content/dam/assessment/tickets (each ticket = one Content Fragment)
>
> Create class:
>   com.mysite.core.repositories.impl.ContentFragmentTicketRepository
>
> Requirements:
> 1. OSGi component:
>    @Component(service = TicketRepository.class, property = "impl.type=contentfragment")
>    @Reference private ResourceResolverFactory resolverFactory;
>    Define a constant SERVICE_SUBSERVICE = "assessment-service" and TICKETS_PATH =
>    "/content/dam/assessment/tickets".
>
> 2. Implement ONLY read methods now. For create(...) and update(...) add stubs that throw
>    UnsupportedOperationException("implemented in Task 3.1.2"):
>    - List<TicketDTO> getAll()
>    - Optional<TicketDTO> getById(String id)
>    - List<TicketDTO> findByStatus(String status)
>    - List<TicketDTO> searchByTitle(String query)   // case-insensitive "contains" on title
>
> 3. Private helper: TicketDTO toDto(ContentFragment cf) mapping:
>    - element "ticketId"    -> dto.id
>    - "title","description","priority","status","assignedTo","createdBy" -> same-named fields
>    - "createdAt","updatedAt" ISO-8601 text -> Instant via safe parse (null/blank -> null)
>    Guard every getElement(...) against null (element may be absent).
>    (This helper moves to a dedicated mapper in Task 3.1.5 — keep private for now.)
>
> 4. Iteration/logic:
>    - Resolve TICKETS_PATH; if the folder resource is null, return empty list / Optional.empty()
>    - Iterate child resources; adapt each to ContentFragment; skip children that are not CFs
>    - getById: match on the ticketId element == id; return Optional.empty() if none
>    - findByStatus / searchByTitle: filter in-memory over getAll() for now (correctness first;
>      JCR query optimization is a later concern)
>    - Collections never return null (return empty list)
>
> 5. Quality: constants (no magic strings), SLF4J logging on exceptions, Javadoc on class + methods.
>    Do NOT leak ContentFragment/Resource outside the class (return only DTOs/Optionals).
>
> After generating:
> - Confirm mvn clean install compiles.
> - Tell me how to smoke test: create ONE Content Fragment via AEM UI under
>   /content/dam/assessment/tickets using the Ticket model, and how the component becomes
>   active/satisfied in the OSGi console.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Implemented `ContentFragmentTicketRepository` in `com.mysite.core.repositories.impl` as an OSGi `@Component` with `impl.type=contentfragment`, injecting `ResourceResolverFactory` and resolving tickets via `getServiceResourceResolver` with subservice `assessment-service` (try-with-resources). Read methods `getAll`, `getById`, `findByStatus`, and `searchByTitle` iterate CF children under `/content/dam/assessment/tickets`, adapt via `ContentFragment`, and map to `TicketDTO` with private `toDto` (`ticketId`→`id`, ISO-8601 text→`Instant`, null-safe element reads). `getAll` sorts by `createdAt` descending; status/title filters run in-memory over `getAll()`. `create` and `update` throw `UnsupportedOperationException` for Task 3.1.2. Verified `mvn clean install -pl core -am` compiles. Updated `implementation-plan.md` — 3.1.1 complete; Active Task → 3.1.2.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Write operations | Stubbed with `UnsupportedOperationException("implemented in Task 3.1.2")` |
| Resolver | `ResourceResolverFactory.SUBSERVICE` + `assessment-service`; never administrative resolver |
| CF iteration | Direct children of `TICKETS_PATH`; skip resources that do not adapt to `ContentFragment` |
| Filtering | `findByStatus` / `searchByTitle` filter in-memory over `getAll()` (no JCR query yet) |
| `assignedTo` | Blank CF value mapped to `null` on DTO (nullable field) |
| Timestamps | `Instant.parse` with warn log on failure; null/blank → null |
| Mapper | Private `toDto` in repository class; dedicated mapper deferred to Task 3.1.5 |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentTicketRepository.java` | Created |
| `implementation-plan.md` | Updated — 3.1.1 complete; Active Sprint 3.1; Active Task 3.1.2 |
| `ai-prompts/implementation/05-content-fragment-ticket-repository-read.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core -am` | SUCCESS |

---

## Smoke test notes

1. Deploy: `mvn clean install -PautoInstallBundle -pl core` (or full package).
2. Create Ticket CF in AEM Assets at `/content/dam/assessment/tickets` using Ticket model; set `ticketId` element (e.g. `TKT-1001`) and ISO-8601 timestamps.
3. OSGi Console → Components → search `ContentFragmentTicketRepository` → expect **Active** / **Satisfied**; service `TicketRepository`, property `impl.type=contentfragment`.

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 3.1 / Task 3.1.1 | Complete |
| DOD-3 (Sprint 3.1) | No `ContentFragment`/`Resource` types above repository layer |
| DOD-4 (Sprint 3.1) | Service user resolver (`assessment-service`) |
| [01-architecture.mdc](../../.cursor/rules/01-architecture.mdc) | CF adapter with `impl.type=contentfragment` |
| [04-aem-correctness.mdc](../../.cursor/rules/04-aem-correctness.mdc) | CF APIs, service resolver, tickets path |
| Downstream | Task 3.1.2 (`create`, `update`, `TKT-{n}` counter); Task 3.1.5 (dedicated mapper) |
