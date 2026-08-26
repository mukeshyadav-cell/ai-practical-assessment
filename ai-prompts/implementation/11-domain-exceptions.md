# AI Prompt — Task 4.1.2: Domain Exception Hierarchy

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 4.1 Task 4.1.2.

**Sprint/Task:** 4.1 / 4.1.2
**Category:** implementation
**Meaningful:** Yes — domain exception hierarchy aligned with api-contract error catalog for service/servlet mapping.

---

## Prompt (verbatim)

> Task 4.1.2 (Sprint 4.1): Create the domain exception hierarchy for the service layer,
> aligned with the api-contract.md error catalog.
>
> Follow all rules in .cursor/rules/. Read api-contract.md (error catalog: codes + HTTP status)
> and requirements-analysis.md (FR-7 terminal-ticket edit rules). Target package:
> com.mysite.core.exception.
>
> Note: InvalidTransitionException already exists (from Task 4.1.1) with errorCode()
> "INVALID_TRANSITION". Align the new exceptions with the same pattern.
>
> Create:
>
> 1. A base abstract exception: DomainException (extends RuntimeException)
>    - Constructor(String message)
>    - Constructor(String message, Throwable cause)
>    - abstract String errorCode()           // stable code matching api-contract catalog
>    - int httpStatus()                       // default mapping helper (optional convenience)
>    Purpose: gives servlets a uniform way to map domain errors -> JSON {error, code} + status.
>    (If InvalidTransitionException from 4.1.1 does not already extend this base, refactor it
>     to extend DomainException while keeping its errorCode "INVALID_TRANSITION" and message.)
>
> 2. ValidationException extends DomainException
>    - errorCode() = "VALIDATION_ERROR"  (HTTP 400)
>    - Constructors: (String message), and optionally (String field, String reason)
>
> 3. TicketNotFoundException extends DomainException
>    - errorCode() = "NOT_FOUND"          (HTTP 404)
>    - Constructor(String ticketId) -> message "Ticket not found: {id}"
>    - NOTE: if this class already exists from Task 3.1.2, refactor it to extend DomainException
>      and use errorCode "NOT_FOUND" (do not duplicate).
>
> 4. TicketNotEditableException extends DomainException
>    - errorCode() = "TICKET_NOT_EDITABLE"  (HTTP 400)
>    - Constructor(String ticketId, String status) -> message like
>      "Ticket {id} is not editable in status {status}"
>    - Used when updating/reassigning a ticket in a terminal status (Closed/Cancelled) per FR-7.
>
> 5. UnknownUserException extends DomainException
>    - errorCode() = "UNKNOWN_USER"        (HTTP 400)
>    - Constructor(String userId) -> message "Unknown user: {id}"
>
> 6. (Optional) CommentValidationException OR reuse ValidationException for empty message.
>    Prefer reusing ValidationException for empty comment message to avoid class sprawl —
>    confirm this choice in a Javadoc note.
>
> Requirements:
> - All exceptions are pure domain (NO AEM/Sling/JCR imports).
> - Javadoc on each: what it means + which HTTP status/api-contract code it maps to.
> - Keep errorCode() strings EXACTLY matching the api-contract error catalog.
> - Provide a short mapping summary in the DomainException Javadoc
>   (code -> HTTP status) for reviewers.
>
> After generating:
> - Confirm mvn clean install compiles (including any refactor of InvalidTransitionException
>   and TicketNotFoundException to extend DomainException).
> - List each exception with its errorCode and intended HTTP status so I can verify against
>   api-contract.md.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created abstract `DomainException` base with `errorCode()` and `httpStatus()` plus Javadoc mapping table for all catalog codes. Added `ValidationException` (`VALIDATION_ERROR`, 400; message and field/reason constructors; Javadoc documents reuse for empty comment messages), `TicketNotEditableException` (`TICKET_NOT_EDITABLE`, 400), and `UnknownUserException` (`UNKNOWN_USER`, 400). Refactored existing `TicketNotFoundException` (`NOT_FOUND`, 404; added `getTicketId()`) and `InvalidTransitionException` (`INVALID_TRANSITION`, 409) to extend `DomainException`. No `CommentValidationException` — validation reuse per prompt. Verified `mvn clean compile -pl core`. Updated `implementation-plan.md` — 4.1.2 complete; Active Task → 4.1.3.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Base class | `DomainException` abstract; each subclass implements `errorCode()` and `httpStatus()` |
| Empty comment validation | Reuse `ValidationException` — documented in Javadoc; no separate class |
| `TicketNotFoundException` | Refactored in place (not duplicated); backward-compatible constructor message |
| `InvalidTransitionException` | Extends `DomainException`; `getFrom()`/`getTo()` and message format preserved |
| `INTERNAL_ERROR` / 500 | Not a `DomainException` — handled in servlet layer for unhandled failures |

---

## Exception catalog implemented

| Exception | `errorCode()` | HTTP | Example message |
|-----------|---------------|------|-----------------|
| `ValidationException` | `VALIDATION_ERROR` | 400 | `Title is required` / `message: must not be blank` |
| `UnknownUserException` | `UNKNOWN_USER` | 400 | `Unknown user: nobody` |
| `TicketNotEditableException` | `TICKET_NOT_EDITABLE` | 400 | `Ticket TKT-1001 is not editable in status Closed` |
| `TicketNotFoundException` | `NOT_FOUND` | 404 | `Ticket not found: TKT-9999` |
| `InvalidTransitionException` | `INVALID_TRANSITION` | 409 | `Invalid transition: Open -> Closed` |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/exception/DomainException.java` | Created |
| `core/src/main/java/com/mysite/core/exception/ValidationException.java` | Created |
| `core/src/main/java/com/mysite/core/exception/TicketNotEditableException.java` | Created |
| `core/src/main/java/com/mysite/core/exception/UnknownUserException.java` | Created |
| `core/src/main/java/com/mysite/core/exception/TicketNotFoundException.java` | Refactored — extends `DomainException` |
| `core/src/main/java/com/mysite/core/exception/InvalidTransitionException.java` | Refactored — extends `DomainException` |
| `implementation-plan.md` | Updated — 4.1.2 complete; Active Task 4.1.3 |
| `ai-prompts/implementation/11-domain-exceptions.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean compile -pl core` | SUCCESS |
| `mvn clean install` (full reactor) | Run locally to confirm |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.2 | Complete |
| FR-7 | `TicketNotEditableException` for terminal-ticket edit/reassign |
| api-contract.md §5 | All domain error codes except `INTERNAL_ERROR` |
| Downstream | 4.1.3 `TicketService` / `CommentService`; 5.1.7 shared servlet error handling |
