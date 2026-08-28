# Final AI Usage Summary — AEM Support Ticket Management System

**Date:** 2026-08-27  
**Purpose:** Consolidated record of how AI tools were used across the full SDLC, how output was validated, and a responsible-use statement for the practical assessment.

**Sprint/Task:** 8.1 / 8.1.4

---

## Tools & Roles

| Tool | Role in this project |
|------|----------------------|
| **Cursor** | Primary development environment; code generation, refactoring, documentation drafting; governed by `.cursor/rules/` |
| **External planning assistant** | Early requirements/architecture brainstorming; outputs formalized in Sprint 1.1 and saved under `ai-prompts/planning/` |
| **Maven + AEM SDK** | Build/deploy — human-operated validation layer (not AI) |
| **Developer** | Scope decisions, runtime debugging, Quality Gate sign-off, prompt-history curation, rejection of incorrect AI output |

AI was a **drafting and acceleration** tool. No sprint closed on AI output alone without build/test/browser verification.

---

## AI Usage by SDLC Phase

| Phase | Sprints | How AI was used | How output was validated / corrected |
|-------|---------|-----------------|--------------------------------------|
| **Planning** | 1.1 | Drafted `requirements-analysis.md`, `acceptance-criteria.md`, `data-model.md`, `api-contract.md`; reconciled `implementation-plan.md` (1.1.5) | Cross-doc consistency check; removed ticket delete + separate search endpoint; human review of FR/AC traceability |
| **Design / architecture** | 1.1, 2.1, 8.1 | Module map in `design-notes.md`; architecture decisions doc; trade-off tables | Aligned to `.cursor/rules/01-architecture.mdc`; Sprint 8.1 required code cross-check, not memory |
| **Implementation** | 2.1–6.2 | CFMs, repoinit, repositories, services, servlets, routing filters, TypeScript UI (~33 saved implementation prompts) | `mvn clean install`; OSGi Active; curl per endpoint; browser E2E; developer fixes (CFM XML, service user, filters) |
| **Testing** | 4.1, 7.1 | Generated `TicketStateMachineTest`; expanded service tests in 7.1 | `mvn test -pl core`; Sprint 7.1 audit — expand gaps, do not rewrite passing tests |
| **Debugging** | 2.1–6.2 | AI diagnosed from symptoms/logs when prompted (CSRF, clientlib, routing) | Developer verified in AEM; fixes recorded in `ai-prompts/debugging/` (7 files) |
| **Code review** | 8.1 | Drafted `code-review-notes.md`, `pr-description.md` from actual codebase | Honest improvement areas (comment `createdBy` trust, in-memory queries) flagged for human review |
| **Documentation** | 2.1, 8.1 | README, lifecycle docs, reflection, this summary | "Base on ACTUAL project"; verification flags on uncertain README steps; developer edits `candidate-info.md`, `reflection.md` |

---

## Prompt Discipline

### Rules-first

`.cursor/rules/` loaded every session:

| Rule file | Effect |
|-----------|--------|
| `00-project-overview.mdc` | Scope, tech stack, namespaces |
| `01-architecture.mdc` | Repository Pattern, DTO boundary |
| `02-state-machine.mdc` | Exact transitions; dedicated class |
| `07-documentation.mdc` | Lifecycle artifacts, prompt saving |
| `sprint-context.mdc` | `implementation-plan.md` as source of truth |

### Sprint-based with Quality Gates

| # | Sprint | QG criterion |
|---|--------|--------------|
| 1 | 1.1 | Planning docs mutually consistent |
| 2 | 2.1 | `mvn clean install`; bundle Active |
| 3 | 3.1 | Create/read Ticket + Comment via repo |
| 4 | 4.1 | State machine unit tests green |
| 5 | 5.1 | All endpoints via curl/Postman |
| 6 | 6.1 | Full browser E2E flow |
| 7 | 6.2 | Enhancements + no regressions |
| 8 | 7.1 | `mvn test` green |
| 9 | 8.1 | Lifecycle artifacts + README verify |

### Prompt history capture

| Artifact | Count | Location |
|----------|-------|----------|
| Sprint prompt-history files | **8** (1.1–7.1 incl. 6.2) | `prompt-history/sprint-*.md` |
| Sprint 8.1 log | Pending QG | `prompt-history/sprint-8.1.md` (at Quality Gate) |

### Categorized task prompts (`ai-prompts/`)

| Category | Files (approx.) | Examples |
|----------|-----------------|----------|
| `planning/` | 5 | Requirements, AC, data model, API contract |
| `implementation/` | 33 | CF repos, servlets, UI tasks 01–33 |
| `testing/` | 3 | State machine audit, service tests |
| `debugging/` | 7 | CFM vault, service user, CSRF, clientlib |
| `design/` | 1 | Task 8.1.1 lifecycle docs |
| `documentation/` | 3 | Module map, 8.1.1, 8.1.3 |
| `code-review/` | 1 | Task 8.1.2 |
| `tool-specific/` | 1 | `cursor-workflow.mdc` (stub) |
| **Total** | **~53** | |

### Fresh-session context management

Each sprint started with a **priming prompt**: read rules + `implementation-plan.md` Current Status + relevant docs → summarize back → wait for task prompt. This reduced carry-over hallucination from prior sprints.

---

## Examples of Correcting or Rejecting AI Output

| Instance | AI output | Human action | Evidence |
|----------|-----------|--------------|----------|
| **CFM granite namespace** | CFM XML with `<granite:data>` but no `xmlns:granite` | Developer added namespace; build passed | `ai-prompts/debugging/01-cfm-vault-filter-granite-namespace.md` |
| **Vault DAM filter** | Archetype exclude blocked CF paths | Developer commented out exclude in `filter.xml` | Same |
| **Suffix servlet routing** | Registered `TicketByIdServlet` as suffix servlet per contract | **Rejected** after live SDK 404; implemented `TicketByIdRoutingFilter` et al. | `prompt-history/sprint-5.1.md` |
| **Implementation plan drift** | Task 3.1.2 mentioned ticket delete | Removed in 1.1.5 reconciliation | `implementation-plan.md` cross-check table |
| **State machine tests (7.1)** | Existing 54 tests from 4.1.6 | Audited + 8 gap tests; did **not** rewrite passing suite | `prompt-history/sprint-7.1.md` |
| **Clientlib on page** | Page policy assumed to load clientlib | Developer/AI fix: explicit include in `ticketapp.html` after Network tab showed no JS | `ai-prompts/debugging/04-clientlibs-js-css-issue.md` |
| **Lifecycle docs (8.1)** | Risk of invented features | Prompt required ACTUAL codebase + "flag if unsure" | `ai-prompts/design/02-task-8.1.1-lifecycle-docs.md` |
| **HTTP transition curls (5.1.4)** | Documented full curl matrix | Some blocked when DAM empty — unit tests used as proof instead | `ai-prompts/implementation/18-ticket-status-servlet.md` |

> **Note for developer:** Confirm whether an external planning assistant was used for Sprint 1.1 only or also later — adjust "Tools" section if needed.

---

## Project Metrics

| Metric | Value | Source |
|--------|-------|--------|
| Sprints completed (implementation) | 8 (+ 8.1 in progress) | `implementation-plan.md` |
| Functional requirements | FR-1–FR-19 | `requirements-analysis.md` |
| Acceptance criteria | AC-1–AC-51 | `acceptance-criteria.md` |
| REST endpoints | 10 (+ `/me`) | `api-contract.md` |
| Java unit tests (assessment scope) | ~96 | 62 + 23 + 11 (`prompt-history/sprint-7.1.md`) |
| Sprint prompt-history files | 8 | `prompt-history/README.md` |
| Categorized `ai-prompts/` files | ~53 | `ai-prompts/` tree |
| Documented debugging incidents | 8 | `debugging-notes.md` |
| OSGi servlets + routing filters | 8 servlets + 5 filters | `design-notes.md` |

---

## Honest Assessment: Strengths & Limits

### Where AI-assisted development excelled

- **Speed on repetitive Java/TS** — DTOs, mappers, servlet boilerplate, error JSON handling
- **Consistency when rules were explicit** — state machine table, AC-mapped tests, package structure
- **Documentation drafts** — tables, traceability, curl examples (with human fact-check)
- **Debugging assistance** — given AEM log lines or Network tab symptoms, AI proposed plausible fixes quickly

### Where AI fell short

- **AEM runtime behavior** — suffix servlet registration, CSRF, service-user ACLs, clientlib inclusion on editable templates
- **XML/content-package edge cases** — granite namespaces, vault filters
- **Silent "working" code** — compiles and even unit-tests pass while integration fails (empty list from `LoginException` swallowed in repository)
- **Documentation fabrication** — without "base on actual project" constraints, AI invents plausible but wrong setup steps
- **Architectural judgment** — scope cuts (no GraphQL, no DB, unit-only tests) required human product decisions

### Importance of human oversight

This project would not have passed Quality Gates with copy-paste AI output. Every sprint's gate depended on **developer verification on a running AEM SDK**. The prompt-history and `ai-prompts/debugging/` folders exist because AI was wrong often enough that the corrections are part of the learning evidence.

---

## Responsible Use Statement

| Principle | How it was applied |
|-----------|-------------------|
| **No secrets committed** | Repoinit uses obvious placeholder password `changeme-local-dev-only`; README states local `admin:admin` is SDK default only |
| **AI output reviewed and owned** | Developer ran builds, tests, curl, and browser checks; fixes documented with root cause |
| **Understanding generated code** | Architecture rules required knowing *why* (e.g., `changeStatus` only path, terminal comment asymmetry) — not just that code exists |
| **Traceability** | FR → sprint → prompt → artifact chain via `implementation-plan.md` and prompt history |
| **Honest documentation** | Sprint 8.1 docs flag uncertain README steps; code review lists real debt |
| **No undeclared automation** | `it.tests` / `ui.tests` unused by choice; no hidden CI bypass |

The developer retains responsibility for all code and documentation in this repository. AI accelerated drafting; humans approved what shipped.

---

## Related Documents

| Document | Purpose |
|----------|---------|
| [tool-workflow.md](tool-workflow.md) | Day-to-day AI workflow |
| [reflection.md](reflection.md) | Personal project reflection (editable) |
| [prompt-history/README.md](prompt-history/README.md) | Sprint transcript index |
| [code-review-notes.md](code-review-notes.md) | Technical self-review |
| [candidate-info.md](candidate-info.md) | Candidate placeholders |
