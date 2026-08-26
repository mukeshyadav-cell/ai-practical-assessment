# AI Prompt — Task 4.1.5: Validation Consolidation (TicketValidator + CommentValidator)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 4.1 Task 4.1.5.

**Sprint/Task:** 4.1 / 4.1.5
**Category:** implementation
**Meaningful:** Yes — audit, reusable validators, service refactor, and AC-6 gap fix.

---

## Prompt (verbatim)

> Task 4.1.5 (Sprint 4.1): Consolidate and harden validation across the services into a
> reusable validator, and close any gaps against the acceptance criteria.
>
> Follow all rules in .cursor/rules/. Read acceptance-criteria.md (all validation ACs and
> NFR-AC), requirements-analysis.md (FR-1..FR-15, FR-7 terminal rules), api-contract.md
> (error codes), TicketServiceImpl, CommentServiceImpl, TicketStatus enum, and the domain
> exceptions. Target: com.mysite.core.validation (create) + refactor the two service impls.
>
> Part A — Audit (report first, before refactor):
> List a table of every validation rule the services SHOULD enforce (from the ACs/FRs):
> - Rule | Where enforced now | Exception thrown | Matches AC? (Yes/gap)
> Cover at minimum:
> - Ticket: title required/non-blank; priority in {P1,P2,P3,P4}; status label valid;
>   create forces Open; update blocked on terminal (TicketNotEditableException);
>   reassign requires known user (UnknownUserException); reassign blocked on terminal.
> - changeStatus: unknown status label -> ValidationException; invalid transition ->
>   InvalidTransitionException (409).
> - Comment: message required/non-blank; ticket must exist; createdBy required;
>   comments allowed on terminal tickets.
> Point out any GAPS you find in the current implementation.
>
> Part B — Create a reusable validator:
> com.mysite.core.validation.TicketValidator (and CommentValidator if useful), with small,
> pure, static (or injectable) methods, e.g.:
> - void validateForCreate(TicketDTO)         // title non-blank, priority valid
> - void validatePriority(String priority)    // throws ValidationException if not P1..P4
> - void validateStatusLabel(String status)   // throws ValidationException if not a known label
> - void validateForUpdate(TicketDTO changes) // validate provided mutable fields
> - void validateComment(CommentDTO)          // message non-blank, createdBy non-blank
> Each throws the appropriate DomainException subclass with a clear message.
> Keep validators pure (no AEM/Sling/JCR; no repository calls — existence checks that need a
> repository stay in the service, e.g., "known user", "ticket exists").
>
> Part C — Refactor services to use the validator:
> - Replace inline field/enum validation in TicketServiceImpl and CommentServiceImpl with calls
>   to the validator methods.
> - Keep repository-dependent checks (user exists, ticket exists, terminal-status guard) in the
>   services (they need repository/state access), but ensure they throw the correct exceptions.
> - Do NOT change behavior for valid inputs. Fill any gaps found in Part A so ALL invalid inputs
>   are rejected with the correct DomainException.
>
> Constraints:
> - Validators are framework-free and unit-test-friendly (pure functions).
> - Consistent messages; Javadoc; no duplicate validation logic left in the services.
> - Terminal-status determination: reuse TicketStatus (CLOSED/CANCELLED are terminal).
>
> After generating:
> - Confirm mvn clean install compiles and services remain satisfied in OSGi.
> - Provide the final validation coverage table (Rule | Enforced in | Exception | AC covered)
>   so I can verify completeness against acceptance-criteria.md.
> Do NOT write unit tests yet (Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Audited validation across `TicketServiceImpl` and `CommentServiceImpl`; identified gaps: **AC-6** (description required on create) and blank description on update not enforced. Created pure static `TicketValidator` (`validateForCreate`, `validateForUpdate`, `validateTitle`, `validateDescription`, `validatePriority`, `validateStatusLabel`, `parseStatusLabel`, `assertEditable`, `requireTicketId`, `validateAssigneeRequired`) and `CommentValidator` (`validateComment`). Refactored both service impls to delegate field/enum validation to validators while keeping repository-dependent checks (`requireExistingUser`, `requireTicketExists`, state machine) in services. Terminal detection via `TicketStatus.CLOSED`/`CANCELLED` in `assertEditable`. Verified `mvn clean compile test -pl core`. Updated `implementation-plan.md` — 4.1.5 complete; Active Task → 4.1.6.

---

## Part A — Audit gaps found

| Gap | Resolution |
|-----|------------|
| AC-6: description not required on create | Added `validateDescription` in `validateForCreate` |
| Blank description on update accepted | Added `validateDescription` in `validateForUpdate` when field provided |

All other rules were already enforced correctly before refactor.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Validator style | Pure static methods in `TicketValidator` / `CommentValidator` |
| Repository checks | Stay in services (user exists, ticket exists) |
| Terminal status | `TicketValidator.assertEditable` uses `TicketStatus.CLOSED` / `CANCELLED` |
| Empty comment | Reuse `ValidationException` — documented in `CommentValidator` Javadoc |
| Tests | Deferred to Task 4.1.6 per prompt |

---

## Final validation coverage table

| Rule | Enforced in | Exception | AC covered |
|------|-------------|-----------|------------|
| Title required (create/update) | `TicketValidator` | `ValidationException` | AC-3 |
| Description required (create/update if provided) | `TicketValidator` | `ValidationException` | AC-6 |
| Priority P1–P4 | `TicketValidator` | `ValidationException` | AC-4 |
| Create forces Open | `TicketServiceImpl` | — | AC-1 |
| Known assignee on create | `TicketServiceImpl` | `UnknownUserException` | AC-5 |
| Valid status filter | `TicketValidator` | `ValidationException` | AC-11 |
| Ticket id required | `TicketValidator` | `ValidationException` | — |
| Ticket exists | Service impls | `TicketNotFoundException` | AC-10, AC-46 |
| Terminal update/reassign blocked | `TicketValidator.assertEditable` | `TicketNotEditableException` | AC-16–17, AC-20–21 |
| Reassign assignee required | `TicketValidator` | `ValidationException` | AC-18 |
| Known assignee on reassign | `TicketServiceImpl` | `UnknownUserException` | AC-19 |
| Valid status labels | `TicketValidator` | `ValidationException` | AC-36 |
| Invalid transition | `TicketStateMachine` | `InvalidTransitionException` | AC-27–35 |
| Comment message required | `CommentValidator` | `ValidationException` | AC-45 |
| Comment createdBy required | `CommentValidator` | `ValidationException` | AC-41 |
| Comments on terminal tickets | no block (by design) | — | AC-42, AC-43 |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/validation/TicketValidator.java` | Created |
| `core/src/main/java/com/mysite/core/validation/CommentValidator.java` | Created |
| `core/src/main/java/com/mysite/core/services/impl/TicketServiceImpl.java` | Refactored — uses validators |
| `core/src/main/java/com/mysite/core/services/impl/CommentServiceImpl.java` | Refactored — uses validators |
| `implementation-plan.md` | Updated — 4.1.5 complete; Active Task 4.1.6 |
| `ai-prompts/implementation/14-validation-consolidation.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean compile test -pl core` | SUCCESS |
| OSGi services satisfied | Verify locally after bundle deploy |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.5 | Complete |
| FR-1, FR-7, FR-15 | Required fields, terminal edit rules, comment validation |
| AC-3–AC-6, AC-16–AC-21, AC-36, AC-45–AC-46 | Validation rules consolidated |
| NFR-AC-1 | Domain exceptions with clear messages for 400 responses |
| DOD-4 (Sprint 4.1) | Validation runs before persistence |
| Downstream | 4.1.6 unit tests; 5.1 servlet error mapping |
