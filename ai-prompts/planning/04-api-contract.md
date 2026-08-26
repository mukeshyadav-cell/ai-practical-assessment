# AI Prompt — Task 1.1.4: api-contract.md

**Date:** 2025-08-26
**Purpose:** Record the planning prompt and AI response summary for Sprint 1.1 Task 1.1.4.

**Sprint/Task:** 1.1 / 1.1.4
**Category:** planning
**Meaningful:** Yes — task instruction (1.1.4), full REST contract, and explicit status/assignee sub-resource design.

---

## Prompt (verbatim)

> Task 1.1.4 (Sprint 1.1): Create api-contract.md at the repo root.
>
> Define the complete REST API contract at /bin/api/v1 for tickets, comments, and users.
> Planning only — no servlet code. Sections: conventions, endpoints with JSON examples,
> status transition detail, error catalog, example payloads, traceability to FR/AC.
>
> When done, remind me to save this prompt + response summary to ai-prompts/planning/.

---

## Design decisions documented

| Topic | Decision |
|-------|----------|
| Status / assignee | Separate `PUT /tickets/{id}/status` and `PUT /tickets/{id}/assignee`; field `PUT` excludes status and assignee |
| Create responses | **201** for POST ticket and POST comment |
| User detail | Added `GET /users/{userId}` for FR-17 / AC-48, AC-49 |
| Error body | `{ "error": "...", "code": "..." }` |
| Servlet paths | `sling.servlet.paths` per endpoint; suffix routing for `{id}/status`, `{id}/assignee`, `{id}/comments` |

---

## AI response summary

Created `api-contract.md` at the repo root with conventions, ten REST endpoints (including GET user by id), JSON request/response examples, status transition section with allowed-transition table, error catalog (VALIDATION_ERROR, UNKNOWN_USER, TICKET_NOT_EDITABLE, NOT_FOUND, INVALID_TRANSITION, INTERNAL_ERROR), DTO example payloads, and FR/AC traceability matrix. Documented explicit sub-resource design for status and assignee changes. Updated `implementation-plan.md` to mark 1.1.4 complete and advance Active Task to 1.1.5. No servlet code written.

---

## Artifacts produced

| File | Change |
|------|--------|
| `api-contract.md` | Created |
| `implementation-plan.md` | Updated — 1.1.4 complete; Active Task 1.1.5 |
| `ai-prompts/planning/04-api-contract.md` | Created (this file) |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 1.1 / Task 1.1.4 | Complete |
| Sources | `requirements-analysis.md`, `acceptance-criteria.md`, `data-model.md` |
| Downstream | Sprint 5.1 servlets, `it.tests` (Sprint 7.1) |
