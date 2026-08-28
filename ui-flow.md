# UI Flow — Screens & User Journeys

**Date:** 2026-08-27  
**Purpose:** Describe the ticketing UI views, navigation, user journeys, and Sprint 6.2 enhancements for the AEM Support Ticket Management System.

**Sprint/Task:** 8.1 / 8.1.1

---

## Application Shell

| Item | Value |
|------|-------|
| Page URL | `/content/assessment/us/en/tickets.html` |
| Template | Static AEM page with `ticketapp` component |
| Clientlib | `assessment.ticketing` (TypeScript from `ui.frontend`, built via Webpack) |
| Navigation model | **SPA-style** — single page; `?id={ticketId}` switches list ↔ detail |
| Entry point | `main.ts` → `loadCurrentUser()` → `switchView()` → `initTicketList()` or `initTicketDetail()` |

**HTL structure** (`ticketapp`):

```
ticketapp
├── ticketlist   (#ticket-list-view)
├── ticketdetail (#ticket-detail-view)
└── ticketform   (modal overlay — create/edit)
```

The clientlib is included from `ticketapp.html` (component-level), not only page policy.

---

## Screens & Controls

### List view (`#ticket-list-view`)

Rendered by `ticketlist` HTL skeleton + `list.ts`.

| Control | Element ID | Behavior |
|---------|------------|----------|
| Heading | `#ticket-list-heading` | "Support tickets" |
| Search | `#ticket-search` | Debounced (300 ms); server `?q=` title search |
| Status filter | `#ticket-status-filter` | Server `?status=` (All, Open, In Progress, Resolved, Closed, Cancelled) |
| Priority filter | `#ticket-priority-filter` | **Client-side** filter (All, P1–P4) — Sprint 6.2 |
| Sort | `#ticket-sort` | **Client-side** sort — Sprint 6.2 (Newest, Oldest, Recently Updated, Priority, Ticket ID) |
| Result summary | `#ticket-list-summary` | "Showing N tickets" / "Showing N of M tickets" — Sprint 6.2 |
| Clear filters | `#ticket-list-clear-filters` | Resets q, status, priority — Sprint 6.2 |
| Ticket cards | `#ticket-list-root` | Click card → detail (`?id=`) |
| New ticket | Button in `list.ts` | Opens create modal (`form.ts`) |

**Pipeline (after fetch):** `GET /tickets` → client priority filter → client sort → render cards + summary.

### Detail view (`#ticket-detail-view`)

Rendered by `ticketdetail` HTL + `detail.ts`. Visible when URL has `?id={ticketId}`.

| Section | API | Notes |
|---------|-----|-------|
| Ticket header | `GET /tickets/{id}` | ID, status/priority badges, title, description |
| Metadata | same | Assignee, createdBy, createdAt, updatedAt |
| Back link | — | Removes `?id=` → list view |
| Edit | `PUT /tickets/{id}` | Modal form; **hidden/disabled on Closed/Cancelled** |
| Status change | `PUT /tickets/{id}/status` | Dropdown shows only `getAllowedNextStatuses()` (mirrors server state machine) |
| Reassign | `PUT /tickets/{id}/assignee` | User picker from `GET /users`; disabled on terminal tickets |
| Comments list | `GET /tickets/{id}/comments` | Ordered by `createdAt` asc |
| Add comment | `POST /tickets/{id}/comments` | **Allowed even on Closed/Cancelled** |

### Create / edit form (modal — `ticketform`)

| Mode | Trigger | API | Post-success |
|------|---------|-----|--------------|
| Create | "+ New Ticket" | `POST /tickets` + CSRF | Full page navigation to `?id={newId}` |
| Edit | "Edit" on detail | `PUT /tickets/{id}` + CSRF | Toast + refresh detail |

Fields: title, description, priority (P1–P4), assignee (create only — from `GET /users`). `createdBy` from `GET /me` via `userContext.ts`.

---

## User Journeys

### 1. Create a ticket

1. Open `/content/assessment/us/en/tickets.html` (logged in to AEM author).
2. UI loads current user (`GET /me`); optional "Logged in as …" header.
3. Click **+ New Ticket** → modal opens; assignee dropdown populated from `GET /users`.
4. Fill title, description, priority; optionally select assignee.
5. Save → `POST /tickets` with `CSRF-Token` header and `createdBy` from session user.
6. On success → full page redirect to `?id=TKT-xxxx` (detail view loads fresh).

### 2. View ticket and add comment

1. From list, click a ticket card → URL `?id=TKT-xxxx`, detail view shown.
2. Detail fetches ticket + comments in parallel.
3. Type comment → Submit → `POST /tickets/{id}/comments` with CSRF + `createdBy`.
4. Success toast; comment list refreshes.

### 3. Reassign a ticket

1. On detail (non-terminal ticket), open reassign control.
2. Select user from dropdown (`GET /users`).
3. Submit → `PUT /tickets/{id}/assignee` with `{ "assignedTo": "agent-1" }`.
4. Success toast; detail refreshes. **Fails with error on Closed/Cancelled** (server `TICKET_NOT_EDITABLE`).

### 4. Move through status lifecycle to Closed

Typical happy path:

```
Open → In Progress → Resolved → Closed
```

1. On detail, status dropdown shows only valid next statuses (e.g. Open → "In Progress" or "Cancelled").
2. Select next status → `PUT /tickets/{id}/status` with `{ "status": "…" }`.
3. For **Close** or **Cancel** from applicable states → confirmation dialog (Sprint 6.2) before submit.
4. Repeat until **Closed**. Terminal state: status dropdown empty; edit/reassign disabled; comments still allowed.

Alternate path: Open → Cancelled or In Progress → Cancelled (also requires confirmation).

### 5. Search, filter, and sort

1. **Search:** type in search box → debounced `GET /tickets?q=…` (title match).
2. **Status:** change status dropdown → `GET /tickets?status=…` (combines with `q` when both set).
3. **Priority:** client-side filter on fetched results (no extra API param).
4. **Sort:** client-side reorder (newest default = `createdAt` desc).
5. Summary line updates: "Showing N tickets" or "Showing N of M tickets" when client filters reduce the set.

---

## Flow Diagram

```
                    ┌──────────────────────────────────────┐
                    │  /tickets.html  (ticketapp root)       │
                    │  loadCurrentUser() → GET /me           │
                    └─────────────────┬────────────────────┘
                                      │
                         ?id= absent  │  ?id= present
                                      │
              ┌───────────────────────┴───────────────────────┐
              ▼                                               ▼
   ┌─────────────────────┐                         ┌─────────────────────┐
   │  LIST VIEW          │   click card / ?id=     │  DETAIL VIEW        │
   │  GET /tickets       │ ──────────────────────► │  GET /tickets/{id}  │
   │  ?q & ?status       │                         │  GET …/comments     │
   │  + client priority  │ ◄────────────────────── │                     │
   │  + client sort      │   Back to list          │  Actions:           │
   │  + New Ticket       │                         │  • Edit (PUT)       │
   └─────────┬───────────┘                         │  • Status (PUT)     │
             │                                     │  • Reassign (PUT)   │
             │  + New Ticket                     │  • Comment (POST)   │
             ▼                                     └─────────────────────┘
   ┌─────────────────────┐
   │  CREATE MODAL       │
   │  POST /tickets      │──success──► full reload ?id=newId
   └─────────────────────┘

Status lifecycle (server-enforced):
  Open ──► In Progress ──► Resolved ──► Closed
    │            │
    └─ Cancelled ◄┘
```

---

## Sprint 6.2 Enhancements

| Enhancement | User-visible effect |
|-------------|---------------------|
| **6.2.1 Sort** | Sort dropdown; five options; combines with search/status/priority |
| **6.2.2 Result summary** | "Showing N tickets" / filtered count; Clear filters button |
| **6.2.3 Priority filter** | Priority dropdown; client-side only |
| **6.2.4 `/me` + createdBy** | Ticket create and comments use logged-in AEM user, not hardcoded `agent-1` |
| **6.2.5 Confirmations + toasts** | Confirm before Close/Cancel; success/error toasts for create, update, status, reassign, comment (top-center placement) |

---

## Error & Loading States

| State | Behavior |
|-------|----------|
| Loading | "Loading…" message in list/detail panels |
| Empty list | "No tickets yet." or "No tickets match your filters." |
| API error | Inline message in list/detail; toasts for action failures |
| 404 ticket | Detail shows not-found message |
| 409 invalid status | Error near status control + toast; UI restricts choices but server is authoritative |
| XSS | User content set via `textContent`, not `innerHTML` |

---

## API Paths Used by UI

All paths are **relative** (no hardcoded host) per FR-19:

- `/bin/api/v1/tickets`, `/bin/api/v1/tickets/{id}`
- `/bin/api/v1/tickets/{id}/status`, `/bin/api/v1/tickets/{id}/assignee`
- `/bin/api/v1/tickets/{id}/comments`
- `/bin/api/v1/users`, `/bin/api/v1/me`
- `/libs/granite/csrf/token.json` (mutating requests)
