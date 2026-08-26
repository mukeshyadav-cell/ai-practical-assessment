# AI Prompt — Task 4.1.6: TicketStateMachine Unit Tests

**Date:** 2025-08-26
**Purpose:** Record the testing prompt and AI response summary for Sprint 4.1 Task 4.1.6.

**Sprint/Task:** 4.1 / 4.1.6
**Category:** testing
**Meaningful:** Yes — core state-machine verification with 54 JUnit 5 tests proving AC-22–AC-35.

---

## Prompt (verbatim)

> Task 4.1.6 (Sprint 4.1): Write comprehensive JUnit 5 unit tests for the TicketStateMachine
> that PROVE the state-machine rules. This is the core verification of the exercise.
>
> Follow all rules in .cursor/rules/ (06-testing.mdc). Read acceptance-criteria.md
> (AC-22–AC-35), 02-state-machine.mdc, TicketStateMachine, TicketStatus, and
> InvalidTransitionException. Target: core/src/test/java/com/mysite/core/statemachine/.
> Use JUnit 5 (org.junit.jupiter). NO AEM/Sling mocks needed — the state machine is pure logic.
>
> Create test class: TicketStateMachineTest
>
> Cover EXACTLY the following, with clear behavior-named test methods:
>
> 1. VALID transitions — each must succeed (assertDoesNotThrow on assertCanTransition
>    AND assertTrue on canTransition):
>    - shouldAllowOpenToInProgress
>    - shouldAllowInProgressToResolved
>    - shouldAllowResolvedToClosed
>    - shouldAllowOpenToCancelled
>    - shouldAllowInProgressToCancelled
>    Prefer a @ParameterizedTest with a MethodSource of all 5 valid pairs as well,
>    in addition to (or instead of) individual methods — choose the cleaner approach and
>    keep it readable.
>
> 2. INVALID transitions — each must throw InvalidTransitionException
>    (use assertThrows) AND canTransition must return false:
>    - shouldRejectOpenToResolved
>    - shouldRejectOpenToClosed
>    - shouldRejectInProgressToClosed
>    - shouldRejectInProgressToOpen
>    - shouldRejectResolvedToOpen
>    - shouldRejectResolvedToInProgress
>    - shouldRejectResolvedToCancelled
>    - shouldRejectClosedToOpen (terminal source)
>    - shouldRejectClosedToInProgress
>    - shouldRejectCancelledToOpen (terminal source)
>    - shouldRejectCancelledToInProgress
>    Add a @ParameterizedTest covering a comprehensive matrix of invalid pairs too.
>
> 3. SAME-STATE transitions must be rejected:
>    - shouldRejectSameStateTransition (e.g., Open->Open, Resolved->Resolved)
>
> 4. TERMINAL state checks via allowedNextStatuses:
>    - shouldReturnCorrectNextStatusesForOpen        (== {IN_PROGRESS, CANCELLED})
>    - shouldReturnCorrectNextStatusesForInProgress  (== {RESOLVED, CANCELLED})
>    - shouldReturnCorrectNextStatusesForResolved    (== {CLOSED})
>    - shouldReturnEmptyNextStatusesForClosed        (empty set)
>    - shouldReturnEmptyNextStatusesForCancelled     (empty set)
>
> 5. EXCEPTION content:
>    - shouldIncludeFromAndToInException: assert InvalidTransitionException.getFrom()/getTo()
>      and errorCode() == "INVALID_TRANSITION", and message contains both labels.
>
> 6. LABEL handling (if the state machine exposes String-label overloads):
>    - shouldRejectUnknownStatusLabel: unknown label -> IllegalArgumentException (or
>      ValidationException, per the implementation) — assert the actual behavior.
>    - shouldMapLabelsToEnumCorrectly: TicketStatus.fromLabel("In Progress") == IN_PROGRESS
>
> Requirements:
> - Use assertThrows / assertDoesNotThrow / assertEquals / assertTrue / assertFalse.
> - Parameterized tests should use @MethodSource providing (from, to) pairs.
> - Descriptive @DisplayName on tests where helpful.
> - Do NOT test AEM/repository/service here — ONLY the state machine (pure).
> - Ensure EVERY allowed transition is proven AND every terminal/backward/skip transition is rejected.
>
> After generating:
> - Run: mvn test  (or mvn clean install) — ALL tests must pass.
> - Report the total number of test cases and confirm all valid transitions pass and all
>   invalid ones are rejected. If any test fails, show the failure so we can fix the state
>   machine or the test.
> When done, remind me to save this prompt + response summary to ai-prompts/testing/.

---

## AI response summary

Created `TicketStateMachineTest` in `core/src/test/java/com/mysite/core/statemachine/` with 54 JUnit 5 test cases: 5 named valid transitions plus `@ParameterizedTest` over all 5 valid pairs; 11 named invalid AC cases plus `@ParameterizedTest` over the full 20-pair invalid matrix (all enum combinations minus the 5 allowed); `@EnumSource` same-state rejection for all 5 statuses; 5 `allowedNextStatuses` assertions; exception content test (`getFrom`/`getTo`, `errorCode`, message labels); label tests (`IllegalArgumentException` for unknown labels, `fromLabel("In Progress")`). Helpers `assertValidTransition` / `assertInvalidTransition` centralize assertions. Ran `mvn test -pl core` — 54/54 `TicketStateMachineTest` passed, 59 total core tests, BUILD SUCCESS. Updated `implementation-plan.md` — 4.1.6 complete; Sprint 4.1 → Quality Gate.

---

## Test coverage breakdown

| Category | Count |
|----------|-------|
| Valid transitions (named + parameterized) | 10 |
| Invalid transitions (named AC cases + 20-pair matrix) | 31 |
| Same-state rejections (`@EnumSource` × 5) | 5 |
| `allowedNextStatuses` | 5 |
| Exception content | 1 |
| Label handling | 2 |
| **Total `TicketStateMachineTest`** | **54** |

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Structure | Named tests for readability + parameterized matrix for exhaustive coverage |
| Invalid matrix | All 5×5 enum pairs minus 5 valid = 20 invalid combinations |
| Unknown labels | Assert `IllegalArgumentException` (actual `TicketStatus.fromLabel` behavior) |
| Mocks | None — pure state machine, no AEM/Sling |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/test/java/com/mysite/core/statemachine/TicketStateMachineTest.java` | Created |
| `implementation-plan.md` | Updated — 4.1.6 complete; Sprint 4.1 Quality Gate |
| `ai-prompts/testing/01-ticket-state-machine-test.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn test -pl core` | SUCCESS |
| `TicketStateMachineTest` | 54 tests, 0 failures |
| Full core module | 59 tests, BUILD SUCCESS |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 4.1 / Task 4.1.6 | Complete |
| AC-22–AC-26 | All 5 valid transitions proven |
| AC-27–AC-35 | All representative invalid transitions rejected |
| FR-8, FR-9 | State machine enforcement verified at unit level |
| Sprint 4.1 DOD-1, DOD-2 | Every valid transition has a test; invalid transitions rejected |
| Sprint 4.1 Quality Gate | State-machine unit tests pass |
