# AI Prompt — Task 8.1.3: README, candidate info & tool workflow

**Date:** 2026-08-27
**Purpose:** Record the documentation prompt and AI response summary for Sprint 8.1 Task 8.1.3.

**Sprint/Task:** 8.1 / 8.1.3
**Category:** documentation
**Meaningful:** Yes — Task 8.1.3 instruction with README accuracy requirements, three named artifacts, and explicit verification flags.

---

## Prompt (verbatim)

> Task 8.1.3 (Sprint 8.1): Create README.md, candidate-info.md, and tool-workflow.md at the
> repo root. The README setup instructions MUST be accurate and work from scratch — this is
> critical (broken setup is heavily penalized).
>
> Follow all rules in .cursor/rules/ (07-documentation). Base everything on the ACTUAL project.
> Each doc: clear title + purpose. Be accurate; do NOT invent steps or features.
>
> Create:
>
> 1. README.md — the primary project document. Sections:
>    - Title + one-paragraph description (AEM Support Ticket Management System on AEMaaCS).
>    - Features (mandatory core): create/list/view/update/reassign tickets, comments,
>      keyword search, status filter, enforced state machine. Note stretch enhancements
>      (sort, priority filter, /me, toasts, confirmations).
>    - Tech stack: AEMaaCS SDK (archetype 57), Java (state actual version; note 17-compatible),
>      Maven multi-module, Content Fragments, Sling Servlets, TypeScript UI.
>    - Architecture (brief; link to design-notes.md): layered + Repository Pattern (CF, swappable).
>    - Prerequisites: JDK version, Maven, AEMaaCS SDK (local Quickstart) running on author :4502.
>    - Setup & Run — EXACT, ordered, copy-pasteable steps:
>        a) Start the local AEMaaCS SDK author instance (port 4502).
>        b) Build + deploy: mvn clean install -PautoInstallSinglePackage
>        c) What gets provisioned automatically (repoinit): DAM folders, service user
>           assessment-service, seed users agent-1/agent-2, CFMs (Ticket, Comment).
>        d) Seed sample data: state whether sample tickets are created manually or via API.
>           Provide the exact steps/commands to create a couple of sample tickets
>           (curl POST examples to /bin/api/v1/tickets) so a reviewer can populate data quickly.
>        e) Open the app: http://localhost:4502/content/assessment/us/en/tickets.html
>           (note: log in as agent-1 for realistic createdBy, or admin/admin).
>    - Running tests: mvn test (state what is covered: state machine + services).
>    - API reference (brief; link to api-contract.md): list the /bin/api/v1/* endpoints in a table
>      (method | path | purpose) + a couple of representative curl examples (create ticket;
>      change status showing 200 valid and 409 invalid transition).
>    - Project structure: short module map (core, ui.apps, ui.content, ui.config, ui.frontend, ...).
>    - State machine: the allowed transitions table.
>    - Known limitations / out of scope: it.tests/ui.tests unused, DB adapter designed-not-built,
>      ID counter not atomic, no full auth layer.
>    - Documentation index: links to all lifecycle docs + prompt-history.
>    - IMPORTANT: no secrets in the README; seed passwords (if any) are demo placeholders — say so.
>
> 2. candidate-info.md — fill with clearly-labeled PLACEHOLDERS I will complete:
>    - Name, email, role/experience, date.
>    - Time spent (approx).
>    - Brief note on approach.
>    Mark placeholders like <YOUR NAME> so I can fill them in.
>
> 3. tool-workflow.md — how AI tools were used across the SDLC:
>    - Tools used: Cursor (with .cursor/rules/), plus an external planning assistant for
>      architecture/requirements (documented in ai-prompts/planning + design).
>    - Workflow: rules-first setup; sprint-based development with Quality Gates; prompt history
>      captured per sprint (prompt-history/) and categorized (ai-prompts/).
>    - How context was managed: fresh session per sprint; implementation-plan.md + rules as
>      source of truth; priming prompts.
>    - How AI output was validated/corrected: examples of reviewing/correcting AI (e.g., CFM XML,
>      state machine verification, fabrication checks in docs) — reference real instances from
>      prompt history.
>    - Categories of prompts (planning, design, implementation, testing, debugging, code-review,
>      documentation) and where they live.
>    - Honest note on what AI did well vs where human judgment was essential.
>
> Constraints:
> - README steps must be ACCURATE to how THIS project builds/deploys/seeds. If any step is
>   uncertain (e.g., exact seeding mechanism, whether sample data auto-loads), SAY SO and mark
>   it for me to verify — do NOT guess silently.
> - No secrets. Tables where helpful. Professional, concise.
>
> After generating:
> - List the three files.
> - CRITICAL: flag every step in the README you are NOT 100% sure works, so I can verify on a
>   clean setup before submission.
> When done, remind me to save this prompt + response summary to ai-prompts/documentation/.

---

## AI response summary

Replaced archetype **`README.md`** with project-specific documentation: features, tech stack (Java 21 / Maven release 11 / archetype 57 SDK API), architecture link, prerequisites, ordered setup (start AEM → `mvn clean install -PautoInstallSinglePackage` → repoinit provisioning table → manual curl seed tickets — **no auto-loaded sample CFs** → open `tickets.html`), `mvn test -pl core`, API endpoint table + curl examples (create, 200 valid transition, 409 invalid), module map, state machine table, limitations, documentation index, security note. Created **`candidate-info.md`** with `<YOUR NAME>` / `<YOUR EMAIL>` placeholders for time spent and approach. Created **`tool-workflow.md`** covering Cursor + `.cursor/rules/`, sprint Quality Gates, prompt history, real validation examples (CFM granite namespace, service user, CSRF, clientlib, suffix routing), prompt categories, AI strengths vs human judgment, and a 9-item README verification checklist. Updated `implementation-plan.md` — 8.1.3 complete; Active Task 8.1.4. Flagged 10 README steps requiring clean-SDK verification (AEM running before deploy, repoinit passwords, curl CSRF, TKT-1001 id, ticketapp via template structure, etc.).

---

## Artifacts produced

| File | Change |
|------|--------|
| `README.md` | Replaced archetype template — primary project document |
| `candidate-info.md` | Created — developer placeholders |
| `tool-workflow.md` | Created — AI SDLC workflow + verification checklist |
| `implementation-plan.md` | Updated — 8.1.3 complete; Active Task 8.1.4 |
| `ai-prompts/documentation/03-task-8.1.3-readme-candidate-tool-workflow.md` | Created (this file) |

---

## Developer verification flags (from AI response)

| # | README step / claim | Verify on clean SDK |
|---|---------------------|---------------------|
| 1 | AEM running before Maven deploy | Deploy fails if author down |
| 2 | Full `mvn clean install -PautoInstallSinglePackage` SUCCESS | npm/RAM/network |
| 3 | Seed users `changeme-local-dev-only` password | Repoinit login |
| 4 | `curl POST` without CSRF (Basic auth) | 403 possible — test |
| 5 | First ticket id `TKT-1001` | Counter behavior |
| 6 | `createdBy` = `admin` when curling as admin | JSON field |
| 7 | Tickets page renders `ticketapp` (template structure) | UI not empty |
| 8 | Node/npm auto via frontend-maven-plugin | Clean machine |
| 9 | No sample tickets auto-load | Empty list until POST |
| 10 | Maven deploy `admin:admin` | Local SDK only |

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 8.1 / Task 8.1.3 | Complete |
| DOD-2 (Sprint 8.1) | README setup documented (pending clean-SDK verify) |
| 07-documentation.mdc | README, candidate-info, tool-workflow |
| Downstream | 8.1.4 — reflection.md, final-ai-usage-summary.md |
