# AI Prompt — Task 7.1.3: TicketServiceImpl Unit Tests

**Date:** 2026-08-27
**Purpose:** Record the testing prompt and AI response summary for Sprint 7.1 Task 7.1.3.

**Sprint/Task:** 7.1 / 7.1.3
**Category:** testing
**Meaningful:** Yes — Mockito-based service-layer unit tests for TicketServiceImpl business orchestration.

---

## Prompt (verbatim)

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

---

## AI response summary

Created `TicketServiceImplTest` in `core/src/test/java/com/mysite/core/services/` with **23 JUnit 5 tests** using `@ExtendWith(MockitoExtension.class)`, mocked `TicketRepository` and `UserRepository`, and `@InjectMocks` into `TicketServiceImpl` (real `TicketStateMachine` used internally — no mock). Mockito was already present in `core/pom.xml` (test scope); no pom change required.

Tests cover all requested behaviors: create (force Open status, validation, unknown/blank assignee), get (found/not found), update (mutable fields, terminal guard, status/assignee unchanged), reassign (valid user, unknown user, terminal), changeStatus (valid/invalid transition via real state machine, unknown label, not found), and listTickets (search, status filter, getAll, `createdAt` desc sort). Exception tests assert type and `errorCode()` where relevant.

Ran `mvn test -pl core` — **23/23** `TicketServiceImplTest` passed; **90/90** full core module, BUILD SUCCESS. Updated `implementation-plan.md` — 7.1.3 complete; active task advanced to 7.1.4.

---

## Test coverage breakdown

| Area | Tests | Key assertions |
|------|-------|----------------|
| createTicket | 6 | Status forced Open; ValidationException; UnknownUserException; blank assignee OK |
| getTicket | 2 | Found returns DTO; missing throws NOT_FOUND |
| updateTicket | 4 | Mutable fields updated; terminal TICKET_NOT_EDITABLE; status/assignee preserved |
| reassignTicket | 3 | Valid reassign; UNKNOWN_USER; terminal guard |
| changeStatus | 4 | Valid transition; INVALID_TRANSITION; unknown label; not found |
| listTickets | 4 | searchByTitle / findByStatus / getAll delegation; createdAt desc sort |
| **Total** | **23** | |

---

## Key decisions

| Topic | Decision |
|-------|----------|
| State machine | Real instance inside `TicketServiceImpl` — not mocked |
| Injection | `@InjectMocks` with field injection for OSGi `@Reference` repository fields |
| Mockito dependency | Already in `core/pom.xml` — no addition needed |
| Fixtures | Private helpers `validCreateInput()` and `existingOpenTicket()` |
| AEM/JCR | No Sling mocks — pure unit tests with repository mocks only |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/test/java/com/mysite/core/services/TicketServiceImplTest.java` | Created |
| `implementation-plan.md` | Updated — 7.1.3 complete; active task → 7.1.4 |
| `ai-prompts/testing/08-ticket-service-impl-test.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn test -pl core` | SUCCESS |
| `TicketServiceImplTest` | 23 tests, 0 failures |
| Full core module | 90 tests, BUILD SUCCESS |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 7.1 / Task 7.1.3 | Complete |
| AC-3–AC-6 | Create validation (title, priority, unknown assignee) |
| AC-16–AC-21 | Terminal edit/reassign guards |
| AC-22, AC-27–AC-28 | changeStatus valid/invalid via real state machine |
| FR-1, FR-6, FR-7, FR-8, FR-9 | Service-layer business logic verified |
| 06-testing.mdc | Mockito mocks; no AEM; real state machine |
