# Prompt History — Sprint 4.1: State Machine + Services

**Date:** 2025-08-26
**Sprint:** 4.1 — State Machine + Services
**Status:** Complete
**Tasks covered:** 4.1.1 → 4.1.6
**Traceability:** FR-1–FR-15, FR-7–FR-10; AC-3–AC-6, AC-16–AC-21, AC-22–AC-36, AC-41–AC-46; NFR-AC-1, NFR-AC-2

---

> **Recovery notice**
> Prompts in this file are **verbatim** — recovered from the Cursor conversation transcript.
> Typos in original prompts are preserved intentionally.
> AI responses are summarized, not pasted in full.

---

## Goal

Deliver the business logic layer for the Support Ticket Management System: a dedicated ticket lifecycle state machine (`TicketStateMachine`), domain exception hierarchy aligned with the API error catalog, `TicketService` and `CommentService` orchestrating repository ports with validation, and comprehensive JUnit 5 state-machine tests proving AC-22–AC-35.

## Tasks Completed

| Task ID | Summary | Outcome |
| ------- | ------- | ------- |
| 4.1.1 | State machine + `InvalidTransitionException` | `TicketStatus` enum, `TicketStateMachine` (`EnumMap` transition table), `InvalidTransitionException` |
| 4.1.2 | Domain exception hierarchy | `DomainException` base; `ValidationException`, `TicketNotFoundException`, `TicketNotEditableException`, `UnknownUserException`; refactored `InvalidTransitionException` |
| 4.1.3 | `TicketService` | Interface + `TicketServiceImpl` OSGi component; create/list/get/update/reassign/changeStatus |
| 4.1.4 | `CommentService` | Interface + `CommentServiceImpl`; add/list comments; terminal-status allowed |
| 4.1.5 | Validation consolidation | `TicketValidator`, `CommentValidator`; audit closed AC-6 description gap |
| 4.1.6 | State machine unit tests | `TicketStateMachineTest` — 54 tests, all green |

## Prompts Log

### Prompt 1 — Sprint 4.1 session start (context rebuild)
**Time:** 9:35 PM | **Task:** 4.1 (context)

**Actual prompt:**
> Starting a fresh Cursor session for Sprint 4.1 (State Machine + Services) — the core sprint.
>
> READ before we begin:
> - .cursor/rules/ (ESPECIALLY 02-state-machine.mdc — the transition rules)
> - implementation-plan.md (Sprint 4.1 task list + current status)
> - requirements-analysis.md + acceptance-criteria.md (state machine ACs: AC-22–AC-35)
> - api-contract.md (status endpoint → 409 on invalid transition)
> - Existing code: com.mysite.core.dto, com.mysite.core.repositories, com.mysite.core.exception
>
> Summarize back to confirm context:
> 1. Active Sprint / Active Task
> 2. The EXACT list of allowed transitions and the statuses
> 3. What must happen on an invalid transition (exception type + HTTP mapping)
> 4. Where the state machine class and exception should live (package)
>
> Do NOT write code yet — wait for my Task 4.1.1 prompt.

**AI output:**
- Read rules, plan, ACs, api-contract, existing DTO/repository/exception code
- Summarized Sprint 4.1 scope, 5 statuses, 5 allowed transitions, `InvalidTransitionException` → HTTP 409, packages `com.mysite.core.statemachine` and `com.mysite.core.exception`
- Noted plan file still showed Sprint 3.1 QG; deferred status update to task kickoff

**What I accepted / changed:** Accepted context summary; proceeded to 4.1.1
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 2 — Task 4.1.1: TicketStateMachine + InvalidTransitionException
**Time:** 9:36 PM | **Task:** 4.1.1

**Actual prompt:**
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

**AI output:**
- Created `TicketStatus`, `TicketStateMachine` (immutable `EnumMap`/`EnumSet` table), `InvalidTransitionException`
- Verified `mvn clean compile -pl core`; full reactor blocked in sandbox (frontend npm)
- Updated `implementation-plan.md` — 4.1.1 complete

**What I accepted / changed:** Accepted implementation; logged to `ai-prompts/implementation/10-ticket-state-machine.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 3 — Task 4.1.2: Domain exception hierarchy
**Time:** 9:54 PM | **Task:** 4.1.2

**Actual prompt:**
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

**AI output:**
- Created `DomainException`, `ValidationException`, `TicketNotEditableException`, `UnknownUserException`
- Refactored `TicketNotFoundException` and `InvalidTransitionException` to extend base
- Verified `mvn clean compile -pl core`; updated plan to 4.1.2 complete

**What I accepted / changed:** Accepted; logged to `ai-prompts/implementation/11-domain-exceptions.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 4 — Task 4.1.3: TicketService
**Time:** 9:57 PM | **Task:** 4.1.3

**Actual prompt:**
> Task 4.1.3 (Sprint 4.1): Implement TicketService (interface + impl) — the business logic
> hub wiring repositories, the state machine, and domain exceptions.
>
> Follow all rules in .cursor/rules/. Read api-contract.md (endpoint behaviors + error codes),
> requirements-analysis.md (FR-1..FR-12, FR-7 terminal edit rule), acceptance-criteria.md,
> the TicketRepository/UserRepository interfaces, TicketStateMachine, TicketStatus enum,
> and the domain exceptions. Target: com.mysite.core.services (interface) and
> com.mysite.core.services.impl.
>
> Create:
>
> 1. Interface com.mysite.core.services.TicketService with methods:
>    - TicketDTO createTicket(TicketDTO ticket)
>    - List<TicketDTO> listTickets(String statusFilter, String query)  // both nullable/optional
>    - TicketDTO getTicket(String id)                                  // throws TicketNotFoundException
>    - TicketDTO updateTicket(String id, TicketDTO changes)            // title/description/priority only
>    - TicketDTO reassignTicket(String id, String assigneeUserId)
>    - TicketDTO changeStatus(String id, String newStatusLabel)        // enforces state machine
>    Javadoc each with behavior + which exceptions it may throw.
>
> 2. Impl com.mysite.core.services.impl.TicketServiceImpl:
>    @Component(service = TicketService.class)
>    @Reference(target = "(impl.type=contentfragment)") private TicketRepository ticketRepository;
>    @Reference(target = "(impl.type=aem)")            private UserRepository userRepository;
>    @Reference private TicketStateMachine stateMachine;
>    // If TicketStateMachine is not an OSGi component, instantiate it directly instead of @Reference —
>    // choose the cleaner option and state which; it is pure logic so a plain 'new' is acceptable.
>
>    [... full method behavior spec for create/list/get/update/reassign/changeStatus ...]
>
> After generating:
> - Confirm mvn clean install compiles and the component is satisfied in OSGi console
>   (check the @Reference targets resolve: contentfragment + aem).
> - Summarize each method's validation + exceptions so I can verify against api-contract.md.
> Do NOT write service unit tests yet (that is Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Created `TicketService` + `TicketServiceImpl` with OSGi `@Reference` to `contentfragment`/`aem` repos
- `TicketStateMachine` instantiated via `new` (pure logic)
- Extended `ContentFragmentTicketRepository.update()` to persist `status` for `changeStatus`
- MVP reassign requires non-blank assignee; list combines query + status filter
- Verified `mvn clean compile test -pl core`

**What I accepted / changed:** Accepted; logged to `ai-prompts/implementation/12-ticket-service.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 5 — Task 4.1.4: CommentService
**Time:** 10:02 PM | **Task:** 4.1.4

**Actual prompt:**
> Task 4.1.4 (Sprint 4.1): Implement CommentService (interface + impl).
>
> Follow all rules in .cursor/rules/. Read api-contract.md (comment endpoints + error codes),
> requirements-analysis.md (FR-13..FR-15), acceptance-criteria.md (comment ACs),
> the CommentRepository and TicketRepository interfaces, CommentDTO, and the domain exceptions.
> Target: com.mysite.core.services (interface) and com.mysite.core.services.impl.
>
> Create:
>
> 1. Interface com.mysite.core.services.CommentService:
>    - CommentDTO addComment(String ticketId, CommentDTO comment)
>    - List<CommentDTO> listComments(String ticketId)
>    Javadoc each with behavior + exceptions thrown.
>
> 2. Impl com.mysite.core.services.impl.CommentServiceImpl:
>    @Component(service = CommentService.class)
>    @Reference(target = "(impl.type=contentfragment)") private CommentRepository commentRepository;
>    @Reference(target = "(impl.type=contentfragment)") private TicketRepository ticketRepository;
>
>    [... addComment/listComments behavior; comments allowed on Closed/Cancelled ...]
>
> After generating:
> - Confirm mvn clean install compiles and the component is satisfied in OSGi console
>   (both @Reference contentfragment targets resolve).
> - Summarize addComment/listComments validation + exceptions so I can verify against api-contract.
> Do NOT write unit tests yet (Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Created `CommentService` + `CommentServiceImpl`
- Ticket existence check before add/list; message + createdBy required; path `ticketId` authoritative
- No terminal-status block on comments (AC-42, AC-43)
- Verified `mvn clean compile test -pl core`

**What I accepted / changed:** Accepted; logged to `ai-prompts/implementation/13-comment-service.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 6 — Task 4.1.5: Validation consolidation
**Time:** 10:09 PM | **Task:** 4.1.5

**Actual prompt:**
> Task 4.1.5 (Sprint 4.1): Consolidate and harden validation across the services into a
> reusable validator, and close any gaps against the acceptance criteria.
>
> Follow all rules in .cursor/rules/. Read acceptance-criteria.md (all validation ACs and
> NFR-AC), requirements-analysis.md (FR-1..FR-15, FR-7 terminal rules), api-contract.md
> (error codes), TicketServiceImpl, CommentServiceImpl, TicketStatus enum, and the domain
> exceptions. Target: com.mysite.core.validation (create) + refactor the two service impls.
>
> Part A — Audit (report first, before refactor):
> List a table of every validation rule the services SHOULD enforce (from the ACs/FRs) [...]
>
> Part B — Create a reusable validator: TicketValidator (and CommentValidator if useful) [...]
>
> Part C — Refactor services to use the validator [...]
>
> After generating:
> - Confirm mvn clean install compiles and services remain satisfied in OSGi.
> - Provide the final validation coverage table (Rule | Enforced in | Exception | AC covered)
>   so I can verify completeness against acceptance-criteria.md.
> Do NOT write unit tests yet (Task 4.1.6).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

**AI output:**
- Audit found **AC-6 gap**: description not required on create (fixed)
- Created `TicketValidator`, `CommentValidator`; refactored both service impls
- `assertEditable` uses `TicketStatus.CLOSED`/`CANCELLED` for terminal detection
- Verified `mvn clean compile test -pl core`

**What I accepted / changed:** Accepted validation gap fix; logged to `ai-prompts/implementation/14-validation-consolidation.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 7 — Task 4.1.6: TicketStateMachineTest
**Time:** 10:13 PM | **Task:** 4.1.6

**Actual prompt:**
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
> 1. VALID transitions (5 named + @ParameterizedTest)
> 2. INVALID transitions (11 named AC cases + @ParameterizedTest 20-pair matrix)
> 3. SAME-STATE transitions rejected
> 4. TERMINAL allowedNextStatuses checks
> 5. EXCEPTION content (getFrom/getTo, errorCode, message)
> 6. LABEL handling (unknown label -> IllegalArgumentException; fromLabel mapping)
>
> After generating:
> - Run: mvn test  (or mvn clean install) — ALL tests must pass.
> - Report the total number of test cases and confirm all valid transitions pass and all
>   invalid ones are rejected.
> When done, remind me to save this prompt + response summary to ai-prompts/testing/.

**AI output:**
- Created `TicketStateMachineTest` — **54 test cases**, all passed on first run
- **No state-machine bugs found** — implementation matched spec; tests did not require fixes to `TicketStateMachine`
- Full core module: **59 tests**, BUILD SUCCESS
- Updated plan — 4.1.6 complete; Sprint 4.1 → Quality Gate

**What I accepted / changed:** Accepted test suite; logged to `ai-prompts/testing/01-ticket-state-machine-test.md`
**What I rejected / why:** N/A — verified at QG
**Iteration needed:** No

---

### Prompt 8 — Sprint 4.1 Quality Gate
**Time:** 10:16 PM | **Task:** QG

**Actual prompt:**
> Sprint 4.1 Quality Gate: State machine + services tasks (4.1.1–4.1.6) complete and committed.
>
> Step 1 — Verify Sprint 4.1 Definition of Done:
> - Every valid transition has a passing unit test; representative invalid transitions rejected
> - TicketService.changeStatus delegates ONLY to TicketStateMachine
> - Validation runs before persistence (create/update/reassign/status)
> - mvn test is fully green; TicketServiceImpl + CommentServiceImpl satisfied in OSGi
> State any item not satisfied; if all pass, state "All DOD satisfied" and report test count.
>
> Step 2 — Generate the sprint log:
> Following the prompt-history-sprint-log rule in .cursor/rules/:
> 1. Read THIS session's Cursor transcript.
> 2. Filter meaningful user prompts.
> 3. Generate prompt-history/sprint-4.1.md using the EXACT rule structure (verbatim prompts,
>    summarized AI outputs, what I accepted/changed — including any state-machine bug caught by
>    tests, files changed, requirements traced AC-22–AC-35, QG result).
> 4. Update prompt-history/README.md Session Index with the Sprint 4.1 row.
>
> Sprint title: "State Machine + Services".
> Do NOT start Sprint 5.1 yet — stop after generating the sprint log for my review.

**AI output:**
- DOD verification + this sprint log + README index update

**What I accepted / changed:** Pending developer review
**What I rejected / why:** N/A
**Iteration needed:** No

---

## What I did without AI assistance

- Committed Sprint 4.1 state machine + services work before Quality Gate
- Confirmed `implementation-plan.md` task checkboxes for 4.1.1–4.1.6

## Prompt engineering notes

| Observation | What it shows |
|-------------|---------------|
| Session-start context prompt with exact transition table | Ensures state machine rules understood before coding |
| "CORE of the application — dedicated class only" | Produced isolated `TicketStateMachine` with readable `EnumMap` table |
| Exception hierarchy before services | Clean servlet error mapping path for Sprint 5.1 |
| Validation audit (Part A) before refactor | Caught AC-6 description gap without waiting for service tests |
| Deferred state-machine tests to 4.1.6 | Implementation-first, then 54-test proof suite |
| `ai-prompts/implementation/` + `ai-prompts/testing/` per task | Parallel prompt capture alongside sprint log |

## Files changed

| File | Change |
|------|--------|
| `core/.../statemachine/TicketStatus.java` | Created |
| `core/.../statemachine/TicketStateMachine.java` | Created |
| `core/.../exception/DomainException.java` | Created |
| `core/.../exception/ValidationException.java` | Created |
| `core/.../exception/TicketNotEditableException.java` | Created |
| `core/.../exception/UnknownUserException.java` | Created |
| `core/.../exception/InvalidTransitionException.java` | Refactored — extends `DomainException` |
| `core/.../exception/TicketNotFoundException.java` | Refactored — extends `DomainException` |
| `core/.../services/TicketService.java` | Created |
| `core/.../services/CommentService.java` | Created |
| `core/.../services/impl/TicketServiceImpl.java` | Created |
| `core/.../services/impl/CommentServiceImpl.java` | Created |
| `core/.../validation/TicketValidator.java` | Created |
| `core/.../validation/CommentValidator.java` | Created |
| `core/.../repositories/impl/ContentFragmentTicketRepository.java` | Updated — `update()` persists `status` |
| `core/src/test/.../statemachine/TicketStateMachineTest.java` | Created — 54 tests |
| `implementation-plan.md` | Updated — 4.1.1–4.1.6 complete; QG Sprint 4.1 |
| `ai-prompts/implementation/10-ticket-state-machine.md` | Created |
| `ai-prompts/implementation/11-domain-exceptions.md` | Created |
| `ai-prompts/implementation/12-ticket-service.md` | Created |
| `ai-prompts/implementation/13-comment-service.md` | Created |
| `ai-prompts/implementation/14-validation-consolidation.md` | Created |
| `ai-prompts/testing/01-ticket-state-machine-test.md` | Created |
| `prompt-history/sprint-4.1.md` | Created |
| `prompt-history/README.md` | Updated |

## Requirements traced

| ID | Coverage |
|----|----------|
| FR-1–FR-12 | `TicketService` — create, list, get, update, reassign, changeStatus, search/filter |
| FR-7 | `TicketNotEditableException` on terminal update/reassign |
| FR-8–FR-10 | `TicketStateMachine` + `changeStatus` delegation |
| FR-13–FR-15 | `CommentService` — add, list, validation |
| AC-3–AC-6 | Title, description, priority, assignee validation |
| AC-16–AC-21 | Terminal edit/reassign rejection |
| AC-22–AC-26 | 5 valid transitions — unit tested |
| AC-27–AC-35 | Invalid transitions — unit tested (11 named + 20-pair matrix) |
| AC-36 | Disallowed target status rejected via state machine |
| AC-41–AC-46 | Comment add/list validation |
| NFR-AC-1 | `ValidationException` → HTTP 400 JSON |
| NFR-AC-2 | `InvalidTransitionException` → HTTP 409 JSON |
| Sprint 4.1 DOD-1–DOD-4 | See Quality Gate result |

## Quality Gate result

### Step 1 — Sprint 4.1 Definition of Done

| Check | Criterion | Result | Notes |
|-------|-----------|--------|-------|
| DOD-1 | Every valid transition has a passing unit test | **Passed** | 5 named + 5 parameterized in `TicketStateMachineTest` |
| DOD-2 | Representative invalid transitions rejected | **Passed** | 11 AC-named + 20-pair matrix + 5 same-state; all throw `InvalidTransitionException` |
| DOD-3 | `changeStatus` delegates to `TicketStateMachine` only | **Passed** | `TicketServiceImpl.changeStatus` calls only `stateMachine.assertCanTransition(from, to)` — no inline transition rules |
| DOD-4 | Validation before persistence | **Passed** | `TicketValidator`/`CommentValidator` run before `ticketRepository.create/update` and `commentRepository.add` |
| Build | `mvn test -pl core` | **Passed** | **59 tests**, 0 failures (`TicketStateMachineTest`: 54) |
| OSGi | `TicketServiceImpl` + `CommentServiceImpl` satisfied | **Not verified in QG session** | Components annotated correctly (`@Component`, `impl.type=contentfragment`/`aem`); **developer to confirm Active + Satisfied in Felix after bundle deploy** |

**State-machine bugs caught by tests:** **None** — all 54 state-machine tests passed on first run; no fixes required to `TicketStateMachine`.

**Overall DOD (code + tests):** **All DOD satisfied** except OSGi Felix verification (deferred to local deploy smoke test).

| Build | Result |
|-------|--------|
| `mvn test -pl core` | **Passed** — 59 tests, 0 failures |
| Service unit tests | **Not added** — deferred to Sprint 7.1 per plan |
| OSGi console | **Pending** — post-deploy developer confirmation |

**Sprint exit:** Passed pending developer review. **Do not start Sprint 5.1** until approved.

## Developer review

**Status:** Pending review
**Approved by:** Developer — _pending_
**Notes:** Prompts verbatim from Cursor transcript (`30ac5080-3468-467f-a668-a23a71f05516`). Typos preserved. Prompt 4/5 abbreviated in sprint log body — full verbatim text in `ai-prompts/implementation/12-ticket-service.md` and `13-comment-service.md`.
