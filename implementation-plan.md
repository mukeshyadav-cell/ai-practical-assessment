# Implementation Plan — Sprint & Task Tree

**Project:** AEM Support Ticket Management System
**Java base package:** com.mysite.core
**Content namespace:** assessment (/apps/assessment, /conf/assessment, /content/dam/assessment)
**Archetype:** AEM Project Archetype 57 (AEMaaCS)
**Frontend:** TypeScript via ui.frontend Webpack pipeline (plain JS permitted where simpler)

---

## Current Status
- **Active Sprint:** 1.1
- **Active Task:** 1.1.1
- **Last Completed:** (none)

---

## Sprint 1.1 — Planning & Analysis
- [ ] 1.1.1 — requirements-analysis.md
- [ ] 1.1.2 — acceptance-criteria.md
- [ ] 1.1.3 — data-model.md (entities, CFM fields, package/module decisions)
- [ ] 1.1.4 — api-contract.md (REST endpoints, request/response, status codes)
- [ ] 1.1.5 — implementation-plan.md (finalize/refine this file)
  **Quality Gate:** All planning docs complete, reviewed, mutually consistent.

## Sprint 2.1 — Project Scaffold & Foundation
- [ ] 2.1.1 — Verify archetype build (mvn clean install) + document module map
- [ ] 2.1.2 — Content Fragment Models (Ticket, Comment) under /conf/assessment/settings/dam/cfm/models
- [ ] 2.1.3 — System service user + serviceusermapping (read/write DAM, read users)
- [ ] 2.1.4 — DTOs in com.mysite.core.dto (TicketDTO, CommentDTO, UserDTO)
- [ ] 2.1.5 — Repository interfaces in com.mysite.core.repositories
  **Quality Gate:** Project builds, deploys to local SDK, bundle is Active.

## Sprint 3.1 — Repository Layer (Content Fragment adapters)
- [ ] 3.1.1 — TicketRepository CF impl — read (getAll, getById, findByStatus, search)
- [ ] 3.1.2 — TicketRepository CF impl — write (create, update, delete) + ID generation
- [ ] 3.1.3 — CommentRepository CF impl (add, listByTicket)
- [ ] 3.1.4 — UserRepository AEM impl (getById, search, getAll)
- [ ] 3.1.5 — CF <-> DTO mappers (reusable)
  **Quality Gate:** Can create and read a Ticket + Comment via a test/harness.

## Sprint 4.1 — State Machine + Services
- [ ] 4.1.1 — com.mysite.core.statemachine.TicketStateMachine + InvalidTransitionException
- [ ] 4.1.2 — TicketService (create, list, get, update, reassign, changeStatus, search/filter)
- [ ] 4.1.3 — CommentService (add comment, list by ticket)
- [ ] 4.1.4 — Validation (required fields, valid enums, known assignee)
  **Quality Gate:** State-machine unit tests pass (valid + invalid transitions).

## Sprint 5.1 — REST API (Servlets)
- [ ] 5.1.1 — Ticket servlets (GET list, GET by id, POST create, PUT update/reassign/status)
- [ ] 5.1.2 — Comment servlet (POST add, GET list by ticket)
- [ ] 5.1.3 — Search/filter endpoint (keyword + status)
- [ ] 5.1.4 — Error handling + HTTP status mapping (400/404/409/500) + JSON errors
  **Quality Gate:** All endpoints verified via Postman/curl.

## Sprint 6.1 — UI (HTL + TypeScript)
- [ ] 6.1.1 — Ticket list screen (with search + status filter)
- [ ] 6.1.2 — Ticket detail screen (fields + comments)
- [ ] 6.1.3 — Create/edit ticket form (assignee from user list)
- [ ] 6.1.4 — Add comment UI
- [ ] 6.1.5 — Status change UI (only valid transitions shown)
  **Quality Gate:** Full user flow works end-to-end in the browser.

## Sprint 7.1 — Integration Tests
- [ ] 7.1.1 — State-machine transition tests (all valid succeed)
- [ ] 7.1.2 — State-machine rejection tests (invalid rejected, HTTP 409)
- [ ] 7.1.3 — CRUD + comment + search happy-path tests
  **Quality Gate:** All integration tests green in it.tests module.

## Sprint 8.1 — Documentation & Reflection
- [ ] 8.1.1 — design-notes.md, ui-flow.md, test-strategy.md, debugging-notes.md
- [ ] 8.1.2 — code-review-notes.md, pr-description.md
- [ ] 8.1.3 — README.md (setup instructions), candidate-info.md, tool-workflow.md
- [ ] 8.1.4 — reflection.md, final-ai-usage-summary.md
- [ ] 8.1.5 — Consolidate ai-prompts/ folders
  **Quality Gate:** All lifecycle artifacts complete; README setup verified from scratch.

---

## Sprint Numbering
- Use X.1 per phase (1.1, 2.1, ...). Split to X.2 only if a phase genuinely grows.
- Each sprint ends with a Quality Gate + generated prompt-history/sprint-X.X.md.

## Namespace Reminder
- Java code: com.mysite.core.*
- Content/app: /apps/assessment, /conf/assessment, /content/dam/assessment
- Never mix the two.
