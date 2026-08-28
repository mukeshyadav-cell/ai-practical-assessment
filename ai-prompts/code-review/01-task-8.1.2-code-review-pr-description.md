# AI Prompt — Task 8.1.2: Code review notes & PR description

**Date:** 2026-08-27
**Purpose:** Record the code-review prompt and AI response summary for Sprint 8.1 Task 8.1.2.

**Sprint/Task:** 8.1 / 8.1.2
**Category:** code-review
**Meaningful:** Yes — Task 8.1.2 instruction with two named artifacts, honesty requirements, and security/checklist specs.

---

## Prompt (verbatim)

> Task 8.1.2 (Sprint 8.1): Create code-review-notes.md and pr-description.md at the repo root.
>
> Follow all rules in .cursor/rules/ (07-documentation). Base content on the ACTUAL codebase and
> project history. Be honest and specific — include genuine strengths AND real areas for
> improvement. Each doc: title + date + purpose header.
>
> Create:
>
> 1. code-review-notes.md — Self Code Review
>    Perform a genuine code review of the implementation. Sections:
>    - Review scope: what was reviewed (core services, servlets, repositories, state machine,
>      mappers, validation, UI TypeScript).
>    - Strengths (with specific examples):
>        * Clean layering + Repository Pattern (swappability)
>        * State machine as pure, well-tested domain logic
>        * DTO boundary (no ContentFragment/Resource leaks above repo)
>        * Consistent error handling via ServletResponseUtil + domain exception hierarchy
>        * Reusable UI api.ts + unified list state
>    - Observations / potential improvements (be honest, specific, and constructive):
>        * ID counter not atomic (concurrency) — acceptable for scope, note the risk
>        * In-memory filtering/sorting in repositories (JCR query could optimize at scale)
>        * Client-side priority/sort filtering asymmetry vs server-side status
>        * createdBy relies on /me; no full auth/authorization layer (scope)
>        * Any duplication, magic strings, or areas that could be refactored
>        * Test coverage gaps (unit-only; no integration/E2E by scope)
>    - Security review: no secrets committed, service user least-privilege, XSS-escaping in UI,
>      no stack-trace leakage in servlets. Note anything to watch.
>    - Code review checklist table: item | status (Pass/Note) | comment.
>    - Overall assessment: a short honest verdict on code quality + maintainability.
>
> 2. pr-description.md — Pull Request Description
>    Write as if opening ONE PR for the whole feature. Sections:
>    - Title: e.g., "feat: Support Ticket Management System (AEMaaCS)"
>    - Summary: what this PR delivers (1 short paragraph).
>    - What changed (by area): entities/CFMs, repositories, state machine, services, REST API,
>      UI, tests, docs. Use concise bullets.
>    - Key design decisions (brief; link/refer to design-notes.md).
>    - How to test / verify:
>        * build + deploy commands (mvn clean install -PautoInstallSinglePackage)
>        * run unit tests (mvn test)
>        * open the app URL; run the end-to-end flow
>        * representative curl commands (create, status transition 200, invalid transition 409)
>    - Requirements coverage: short table mapping mandatory-core features -> where implemented.
>    - Out of scope: it.tests/ui.tests, full auth, DB adapter (designed, not implemented).
>    - Checklist: builds, tests green, no secrets, docs updated, state machine enforced.
>
> Constraints:
> - Accurate to the real code; do NOT invent. Include REAL improvement areas (honesty is valued).
> - Tables where helpful; concise, professional tone.
>
> After generating:
> - List the two files created.
> - Flag anything you were unsure about so I can verify/correct.
> When done, remind me to save this prompt + response summary to ai-prompts/code-review/.

---

## AI response summary

Created **`code-review-notes.md`** (self-review: scope table, five strengths with code references — OSGi repo targets, pure `TicketStateMachine`, DTO/mapper boundary, `ServletResponseUtil` error catalog, `api.ts`/`list.ts` UI patterns; nine constructive improvements — non-atomic ID counter, in-memory `getAll()`/`findByStatus` scans, client/server filter asymmetry, comment `createdBy` body trust vs ticket create session overwrite, status string duplication TS/Java, silent `LoginException` → empty list, routing filters vs api-contract, archetype demo code, unit-only test gaps; security section; 18-row checklist; verdict "good quality for scope") and **`pr-description.md`** (title `feat: Support Ticket Management System (AEMaaCS)`, summary, changes by sprint, design links, build/test/curl instructions, FR-1–FR-19 coverage table, out-of-scope, PR checklist). Updated `implementation-plan.md` — 8.1.2 complete; Active Task 8.1.3. Flagged for developer: comment `createdBy` spoofing risk, test counts not re-run, curl needs local `TKT-*`, archetype `ui.tests` admin placeholders.

---

## Artifacts produced

| File | Change |
|------|--------|
| `code-review-notes.md` | Created — self-review, security, checklist, verdict |
| `pr-description.md` | Created — full PR description with FR coverage |
| `implementation-plan.md` | Updated — 8.1.2 complete; Active Task 8.1.3 |
| `ai-prompts/code-review/01-task-8.1.2-code-review-pr-description.md` | Created (this file) |

---

## Developer verification flags (from AI response)

| Item | Action needed |
|------|----------------|
| Comment `createdBy` from request body | Confirm intentional or fix to always use session user |
| Test counts (62 / 23 / 11) | Re-run `mvn test -pl core` locally |
| curl examples | Verify against real tickets on local SDK |
| PR checklist unchecked items | Developer sign-off |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 8.1 / Task 8.1.2 | Complete |
| DOD-1 (Sprint 8.1) | Lifecycle artifacts (partial — 6 of 9+ docs) |
| FR-19 | Referenced in code-review security + PR checklist |
| Downstream | 8.1.3 — README.md, candidate-info.md, tool-workflow.md |
