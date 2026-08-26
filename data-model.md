# Data Model — AEM Support Ticket Management System

**Date:** 2025-08-26
**Purpose:** Content Fragment Models, DTOs, relationships, and persistence paths for repository-layer design (DB-swappable).

**Sprint/Task:** 1.1 / 1.1.3
**Sources:** [requirements-analysis.md](requirements-analysis.md), [acceptance-criteria.md](acceptance-criteria.md)
**Namespaces:** Java `com.mysite.core`; CFM `/conf/assessment/settings/dam/cfm/models`; data `/content/dam/assessment`

---

## 2. Design Principles

| Principle | Application |
|-----------|-------------|
| **Text-ID relationships** | `ticketId`, `assignedTo`, and `createdBy` are stored as plain **String** fields in Content Fragments — not AEM Content References. Same values become foreign-key columns when swapping to a relational DB. |
| **Source-agnostic DTOs** | `TicketDTO`, `CommentDTO`, and `UserDTO` in `com.mysite.core.dto` are plain POJOs. No `ContentFragment`, `Resource`, `ResourceResolver`, or JCR types appear on DTOs or above the repository layer. |
| **Users are not CFs** | User records are **not** Content Fragment Models. Users are read from AEM **UserManager** (JCR-backed authorizables) and exposed only via `UserDTO`. |
| **Repository swappability** | CF adapters use OSGi `impl.type=contentfragment`; future DB adapters use `impl.type=database`. Services and servlets depend on repository **interfaces** only. |
| **Timestamps** | `java.time.Instant` in Java; ISO-8601 strings in JSON and stored as text in CF elements for portability. |
| **Enum consistency** | Priority and status values match [requirements-analysis.md](requirements-analysis.md) and [acceptance-criteria.md](acceptance-criteria.md) — **not** alternate labels (e.g. priority is `P1`–`P4`, not Low/Medium/High/Critical). |

---

## 3. Content Fragment Model: Ticket

**CFM path:** `/conf/assessment/settings/dam/cfm/models/ticket`
**Model title:** Ticket
**Fragment folder:** `/content/dam/assessment/tickets`

| Field | CFM Field Type | Required | Validation | Notes |
|-------|----------------|----------|------------|-------|
| ticketId | Text (single line) | Yes | Non-blank; immutable after create | Business identifier; see ID strategy below. CF node name may equal `ticketId` (e.g. `TKT-1001`). |
| title | Text (single line) | Yes | Non-blank; max 200 characters | Keyword search field (title only). |
| description | Text (multi line) | Yes | Non-blank; max 5000 characters | Not searched by keyword filter. |
| priority | Enumeration | Yes | Must be one of enum values | **Enum values:** `P1`, `P2`, `P3`, `P4` (`P1` = highest urgency). |
| status | Enumeration | Yes | Must be one of enum values; default `Open` on create | **Enum values:** `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled`. |
| assignedTo | Text (single line) | No | If non-blank: must match a seeded user `userId` | Foreign key to user (text ID). Blank/null = unassigned. |
| createdBy | Text (single line) | Yes | Non-blank; immutable after create | Foreign key to user (session user at create). |
| createdAt | Text (single line) | Yes | ISO-8601 instant; immutable | Stored as text for DB portability; parsed to `Instant` in mapper. |
| updatedAt | Text (single line) | Yes | ISO-8601 instant | Updated on every successful ticket mutation. |

### ticketId generation strategy

| Rule | Detail |
|------|--------|
| Format | `TKT-{sequence}` — e.g. `TKT-1001`, `TKT-1002` |
| Sequence | Monotonic integer, zero-padded to 4 digits minimum (expand if needed) |
| Generation | Service/repository on create; not client-supplied |
| Storage | `ticketId` element value; CF asset path `/content/dam/assessment/tickets/{ticketId}` |
| Counter | Persistent counter node under app config (e.g. `/var/assessment/ticket-id-counter`) or equivalent — implementation detail in Sprint 3.1.2 |
| Immutability | `ticketId` never changes after create |

---

## 4. Content Fragment Model: Comment

**CFM path:** `/conf/assessment/settings/dam/cfm/models/comment`
**Model title:** Comment
**Fragment folder:** `/content/dam/assessment/comments`

| Field | CFM Field Type | Required | Validation | Notes |
|-------|----------------|----------|------------|-------|
| commentId | Text (single line) | Yes | Non-blank; immutable after create | Business identifier; see ID strategy below. |
| ticketId | Text (single line) | Yes | Non-blank; must reference existing ticket `ticketId` | Foreign key to Ticket (text ID, not content reference). |
| message | Text (multi line) | Yes | Non-blank; max 2000 characters | |
| createdBy | Text (single line) | Yes | Non-blank; immutable after create | Foreign key to user (session user at create). |
| createdAt | Text (single line) | Yes | ISO-8601 instant; immutable | Stored as text; parsed to `Instant` in mapper. |

### commentId generation strategy

| Rule | Detail |
|------|--------|
| Format | `CMT-{sequence}` — e.g. `CMT-1001`, `CMT-1002` |
| Sequence | Monotonic integer, independent of ticket sequence |
| Generation | Service/repository on create; not client-supplied |
| Storage | `commentId` element value; CF asset path `/content/dam/assessment/comments/{commentId}` |
| Counter | Persistent counter (e.g. `/var/assessment/comment-id-counter`) — Sprint 3.1.3 |
| Immutability | `commentId` never changes after create |

---

## 5. User (AEM UserManager — NOT a CFM)

Users are **not** modeled as Content Fragments. They are AEM authorizables resolved at read time via `UserManager`.

### Fields exposed via UserDTO

| DTO field | Source | Required | Notes |
|-----------|--------|----------|-------|
| userId | `Authorizable.getID()` from UserManager | Yes | Primary key for FK fields `assignedTo`, `createdBy`. Aligns with repoinit seed ids (e.g. `agent-1`). |
| displayName | `profile/givenName` + `profile/familyName` (JCR profile properties) | Yes | Concatenated with space; fallback to `userId` if names missing. |
| email | `profile/email` | Yes | Valid email format expected on seeded users. |

### Resolution approach

| Step | API / location |
|------|----------------|
| Obtain UserManager | `resourceResolver.adaptTo(UserManager.class)` |
| Load user | `userManager.getAuthorizable(userId)` |
| Read profile | JCR properties under authorizable path: `profile/givenName`, `profile/familyName`, `profile/email` (per AEM user profile conventions) |
| Filter | Skip `authorizable.isSystemUser()` when listing assignable users |
| Service access | System service user via `ResourceResolverFactory` + `assessment-service` subservice (Sprint 2.1.3) — not administrative resolver |

### Seeding approach

| Item | Plan |
|------|------|
| Creation | Users created in AEM JCR via **RepositoryInitializer** repoinit scripts (Task 2.1.3) or AEM security UI for local dev |
| Reference | Tickets and comments store `userId` strings only; no user CF assets |
| Assignee validation | `UserRepository` / service checks `userId` exists before persisting `assignedTo` or accepting create with assignee |
| CRUD scope | Read-only list + get-by-id for mandatory core; no user create/update/delete APIs |

**Planned seed users (illustrative — finalize in Task 2.1.3):**

| userId | displayName (target) | email (target) |
|--------|------------------------|----------------|
| agent-1 | Agent One | agent1@example.com |
| agent-2 | Agent Two | agent2@example.com |

---

## 6. DTO Definitions (Java — `com.mysite.core.dto`)

Plain POJOs; Jackson-serializable; no AEM types. JSON property names follow API contract (Task 1.1.4); `id` in JSON maps from CF `ticketId` / `commentId` where applicable.

### TicketDTO

| Field | Java type | Nullable | Maps from (CF / source) |
|-------|-----------|----------|-------------------------|
| id | `String` | No | CF element `ticketId` |
| title | `String` | No | CF `title` |
| description | `String` | No | CF `description` |
| priority | `String` | No | CF `priority` — `P1`, `P2`, `P3`, `P4` |
| status | `String` | No | CF `status` — lifecycle enum |
| assignedTo | `String` | Yes | CF `assignedTo` — userId or null |
| createdBy | `String` | No | CF `createdBy` — userId |
| createdAt | `Instant` | No | CF `createdAt` (parsed from ISO-8601 text) |
| updatedAt | `Instant` | No | CF `updatedAt` (parsed from ISO-8601 text) |

### CommentDTO

| Field | Java type | Nullable | Maps from (CF / source) |
|-------|-----------|----------|-------------------------|
| id | `String` | No | CF element `commentId` |
| ticketId | `String` | No | CF `ticketId` |
| message | `String` | No | CF `message` |
| createdBy | `String` | No | CF `createdBy` — userId |
| createdAt | `Instant` | No | CF `createdAt` (parsed from ISO-8601 text) |

### UserDTO

| Field | Java type | Nullable | Maps from (source) |
|-------|-----------|----------|---------------------|
| userId | `String` | No | `Authorizable.getID()` |
| displayName | `String` | No | Profile givenName + familyName |
| email | `String` | No | Profile `profile/email` |

**Package / module:** `core` module only (`core/src/main/java/com/mysite/core/dto/`). Implemented in Sprint 2.1.4.

---

## 7. Relationships Diagram

```
                    ┌─────────────────────────────────────┐
                    │  User (UserManager / JCR)           │
                    │  PK: userId                         │
                    │  displayName, email                 │
                    │  NOT a Content Fragment             │
                    └──────────────┬──────────────────────┘
                                   │
           assignedTo (FK text)    │    createdBy (FK text)
                   │               │              │
                   │               │              │
                   ▼               │              ▼
    ┌──────────────────────────────┴──────────────────────────────┐
    │  Ticket (Content Fragment)                                  │
    │  PK: ticketId          folder: /content/dam/assessment/   │
    │                        tickets/<ticketId>                   │
    │  title, description, priority, status, createdAt, updatedAt │
    └──────────────────────────────┬──────────────────────────────┘
                                   │
                                   │ 1
                                   │
                                   │ ticketId (FK text)
                                   │
                                   │ *
                                   ▼
    ┌─────────────────────────────────────────────────────────────┐
    │  Comment (Content Fragment)                                 │
    │  PK: commentId        folder: /content/dam/assessment/      │
    │                       comments/<commentId>                  │
    │  message, createdBy (FK → User.userId), createdAt           │
    └─────────────────────────────────────────────────────────────┘

Legend:
  PK  = primary identifier (text string)
  FK  = foreign key stored as plain text, NOT Content Reference
  1---*  Ticket has many Comments (via ticketId)
  *---1  Ticket references User for assignedTo and createdBy (optional assignee)
```

---

## 8. Persistence Layout (JCR paths)

### Content Fragment Models (definitions)

| Model | JCR path |
|-------|----------|
| Ticket CFM | `/conf/assessment/settings/dam/cfm/models/ticket` |
| Comment CFM | `/conf/assessment/settings/dam/cfm/models/comment` |

### Content Fragment instances (data)

| Entity | Parent folder | Instance path pattern | Example |
|--------|---------------|----------------------|---------|
| Ticket | `/content/dam/assessment/tickets` | `/content/dam/assessment/tickets/{ticketId}` | `/content/dam/assessment/tickets/TKT-1001` |
| Comment | `/content/dam/assessment/comments` | `/content/dam/assessment/comments/{commentId}` | `/content/dam/assessment/comments/CMT-1001` |

### DAM folder configuration

| Path | Purpose |
|------|---------|
| `/content/dam/assessment` | Root DAM folder (repoinit creates with `cq:conf` → `/conf/assessment`) |
| `/content/dam/assessment/tickets` | Ticket CF parent folder |
| `/content/dam/assessment/comments` | Comment CF parent folder |

### Users (JCR — not under DAM)

| Item | Typical path / access |
|------|----------------------|
| Authorizable users | `/home/users/{userId}` (or repoinit-defined paths) |
| Profile properties | `profile/givenName`, `profile/familyName`, `profile/email` on user node |
| Listing | `UserManager` query / iteration; filter system users |

### Application metadata (implementation)

| Path | Purpose |
|------|---------|
| `/var/assessment/ticket-id-counter` | Monotonic ticket sequence (planned Sprint 3.1.2) |
| `/var/assessment/comment-id-counter` | Monotonic comment sequence (planned Sprint 3.1.3) |

### AEM APIs (repository layer)

| Operation | Classes (reference) |
|-----------|---------------------|
| Read/write CF | `com.adobe.cq.dam.cfm.ContentFragment`, `ContentFragmentManager`, `FragmentTemplate` |
| Adapt resource | `resource.adaptTo(ContentFragment.class)` |
| Resolve service user | `ResourceResolverFactory.getServiceResourceResolver` with `assessment-service` subservice |

---

## 9. Future DB Mapping (swappability)

When `impl.type=database` is introduced, CF fields map to relational tables without changing DTOs or services.

### tickets table

| CF field / DTO field | DB column | SQL type | Constraints |
|---------------------|-----------|----------|-------------|
| ticketId → `id` | `id` | `VARCHAR(64)` | PRIMARY KEY |
| title | `title` | `VARCHAR(200)` | NOT NULL |
| description | `description` | `TEXT` | NOT NULL |
| priority | `priority` | `VARCHAR(8)` | NOT NULL, CHECK in (`P1`,`P2`,`P3`,`P4`) |
| status | `status` | `VARCHAR(32)` | NOT NULL |
| assignedTo | `assigned_to` | `VARCHAR(128)` | NULL, FK → `users.id` |
| createdBy | `created_by` | `VARCHAR(128)` | NOT NULL, FK → `users.id` |
| createdAt | `created_at` | `TIMESTAMP` | NOT NULL |
| updatedAt | `updated_at` | `TIMESTAMP` | NOT NULL |

### comments table

| CF field / DTO field | DB column | SQL type | Constraints |
|---------------------|-----------|----------|-------------|
| commentId → `id` | `id` | `VARCHAR(64)` | PRIMARY KEY |
| ticketId | `ticket_id` | `VARCHAR(64)` | NOT NULL, FK → `tickets.id` |
| message | `message` | `VARCHAR(2000)` | NOT NULL |
| createdBy | `created_by` | `VARCHAR(128)` | NOT NULL, FK → `users.id` |
| createdAt | `created_at` | `TIMESTAMP` | NOT NULL |

### users table (mirror of AEM users for DB adapter)

| UserDTO field | DB column | SQL type | Constraints |
|---------------|-----------|----------|-------------|
| userId | `id` | `VARCHAR(128)` | PRIMARY KEY |
| displayName | `display_name` | `VARCHAR(256)` | NOT NULL |
| email | `email` | `VARCHAR(256)` | NOT NULL |

**FK behavior:** Text-ID fields (`ticketId`, `assignedTo`, `createdBy`) in CF become explicit `FOREIGN KEY` constraints in SQL. No AEM path or UUID reference columns required.

**Index recommendations (DB adapter):** `tickets(status)`, `tickets(created_at DESC)`, `tickets(title)` for search; `comments(ticket_id)`, `comments(created_at)`.

---

## 10. Validation Summary

Consolidated from [requirements-analysis.md](requirements-analysis.md) and [acceptance-criteria.md](acceptance-criteria.md). Enforced in **service layer** before persistence.

### Ticket

| Field | Required | Rules |
|-------|----------|-------|
| ticketId | System | Generated `TKT-{n}`; immutable; non-blank |
| title | Yes | Non-blank; length ≤ 200 |
| description | Yes | Non-blank; length ≤ 5000 |
| priority | Yes | Enum: `P1`, `P2`, `P3`, `P4` only |
| status | Yes | Enum: `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled`; default `Open` on create; changes only via state machine |
| assignedTo | No | If present: non-blank and must match existing seeded `userId` |
| createdBy | Yes (system) | Non-blank; from authenticated session |
| createdAt | Yes (system) | ISO-8601; set on create; immutable |
| updatedAt | Yes (system) | ISO-8601; updated on each successful mutation |

**Business rules (not field-level):** No field update or reassign when status is `Closed` or `Cancelled` (HTTP 400). Status changes only via `TicketStateMachine` (invalid → HTTP 409).

### Comment

| Field | Required | Rules |
|-------|----------|-------|
| commentId | System | Generated `CMT-{n}`; immutable; non-blank |
| ticketId | Yes | Non-blank; ticket must exist |
| message | Yes | Non-blank; length ≤ 2000 |
| createdBy | Yes (system) | Non-blank; from authenticated session |
| createdAt | Yes (system) | ISO-8601; set on create; immutable |

**Business rules:** Comments allowed on all statuses including `Closed` and `Cancelled`.

### User (read path)

| Field | Required | Rules |
|-------|----------|-------|
| userId | Yes | Non-blank; must exist in UserManager for get-by-id |
| displayName | Yes | Non-blank when returned (fallback to userId) |
| email | Yes | Non-blank; valid email on seeded users |

---

## Traceability

| Downstream artifact | Maps from |
|---------------------|-----------|
| `api-contract.md` (1.1.4) | DTO fields, HTTP shapes, `id` vs CF element names |
| Sprint 2.1.2 | CFM XML under `/conf/assessment/settings/dam/cfm/models` |
| Sprint 2.1.4 | `TicketDTO`, `CommentDTO`, `UserDTO` Java classes |
| Sprint 3.1.x | CF adapters, ID counters, mappers |
| `it.tests` (7.1) | AC scenarios against persisted CF paths |

---

## Enum reference (authoritative)

| Field | Allowed values |
|-------|----------------|
| priority | `P1`, `P2`, `P3`, `P4` |
| status | `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled` |
