# AI Prompt — Task 4.1.1: TicketStateMachine + InvalidTransitionException

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 4.1 Task 4.1.1.

**Sprint/Task:** 4.1 / 4.1.1
**Category:** implementation
**Meaningful:** Yes — core state machine domain logic; dedicated class enforcing AC-22–AC-35 transition rules.

---

## Prompt (verbatim)

> Task 4.1.1 (Sprint 4.1): Implement the TicketStateMachine and InvalidTransitionException.
>
> Follow all rules in .cursor/rules/ — ESPECIALLY 02-state-machine.mdc. Read
> acceptance-criteria.md (AC-22–AC-35) and api-contract.md (invalid transition -> HTTP 409).
> Target packages: com.mysite.core.statemachine and com.mysite.core.exception.
>
> This is the CORE of the application. It must be a dedicated, self-contained class —
> no transition logic anywhere else in the codebase.
>
> Create:
>
> 1. An enum com.mysite.core.statemachine.TicketStatus with values and their display labels:
>    - OPEN("Open"), IN_PROGRESS("In Progress"), RESOLVED("Resolved"),
>      CLOSED("Closed"), CANCELLED("Cancelled")
>    - String getLabel()
>    - static TicketStatus fromLabel(String label)  // maps "In Progress" -> IN_PROGRESS;
>        throws IllegalArgumentException for unknown labels (guard null/blank)
>    - static boolean isValidLabel(String label)
>    This lets the rest of the app use labels ("Open") from the CF/JSON while the state
>    machine works with the enum.
>
> 2. com.mysite.core.exception.InvalidTransitionException (extends RuntimeException):
>    - Constructor(TicketStatus from, TicketStatus to) producing message like
>      "Invalid transition: Open -> Closed"
>    - Also expose getFrom()/getTo() and a stable errorCode() returning "INVALID_TRANSITION"
>      (aligns with api-contract error catalog; used by servlet to map -> HTTP 409)
>
> 3. com.mysite.core.statemachine.TicketStateMachine (stateless, injectable as needed):
>    Define the allowed transitions EXACTLY:
>      OPEN        -> IN_PROGRESS
>      IN_PROGRESS -> RESOLVED
>      RESOLVED    -> CLOSED
>      OPEN        -> CANCELLED
>      IN_PROGRESS -> CANCELLED
>    CLOSED and CANCELLED are terminal (no outgoing transitions).
>
>    Methods:
>    - boolean canTransition(TicketStatus from, TicketStatus to)
>    - void assertCanTransition(TicketStatus from, TicketStatus to)
>        // throws InvalidTransitionException if not allowed
>    - Set<TicketStatus> allowedNextStatuses(TicketStatus from)
>        // returns the valid next statuses (empty for terminal states) — used by the UI later
>    - Convenience overloads accepting String labels (delegate to enum fromLabel), which
>      throw IllegalArgumentException (not InvalidTransition) for unknown status labels.
>
>    Implementation:
>    - Use an EnumMap<TicketStatus, Set<TicketStatus>> (or Guava-free equivalent) as the
>      transition table, built once (static or constructor).
>    - No external state; pure logic. No AEM/Sling/JCR imports.
>
> 4. Design constraints:
>    - Do NOT reference any repository, service, servlet, or AEM type here — pure domain logic.
>    - Javadoc on enum, exception, and state machine (document the transition table clearly).
>    - Make the allowed transitions easy to read (a clear table/definition), so a reviewer
>      can verify correctness at a glance.
>
> After generating:
> - Confirm mvn clean install compiles.
> - List the transition table you implemented so I can verify it matches the spec exactly.
> - Do NOT write unit tests yet (tests are Task 4.1.6) — but ensure the API is test-friendly.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Implemented three pure-domain classes in `com.mysite.core.statemachine` and `com.mysite.core.exception`: `TicketStatus` enum (labels, `fromLabel`, `isValidLabel`), `InvalidTransitionException` (`getFrom`/`getTo`, `errorCode()` → `INVALID_TRANSITION`, message `"Invalid transition: {from} -> {to}"`), and `TicketStateMachine` with an immutable `EnumMap`/`EnumSet` transition table. API: `canTransition`, `assertCanTransition`, `allowedNextStatuses` (enum + String label overloads). Terminal states `CLOSED` and `CANCELLED` return empty next-status sets. Verified `mvn clean compile -pl core` and `mvn clean test -pl core` (full reactor `mvn clean install` not confirmed in sandbox due to `ui.frontend` npm and `.m2` install limits). Updated `implementation-plan.md` — 4.1.1 complete; Active Task → 4.1.2.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Transition table | `EnumMap<TicketStatus, Set<TicketStatus>>` built once in static initializer; unmodifiable sets |
| Exception message | Colon format per task prompt: `"Invalid transition: Open -> Closed"` (api-contract omits colon — servlet can format from `getFrom()`/`getTo()` if needed) |
| Null handling | `canTransition(null, …)` → `false`; `assertCanTransition(null, …)` → `IllegalArgumentException` |
| OSGi | Plain stateless class (no `@Component`); services can inject or instantiate |
| Tests | Deferred to Task 4.1.6 per prompt |

---

## Transition table implemented

| From | Allowed next statuses |
|------|----------------------|
| Open | In Progress, Cancelled |
| In Progress | Resolved, Cancelled |
| Resolved | Closed |
| Closed | *(none — terminal)* |
| Cancelled | *(none — terminal)* |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/statemachine/TicketStatus.java` | Created |
| `core/src/main/java/com/mysite/core/statemachine/TicketStateMachine.java` | Created |
| `core/src/main/java/com/mysite/core/exception/InvalidTransitionException.java` | Created |
| `implementation-plan.md` | Updated — 4.1.1 complete; Active Sprint 4.1; Active Task 4.1.2 |
| `ai-prompts/implementation/10-ticket-state-machine.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean compile -pl core` | SUCCESS |
| `mvn clean test -pl core` | SUCCESS |
| `mvn clean install` (full reactor) | Not run in agent sandbox — run locally |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.1 | Complete |
| FR-8, FR-9, FR-10 | State machine enforcement foundation |
| AC-22–AC-26 | Five valid transitions defined in table |
| AC-27–AC-35 | All other pairings rejected via `canTransition` / `assertCanTransition` |
| NFR-AC-2 | `InvalidTransitionException.errorCode()` → `INVALID_TRANSITION` for HTTP 409 |
| [02-state-machine.mdc](../../.cursor/rules/02-state-machine.mdc) | Dedicated class; no scattered transition logic |
| [api-contract.md](../../api-contract.md) | Error catalog `INVALID_TRANSITION` → 409 |
| Downstream | 4.1.3 `TicketService.changeStatus`; 4.1.6 unit tests; 5.1.4 `TicketStatusServlet` |
