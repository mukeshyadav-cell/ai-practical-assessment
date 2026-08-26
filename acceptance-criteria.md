# Acceptance Criteria — AEM Support Ticket Management System

**Date:** 2025-08-26
**Purpose:** Testable acceptance criteria mapped to functional requirements (FR-x) for integration test traceability.

**Sprint/Task:** 1.1 / 1.1.2
**Source:** [requirements-analysis.md](requirements-analysis.md)

---

## 2. Traceability Rule

- Every acceptance criterion has a stable ID: **AC-1**, **AC-2**, … (sequential, no gaps).
- Each AC references the **FR** it verifies (e.g., "Covers FR-2").
- **Every FR must have at least one AC** — verified by the FR coverage matrix below.
- State-machine ACs (AC-22–AC-35) additionally cover **FR-8**, **FR-9**, and **FR-10**.
- Non-functional ACs use prefix **NFR-AC-1** … for HTTP, security, and persistence checks.
- Gherkin scenarios use **Given / When / Then** and assume an authenticated AEM user unless noted.

### FR Coverage Matrix

| FR | Description (summary) | AC IDs |
|----|-------------------------|--------|
| FR-1 | Create ticket | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6 |
| FR-2 | List tickets | AC-7, AC-8 |
| FR-3 | View ticket detail | AC-9, AC-10 |
| FR-4 | Filter by status | AC-11, AC-12 |
| FR-5 | Update mutable fields | AC-13, AC-14, AC-15 |
| FR-6 | Reassign ticket | AC-16, AC-17 |
| FR-7 | Reject edits on terminal tickets | AC-18, AC-19, AC-20, AC-21 |
| FR-8 | Change status (valid transitions) | AC-22, AC-23, AC-24, AC-25, AC-26 |
| FR-9 | Reject invalid transitions | AC-27, AC-28, AC-29, AC-30, AC-31, AC-32, AC-33, AC-34, AC-35 |
| FR-10 | Constrain offered/accepted statuses | AC-36 |
| FR-11 | Keyword search (title only) | AC-37, AC-38, AC-39 |
| FR-12 | Combined search + filter | AC-40 |
| FR-13 | Add comment | AC-41, AC-42, AC-43 |
| FR-14 | List comments | AC-44 |
| FR-15 | Reject invalid comment | AC-45, AC-46 |
| FR-16 | List users | AC-47 |
| FR-17 | Resolve user display | AC-48, AC-49 |
| FR-18 | REST-only data access | AC-50, NFR-AC-1 |
| FR-19 | No direct JCR/GraphQL from UI | AC-51 |

---

## 3. Acceptance Criteria (Given / When / Then)

### Create Ticket — Covers FR-1

**AC-1** — Create ticket with valid data (Covers FR-1)

```gherkin
Given an authenticated AEM user
  And a seeded user exists with id "agent-1"
When the client POSTs to /bin/api/v1/tickets with:
  | title       | Login page broken        |
  | description | Users cannot sign in     |
  | priority    | P2                       |
  | assignedTo  | agent-1                  |
Then the response status is 201 (or 200 per api-contract.md)
  And the response body includes id, title, description, priority P2,
      status "Open", assignedTo "agent-1", createdBy (session user),
      createdAt, and updatedAt as ISO-8601 strings
  And status is "Open" (not supplied or overridden by client)
```

**AC-2** — Create ticket without assignee (Covers FR-1, assumption A-4)

```gherkin
Given an authenticated AEM user
When the client POSTs to /bin/api/v1/tickets with title, description, and priority P3
  And assignedTo is omitted or blank
Then the response status is 201 (or 200 per api-contract.md)
  And assignedTo is null or empty in the response
  And status is "Open"
```

**AC-3** — Reject create with missing title (Covers FR-1)

```gherkin
Given an authenticated AEM user
When the client POSTs to /bin/api/v1/tickets without a title (blank or missing)
  And description and priority P1 are provided
Then the response status is 400
  And the response body is JSON with a clear validation error message
```

**AC-4** — Reject create with invalid priority (Covers FR-1)

```gherkin
Given an authenticated AEM user
When the client POSTs to /bin/api/v1/tickets with title, description,
  and priority "High" (not P1–P4)
Then the response status is 400
  And the response body is JSON with a clear validation error message
```

**AC-5** — Reject create with unknown assignee (Covers FR-1)

```gherkin
Given an authenticated AEM user
When the client POSTs to /bin/api/v1/tickets with valid title, description, priority P2,
  and assignedTo "non-existent-user"
Then the response status is 400
  And the response body is JSON indicating the assignee is invalid or unknown
```

**AC-6** — Reject create with missing description (Covers FR-1)

```gherkin
Given an authenticated AEM user
When the client POSTs to /bin/api/v1/tickets with title and priority P1
  And description is blank or missing
Then the response status is 400
  And the response body is JSON with a clear validation error message
```

### List Tickets — Covers FR-2

**AC-7** — List all tickets (Covers FR-2)

```gherkin
Given tickets A and B exist in the repository
When the client GETs /bin/api/v1/tickets with no filters
Then the response status is 200
  And the response body is a JSON array containing both tickets
```

**AC-8** — List sorted by createdAt descending (Covers FR-2, assumption A-6)

```gherkin
Given ticket "older" was created before ticket "newer"
When the client GETs /bin/api/v1/tickets
Then the response status is 200
  And "newer" appears before "older" in the JSON array
```

### View Ticket Detail — Covers FR-3

**AC-9** — View existing ticket (Covers FR-3)

```gherkin
Given a ticket exists with id "ticket-001"
When the client GETs /bin/api/v1/tickets/ticket-001
Then the response status is 200
  And the response body is JSON with all ticket fields for ticket-001
```

**AC-10** — View non-existent ticket returns 404 (Covers FR-3)

```gherkin
Given no ticket exists with id "missing-id"
When the client GETs /bin/api/v1/tickets/missing-id
Then the response status is 404
  And the response body is JSON with a clear not-found error message
```

### Status Filter — Covers FR-4

**AC-11** — Filter list by status (Covers FR-4)

```gherkin
Given an "Open" ticket and an "In Progress" ticket exist
When the client GETs /bin/api/v1/tickets?status=Open
Then the response status is 200
  And only tickets with status "Open" are returned
```

**AC-12** — No status filter returns all (Covers FR-4)

```gherkin
Given tickets exist in multiple statuses
When the client GETs /bin/api/v1/tickets without a status parameter
Then the response status is 200
  And tickets from all statuses are included
```

### Update Ticket Fields — Covers FR-5, FR-7

**AC-13** — Update fields on Open ticket (Covers FR-5)

```gherkin
Given a ticket exists with status "Open"
When the client PUTs (or PATCH per api-contract.md) updated title, description, and priority P1
Then the response status is 200
  And the response body reflects the updated fields
  And updatedAt is greater than or equal to the previous updatedAt
```

**AC-14** — Update fields on In Progress ticket (Covers FR-5)

```gherkin
Given a ticket exists with status "In Progress"
When the client updates title, description, or priority
Then the response status is 200
  And the updated values are persisted
```

**AC-15** — Update fields on Resolved ticket (Covers FR-5, resolves open question in requirements-analysis)

```gherkin
Given a ticket exists with status "Resolved"
When the client updates title, description, or priority
Then the response status is 200
  And the updated values are persisted
```

**AC-16** — Reject field update on Closed ticket (Covers FR-7)

```gherkin
Given a ticket exists with status "Closed"
When the client attempts to update title, description, or priority
Then the response status is 400
  And the response body is JSON with a clear error (ticket not editable)
  And no field values are changed in persistence
```

**AC-17** — Reject field update on Cancelled ticket (Covers FR-7)

```gherkin
Given a ticket exists with status "Cancelled"
When the client attempts to update title, description, or priority
Then the response status is 400
  And the response body is JSON with a clear error
```

### Reassign Ticket — Covers FR-6, FR-7

**AC-18** — Reassign to valid seeded user (Covers FR-6)

```gherkin
Given a ticket exists with status "Open"
  And seeded user "agent-2" exists
When the client reassigns assignedTo to "agent-2"
Then the response status is 200
  And assignedTo in the response is "agent-2"
```

**AC-19** — Reject reassign to unknown user (Covers FR-6)

```gherkin
Given a ticket exists with status "In Progress"
When the client reassigns assignedTo to "unknown-user-id"
Then the response status is 400
  And the response body is JSON indicating invalid assignee
```

**AC-20** — Reject reassign on Closed ticket (Covers FR-7)

```gherkin
Given a ticket exists with status "Closed"
When the client attempts to change assignedTo
Then the response status is 400
  And assignedTo is unchanged in persistence
```

**AC-21** — Reject reassign on Cancelled ticket (Covers FR-7)

```gherkin
Given a ticket exists with status "Cancelled"
When the client attempts to change assignedTo
Then the response status is 400
  And assignedTo is unchanged in persistence
```

### Status Change Constraint — Covers FR-10

**AC-36** — API rejects disallowed target status (Covers FR-10)

```gherkin
Given a ticket exists with status "Open"
When the client requests a status change to "Resolved" (skipping "In Progress")
Then the response status is 409
  And the ticket status remains "Open"
  And the response body is JSON with a clear transition error message
```

### Keyword Search — Covers FR-11

**AC-37** — Search matches title (Covers FR-11)

```gherkin
Given a ticket with title "Password reset failure" exists
  And a ticket with title "Dashboard layout" exists
When the client GETs /bin/api/v1/tickets?q=password
Then the response status is 200
  And only tickets whose title contains "password" (case-insensitive) are returned
```

**AC-38** — Search is case-insensitive (Covers FR-11, assumption A-2)

```gherkin
Given a ticket with title "Login Issue" exists
When the client GETs /bin/api/v1/tickets?q=login
Then the response status is 200
  And the ticket "Login Issue" is included in results
```

**AC-39** — Search does not match description (Covers FR-11)

```gherkin
Given a ticket whose title is "Generic title"
  And whose description contains the unique word "xyzzyunique"
When the client GETs /bin/api/v1/tickets?q=xyzzyunique
Then the response status is 200
  And that ticket is not in the results
```

### Combined Search and Filter — Covers FR-12

**AC-40** — Keyword search combined with status filter (Covers FR-12)

```gherkin
Given an "Open" ticket titled "Login bug" exists
  And an "Closed" ticket titled "Login regression" exists
When the client GETs /bin/api/v1/tickets?q=login&status=Open
Then the response status is 200
  And only "Open" tickets with matching titles are returned
  And the "Closed" ticket is excluded
```

### Add Comment — Covers FR-13, FR-15

**AC-41** — Add valid comment (Covers FR-13)

```gherkin
Given a ticket exists with id "ticket-001" and status "Open"
When the client POSTs to /bin/api/v1/tickets/ticket-001/comments with message "Investigating root cause"
Then the response status is 201 (or 200 per api-contract.md)
  And the response includes id, ticketId "ticket-001", message, createdBy, createdAt
```

**AC-42** — Add comment on Closed ticket (Covers FR-13, assumption A-3)

```gherkin
Given a ticket exists with status "Closed"
When the client POSTs a comment with a non-empty message
Then the response status is 201 (or 200 per api-contract.md)
  And the comment is persisted and linked to the ticket
```

**AC-43** — Add comment on Cancelled ticket (Covers FR-13, assumption A-3)

```gherkin
Given a ticket exists with status "Cancelled"
When the client POSTs a comment with a non-empty message
Then the response status is 201 (or 200 per api-contract.md)
  And the comment is persisted
```

**AC-45** — Reject empty comment message (Covers FR-15)

```gherkin
Given a ticket exists
When the client POSTs a comment with blank or missing message
Then the response status is 400
  And the response body is JSON with a validation error
```

**AC-46** — Reject comment on non-existent ticket (Covers FR-15)

```gherkin
Given no ticket exists with id "ghost-ticket"
When the client POSTs a comment to /bin/api/v1/tickets/ghost-ticket/comments
Then the response status is 404
  And the response body is JSON with a not-found error
```

### List Comments — Covers FR-14

**AC-44** — List comments ordered by createdAt ascending (Covers FR-14)

```gherkin
Given ticket "ticket-001" has comments C1 (older) and C2 (newer)
When the client GETs /bin/api/v1/tickets/ticket-001/comments
Then the response status is 200
  And C1 appears before C2 in the JSON array
```

### Users — Covers FR-16, FR-17

**AC-47** — List seeded users (Covers FR-16)

```gherkin
Given seeded users exist in AEM UserManager
When the client GETs /bin/api/v1/users
Then the response status is 200
  And the response is a JSON array of users with id, name, and email
  And no user create/update/delete endpoint is exposed
```

**AC-48** — Resolve user by id for display (Covers FR-17)

```gherkin
Given seeded user "agent-1" exists with name and email
When the client GETs /bin/api/v1/users/agent-1
Then the response status is 200
  And the body includes id "agent-1", name, and email
```

**AC-49** — Unknown user id returns 404 (Covers FR-17)

```gherkin
Given no user exists with id "nobody"
When the client GETs /bin/api/v1/users/nobody
Then the response status is 404
```

### API Surface — Covers FR-18, FR-19

**AC-50** — Ticket and comment APIs under /bin/api/v1 (Covers FR-18)

```gherkin
Given the deployed application
When integration tests call ticket, comment, and user list endpoints
Then all URLs begin with /bin/api/v1/
  And no GraphQL or direct JCR servlet paths are used for ticket data
```

**AC-51** — Frontend uses relative API paths (Covers FR-19)

```gherkin
Given the ui.frontend TypeScript sources and compiled clientlibs
When reviewed at Sprint 6.1 Quality Gate
Then fetch calls use relative paths under /bin/api/v1
  And no hardcoded hostnames, API keys, or secrets appear in client code
```

---

## 4. State Machine Acceptance Criteria — CRITICAL

Covers **FR-8**, **FR-9**, and **FR-10**. Enforcement class: `com.mysite.core.statemachine.TicketStateMachine`. Invalid transitions throw `InvalidTransitionException` → HTTP **409**.

### Valid Transitions (must succeed)

| AC-ID | From | Event / To | Expected Result | HTTP Status |
|-------|------|------------|-----------------|-------------|
| AC-22 | Open | Start work → In Progress | Status updated to In Progress; updatedAt refreshed | 200 |
| AC-23 | In Progress | Resolve → Resolved | Status updated to Resolved | 200 |
| AC-24 | Resolved | Close → Closed | Status updated to Closed | 200 |
| AC-25 | Open | Cancel → Cancelled | Status updated to Cancelled | 200 |
| AC-26 | In Progress | Cancel → Cancelled | Status updated to Cancelled | 200 |

**AC-22** (Gherkin) — Open → In Progress (Covers FR-8)

```gherkin
Given a ticket exists with status "Open"
When the client requests status change to "In Progress"
Then the response status is 200
  And the ticket status is "In Progress"
```

**AC-23** (Gherkin) — In Progress → Resolved (Covers FR-8)

```gherkin
Given a ticket exists with status "In Progress"
When the client requests status change to "Resolved"
Then the response status is 200
  And the ticket status is "Resolved"
```

**AC-24** (Gherkin) — Resolved → Closed (Covers FR-8)

```gherkin
Given a ticket exists with status "Resolved"
When the client requests status change to "Closed"
Then the response status is 200
  And the ticket status is "Closed"
```

**AC-25** (Gherkin) — Open → Cancelled (Covers FR-8)

```gherkin
Given a ticket exists with status "Open"
When the client requests status change to "Cancelled"
Then the response status is 200
  And the ticket status is "Cancelled"
```

**AC-26** (Gherkin) — In Progress → Cancelled (Covers FR-8)

```gherkin
Given a ticket exists with status "In Progress"
When the client requests status change to "Cancelled"
Then the response status is 200
  And the ticket status is "Cancelled"
```

### Invalid Transitions (must be rejected → HTTP 409)

| AC-ID | From | Event / To | Expected Result | HTTP Status |
|-------|------|------------|-----------------|-------------|
| AC-27 | Open | → Resolved (skip In Progress) | Status remains Open; clear JSON error | 409 |
| AC-28 | Open | → Closed (skip workflow) | Status remains Open; clear JSON error | 409 |
| AC-29 | In Progress | → Closed (skip Resolved) | Status remains In Progress; clear JSON error | 409 |
| AC-30 | Resolved | → Open (reopen) | Status remains Resolved; clear JSON error | 409 |
| AC-31 | Resolved | → In Progress (reopen) | Status remains Resolved; clear JSON error | 409 |
| AC-32 | Closed | → Open | Status remains Closed; clear JSON error | 409 |
| AC-33 | Closed | → In Progress | Status remains Closed; clear JSON error | 409 |
| AC-34 | Cancelled | → Open | Status remains Cancelled; clear JSON error | 409 |
| AC-35 | Cancelled | → Resolved | Status remains Cancelled; clear JSON error | 409 |

**AC-27** (Gherkin) — Reject Open → Resolved (Covers FR-9)

```gherkin
Given a ticket exists with status "Open"
When the client requests status change to "Resolved"
Then the response status is 409
  And the ticket status remains "Open"
  And the response body is JSON describing the invalid transition
```

**AC-28** (Gherkin) — Reject Open → Closed (Covers FR-9)

```gherkin
Given a ticket exists with status "Open"
When the client requests status change to "Closed"
Then the response status is 409
  And the ticket status remains "Open"
```

**AC-29** (Gherkin) — Reject In Progress → Closed (Covers FR-9)

```gherkin
Given a ticket exists with status "In Progress"
When the client requests status change to "Closed"
Then the response status is 409
  And the ticket status remains "In Progress"
```

**AC-30** (Gherkin) — Reject Resolved → Open (Covers FR-9)

```gherkin
Given a ticket exists with status "Resolved"
When the client requests status change to "Open"
Then the response status is 409
  And the ticket status remains "Resolved"
```

**AC-31** (Gherkin) — Reject Resolved → In Progress (Covers FR-9)

```gherkin
Given a ticket exists with status "Resolved"
When the client requests status change to "In Progress"
Then the response status is 409
  And the ticket status remains "Resolved"
```

**AC-32** (Gherkin) — Reject Closed → Open (Covers FR-9)

```gherkin
Given a ticket exists with status "Closed"
When the client requests status change to "Open"
Then the response status is 409
  And the ticket status remains "Closed"
```

**AC-33** (Gherkin) — Reject Closed → In Progress (Covers FR-9)

```gherkin
Given a ticket exists with status "Closed"
When the client requests status change to "In Progress"
Then the response status is 409
  And the ticket status remains "Closed"
```

**AC-34** (Gherkin) — Reject Cancelled → Open (Covers FR-9)

```gherkin
Given a ticket exists with status "Cancelled"
When the client requests status change to "Open"
Then the response status is 409
  And the ticket status remains "Cancelled"
```

**AC-35** (Gherkin) — Reject Cancelled → Resolved (Covers FR-9)

```gherkin
Given a ticket exists with status "Cancelled"
When the client requests status change to "Resolved"
Then the response status is 409
  And the ticket status remains "Cancelled"
```

---

## 5. Non-Functional Acceptance Criteria

| ID | Criterion | Covers |
|----|-----------|--------|
| NFR-AC-1 | **Validation errors → HTTP 400 with JSON body** — Given a request with missing/invalid fields (blank title, invalid priority, unknown assignee, empty comment), When the API processes it, Then status is 400 and body is JSON with a human-readable error message (not empty HTML). | FR-1, FR-6, FR-15, NFR validation |
| NFR-AC-2 | **Invalid transitions → HTTP 409 with clear message** — Given an invalid status transition (see AC-27–AC-35), When the client requests the change, Then status is 409 and JSON body identifies the invalid from/to statuses. | FR-9 |
| NFR-AC-3 | **No secrets in code/repo** — Given a scan of committed source, config, and planning docs at Sprint 1.1 Quality Gate, Then no API keys, passwords, tokens, or private credentials appear in the repository. | NFR security |
| NFR-AC-4 | **Data persists across AEM restarts** — Given a ticket and comment were created and stored as Content Fragments under `/content/dam/assessment`, When the AEM author instance restarts and repositories are re-read, Then the same ticket and comment data is returned via GET APIs. | NFR persistence |
| NFR-AC-5 | **Not-found → HTTP 404 with JSON body** — Given a request for a missing ticket or user id, When the API processes it, Then status is 404 and body is JSON with a clear not-found message. | FR-3, FR-15, FR-17 |
| NFR-AC-6 | **Timestamps ISO-8601 in JSON** — Given any successful ticket or comment response, Then createdAt and updatedAt fields are ISO-8601 strings parseable as instants. | NFR data formats |

**NFR-AC-1** (Gherkin)

```gherkin
Given an authenticated AEM user
When the client POSTs a ticket with invalid data (e.g., missing title)
Then the response status is 400
  And Content-Type indicates JSON
  And the body contains a non-empty error message field
```

**NFR-AC-4** (Gherkin)

```gherkin
Given a ticket "persist-001" and its comments exist in Content Fragments
When the AEM author instance is restarted
  And the client GETs /bin/api/v1/tickets/persist-001
Then the response status is 200
  And ticket data matches pre-restart values
```

---

## 6. Definition of Done — Sprint 1.1 (Planning)

Sprint 1.1 planning is **complete** when:

| # | Criterion | Status |
|---|-----------|--------|
| DOD-1 | `requirements-analysis.md` exists at repo root with FR-1–FR-19 | Done (1.1.1) |
| DOD-2 | `acceptance-criteria.md` exists at repo root with AC-1–AC-51, NFR-AC-1–6 | This task (1.1.2) |
| DOD-3 | `data-model.md` exists (1.1.3) | Pending |
| DOD-4 | `api-contract.md` exists (1.1.4) | Pending |
| DOD-5 | `implementation-plan.md` finalized (1.1.5) | Pending |
| DOD-6 | Every FR has ≥ 1 AC (see FR Coverage Matrix) | Verified in this document |
| DOD-7 | State machine: all 5 valid and representative invalid transitions have ACs | AC-22–AC-35 |
| DOD-8 | Planning docs are mutually consistent (enums, paths, HTTP codes, mutability rules) | Quality Gate review |
| DOD-9 | No implementation code, CFMs, or integration tests written in Sprint 1.1 | Ongoing |
| DOD-10 | Prompt history saved under `ai-prompts/planning/` for 1.1.1 and 1.1.2 | Partial (1.1.1 done) |

**Quality Gate (Sprint 1.1):** All planning docs complete, reviewed, mutually consistent → generate `prompt-history/sprint-1.1.md`.

---

## Resolved from requirements-analysis open questions

| Open question | Resolution in this document |
|---------------|----------------------------|
| Assignee required on create? | **Optional** — AC-2 |
| Resolved ticket edits? | **Allowed** — AC-15 |
| Comment on Cancelled? | **Allowed** — AC-43 (same as Closed via AC-42) |

---

## Downstream traceability

| Artifact | Maps from |
|----------|-----------|
| `data-model.md` (1.1.3) | Entity fields referenced in AC scenarios |
| `api-contract.md` (1.1.4) | HTTP methods, paths, status codes per AC |
| `it.tests` (Sprint 7.1) | AC-1–AC-51, NFR-AC-1–6 implemented as `*IT.java` |
