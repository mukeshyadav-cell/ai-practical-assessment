# Requirements Analysis — AEM Support Ticket Management System

**Date:** 2025-08-25
**Purpose:** Mandatory-core functional and non-functional requirements for Sprint 1.1 planning.

**Sprint/Task:** 1.1 / 1.1.1
**Namespaces:** Java `com.mysite.core`; content `/apps/assessment`, `/conf/assessment`, `/content/dam/assessment`

---

## 1. Problem Statement

Support teams need a lightweight ticket tracker inside AEM as a Cloud Service (AEMaaCS) to manage the create → triage → resolve → close workflow without leaving the AEM authoring environment. This project demonstrates responsible full-stack AEM development using Content Fragments for persistence, Sling Servlets for REST APIs, the Repository Pattern for future database swappability, and HTL plus TypeScript for the UI. Ticket status transitions are governed by a strict state machine enforced in the service layer—not optional UI behavior—so invalid lifecycle changes are rejected consistently at the API.

---

## 2. In-Scope vs Out-of-Scope

### In-Scope

- **Entities:** User (seeded), Ticket, Comment with the fields defined in Section 3
- **Ticket operations:** create, list, view detail, update fields, reassign, keyword search (title only), status filter
- **Comment operations:** add comment, list comments by ticket
- **State machine:** enforce allowed transitions only; reject all others with HTTP 409 Conflict
- **Persistence:** Content Fragments for Ticket and Comment under `/content/dam/assessment`
- **Users:** seeded via AEM UserManager (JCR); read-only listing for assignee selection
- **API:** Sling Servlets at `/bin/api/v1/*` for all data access (no GraphQL)
- **Architecture:** Repository Pattern with `impl.type=contentfragment` adapters; swappable to `impl.type=database` later without UI/servlet changes
- **UI:** HTL components plus TypeScript via `ui.frontend` Webpack pipeline into clientlibs (`assessment.ticketing`)
- **Relationships:** foreign keys as text IDs (`ticketId`, `assignedTo`, `createdBy`), not AEM content references

### Out-of-Scope

- User CRUD or custom authentication beyond AEM's built-in login
- Sprints, dashboards, project management, notifications, attachments
- GraphQL or external database implementation (design for swappability only; do not implement)
- Ticket delete (not part of mandatory core)
- Pagination (full list returned; see Assumptions)
- Multi-language UI beyond English for mandatory core

---

## 3. Entity Breakdown

Relationships (`ticketId`, `assignedTo`, `createdBy`) are stored as **text ID strings**, not Content References, to preserve a clean migration path to a relational database.

### User (seeded — not Content Fragment-backed)

| Field | Type | Required? | Validation Rule |
|-------|------|-----------|-----------------|
| id | String | Yes | Non-blank; must match AEM user identifier from UserManager / repoinit seed |
| name | String | Yes | Non-blank |
| email | String | Yes | Non-blank; valid email format |

### Ticket (Content Fragment)

| Field | Type | Required? | Validation Rule |
|-------|------|-----------|-----------------|
| id | String | Yes (system) | Generated on create; immutable |
| title | String | Yes | Non-blank; max 200 characters |
| description | String | Yes | Non-blank; max 5000 characters |
| priority | Enum | Yes | One of: `P1`, `P2`, `P3`, `P4` (P1 = highest urgency) |
| status | Enum | Yes | One of: `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled`; default `Open` on create |
| assignedTo | String | No | If present: must reference a seeded user `id`; may be null/blank (unassigned) |
| createdBy | String | Yes | Set from authenticated AEM session user; immutable after create |
| createdAt | Instant (ISO-8601) | Yes | Set on create; immutable |
| updatedAt | Instant (ISO-8601) | Yes | Set on every successful ticket mutation |

### Comment (Content Fragment)

| Field | Type | Required? | Validation Rule |
|-------|------|-----------|-----------------|
| id | String | Yes (system) | Generated on create; immutable |
| ticketId | String | Yes | Must reference an existing ticket `id` |
| message | String | Yes | Non-blank; max 2000 characters |
| createdBy | String | Yes | Set from authenticated AEM session user; immutable |
| createdAt | Instant (ISO-8601) | Yes | Set on create; immutable |

---

## 4. State Machine

**Statuses:** `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled`

**Initial status on ticket creation:** `Open` (only)

**Enforcement:** `com.mysite.core.statemachine.TicketStateMachine` (dedicated class; logic must not be scattered across servlets). Invalid transitions throw `com.mysite.core.exception.InvalidTransitionException`, mapped to HTTP **409 Conflict** with a clear JSON error message.

| From-State | Event / Action | To-State | Allowed? |
|------------|----------------|----------|------------|
| Open | Start work | In Progress | Yes |
| In Progress | Resolve | Resolved | Yes |
| Resolved | Close | Closed | Yes |
| Open | Cancel | Cancelled | Yes |
| In Progress | Cancel | Cancelled | Yes |
| Open | Resolve (skip In Progress) | Resolved | No |
| Open | Close (skip workflow) | Closed | No |
| In Progress | Close (skip Resolved) | Closed | No |
| Resolved | Reopen | Open | No |
| Resolved | Cancel | Cancelled | No |
| Closed | Any status change | Any | No |
| Cancelled | Any status change | Any | No |
| Any other pairing | — | — | No |

**Terminal states:** `Closed` and `Cancelled` — no further status transitions are permitted.

**Mutability by status (business rules beyond transitions):**

| Status | Update fields / reassign | Status change | Add comment |
|--------|--------------------------|---------------|-------------|
| Open | Allowed | Allowed (valid transitions only) | Allowed |
| In Progress | Allowed | Allowed (valid transitions only) | Allowed |
| Resolved | Allowed | Allowed (valid transitions only) | Allowed |
| Closed | Not allowed | Not allowed | Allowed |
| Cancelled | Not allowed | Not allowed | Allowed |

---

## 5. Functional Requirements

### Tickets — Create & Read

| ID | Requirement |
|----|-------------|
| FR-1 | **Create ticket** — Accept `title`, `description`, `priority` (P1–P4), and optional `assignedTo`. Set `status` to `Open`, `createdBy` and `createdAt` from the authenticated AEM session, and `updatedAt` on create. Reject invalid or missing required fields. |
| FR-2 | **List tickets** — Return all tickets, default sorted by `createdAt` descending (newest first). |
| FR-3 | **View ticket detail** — Return a single ticket by `id`. Return not-found when the ticket does not exist. |
| FR-4 | **Filter by status** — List tickets filtered by a single status value, or return all when no filter is applied. |

### Tickets — Update & Reassign

| ID | Requirement |
|----|-------------|
| FR-5 | **Update mutable fields** — Allow updating `title`, `description`, and `priority` when ticket status is `Open`, `In Progress`, or `Resolved`. |
| FR-6 | **Reassign ticket** — Allow changing `assignedTo` when status is `Open`, `In Progress`, or `Resolved`. Assignee must be a seeded user `id`. |
| FR-7 | **Reject edits on terminal tickets** — Reject field updates and reassign when status is `Closed` or `Cancelled`. |

### Tickets — Status

| ID | Requirement |
|----|-------------|
| FR-8 | **Change status** — Allow status changes only for transitions marked "Yes" in the state machine table (Section 4). |
| FR-9 | **Reject invalid transitions** — Return HTTP 409 Conflict with a clear JSON error message for disallowed transitions. |
| FR-10 | **Constrain offered transitions** — UI and API must not offer or accept status values that are not valid next states for the ticket's current status. |

### Search & Filter

| ID | Requirement |
|----|-------------|
| FR-11 | **Keyword search** — Match tickets by **title only** using case-insensitive partial match. |
| FR-12 | **Combined search and filter** — Keyword search may be combined with status filter on the ticket list. |

### Comments

| ID | Requirement |
|----|-------------|
| FR-13 | **Add comment** — Add a comment to an existing ticket, including tickets in `Closed` or `Cancelled` status. |
| FR-14 | **List comments** — Return comments for a ticket ordered by `createdAt` ascending. |
| FR-15 | **Reject invalid comment target** — Reject add-comment when `ticketId` does not reference an existing ticket. |

### Users

| ID | Requirement |
|----|-------------|
| FR-16 | **List users** — Return seeded users for assignee picker and display (read-only; no user CRUD). |
| FR-17 | **Resolve user display** — Resolve user `name` and `email` by `id` for UI presentation. |

### Architecture & API Surface

| ID | Requirement |
|----|-------------|
| FR-18 | **REST-only data access** — All browser and integration data access goes through Sling Servlets at `/bin/api/v1/*`. |
| FR-19 | **No direct JCR/GraphQL from UI** — Frontend uses relative `/bin/api/v1` paths only; no hardcoded hostnames or secrets. |

---

## 6. Non-Functional Requirements

### Validation

- Reject null/blank required fields, invalid enum values, unknown assignee IDs, and invalid state transitions **before** persistence.
- Validation lives in the service layer; servlets remain thin (parse, call service, serialize, set HTTP status).

### Persistence

- **Ticket** and **Comment** entities stored as Content Fragments under `/content/dam/assessment`.
- **User** data read from AEM UserManager / JCR (seeded users, not CF-backed).
- Repository layer handles CRUD and queries only; no business rules in repositories.

### Architecture & Swappability

- Strict layering: UI (HTL/TS) → Servlet → Service → Repository (interface) → CF Adapter → Content Fragments.
- DTOs (`TicketDTO`, `CommentDTO`, `UserDTO`) in `com.mysite.core.dto`; no `ContentFragment`, `Resource`, or JCR types above the repository layer.
- Repository implementations use OSGi property `impl.type=contentfragment`; future `impl.type=database` adapters must not require UI or servlet changes.

### Data Formats

- Timestamps: `java.time.Instant` internally; serialize as ISO-8601 in JSON.
- JSON serialization: Jackson (`com.fasterxml.jackson`) consistently; do not mix JSON libraries.

### Security

- No secrets in code, configuration committed to the repo, or documentation.
- Actor identity (`createdBy`) derived from the authenticated AEM session.
- Escape or encode user-provided content rendered in the DOM to prevent XSS.

### Error Handling

- Use meaningful domain exceptions; never swallow exceptions silently.
- HTTP status mapping (detailed request/response shapes deferred to `api-contract.md`):
  - **400** — validation failure (missing/invalid fields, unknown assignee)
  - **404** — ticket or resource not found
  - **409** — invalid state transition
  - **500** — unexpected server error

### Compatibility & Build

- Java 21 toolchain with source compatible with Java 17 (no Java 21-only preview features).
- AEM Project Archetype 57 (AEMaaCS); Maven multi-module build (`all`, `core`, `ui.apps`, `ui.config`, `ui.content`, `ui.frontend`, `dispatcher`, `it.tests`).
- TypeScript frontend built via `ui.frontend` Webpack pipeline; plain JavaScript permitted where simpler.

### Testing (requirements-level)

- Every **valid** state transition must have a passing test.
- Representative **invalid** transitions must have tests proving rejection.
- Happy-path tests for create, list, view, update, comment, and search (deferred to Sprint 7.1 implementation).

---

## 7. Assumptions

| # | Assumption |
|---|------------|
| A-1 | Priority enum values are `P1` (highest urgency) through `P4` (lowest). |
| A-2 | Keyword search matches **title only**, case-insensitive partial match. |
| A-3 | Tickets in `Closed` or `Cancelled` status are read-only for field updates, reassign, and status changes; **adding comments remains allowed**. |
| A-4 | `assignedTo` is optional on create; unassigned tickets are permitted. |
| A-5 | Field updates and reassign are allowed in `Open`, `In Progress`, and `Resolved` statuses. |
| A-6 | Ticket list default sort is `createdAt` descending; no pagination in mandatory core. |
| A-7 | `createdBy` on tickets and comments is the current AEM logged-in user's identifier. |
| A-8 | User `id` aligns with the AEM UserManager identifier used in repoinit seed scripts. |
| A-9 | Max field lengths: title 200, description 5000, comment message 2000 characters (may be refined in `data-model.md`). |
| A-10 | Local development targets a single AEM author instance; Dispatcher is not required to satisfy mandatory-core FRs. |
| A-11 | UI is English-only for mandatory core. |
| A-12 | Ticket delete is not required; lifecycle ends at `Closed` or `Cancelled`. |

---

## 8. Open Questions / Risks

| Item | Risk / Question | Mitigation / Next Step |
|------|-----------------|------------------------|
| User seed set | Which users and `id` values will repoinit create? | Define in Task 2.1.3 (service user + user seeding) |
| CF search performance | Title-only search may scan all ticket Content Fragments at scale | Acceptable for assessment scope; DB adapter would use indexed query |
| Assignee required on create? | Currently optional (A-4) | Confirm in `acceptance-criteria.md` (Task 1.1.2) |
| Resolved ticket edits | FR-5 allows edits in `Resolved` status | Confirm or restrict to `Open` / `In Progress` only in acceptance criteria |
| Servlet authentication | AEM default login is sufficient; servlet auth binding mode unclear | Decide in `api-contract.md` (Task 1.1.4) |
| Comment on Cancelled | Same rules as Closed (comments allowed; no other mutations) | Documented in Section 4 mutability table |
| ID generation strategy | UUID vs sequential IDs | Defer to `data-model.md` (Task 1.1.3) |
| Cross-doc consistency | Planning docs must align on enums, paths, and HTTP contracts | Sprint 1.1 Quality Gate review |

---

## Traceability

| Downstream Artifact | Maps From |
|---------------------|-----------|
| `acceptance-criteria.md` (1.1.2) | FR-1 … FR-19, state machine table |
| `data-model.md` (1.1.3) | Section 3 entities, A-9 field lengths, ID strategy |
| `api-contract.md` (1.1.4) | FR-18, FR-19, NFR error handling |
