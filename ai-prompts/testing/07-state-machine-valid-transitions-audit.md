# AI Prompt — Task 7.1.1: State Machine Valid-Transition Audit & Gap Fill

**Date:** 2026-08-27
**Purpose:** Record the testing prompt and AI response summary for Sprint 7.1 Task 7.1.1.

**Sprint/Task:** 7.1 / 7.1.1
**Category:** testing
**Meaningful:** Yes — audit of existing TicketStateMachineTest, gap-fill only, AC-22–AC-35 verification.

---

## Prompt (verbatim)

> Task 7.1.1 (Sprint 7.1): Verify and, if needed, expand the state-machine unit tests.
> Task 4.1.6 already created TicketStateMachineTest — do NOT rewrite it; audit and fill gaps only.
>
> Follow all rules in .cursor/rules/ (06-testing). Read acceptance-criteria.md (AC-22–AC-35),
> 02-state-machine.mdc, TicketStateMachine, TicketStatus, InvalidTransitionException, and the
> existing test file(s) in core/src/test/java/com/mysite/core/statemachine.
>
> Do the following:
>
> 1. AUDIT the existing state-machine tests. Produce a coverage table:
>    | Transition (from -> to) | Expected | Tested? (existing) |
>    Cover ALL pairs across the 5 statuses (a 5x5 matrix minus same-state), marking each as:
>    - VALID (should succeed): Open->InProgress, InProgress->Resolved, Resolved->Closed,
>      Open->Cancelled, InProgress->Cancelled
>    - INVALID (should be rejected): everything else, including terminal-source (Closed->*,
>      Cancelled->*), backward (Resolved->Open, etc.), skip (Open->Resolved/Closed), same-state.
>    Also check: allowedNextStatuses correctness per state, exception content (from/to/errorCode),
>    and label handling (fromLabel/unknown label).
>
> 2. Report GAPS: list any transition/case NOT already covered by the existing tests.
>
> 3. FILL GAPS ONLY: add the missing test cases to the existing TicketStateMachineTest
>    (or a clearly-named supplementary test class). Prefer a @ParameterizedTest with a
>    @MethodSource that enumerates the FULL matrix of invalid pairs to guarantee completeness,
>    if not already present. Do NOT duplicate cases already covered.
>
> 4. Ensure the complete suite:
>    - All 5 valid transitions pass (assertDoesNotThrow / canTransition == true)
>    - All invalid transitions throw InvalidTransitionException / canTransition == false
>    - Same-state rejected
>    - allowedNextStatuses correct (terminals empty)
>    - Exception getFrom/getTo/errorCode("INVALID_TRANSITION") verified
>
> Constraints:
> - JUnit 5; pure tests (no AEM mocks needed for the state machine).
> - Do NOT modify the state machine itself unless a genuine bug is found (if so, report it first).
>
> After generating:
> - Run: mvn test. Report total test count and confirm ALL pass.
> - Provide the final coverage table showing every valid + invalid transition is now covered.
> When done, remind me to save this prompt + response summary to ai-prompts/testing/.

---

## AI response summary

Audited `TicketStateMachineTest` from Task 4.1.6 (54 tests). The full 5×5 transition matrix was already covered: 5 named valid tests + `@ParameterizedTest` over all valid pairs; 11 named invalid AC cases + `@ParameterizedTest` over all 20 invalid pairs (including same-state); `@EnumSource` same-state rejection; 5 `allowedNextStatuses` tests; exception content (enum path); unknown-label and `fromLabel` tests. **No state-machine production code changes** — no bugs found.

**Gaps filled (8 new tests):** corrected AC-35 traceability (`shouldRejectCancelledToResolved` for Cancelled→Resolved; removed mislabeled AC-35 from Cancelled→In Progress); `allowedNextStatuses(null)`; `allowedNextStatuses("Open")` label overload; null handling in `canTransition` / `assertCanTransition`; label-based valid transition; label-based invalid transition with exception content; `TicketStatus.isValidLabel`.

Ran `mvn test -pl core -Dtest=TicketStateMachineTest` — **62/62 pass** (was 54). Full core module: **67/67 pass**, BUILD SUCCESS. Updated `implementation-plan.md` — 7.1.1 complete; active task advanced to 7.1.2.

---

## Audit result — transition matrix (final)

| Transition | Expected | Covered |
|------------|----------|---------|
| Open → In Progress | VALID | ✓ individual AC-22 + parameterized |
| Open → Resolved | INVALID | ✓ individual AC-27 + parameterized |
| Open → Closed | INVALID | ✓ individual AC-28 + parameterized |
| Open → Cancelled | VALID | ✓ individual AC-25 + parameterized |
| Open → Open | INVALID (same-state) | ✓ same-state + parameterized |
| In Progress → Open | INVALID | ✓ individual + parameterized |
| In Progress → Resolved | VALID | ✓ individual AC-23 + parameterized |
| In Progress → Closed | INVALID | ✓ individual AC-29 + parameterized |
| In Progress → Cancelled | VALID | ✓ individual AC-26 + parameterized |
| In Progress → In Progress | INVALID (same-state) | ✓ same-state + parameterized |
| Resolved → Open | INVALID | ✓ individual AC-30 + parameterized |
| Resolved → In Progress | INVALID | ✓ individual AC-31 + parameterized |
| Resolved → Closed | VALID | ✓ individual AC-24 + parameterized |
| Resolved → Cancelled | INVALID | ✓ individual + parameterized |
| Resolved → Resolved | INVALID (same-state) | ✓ same-state + parameterized |
| Closed → * (all 4 targets) | INVALID | ✓ individual AC-32/33 + parameterized |
| Closed → Closed | INVALID (same-state) | ✓ same-state + parameterized |
| Cancelled → Open | INVALID | ✓ individual AC-34 + parameterized |
| Cancelled → In Progress | INVALID | ✓ individual + parameterized |
| Cancelled → Resolved | INVALID | ✓ individual AC-35 + parameterized |
| Cancelled → Closed | INVALID | ✓ parameterized |
| Cancelled → Cancelled | INVALID (same-state) | ✓ same-state + parameterized |

---

## Gaps found (pre-fill)

| Gap | Resolution |
|-----|------------|
| AC-35 mislabeled (Cancelled→In Progress vs Cancelled→Resolved) | Added `shouldRejectCancelledToResolved` with AC-35; retained In Progress test without AC label |
| Null `TicketStatus` in `canTransition` / `assertCanTransition` | Added null-handling tests |
| `allowedNextStatuses(null)` | Added test |
| `allowedNextStatuses(String)` valid label | Added test |
| Label-based valid/invalid transitions | Added tests |
| `TicketStatus.isValidLabel` | Added test |
| Closed→Resolved, Closed→Cancelled, Cancelled→Closed | Already in parameterized matrix — no duplicate individual tests added |

---

## Test coverage breakdown

| Category | Before | After |
|----------|--------|-------|
| `TicketStateMachineTest` total | 54 | **62** |
| Full `core` module | 59 | **67** |

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Rewrite vs audit | Audit only — no rewrite of existing 4.1.6 tests |
| Supplementary class | Not needed — full invalid matrix already parameterized |
| Production code | No changes — state machine behavior correct |
| AC-35 fix | Dedicated test for Cancelled→Resolved per acceptance-criteria.md |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/test/java/com/mysite/core/statemachine/TicketStateMachineTest.java` | Updated — 8 gap-filling tests |
| `implementation-plan.md` | Updated — 7.1.1 complete; active task → 7.1.2 |
| `ai-prompts/testing/07-state-machine-valid-transitions-audit.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn test -pl core -Dtest=TicketStateMachineTest` | SUCCESS — 62 tests, 0 failures |
| `mvn test -pl core` | SUCCESS — 67 tests, BUILD SUCCESS |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 7.1 / Task 7.1.1 | Complete |
| AC-22–AC-26 | All 5 valid transitions verified |
| AC-27–AC-35 | All invalid transitions verified (AC-35 label corrected) |
| FR-8, FR-9 | State machine rules re-verified at unit level |
| 06-testing.mdc | Pure JUnit 5; no AEM mocks; gap-fill only |
