# AI Prompt — Task 1.1.3: data-model.md

**Date:** 2025-08-26
**Purpose:** Record the planning prompt and AI response summary for Sprint 1.1 Task 1.1.3.

**Sprint/Task:** 1.1 / 1.1.3
**Category:** planning
**Meaningful:** Yes — task instruction (1.1.3), CFM/DTO/persistence design, and ID generation strategy.

---

## Prompt (verbatim)

> Task 1.1.3 (Sprint 1.1): Create data-model.md at the repo root.
>
> Follow all rules in .cursor/rules/. Read requirements-analysis.md and acceptance-criteria.md
> for context. This is a PLANNING document only — do NOT create actual CFMs or code yet.
>
> Goal: Define the complete data model — Content Fragment Models, DTOs, relationships,
> and persistence paths — designed to be swappable to a relational DB later.
>
> Produce data-model.md with design principles, Ticket/Comment CFM tables, User (UserManager),
> DTO definitions, relationships diagram, persistence layout, future DB mapping, validation summary.
>
> When done, remind me to save this prompt + response summary to ai-prompts/planning/.

---

## Design decisions documented

| Topic | Decision |
|-------|----------|
| Priority enum | **P1, P2, P3, P4** (aligned with requirements-analysis; not Low/Medium/High/Critical from task template) |
| ticketId format | `TKT-{sequence}` (e.g. TKT-1001) |
| commentId format | `CMT-{sequence}` (e.g. CMT-1001) |
| DTO `id` vs CF `ticketId` | JSON/DTO uses `id`; CF element stores `ticketId` / `commentId` |
| UserDTO fields | `userId`, `displayName`, `email` from UserManager profile props |
| Timestamps in CF | ISO-8601 text elements, parsed to `Instant` in mappers |

---

## AI response summary

Created `data-model.md` at the repo root with ten sections: design principles, Ticket and Comment CFM field tables, UserManager user model, Java DTO field definitions, ASCII relationship diagram, JCR persistence paths, future relational DB column mapping, and consolidated validation rules. Defined ID strategies TKT-{n} and CMT-{n}, text-ID FK pattern, and enum values consistent with requirements-analysis and acceptance-criteria. Updated `implementation-plan.md` to mark 1.1.3 complete and advance Active Task to 1.1.4. No CFMs or Java code written.

---

## Artifacts produced

| File | Change |
|------|--------|
| `data-model.md` | Created |
| `implementation-plan.md` | Updated — 1.1.3 complete; Active Task 1.1.4 |
| `ai-prompts/planning/03-data-model.md` | Created (this file) |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 1.1 / Task 1.1.3 | Complete |
| Sources | `requirements-analysis.md`, `acceptance-criteria.md` |
| Downstream | `api-contract.md` (1.1.4), Sprint 2.1.2 CFMs, Sprint 2.1.4 DTOs |
