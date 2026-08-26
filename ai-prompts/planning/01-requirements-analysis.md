# AI Prompt — Task 1.1.1: requirements-analysis.md

**Date:** 2025-08-25
**Purpose:** Record the planning prompt and AI response summary for Sprint 1.1 Task 1.1.1.

**Sprint/Task:** 1.1 / 1.1.1
**Category:** planning
**Meaningful:** Yes — task instruction (1.1.1), architectural scope, and clarifying decisions on priority, search, and closed-ticket mutability.

---

## Prompt (verbatim)

> Task 1.1.1 (Sprint 1.1): Create requirements-analysis.md at the repo root.
>
> Follow all rules in .cursor/rules/. Read implementation-plan.md for sprint/task context.
> This is a PLANNING document only — do NOT write any code, CFMs, or servlets yet.
>
> Namespaces to use:
> - Java package: com.mysite.core
> - Content/app: /apps/assessment, /conf/assessment, /content/dam/assessment
>
> Analyze the mandatory-core requirements for the AEM Support Ticket Management System:
>
> ENTITIES:
> - User (seeded): id, name, email
> - Ticket: id, title, description, priority, status, assignedTo, createdBy, createdAt, updatedAt
> - Comment: id, ticketId, message, createdBy, createdAt
>
> FUNCTIONALITY: create ticket, list tickets, view ticket detail, update fields,
> reassign, add comment, keyword search, status filter.
>
> STATE MACHINE (must enforce; reject ALL others):
> - Open -> In Progress
> - In Progress -> Resolved
> - Resolved -> Closed
> - Open -> Cancelled
> - In Progress -> Cancelled
>
> TECH CONSTRAINTS: AEMaaCS, Content Fragments for persistence, Sling Servlets at
> /bin/api/v1/*, users seeded via AEM UserManager, Repository Pattern (swappable to DB later),
> TypeScript frontend via ui.frontend.
>
> The document MUST contain these sections:
> 1. Problem Statement (2-3 sentences)
> 2. In-Scope vs Out-of-Scope (explicit lists)
> 3. Entity breakdown (each field: name, type, required?, validation rule)
> 4. State machine table (from-state | event | to-state | allowed?)
> 5. Functional requirements, numbered FR-1, FR-2, ...
> 6. Non-functional requirements (validation, persistence, no secrets, error handling)
> 7. Assumptions I am making
> 8. Open questions / risks
>
> Ask me clarifying questions BEFORE finalizing if anything is ambiguous.
> When done, remind me to save this prompt + your response summary to ai-prompts/planning/.

---

## Clarifying decisions (asked before finalizing)

| Topic | Decision |
|-------|----------|
| Priority enum | **P1, P2, P3, P4** (P1 = highest urgency) |
| Keyword search | **Title only** on ticket list |
| Closed/Cancelled tickets | **Read-only** for field update, reassign, and status change; **adding comments still allowed** |

---

## Follow-up prompt (implementation)

> Plan: requirements-analysis.md (Task 1.1.1) — Implement the plan as specified. Do NOT edit the plan file itself.

---

## AI response summary

Created `requirements-analysis.md` at the repo root with all eight required sections: problem statement, in/out-of-scope lists, entity field tables (User, Ticket, Comment), state machine transition table plus mutability matrix, functional requirements FR-1 through FR-19, non-functional requirements, assumptions A-1 through A-12, and open questions/risks. Incorporated developer clarifications (P1–P4 priority, title-only search, closed/cancelled tickets comment-only). Updated `implementation-plan.md` to mark 1.1.1 complete and advance Active Task to 1.1.2. No code, CFMs, or servlets were written.

---

## Artifacts produced

| File | Change |
|------|--------|
| `requirements-analysis.md` | Created |
| `implementation-plan.md` | Updated — 1.1.1 complete; Active Task 1.1.2 |
| `ai-prompts/planning/01-requirements-analysis.md` | Created (this file) |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 1.1 / Task 1.1.1 | Complete |
| Downstream | `acceptance-criteria.md` (1.1.2), `data-model.md` (1.1.3), `api-contract.md` (1.1.4) |
