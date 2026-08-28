# Pull Request — Support Ticket Management System (AEMaaCS)

**Date:** 2026-08-27  
**Purpose:** Pull request description for the complete Support Ticket Management System feature branch.

**Sprint/Task:** 8.1 / 8.1.2

---

## Title

```
feat: Support Ticket Management System (AEMaaCS)
```

---

## Summary

This PR delivers a full-stack **Support Ticket Management System** on AEM as a Cloud Service (archetype 57): tickets and comments persisted as **Content Fragments**, business logic and a strict **ticket lifecycle state machine** in the `core` OSGi bundle, **REST APIs** at `/bin/api/v1/*` via Sling Servlets, and a **TypeScript** ticketing UI on `/content/assessment/us/en/tickets.html`. The implementation follows the **Repository Pattern** for future database swappability and includes **~96 Java unit tests** covering the state machine and service layer.

---

## What Changed

### Entities & Content Fragments (Sprint 2.1)

- CFM **ticket** and **comment** models under `/conf/assessment/settings/dam/cfm/models/`
- DAM folders `/content/dam/assessment/tickets`, `/comments`
- Repoinit: service user `assessment-service`, seed users `agent-1`, `agent-2`
- DTOs: `TicketDTO`, `CommentDTO`, `UserDTO`

### Repositories (Sprint 3.1)

- `ContentFragmentTicketRepository` — CRUD + `findByStatus`, `searchByTitle`; `TKT-{n}` ID counter
- `ContentFragmentCommentRepository` — `add`, `listByTicket`; `CMT-{n}` ID counter
- `AemUserRepository` — `getAll`, `getById`, `search` via `UserManager`
- `TicketMapper` / `CommentMapper` — CF element ↔ DTO (no leaks above repo layer)

### State Machine & Services (Sprint 4.1)

- `TicketStateMachine` — pure domain logic; five allowed transitions; terminal `Closed`/`Cancelled`
- `TicketServiceImpl` — create, list, update, reassign, `changeStatus`, search/filter
- `CommentServiceImpl` — add/list comments (allowed on terminal tickets)
- `TicketValidator`, `CommentValidator`; `DomainException` hierarchy

### REST API (Sprint 5.1)

| Endpoint | Methods |
|----------|---------|
| `/bin/api/v1/tickets` | GET, POST |
| `/bin/api/v1/tickets/{id}` | GET, PUT |
| `/bin/api/v1/tickets/{id}/status` | PUT |
| `/bin/api/v1/tickets/{id}/assignee` | PUT |
| `/bin/api/v1/tickets/{id}/comments` | GET, POST |
| `/bin/api/v1/users` | GET |
| `/bin/api/v1/users/{userId}` | GET |
| `/bin/api/v1/me` | GET |

- `ServletResponseUtil` — Jackson + JSR-310; centralized error mapping
- Routing filters for sub-paths (local SDK suffix servlet workaround)

### UI (Sprints 6.1, 6.2)

- HTL components: `ticketapp`, `ticketlist`, `ticketdetail`, `ticketform`
- Clientlib `assessment.ticketing` (Webpack/TypeScript)
- SPA-style list ↔ detail via `?id=` query param
- Search, status filter, priority filter, sort, result summary
- Status change, reassign, create/edit modal, comments
- CSRF token on POST/PUT; `/me` for authenticated `createdBy`
- Terminal transition confirmations + action toasts

### Tests (Sprint 7.1)

- `TicketStateMachineTest` (62 tests)
- `TicketServiceImplTest` (23 tests)
- `CommentServiceImplTest` (11 tests)

### Documentation (Sprint 8.1)

- Planning: `requirements-analysis.md`, `acceptance-criteria.md`, `data-model.md`, `api-contract.md`, `implementation-plan.md`
- Lifecycle: `design-notes.md`, `ui-flow.md`, `test-strategy.md`, `debugging-notes.md`, `code-review-notes.md`, `pr-description.md` (this file)
- Prompt history: `prompt-history/sprint-1.1.md` … `sprint-7.1.md`

---

## Key Design Decisions

See [design-notes.md](design-notes.md) for full trade-off analysis. Highlights:

| Decision | Choice |
|----------|--------|
| Persistence | Content Fragments; text-ID relationships (not content references) |
| Architecture | Repository Pattern (`impl.type=contentfragment`); swappable to DB |
| API | Sling Servlets at `/bin/api/v1/*` — no GraphQL |
| State machine | Pure `TicketStateMachine`; `changeStatus` only path for status mutations |
| Status/assignee | Dedicated sub-resource endpoints (not generic PATCH) |
| Terminal tickets | Edits/reassign blocked; comments still allowed |
| UI filtering | Server: `?status=`, `?q=`; client: priority + sort (Sprint 6.2) |

---

## How to Test / Verify

### Build and deploy

```bash
mvn clean install -PautoInstallSinglePackage
```

Local author default: `http://localhost:4502`

### Unit tests

```bash
mvn test -pl core
```

Expected: all `TicketStateMachineTest`, `TicketServiceImplTest`, and `CommentServiceImplTest` methods pass.

### End-to-end UI flow

1. Log in to AEM author (e.g. `admin`).
2. Open `/content/assessment/us/en/tickets.html`
3. Verify: list → create ticket → detail → add comment → status transitions → reassign → close
4. Verify: search, status filter, priority filter, sort, result count, toasts

### Representative curl commands

```bash
# Create ticket (201)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X POST http://localhost:4502/bin/api/v1/tickets \
  -d '{"title":"PR verify","description":"Smoke test","priority":"P2"}'

# List tickets
curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets

# Valid status transition (200) — ticket must be Open
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"In Progress"}'

# Invalid transition (409) — Open → Closed skips workflow
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:4502/bin/api/v1/tickets/TKT-1001/status \
  -d '{"status":"Closed"}'

# Current user
curl -s -u admin:admin http://localhost:4502/bin/api/v1/me
```

Expected 409 body shape: `{"error":"Invalid transition: Open -> Closed","code":"INVALID_TRANSITION"}`

---

## Requirements Coverage

| Feature | FR | Implementation |
|---------|-----|----------------|
| Create ticket | FR-1 | `POST /tickets`, `TicketServiceImpl.createTicket` |
| List tickets (newest first) | FR-2 | `GET /tickets`, repo sort + service |
| View detail | FR-3 | `GET /tickets/{id}` |
| Status filter | FR-4 | `GET /tickets?status=` |
| Update fields | FR-5 | `PUT /tickets/{id}` |
| Reassign | FR-6 | `PUT /tickets/{id}/assignee` |
| Terminal ticket edits rejected | FR-7 | `TicketNotEditableException` |
| Valid status changes | FR-8 | `PUT /tickets/{id}/status` + state machine |
| Invalid transitions → 409 | FR-9 | `InvalidTransitionException` |
| UI constrains next statuses | FR-10 | `transitions.ts` + server enforcement |
| Title search | FR-11 | `GET /tickets?q=` |
| Combined search + filter | FR-12 | `TicketServiceImpl.listTickets` |
| Add comment | FR-13 | `POST /tickets/{id}/comments` |
| List comments | FR-14 | `GET /tickets/{id}/comments` |
| Invalid comment target | FR-15 | `CommentServiceImpl` + `TicketNotFoundException` |
| List users | FR-16 | `GET /users` |
| Resolve user | FR-17 | `GET /users/{userId}` |
| REST-only access | FR-18 | All `/bin/api/v1/*` servlets |
| Relative API paths in UI | FR-19 | `api.ts` constants |

---

## Out of Scope

| Item | Notes |
|------|-------|
| `it.tests` / `ui.tests` | Archetype modules unused; manual curl/browser verification instead |
| Custom authentication | AEM built-in login only |
| Database adapter | Designed (`impl.type=database`); not implemented |
| Ticket delete | Not in mandatory core |
| Pagination | Full list returned |
| GraphQL | Explicitly excluded |
| Per-ticket authorization | Any logged-in author can use all endpoints |

---

## PR Checklist

- [x] `mvn clean install` succeeds
- [x] `mvn test -pl core` — unit tests green
- [x] State machine enforced server-side (`TicketStateMachine` + 409)
- [x] No secrets in committed application code
- [x] UI uses relative API paths; CSRF on mutations
- [x] Planning + lifecycle docs at repo root
- [x] Prompt history through Sprint 7.1
- [ ] Developer: verify curl examples against local SDK with persisted tickets
- [ ] Developer: review `code-review-notes.md` open items (comment `createdBy`, archetype cleanup)

---

## Related Documents

- [design-notes.md](design-notes.md)
- [test-strategy.md](test-strategy.md)
- [ui-flow.md](ui-flow.md)
- [code-review-notes.md](code-review-notes.md)
- [implementation-plan.md](implementation-plan.md)
