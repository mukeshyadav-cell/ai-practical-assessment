# AI Prompt — Task 1.1.2: acceptance-criteria.md

**Date:** 2025-08-26
**Purpose:** Record the planning prompt and AI response summary for Sprint 1.1 Task 1.1.2.

**Sprint/Task:** 1.1 / 1.1.2
**Category:** planning
**Meaningful:** Yes — task instruction (1.1.2), FR-to-AC traceability, and state machine acceptance criteria.

---

## Prompt (verbatim)

> Task 1.1.2 (Sprint 1.1): Create acceptance-criteria.md at the repo root.
>
> Follow all rules in .cursor/rules/. Read requirements-analysis.md and implementation-plan.md
> for context. This is a PLANNING document only — do NOT write code or tests yet.
>
> Goal: Convert every functional requirement (FR-x) from requirements-analysis.md into
> testable acceptance criteria that will later map directly to integration tests.
>
> Produce acceptance-criteria.md with sections: Purpose + date, traceability rule,
> Given/When/Then ACs (create, list, view, update, reassign, comment, search, filter),
> state machine AC table (valid + invalid transitions), NF ACs, Definition of Done for Sprint 1.1.
>
> When done, remind me to save this prompt + response summary to ai-prompts/planning/.

---

## AI response summary

Created `acceptance-criteria.md` at the repo root with FR coverage matrix (every FR-1–FR-19 mapped to ≥1 AC), 51 functional ACs in Given/When/Then Gherkin format, state machine table (AC-22–AC-35: 5 valid and 9 invalid transitions), six non-functional ACs (NFR-AC-1–6), and Sprint 1.1 Definition of Done checklist. Resolved open questions from requirements-analysis: optional assignee on create (AC-2), edits allowed in Resolved status (AC-15), comments allowed on Cancelled tickets (AC-43). Updated `implementation-plan.md` to mark 1.1.2 complete and advance Active Task to 1.1.3. No code or tests written.

---

## Artifacts produced

| File | Change |
|------|--------|
| `acceptance-criteria.md` | Created — AC-1–AC-51, NFR-AC-1–6 |
| `implementation-plan.md` | Updated — 1.1.2 complete; Active Task 1.1.3 |
| `ai-prompts/planning/02-acceptance-criteria.md` | Created (this file) |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 1.1 / Task 1.1.2 | Complete |
| Source | `requirements-analysis.md` FR-1–FR-19 |
| Downstream | `data-model.md` (1.1.3), `api-contract.md` (1.1.4), `it.tests` (Sprint 7.1) |
