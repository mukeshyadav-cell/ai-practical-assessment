# AI Prompt — Task 3.1.2: Content Fragment TicketRepository Write Adapter

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 3.1 Task 3.1.2.

**Sprint/Task:** 3.1 / 3.1.2
**Category:** implementation
**Meaningful:** Yes — CF create/update, TKT-{n} ID counter, AEM FragmentTemplate APIs, and domain exception for not-found.

---

## Prompt (verbatim)

> Task 3.1.2 (Sprint 3.1): Implement the WRITE operations (create, update) of the
> Content Fragment TicketRepository, plus TKT-{n} ID generation.
>
> Follow all rules in .cursor/rules/. Read data-model.md (TKT-{n} ID strategy with counter at
> /var/assessment/ticket-id-counter; ISO-8601 text timestamps), the TicketRepository interface,
> TicketDTO, and the existing ContentFragmentTicketRepository (read methods already implemented).
>
> Modify: com.mysite.core.repositories.impl.ContentFragmentTicketRepository
> Replace the create/update stubs with real implementations.
>
> IMPORTANT AEM correctness (do NOT invent APIs; if unsure, say so and give closest-correct):
> - Use the service ResourceResolver (subservice "assessment-service"), try-with-resources.
> - To create a Content Fragment:
>     * Adapt the CFM model resource
>       (/conf/assessment/settings/dam/cfm/models/ticket) to
>       com.adobe.cq.dam.cfm.FragmentTemplate
>     * Resolve the parent folder resource (/content/dam/assessment/tickets)
>     * fragmentTemplate.createFragment(parent, <nodeName>, <title>)
>     * Set each element via contentFragment.getElement("<name>").setContent(value, "text/plain")
>       (or the correct API for the element value type; guard nulls)
>     * resolver.commit() to persist
> - If any of these exact method signatures are uncertain, state the assumption and cite the
>   class/package; do not fabricate.
>
> Implement:
>
> 1. TicketDTO create(TicketDTO ticket):
>    - Generate id via a private generateTicketId() that reads+increments a counter at
>      /var/assessment/ticket-id-counter (create the node/property if absent, start at 1000),
>      returning "TKT-" + n (e.g., TKT-1001). Commit the counter update.
>    - Force status = "Open" on create (ignore any incoming status) per state machine rule.
>    - Set createdAt = now (Instant, stored as ISO-8601 text), updatedAt = same.
>    - Node name for the CF = the generated id (e.g., TKT-1001).
>    - Map all DTO fields -> CF elements (id -> element "ticketId").
>    - Return the persisted ticket as a TicketDTO (re-read or build from known values).
>
> 2. TicketDTO update(TicketDTO ticket):
>    - Locate the CF by id (element ticketId) under the tickets folder.
>    - Update mutable fields: title, description, priority, assignedTo
>      (do NOT change status here — status changes go through a dedicated path later;
>       do NOT change ticketId, createdBy, createdAt).
>    - Set updatedAt = now.
>    - Commit and return the updated TicketDTO.
>    - If not found, throw a domain exception (com.mysite.core.exception.TicketNotFoundException;
>      create it if it does not exist) — do NOT create a new ticket on update.
>
> 3. Concurrency note: add a Javadoc comment acknowledging the counter is not atomic and is
>    acceptable for this learning scope; suggest a future improvement (locking/oak counter).
>
> 4. Keep the read methods and private toDto helper intact. SLF4J logging on failures.
>    Never leak ContentFragment/Resource outside the class.
>
> After generating:
> - Confirm mvn clean install compiles.
> - Tell me how to smoke test create/update (e.g., a temporary JUnit test, or defer to the
>   servlet in Sprint 5.1) and how to verify the new CF appears in CRXDE under
>   /content/dam/assessment/tickets with correct elements + the counter at /var/assessment.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Replaced `create`/`update` stubs in `ContentFragmentTicketRepository` with full implementations using `FragmentTemplate.createFragment(Resource, String, String)` and `ContentElement.setContent(String, String)` (verified via `aem-sdk-api` javap). `generateTicketId()` maintains counter at `/var/assessment/ticket-id-counter` (initial value 1000 → first id `TKT-1001`); Javadoc documents non-atomic increment. `create` forces `Open` status, sets ISO-8601 timestamps, node name = generated id. `update` mutates title/description/priority/assignedTo/updatedAt only; throws new `TicketNotFoundException` when missing. Added repoinit ACL for `/var/assessment` so `assessment-service` can write the counter. Verified `mvn clean install -pl core,ui.config -am`. Updated `implementation-plan.md` — 3.1.2 complete; Active Task → 3.1.3.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| CF creation | `FragmentTemplate` from `/conf/assessment/settings/dam/cfm/models/ticket`; `createFragment(ticketsFolder, ticketId, title)` |
| Element writes | `getElement(name).setContent(value, "text/plain")`; null → empty string |
| Counter | Property `counter` on `/var/assessment/ticket-id-counter`; `INITIAL_COUNTER_VALUE = 1000`; next id = `TKT-{counter + 1}` |
| Counter concurrency | Documented as non-atomic; Oak counter / locking suggested for production |
| Status on create | Always `Open` — incoming status ignored |
| Update scope | Mutable: title, description, priority, assignedTo, updatedAt; immutable: ticketId, createdBy, createdAt, status |
| Not found | `TicketNotFoundException` (not create-on-update) |
| Repoinit | Added `/var/assessment` path + `assessment-service` `jcr:read,rep:write` ACL (required for counter) |
| Persist failures | `IllegalStateException` after SLF4J error log |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentTicketRepository.java` | Updated — `create`, `update`, `generateTicketId`, helpers |
| `core/src/main/java/com/mysite/core/exception/TicketNotFoundException.java` | Created |
| `ui.config/.../RepositoryInitializer~assessment.cfg.json` | Updated — `/var/assessment` + ACL |
| `implementation-plan.md` | Updated — 3.1.2 complete; Active Task 3.1.3 |
| `ai-prompts/implementation/06-content-fragment-ticket-repository-write.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core,ui.config -am` | SUCCESS |

---

## AEM APIs verified

| Class | Method | Source |
|-------|--------|--------|
| `com.adobe.cq.dam.cfm.FragmentTemplate` | `createFragment(Resource, String, String)` | `aem-sdk-api` javap |
| `com.adobe.cq.dam.cfm.ContentElement` | `setContent(String, String)` | `aem-sdk-api` javap |

---

## Smoke test notes

1. Deploy: `mvn clean install -PautoInstallSinglePackage` (core + ui.config for repoinit).
2. JUnit with AEM Mocks (before Sprint 5.1 servlets) or manual CRXDE after create.
3. CRXDE: CF at `/content/dam/assessment/tickets/TKT-1001` with correct elements; counter at `/var/assessment/ticket-id-counter` with `counter=1001` after first create.

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 3.1 / Task 3.1.2 | Complete |
| DOD-1 (Sprint 3.1) | Create ticket CF path enabled |
| DOD-4 (Sprint 3.1) | Service user resolver |
| [02-state-machine.mdc](../../.cursor/rules/02-state-machine.mdc) | Initial status `Open` on create |
| [data-model.md](../../data-model.md) | TKT-{n} counter, ISO-8601 timestamps |
| Downstream | Task 3.1.3 (`CommentRepository`); Task 4.1.2 (full exception catalog) |
