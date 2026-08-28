# Prompt History — Sprint 7.1: Unit Tests

**Date:** 2026-08-27
**Sprint:** 7.1 — Unit Tests
**Status:** Complete (pending developer review)
**Tasks covered:** 7.1.1 → 7.1.4
**Traceability:** AC-22–AC-35 (state machine); AC-3–AC-6, AC-16–AC-21 (ticket validation/terminal); AC-41–AC-46 (comments); FR-1, FR-6–FR-9, FR-13–FR-15; Sprint 7.1 Quality Gate

---

> **Recovery notice**
> Prompts in this file are **verbatim** — recovered from the Cursor conversation transcript.
> Typos in original prompts are preserved intentionally.
> AI responses are summarized, not pasted in full.

---

## Goal

Deliver the mandatory Java unit-test suite in `core/src/test`: verify and expand state-machine tests from Task 4.1.6, then add Mockito-based service-layer tests for `TicketServiceImpl` and `CommentServiceImpl` — all green via `mvn test -pl core`, with `it.tests` and `ui.tests` remaining unused.

## Tasks Completed

| Task ID | Summary | Outcome |
| ------- | ------- | ------- |
| 7.1.1 | State machine valid transitions | Audited `TicketStateMachineTest`; added 8 gap-filling tests (62 total); AC-35 label corrected |
| 7.1.2 | State machine invalid transitions | Satisfied within 7.1.1 audit — full invalid matrix already parameterized from 4.1.6; no separate prompt in session |
| 7.1.3 | TicketService unit tests | Created `TicketServiceImplTest` — 23 tests; mocked repos + real state machine |
| 7.1.4 | CommentService unit tests | Created `CommentServiceImplTest` — 11 tests; terminal-ticket comments allowed |

## Prompts Log

### Prompt 1 — Sprint 7.1 session start
**Time:** 5:22 PM | **Task:** 7.1 (context)

**Actual prompt:**
> Starting a fresh Cursor session for Sprint 7.1 (Unit Tests — mandatory testing deliverable).
>
> READ before we begin:
> - .cursor/rules/ (ESPECIALLY 06-testing.mdc — Java unit tests only, no it.tests/ui.tests)
> - implementation-plan.md (Sprint 7.1 tasks + current status)
> - acceptance-criteria.md (state machine ACs AC-22–AC-35; validation/comment ACs)
> - Existing tests: core/src/test/java/com/mysite/core/statemachine (TicketStateMachineTest from 4.1.6)
> - Code under test: TicketStateMachine, TicketStatus, InvalidTransitionException,
>   TicketServiceImpl, CommentServiceImpl, TicketValidator, domain exceptions
>
> IMPORTANT context:
> - Task 4.1.6 already created TicketStateMachineTest. Sprint 7.1 should VERIFY/EXPAND those and
>   add SERVICE-layer unit tests. Do NOT duplicate or rewrite existing passing tests.
> - Testing scope: Java UNIT tests in core/src/test ONLY. it.tests and ui.tests are OUT OF SCOPE.
> - JUnit 5; use Sling/AEM Mocks only where services need a resolver; mock repositories for
>   service tests.
>
> Summarize back to confirm:
> 1. Active Sprint / Active Task (7.1.1)
> 2. What state-machine tests ALREADY exist (from 4.1.6) and their coverage
> 3. TicketService and CommentService method signatures (for service tests in 7.1.3/7.1.4)
> 4. Whether TicketStateMachine is pure (no OSGi) so tests need no mocks
>
> Do NOT write code yet — wait for my Task 7.1.1 prompt.

**AI output:**
- Confirmed Sprint 7.1 / Task 7.1.1; summarized existing 54-test `TicketStateMachineTest` coverage (AC-22–AC-35)
- Listed `TicketService` and `CommentService` method signatures; confirmed `TicketStateMachine` is pure POJO (no OSGi mocks)

**What I accepted / changed:** Accepted context summary; proceeded to 7.1.1
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 2 — Task 7.1.1: State machine audit & gap fill
**Time:** 5:23 PM | **Task:** 7.1.1 (also satisfies 7.1.2 invalid-transition verification)

**Actual prompt:**
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

**AI output:**
- Audited 54 existing tests; full 5×5 matrix already covered via named tests + `@ParameterizedTest invalidTransitionPairs`
- Added 8 gap tests: AC-35 fix (Cancelled→Resolved), null handling, label API, `isValidLabel`
- `TicketStateMachineTest` 54→62; core module 67 tests green; no production code changes
- Logged to `ai-prompts/testing/07-state-machine-valid-transitions-audit.md`

**What I accepted / changed:** Accepted gap-fill approach; 7.1.2 invalid coverage deemed complete within same audit
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 3 — Task 7.1.3: TicketServiceImpl unit tests
**Time:** 6:59 PM | **Task:** 7.1.3

**Actual prompt:**
> Task 7.1.3 (Sprint 7.1): Write JUnit 5 unit tests for TicketServiceImpl, mocking the
> repositories and using the REAL TicketStateMachine, to verify business-logic orchestration.
>
> Follow all rules in .cursor/rules/ (06-testing). Read acceptance-criteria.md (create/update/
> reassign/status ACs), TicketServiceImpl, TicketRepository, UserRepository, TicketStateMachine,
> TicketStatus, TicketDTO, UserDTO, and the domain exceptions.
> Target: core/src/test/java/com/mysite/core/services/TicketServiceImplTest.java
>
> Setup:
> - JUnit 5 (org.junit.jupiter) + Mockito (org.mockito). If Mockito is not already a test
>   dependency in core/pom.xml, add it (test scope). Use @ExtendWith(MockitoExtension.class).
> - Mock: TicketRepository, UserRepository.
> - Use the REAL TicketStateMachine (pure logic; instantiate directly).
> - Inject the mocks + real state machine into TicketServiceImpl (constructor or reflection/
>   field injection consistent with how the impl wires dependencies — check the impl and use the
>   cleanest approach; if fields are @Reference, use a test constructor or Mockito @InjectMocks
>   with field injection).
>
> Write tests (behavior-named, Arrange-Act-Assert):
>
> createTicket:
> - shouldForceStatusOpenOnCreate: given a DTO with status "Resolved", created ticket has status
>   "Open" (repo.create captured/returned with Open).
> - shouldRejectBlankTitle: blank/null title -> ValidationException.
> - shouldRejectInvalidPriority: priority not in P1..P4 -> ValidationException.
> - shouldRejectUnknownAssigneeOnCreate: assignedTo set to a user that userRepository.getById
>   returns empty for -> UnknownUserException.
> - shouldAllowBlankAssignee: blank assignedTo is allowed (no exception).
> - shouldReturnCreatedTicket: valid input -> repo.create called, returns the DTO.
>
> getTicket:
> - shouldReturnTicketWhenFound.
> - shouldThrowNotFoundWhenMissing: repo.getById empty -> TicketNotFoundException.
>
> updateTicket:
> - shouldUpdateMutableFields: title/description/priority updated; repo.update called.
> - shouldThrowNotEditableWhenTerminal: existing status Closed (or Cancelled) ->
>   TicketNotEditableException; repo.update NOT called.
> - shouldThrowNotFoundWhenUpdatingMissing.
> - shouldNotChangeStatusOrAssigneeViaUpdate: verify update does not alter status/assignedTo.
>
> reassignTicket:
> - shouldReassignToValidUser: userRepository.getById returns a user -> repo.update called with
>   new assignedTo.
> - shouldRejectUnknownUserOnReassign -> UnknownUserException; repo.update NOT called.
> - shouldRejectReassignOnTerminalTicket -> TicketNotEditableException.
>
> changeStatus:
> - shouldAllowValidTransition: Open ticket -> "In Progress" succeeds; repo.update called with
>   new status.
> - shouldRejectInvalidTransition: Open ticket -> "Closed" -> InvalidTransitionException;
>   repo.update NOT called.
> - shouldRejectUnknownStatusLabel: "Bogus" -> ValidationException.
> - shouldThrowNotFoundWhenChangingStatusOfMissingTicket.
>
> listTickets (delegation):
> - shouldSearchWhenQueryProvided: q non-blank -> repo.searchByTitle called.
> - shouldFilterByStatusWhenOnlyStatusProvided: status non-blank, q blank -> repo.findByStatus.
> - shouldReturnAllWhenNoFilters -> repo.getAll.
> - (If service sorts by createdAt desc, assert ordering on a small sample.)
>
> Requirements:
> - Use Mockito when(...).thenReturn(...), verify(...), verifyNoInteractions/verify(...,never()).
> - Use assertThrows for exception cases; assert the exception type (and errorCode where relevant).
> - Build TicketDTO fixtures via a small private helper.
> - Do NOT touch AEM/JCR — pure unit tests with mocks + real state machine.
>
> After generating:
> - Ensure core/pom.xml has Mockito (test scope); confirm mvn test compiles and ALL tests pass.
> - Report the test count and confirm all green.
> When done, remind me to save this prompt + response summary to ai-prompts/testing/.

**AI output:**
- Created `TicketServiceImplTest` with 23 tests; `@InjectMocks` + mocked repos; real state machine inside impl
- Mockito already in `core/pom.xml`; 90 core tests green
- Logged to `ai-prompts/testing/08-ticket-service-impl-test.md`

**What I accepted / changed:** Accepted all 23 tests and Mockito pattern
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 4 — Task 7.1.4: CommentServiceImpl unit tests
**Time:** 7:09 PM | **Task:** 7.1.4

**Actual prompt:**
> Task 7.1.4 (Sprint 7.1): Write JUnit 5 unit tests for CommentServiceImpl, mocking
> CommentRepository and TicketRepository, to verify comment business logic.
>
> Follow all rules in .cursor/rules/ (06-testing). Read acceptance-criteria.md (comment ACs),
> CommentServiceImpl, CommentRepository, TicketRepository, CommentDTO, TicketDTO, and the domain
> exceptions. Target: core/src/test/java/com/mysite/core/services/CommentServiceImplTest.java
>
> Setup:
> - JUnit 5 + Mockito (@ExtendWith(MockitoExtension.class)); Mockito already added in 7.1.3.
> - Mock: CommentRepository, TicketRepository.
> - Inject mocks into CommentServiceImpl using the same approach as TicketServiceImplTest
>   (test constructor or @InjectMocks — match how the impl is wired).
>
> Write tests (behavior-named, Arrange-Act-Assert):
>
> addComment:
> - shouldAddCommentWhenTicketExistsAndMessageValid:
>     ticketRepository.getById returns a ticket; commentRepository.add returns the comment;
>     verify commentRepository.add called and the returned CommentDTO is correct.
> - shouldRejectEmptyMessage:
>     blank/null message -> ValidationException; commentRepository.add NOT called.
> - shouldRejectWhitespaceOnlyMessage:
>     message "   " -> ValidationException.
> - shouldThrowNotFoundWhenTicketMissing:
>     ticketRepository.getById empty -> TicketNotFoundException; commentRepository.add NOT called.
> - shouldAllowCommentOnClosedTicket:
>     ticket status "Closed" -> addComment SUCCEEDS (no exception; commentRepository.add called).
>     This proves comments are allowed on terminal tickets.
> - shouldAllowCommentOnCancelledTicket:
>     ticket status "Cancelled" -> succeeds.
> - shouldUsePathTicketIdAsAuthoritative:
>     if the incoming CommentDTO.ticketId differs from the path ticketId, the persisted comment
>     uses the PATH ticketId (verify the value passed to commentRepository.add).
> - (If service requires non-blank createdBy) shouldRejectBlankCreatedBy -> ValidationException.
>
> listComments:
> - shouldReturnCommentsWhenTicketExists:
>     ticketRepository.getById returns a ticket; commentRepository.listByTicket returns a list;
>     verify the list is returned (order preserved as provided by repo).
> - shouldThrowNotFoundWhenListingCommentsOfMissingTicket:
>     ticketRepository.getById empty -> TicketNotFoundException.
> - shouldReturnEmptyListWhenNoComments:
>     commentRepository.listByTicket returns empty -> service returns empty list (not null).
>
> Requirements:
> - Mockito when(...).thenReturn(...), verify(...), verify(...,never()) for no-persistence cases.
> - assertThrows for exceptions; assert exception type (and errorCode where relevant).
> - Small private helpers to build TicketDTO/CommentDTO fixtures (incl. a helper for a ticket
>   with a given status, e.g., closedTicket()).
> - Pure unit tests — no AEM/JCR.
>
> After generating:
> - Confirm mvn test compiles and ALL tests pass (state machine + TicketService + CommentService).
> - Report the total test count across the whole suite.
> When done, remind me to save this prompt + response summary to ai-prompts/testing/.

**AI output:**
- Created `CommentServiceImplTest` with 11 tests; terminal-ticket comment success verified
- Full core suite 101 tests green

**What I accepted / changed:** Accepted all 11 tests
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 5 — Sprint 7.1 Quality Gate
**Time:** 7:12 PM | **Task:** 7.1 (Quality Gate)

**Actual prompt:**
> Sprint 7.1 Quality Gate: All unit test tasks (7.1.1–7.1.4) complete and committed.
>
> Step 1 — Verify Sprint 7.1 Definition of Done:
> - mvn test is fully green
> - State machine: all valid transitions succeed + representative invalid rejected
>   (covered by Task 4.1.6, verified in 7.1.1/7.1.2)
> - TicketService: create (forces Open), validation, reassign (unknown user), updateTicket
>   (terminal block), changeStatus (state machine), list delegation — covered
> - CommentService: empty message rejected, missing ticket rejected, allowed on terminal,
>   list behavior — covered
> - it.tests and ui.tests remain unused (out of scope)
> Provide the total test count and a short coverage summary table
> (component | tests | key scenarios). State any DOD not met; else "All DOD satisfied".
>
> Step 2 — Generate the sprint log:
> Following the prompt-history-sprint-log rule in .cursor/rules/:
> 1. Read THIS session's Cursor transcript.
> 2. Filter meaningful user prompts.
> 3. Generate prompt-history/sprint-7.1.md using the EXACT rule structure (verbatim prompts,
>    summarized AI outputs, what I accepted/changed, files changed, requirements traced
>    AC-22–AC-35 + validation ACs, QG result).
> 4. Update prompt-history/README.md Session Index with the Sprint 7.1 row.
>
> Sprint title: "Unit Tests".
> Do NOT start Sprint 8.1 yet — stop after generating the sprint log for my review.

**AI output:**
- Verified `mvn test -pl core` — 101 tests, BUILD SUCCESS; all DOD satisfied
- Generated `prompt-history/sprint-7.1.md` and updated Session Index

**What I accepted / changed:** Pending developer review
**What I rejected / why:** N/A
**Iteration needed:** No

---

## What I did without AI assistance

- Committed Sprint 7.1 unit test tasks (per QG prompt)
- Manual review of test coverage and `mvn test` verification before QG

## Prompt engineering notes

| Observation | What it shows |
|----|---|
| Audit-before-rewrite for 7.1.1 | Avoided duplicating 54 existing state-machine tests; gap-fill only |
| Single audit task covered 7.1.1 + 7.1.2 | Parameterized invalid matrix from 4.1.6 made separate 7.1.2 prompt unnecessary |
| Explicit test-name checklist for services | Produced complete Mockito suites on first pass (23 + 11 tests) |
| Real state machine in service tests | Validates orchestration without mocking transition logic |

## Files changed

| File | Change |
|---|-----|
| `core/src/test/java/com/mysite/core/statemachine/TicketStateMachineTest.java` | Updated — 8 gap-filling tests (54→62) |
| `core/src/test/java/com/mysite/core/services/TicketServiceImplTest.java` | Created — 23 tests |
| `core/src/test/java/com/mysite/core/services/CommentServiceImplTest.java` | Created — 11 tests |
| `implementation-plan.md` | Updated — tasks 7.1.1–7.1.4 complete; QG status |
| `ai-prompts/testing/07-state-machine-valid-transitions-audit.md` | Created |
| `ai-prompts/testing/08-ticket-service-impl-test.md` | Created |
| `prompt-history/sprint-7.1.md` | Created (this file) |
| `prompt-history/README.md` | Updated — Sprint 7.1 index row |

## Requirements traced

| ID | Coverage |
|----|----------|
| AC-22–AC-26 | All 5 valid transitions — `TicketStateMachineTest` |
| AC-27–AC-35 | All invalid transitions — named + parameterized matrix |
| AC-3–AC-6 | Create validation — `TicketServiceImplTest` |
| AC-16–AC-21 | Terminal edit/reassign guards — `TicketServiceImplTest` |
| AC-41–AC-43 | Comments on any status incl. terminal — `CommentServiceImplTest` |
| AC-45–AC-46 | Empty message / missing ticket — `CommentServiceImplTest` |
| FR-8, FR-9 | State machine unit level |
| FR-1, FR-6, FR-7 | TicketService orchestration |
| FR-13–FR-15 | CommentService orchestration |
| 06-testing.mdc | Java unit tests only; it.tests/ui.tests unused |

## Quality Gate result

| Check | Result |
|----|-----|
| `mvn test -pl core` | Passed — 101 tests, 0 failures |
| State machine valid + invalid transitions | Passed — 62 tests |
| TicketService scenarios | Passed — 23 tests |
| CommentService scenarios | Passed — 11 tests |
| it.tests / ui.tests unused | Confirmed — out of scope |
| Sprint log generated | Passed |

**Sprint exit:** Passed. Ready for Sprint 8.1 (pending developer review).

## Developer review

**Status:** Pending review
**Approved by:** Developer — (pending)
**Notes:** Prompts verbatim from Cursor transcript (`f4939895-428a-49dd-8050-fd38bca68f79`). Typos preserved.
