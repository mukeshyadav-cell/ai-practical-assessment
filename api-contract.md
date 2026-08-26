# API Contract — AEM Support Ticket Management System

**Date:** 2025-08-26
**Purpose:** REST API contract for ticket, comment, and user endpoints — source-agnostic (CF or DB backend).

**Sprint/Task:** 1.1 / 1.1.4
**Sources:** [requirements-analysis.md](requirements-analysis.md), [acceptance-criteria.md](acceptance-criteria.md), [data-model.md](data-model.md)
**REST base:** `/bin/api/v1` | **Servlets (later):** `com.mysite.core.servlets` | **DTOs:** `com.mysite.core.dto`

---

## 2. Conventions

| Topic | Rule |
|-------|------|
| Base path | `/bin/api/v1` — all endpoints are relative (no hardcoded hostnames) |
| Format | JSON request and response bodies; `Content-Type: application/json` |
| Timestamps | ISO-8601 strings (e.g. `2025-08-26T10:30:00Z`) |
| IDs | Strings (e.g. `TKT-1001`, `CMT-1001`, `agent-1`) |
| Authentication | AEM built-in login; `createdBy` derived from authenticated session user |
| HTTP status codes | `200` OK, `201` Created, `204` No Content, `400` Bad Request, `404` Not Found, `409` Conflict, `500` Internal Server Error |
| Error body | `{ "error": "human-readable message", "code": "ERROR_CODE" }` |
| Servlet registration | OSGi property `sling.servlet.paths` (not `resourceTypes`) per [.cursor/rules/04-aem-correctness.mdc](.cursor/rules/04-aem-correctness.mdc) |
| Servlet base type | **GET** handlers extend `SlingSafeMethodsServlet`; **POST/PUT** handlers extend `SlingAllMethodsServlet` |
| Collection servlet | Servlets that expose GET and POST on the same path use `SlingAllMethodsServlet` |

### Design choice: explicit sub-resources for status and assignee

**Chosen approach:** Separate `PUT /tickets/{id}/status` and `PUT /tickets/{id}/assignee` endpoints instead of allowing `status` or `assignedTo` on `PUT /tickets/{id}`.

| Reason | Detail |
|--------|--------|
| State machine clarity | Status changes flow through one endpoint → `TicketService.changeStatus()` → `TicketStateMachine` |
| Predictable errors | Invalid transitions always return **409** from the status endpoint; field validation returns **400** from update/assignee |
| Separation of concerns | `PUT /tickets/{id}` updates content fields only (`title`, `description`, `priority`) |
| Backend swappability | Same service methods whether persistence is CF or DB; servlet paths unchanged |
| UI alignment | Status dropdown calls status endpoint only; assignee picker calls assignee endpoint |

`PUT /tickets/{id}` **must not** accept `status` or `assignedTo` in the body (reject with **400** if present).

---

## 3. Endpoints

### TICKETS

#### GET `/bin/api/v1/tickets` — List tickets

| Item | Detail |
|------|--------|
| Description | List all tickets; optional status filter and title keyword search |
| Servlet | `com.mysite.core.servlets.TicketCollectionServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets` |
| Servlet base | `SlingAllMethodsServlet` (also handles POST) |

**Query parameters**

| Param | Required | Description |
|-------|----------|-------------|
| `status` | No | Filter by single status: `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled` |
| `q` | No | Case-insensitive partial match on **title only** |

**Request body** — none

**Response `200`**

```json
[
  {
    "id": "TKT-1001",
    "title": "Login page broken",
    "description": "Users cannot sign in",
    "priority": "P2",
    "status": "Open",
    "assignedTo": "agent-1",
    "createdBy": "admin",
    "createdAt": "2025-08-26T09:00:00Z",
    "updatedAt": "2025-08-26T09:00:00Z"
  }
]
```

**Status codes**

| Code | When |
|------|------|
| 200 | Success; empty array if no tickets |
| 400 | Invalid `status` enum value |
| 500 | Unexpected server error |

**Covers:** FR-2, FR-4, FR-11, FR-12, FR-18

---

#### GET `/bin/api/v1/tickets/{id}` — Ticket detail

| Item | Detail |
|------|--------|
| Description | Return a single ticket by id |
| Servlet | `com.mysite.core.servlets.TicketByIdServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix after path = `{id}`) |
| Servlet base | `SlingSafeMethodsServlet` |

**Path parameters**

| Param | Description |
|-------|-------------|
| `id` | Ticket id (e.g. `TKT-1001`) |

**Response `200`**

```json
{
  "id": "TKT-1001",
  "title": "Login page broken",
  "description": "Users cannot sign in",
  "priority": "P2",
  "status": "In Progress",
  "assignedTo": "agent-1",
  "createdBy": "admin",
  "createdAt": "2025-08-26T09:00:00Z",
  "updatedAt": "2025-08-26T10:15:00Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 200 | Ticket found |
| 404 | Ticket id does not exist |
| 500 | Unexpected server error |

**Covers:** FR-3, FR-18

---

#### POST `/bin/api/v1/tickets` — Create ticket

| Item | Detail |
|------|--------|
| Description | Create ticket; `status` defaults to `Open`; ids and timestamps set by server |
| Servlet | `com.mysite.core.servlets.TicketCollectionServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets` |
| Servlet base | `SlingAllMethodsServlet` |

**Request body**

```json
{
  "title": "Login page broken",
  "description": "Users cannot sign in",
  "priority": "P2",
  "assignedTo": "agent-1"
}
```

`assignedTo` is optional; omit or null for unassigned.

**Response `201`**

```json
{
  "id": "TKT-1002",
  "title": "Login page broken",
  "description": "Users cannot sign in",
  "priority": "P2",
  "status": "Open",
  "assignedTo": "agent-1",
  "createdBy": "admin",
  "createdAt": "2025-08-26T11:00:00Z",
  "updatedAt": "2025-08-26T11:00:00Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 201 | Ticket created |
| 400 | Missing/blank title or description; invalid priority; unknown assignee (`UNKNOWN_USER` or `VALIDATION_ERROR`) |
| 500 | Unexpected server error |

**Covers:** FR-1, FR-18

---

#### PUT `/bin/api/v1/tickets/{id}` — Update ticket fields

| Item | Detail |
|------|--------|
| Description | Update `title`, `description`, and `priority` only (not status or assignee) |
| Servlet | `com.mysite.core.servlets.TicketByIdServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix = `{id}`; reject suffix containing `/`) |
| Servlet base | `SlingAllMethodsServlet` |

**Request body**

```json
{
  "title": "Login page broken — updated",
  "description": "OAuth redirect fails after patch",
  "priority": "P1"
}
```

**Response `200`**

```json
{
  "id": "TKT-1001",
  "title": "Login page broken — updated",
  "description": "OAuth redirect fails after patch",
  "priority": "P1",
  "status": "Open",
  "assignedTo": "agent-1",
  "createdBy": "admin",
  "createdAt": "2025-08-26T09:00:00Z",
  "updatedAt": "2025-08-26T11:30:00Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 200 | Fields updated (status `Open`, `In Progress`, or `Resolved`) |
| 400 | Validation error; unknown fields `status`/`assignedTo` in body; ticket `Closed`/`Cancelled` (`TICKET_NOT_EDITABLE`) |
| 404 | Ticket not found |
| 500 | Unexpected server error |

**Covers:** FR-5, FR-7, FR-18

---

#### PUT `/bin/api/v1/tickets/{id}/assignee` — Reassign ticket

| Item | Detail |
|------|--------|
| Description | Change assignee (`assignedTo` userId) |
| Servlet | `com.mysite.core.servlets.TicketAssigneeServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix = `{id}/assignee`) |
| Servlet base | `SlingAllMethodsServlet` |

**Request body**

```json
{
  "assignedTo": "agent-2"
}
```

Use `null` or empty string to unassign (optional unassigned tickets per data-model).

**Response `200`**

```json
{
  "id": "TKT-1001",
  "title": "Login page broken",
  "description": "Users cannot sign in",
  "priority": "P2",
  "status": "Open",
  "assignedTo": "agent-2",
  "createdBy": "admin",
  "createdAt": "2025-08-26T09:00:00Z",
  "updatedAt": "2025-08-26T11:45:00Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 200 | Assignee updated |
| 400 | Unknown user (`UNKNOWN_USER`); ticket `Closed`/`Cancelled` (`TICKET_NOT_EDITABLE`) |
| 404 | Ticket not found |
| 500 | Unexpected server error |

**Covers:** FR-6, FR-7, FR-18

---

#### PUT `/bin/api/v1/tickets/{id}/status` — Change ticket status

| Item | Detail |
|------|--------|
| Description | Apply state-machine transition to new status |
| Servlet | `com.mysite.core.servlets.TicketStatusServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix = `{id}/status`) |
| Servlet base | `SlingAllMethodsServlet` |

See **Section 4** for full detail.

**Covers:** FR-8, FR-9, FR-10, FR-18

---

### COMMENTS

#### GET `/bin/api/v1/tickets/{id}/comments` — List comments

| Item | Detail |
|------|--------|
| Description | List comments for a ticket, ordered by `createdAt` ascending |
| Servlet | `com.mysite.core.servlets.CommentCollectionServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix = `{id}/comments`) |
| Servlet base | `SlingSafeMethodsServlet` |

**Response `200`**

```json
[
  {
    "id": "CMT-1001",
    "ticketId": "TKT-1001",
    "message": "Investigating root cause",
    "createdBy": "agent-1",
    "createdAt": "2025-08-26T10:00:00Z"
  },
  {
    "id": "CMT-1002",
    "ticketId": "TKT-1001",
    "message": "Reproduced in staging",
    "createdBy": "agent-2",
    "createdAt": "2025-08-26T10:30:00Z"
  }
]
```

**Status codes**

| Code | When |
|------|------|
| 200 | Success; empty array if no comments |
| 404 | Ticket not found |
| 500 | Unexpected server error |

**Covers:** FR-14, FR-18

---

#### POST `/bin/api/v1/tickets/{id}/comments` — Add comment

| Item | Detail |
|------|--------|
| Description | Add comment to ticket (allowed on all statuses including `Closed`/`Cancelled`) |
| Servlet | `com.mysite.core.servlets.CommentCollectionServlet` |
| `sling.servlet.paths` | `/bin/api/v1/tickets/` (suffix = `{id}/comments`) |
| Servlet base | `SlingAllMethodsServlet` |

**Request body**

```json
{
  "message": "Customer confirmed fix in production"
}
```

**Response `201`**

```json
{
  "id": "CMT-1003",
  "ticketId": "TKT-1001",
  "message": "Customer confirmed fix in production",
  "createdBy": "admin",
  "createdAt": "2025-08-26T12:00:00Z"
}
```

**Status codes**

| Code | When |
|------|------|
| 201 | Comment created |
| 400 | Empty or missing message (`VALIDATION_ERROR`) |
| 404 | Ticket not found |
| 500 | Unexpected server error |

**Covers:** FR-13, FR-15, FR-18

---

### USERS

#### GET `/bin/api/v1/users` — List seeded users

| Item | Detail |
|------|--------|
| Description | List assignable seeded users (excludes system users) |
| Servlet | `com.mysite.core.servlets.UserCollectionServlet` |
| `sling.servlet.paths` | `/bin/api/v1/users` |
| Servlet base | `SlingSafeMethodsServlet` |

**Query parameters**

| Param | Required | Description |
|-------|----------|-------------|
| `q` | No | Optional case-insensitive partial match on `displayName` or `email` |

**Response `200`**

```json
[
  {
    "userId": "agent-1",
    "displayName": "Agent One",
    "email": "agent1@example.com"
  },
  {
    "userId": "agent-2",
    "displayName": "Agent Two",
    "email": "agent2@example.com"
  }
]
```

**Status codes**

| Code | When |
|------|------|
| 200 | Success |
| 500 | Unexpected server error |

**Covers:** FR-16, FR-18

---

#### GET `/bin/api/v1/users/{userId}` — User detail

| Item | Detail |
|------|--------|
| Description | Resolve user display fields by id (for UI labels) |
| Servlet | `com.mysite.core.servlets.UserByIdServlet` |
| `sling.servlet.paths` | `/bin/api/v1/users/` (suffix = `{userId}`) |
| Servlet base | `SlingSafeMethodsServlet` |

**Response `200`**

```json
{
  "userId": "agent-1",
  "displayName": "Agent One",
  "email": "agent1@example.com"
}
```

**Status codes**

| Code | When |
|------|------|
| 200 | User found |
| 404 | User id does not exist |
| 500 | Unexpected server error |

**Covers:** FR-17, FR-18

*Added for FR-17 / AC-48, AC-49 traceability; complements list endpoint.*

---

## 4. State Transition Endpoint Detail

**Endpoint:** `PUT /bin/api/v1/tickets/{id}/status`
**Servlet:** `com.mysite.core.servlets.TicketStatusServlet`

### Request

```json
{
  "status": "In Progress"
}
```

Only the **target** status is sent. The server validates the transition from the ticket's **current** status.

### Success `200`

Returns full updated `TicketDTO`:

```json
{
  "id": "TKT-1001",
  "title": "Login page broken",
  "description": "Users cannot sign in",
  "priority": "P2",
  "status": "In Progress",
  "assignedTo": "agent-1",
  "createdBy": "admin",
  "createdAt": "2025-08-26T09:00:00Z",
  "updatedAt": "2025-08-26T12:15:00Z"
}
```

### Invalid transition `409`

```json
{
  "error": "Invalid transition Open -> Closed",
  "code": "INVALID_TRANSITION"
}
```

### Other status codes

| Code | When |
|------|------|
| 400 | Missing/blank `status`; invalid status enum value; ticket `Closed`/`Cancelled` with any change attempt |
| 404 | Ticket not found |
| 500 | Unexpected server error |

### Allowed transitions (reference)

| From | To | Allowed |
|------|-----|---------|
| Open | In Progress | Yes |
| In Progress | Resolved | Yes |
| Resolved | Closed | Yes |
| Open | Cancelled | Yes |
| In Progress | Cancelled | Yes |
| Any other pairing | — | No → **409** |

### Implementation flow

```
Servlet → TicketService.changeStatus(ticketId, newStatus)
       → TicketStateMachine.validate(current, new)
       → InvalidTransitionException → 409 INVALID_TRANSITION
       → TicketRepository.update (on success)
```

Enforcement lives in `com.mysite.core.statemachine.TicketStateMachine` only (not in servlet).

---

## 5. Error Catalog

| code | HTTP status | Meaning | Example message |
|------|-------------|---------|-----------------|
| `VALIDATION_ERROR` | 400 | Missing, blank, or invalid field value | `Title is required` |
| `UNKNOWN_USER` | 400 | `assignedTo` or assignee body references non-existent user | `Unknown user: nobody` |
| `TICKET_NOT_EDITABLE` | 400 | Update/reassign on `Closed` or `Cancelled` ticket | `Ticket TKT-1001 is not editable in status Closed` |
| `NOT_FOUND` | 404 | Ticket, comment context ticket, or user not found | `Ticket not found: TKT-9999` |
| `INVALID_TRANSITION` | 409 | State machine rejected status change | `Invalid transition Open -> Closed` |
| `INTERNAL_ERROR` | 500 | Unhandled server failure | `An unexpected error occurred` |

All error responses use:

```json
{
  "error": "Human-readable message",
  "code": "ERROR_CODE"
}
```

---

## 6. Example Payloads

### Ticket (TicketDTO)

```json
{
  "id": "TKT-1001",
  "title": "Login page broken",
  "description": "Users cannot sign in after SSO change",
  "priority": "P2",
  "status": "Open",
  "assignedTo": "agent-1",
  "createdBy": "admin",
  "createdAt": "2025-08-26T09:00:00.000Z",
  "updatedAt": "2025-08-26T09:00:00.000Z"
}
```

| Field | Type | Notes |
|-------|------|-------|
| id | string | Maps from CF `ticketId`; format `TKT-{n}` |
| title | string | Max 200 chars |
| description | string | Max 5000 chars |
| priority | string | `P1`, `P2`, `P3`, `P4` |
| status | string | `Open`, `In Progress`, `Resolved`, `Closed`, `Cancelled` |
| assignedTo | string \| null | userId or null |
| createdBy | string | userId |
| createdAt | string | ISO-8601 |
| updatedAt | string | ISO-8601 |

### Comment (CommentDTO)

```json
{
  "id": "CMT-1001",
  "ticketId": "TKT-1001",
  "message": "Investigating root cause",
  "createdBy": "agent-1",
  "createdAt": "2025-08-26T10:00:00.000Z"
}
```

| Field | Type | Notes |
|-------|------|-------|
| id | string | Maps from CF `commentId`; format `CMT-{n}` |
| ticketId | string | FK to ticket |
| message | string | Max 2000 chars |
| createdBy | string | userId |
| createdAt | string | ISO-8601 |

### User (UserDTO)

```json
{
  "userId": "agent-1",
  "displayName": "Agent One",
  "email": "agent1@example.com"
}
```

| Field | Type | Notes |
|-------|------|-------|
| userId | string | Authorizable id |
| displayName | string | From profile names |
| email | string | From profile/email |

---

## 7. Traceability

### Endpoint → FR / AC

| Method | Path | FR | AC (representative) |
|--------|------|-----|---------------------|
| GET | `/bin/api/v1/tickets` | FR-2, FR-4, FR-11, FR-12, FR-18 | AC-7, AC-8, AC-11, AC-12, AC-37–AC-40 |
| GET | `/bin/api/v1/tickets/{id}` | FR-3, FR-18 | AC-9, AC-10 |
| POST | `/bin/api/v1/tickets` | FR-1, FR-18 | AC-1–AC-6 |
| PUT | `/bin/api/v1/tickets/{id}` | FR-5, FR-7, FR-18 | AC-13–AC-17 |
| PUT | `/bin/api/v1/tickets/{id}/assignee` | FR-6, FR-7, FR-18 | AC-18–AC-21 |
| PUT | `/bin/api/v1/tickets/{id}/status` | FR-8, FR-9, FR-10, FR-18 | AC-22–AC-36, AC-36 |
| GET | `/bin/api/v1/tickets/{id}/comments` | FR-14, FR-18 | AC-44 |
| POST | `/bin/api/v1/tickets/{id}/comments` | FR-13, FR-15, FR-18 | AC-41–AC-43, AC-45, AC-46 |
| GET | `/bin/api/v1/users` | FR-16, FR-18 | AC-47 |
| GET | `/bin/api/v1/users/{userId}` | FR-17, FR-18 | AC-48, AC-49 |
| — | Relative `/bin/api/v1` only (UI) | FR-19 | AC-51 |

### Error codes → AC / NFR

| code | AC / NFR |
|------|----------|
| `VALIDATION_ERROR` | NFR-AC-1, AC-3–AC-6, AC-45 |
| `UNKNOWN_USER` | AC-5, AC-19, NFR-AC-1 |
| `TICKET_NOT_EDITABLE` | AC-16, AC-17, AC-20, AC-21 |
| `NOT_FOUND` | AC-10, AC-46, AC-49, NFR-AC-5 |
| `INVALID_TRANSITION` | AC-27–AC-35, AC-36, NFR-AC-2 |
| `INTERNAL_ERROR` | NFR-AC-6 |

### Servlet implementation map (Sprint 5.1)

| Servlet class | `sling.servlet.paths` | Methods |
|---------------|----------------------|---------|
| `TicketCollectionServlet` | `/bin/api/v1/tickets` | GET, POST |
| `TicketByIdServlet` | `/bin/api/v1/tickets/` | GET, PUT |
| `TicketAssigneeServlet` | `/bin/api/v1/tickets/` | PUT (`{id}/assignee` suffix) |
| `TicketStatusServlet` | `/bin/api/v1/tickets/` | PUT (`{id}/status` suffix) |
| `CommentCollectionServlet` | `/bin/api/v1/tickets/` | GET, POST (`{id}/comments` suffix) |
| `UserCollectionServlet` | `/bin/api/v1/users` | GET |
| `UserByIdServlet` | `/bin/api/v1/users/` | GET |

Suffix routing (e.g. `TKT-1001/status` vs `TKT-1001`) is implemented in servlet `doGet`/`doPost`/`doPut` by parsing the path suffix after `/bin/api/v1/tickets/`.

---

## Document consistency

| Topic | Alignment |
|-------|-----------|
| Priority enum | `P1`–`P4` per [data-model.md](data-model.md) |
| Create response | **201** (standardized; supersedes AC "201 or 200" note) |
| Comment create | **201** |
| `204` | Reserved; no delete endpoints in mandatory core |
