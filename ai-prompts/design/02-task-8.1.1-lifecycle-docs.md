# AI Prompt — Task 8.1.1: Lifecycle documentation (design, UI flow, test strategy, debugging)

**Date:** 2026-08-27
**Purpose:** Record the documentation prompt and AI response summary for Sprint 8.1 Task 8.1.1.

**Sprint/Task:** 8.1 / 8.1.1
**Category:** design
**Meaningful:** Yes — Task 8.1.1 instruction with four named lifecycle artifacts, section specs, and accuracy constraints.

---

## Prompt (verbatim)

> Task 8.1.1 (Sprint 8.1): Create four technical lifecycle documents: design-notes.md,
> ui-flow.md, test-strategy.md, debugging-notes.md — all at the repo root.
>
> Follow all rules in .cursor/rules/ (07-documentation). Base the content on the ACTUAL project:
> the code, planning docs, and prompt history. Each doc: title + date + one-line purpose header.
> Be accurate to what was actually built — do NOT invent features. Use tables/diagrams where helpful.
>
> Create:
>
> 1. design-notes.md — Architecture & Design Decisions
>    Sections:
>    - Overview: what the system is + high-level architecture (layers: UI -> Servlet -> Service
>      -> Repository (interface) -> CF adapter -> Content Fragments; users via AEM UserManager).
>    - Architecture diagram (ASCII) of the layers + the swappable repository (impl.type).
>    - Key design decisions WITH trade-offs (this is the important part). Cover:
>        * Repository Pattern (Port/Adapter) for CF->DB swappability
>        * Sling Servlets (not GraphQL) — GraphQL is read-only + swappability
>        * Content Fragments for persistence; text-ID relationships (not content references)
>        * State machine as pure domain logic (no AEM deps) — enables fast unit tests
>        * changeStatus as the single enforcement path; dedicated /status + /assignee endpoints
>        * Comments allowed on terminal tickets (asymmetry vs edits) — rationale
>        * Client-side priority/sort filtering vs server-side status filter — asymmetry + rationale
>        * /me endpoint for createdBy (authenticated user)
>        * Domain exception hierarchy -> HTTP status mapping
>    - Package structure (com.mysite.core.*) and module map (which module holds what).
>    - Known limitations / future improvements (e.g., ID counter not atomic; DB adapter not
>      implemented but designed for; ?priority= not server-side yet).
>
> 2. ui-flow.md — UI Screens & User Journeys
>    Sections:
>    - Screens/views: list view, detail view, create/edit form, plus controls
>      (status change, reassign, add comment). Note it's a single-page (SPA-style) app on a
>      static template at /content/assessment/us/en/tickets.html with ?id= view switching.
>    - User journeys (step-by-step): create a ticket; view + comment; reassign; move through the
>      status lifecycle to Closed; search/filter/sort.
>    - A simple flow diagram (ASCII) list <-> detail with actions.
>    - Note enhancements from 6.2 (sort, filters, result count, toasts, confirmations).
>
> 3. test-strategy.md — Testing Approach
>    Sections:
>    - Scope decision: Java UNIT tests only (core/src/test); it.tests and ui.tests OUT OF SCOPE
>      (state rationale honestly for a learning project).
>    - What is unit-tested: TicketStateMachine (full transition matrix), TicketService, CommentService.
>    - Coverage table: component | key scenarios | test class.
>    - State machine coverage: all valid transitions + representative invalid (map to AC-22–AC-35).
>    - How to run: mvn test.
>    - MANUAL verification: the REST endpoints and full flow were verified via curl/browser.
>      Include a few representative curl examples proving the state machine over HTTP
>      (a 200 valid transition and a 409 invalid transition) — use the curl commands actually used.
>    - What is NOT tested and why (honest).
>
> 4. debugging-notes.md — Issues & Resolutions
>    - Based on the prompt history / ai-prompts/debugging (if any) and the actual build journey,
>      document notable issues encountered and how they were resolved. Examples to check for:
>      CF write API quirks, OSGi component unsatisfied (service user mapping), servlet routing
>      conflicts, clientlib/webpack wiring, /home/users read ACL, Jackson Instant serialization,
>      Java 21 vs 17. For each: symptom -> root cause -> fix.
>    - If few issues were logged, document the ones that did occur honestly; do NOT fabricate.
>
> Constraints:
> - Accurate to the real implementation; no invented features.
> - Tables + ASCII diagrams; clear headers; date + purpose at top of each doc.
> - Where a decision has a trade-off, state BOTH sides briefly (shows understanding).
>
> After generating:
> - List the four files created.
> - Flag anything you were UNSURE about so I can verify/correct (accuracy matters — these are graded).
> When done, remind me to save this prompt + response summary to ai-prompts/documentation/.

---

## AI response summary

Created/expanded four lifecycle documents at the repo root from the actual codebase, planning docs (`requirements-analysis.md`, `api-contract.md`, `data-model.md`), and `ai-prompts/debugging/` + sprint prompt history: **`design-notes.md`** (replaced 2.1.1 stub — architecture overview, ASCII layer diagram, ten design decisions with trade-offs, package/module map, known limitations); **`ui-flow.md`** (SPA list/detail/form views, five user journeys, ASCII flow diagram, Sprint 6.2 enhancements); **`test-strategy.md`** (unit-test-only scope, coverage table ~96 tests across three classes, AC-22–AC-35 mapping, `mvn test`, representative curl 200/409 examples from Sprint 5.1); **`debugging-notes.md`** (eight documented issues: CFM vault/granite, service user ACLs, user API query, suffix-vs-filter routing, clientlib load, CSRF, create redirect, toast CSS). Updated `implementation-plan.md` — 8.1.1 complete; Active Sprint 8.1; Active Task 8.1.2. Flagged uncertainties for developer review: test counts not re-run in sandbox, curl examples need local SDK verification, api-contract suffix vs routing-filter mismatch, Jackson/Java 21 documented as preventive not incident-based.

---

## Artifacts produced

| File | Change |
|------|--------|
| `design-notes.md` | Expanded — architecture, design decisions, module map, limitations |
| `ui-flow.md` | Created — screens, journeys, flow diagram, 6.2 enhancements |
| `test-strategy.md` | Created — scope, coverage, AC mapping, curl examples |
| `debugging-notes.md` | Created — eight issues (symptom → root cause → fix) |
| `implementation-plan.md` | Updated — 8.1.1 complete; Active Task 8.1.2 |
| `ai-prompts/design/02-task-8.1.1-lifecycle-docs.md` | Created (this file) |

---

## Developer verification flags (from AI response)

| Item | Action needed |
|------|----------------|
| Test counts (62 / 23 / 11) | Re-run `mvn test -pl core` locally |
| curl 409/200 examples | Verify against real `TKT-*` on local SDK |
| api-contract suffix vs routing filters | Consider aligning contract in later task |
| Jackson `Instant` | Confirm no separate live bug to document |
| `/home/users` read ACL | Confirm matches deployed repoinit |
| Terminal-ticket UI behavior | Confirm edit disabled, comments allowed in browser |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 8.1 / Task 8.1.1 | Complete |
| DOD-1 (Sprint 8.1) | Four lifecycle artifacts with date + purpose headers |
| 07-documentation.mdc | design-notes, ui-flow, test-strategy, debugging-notes |
| Downstream | 8.1.2 — code-review-notes.md, pr-description.md |
