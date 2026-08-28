# Cursor Workflow — AEM Support Ticket Management System

**Purpose:** Explain how Cursor was used as the primary AI-assisted development environment for this assessment.

**Date:** 2026-08-27  
**Sprint/Task:** 8.1 / 8.1.5

---

> **Rules mirror:** See [`rules/`](rules/) for copies of `.cursor/rules/*.mdc` (authoritative source: `.cursor/rules/` at repo root).

---

## Overview

Cursor was the main IDE and AI pair-programming tool. Development followed a **rules-first**, **sprint-based** process with **Quality Gates**, captured in:

| Artifact | Location |
|----------|----------|
| Cursor rules (authoritative) | `.cursor/rules/` |
| Rules mirror (reviewers) | `ai-prompts/tool-specific/cursor-workflow/rules/` |
| Sprint plan | `implementation-plan.md` |
| Per-task prompts | `ai-prompts/<category>/` |
| Per-sprint transcripts | `prompt-history/sprint-X.X.md` |

See also [tool-workflow.md](../../../tool-workflow.md) and [final-ai-usage-summary.md](../../../final-ai-usage-summary.md) at repo root.

---

## Rules-First Setup

Before each sprint, `.cursor/rules/` were loaded (always-applied in Cursor). Key constraints:

- **Architecture:** Repository Pattern, DTO boundary, no GraphQL (`01-architecture.mdc`, `00-project-overview.mdc`)
- **State machine:** Exact transition table in dedicated `TicketStateMachine` (`02-state-machine.mdc`)
- **Sprint tracking:** Read `implementation-plan.md` Current Status at task start (`sprint-context.mdc`)
- **Documentation:** Lifecycle artifacts and prompt saving (`07-documentation.mdc`)
- **Prompt history:** During-task notices and sprint-log generation rules (`prompt-history-*.mdc`)

This reduced AEM API hallucination and kept package paths consistent (`com.mysite.core`, `/apps/assessment`).

---

## Sprint-Based Development with Quality Gates

| Sprint | Focus | Quality Gate |
|--------|-------|--------------|
| 1.1 | Planning docs | Docs mutually consistent |
| 2.1 | Scaffold, CFMs, repoinit | `mvn clean install`; bundle Active |
| 3.1 | CF repositories | Create/read Ticket + Comment |
| 4.1 | State machine + services | State machine tests green |
| 5.1 | REST API | All endpoints via curl |
| 6.1 | UI | Browser E2E flow |
| 6.2 | UI enhancements | No regressions |
| 7.1 | Unit tests | `mvn test` green |
| 8.1 | Documentation | Lifecycle artifacts + README |

Each task updated `implementation-plan.md` when complete. AI did not advance sprints without developer review.

---

## Fresh-Session-Per-Sprint Context

New Cursor chat at each sprint start. See [session-management.md](session-management.md) for the priming prompt pattern.

Benefits observed:

- Reduced stale context from prior sprints
- Forced re-read of `implementation-plan.md` Current Status
- Clear task boundaries (e.g., "Task 5.1.4 only — do not write docs yet")

---

## Prompt History Capture

Two mechanisms (see [prompt-history-mechanism.md](prompt-history-mechanism.md)):

1. **During task:** Developer saves meaningful prompts to `ai-prompts/<category>/`
2. **At Quality Gate:** `prompt-history/sprint-X.X.md` generated from Cursor transcript (verbatim prompts)

---

## How Prompts Were Categorized (`ai-prompts/`)

| Folder | Contents |
|--------|----------|
| `planning/` | Sprint 1.1 requirements, AC, data model, API contract |
| `design/` | Sprint 8.1 design/lifecycle doc prompts |
| `implementation/` | Sprints 2.1–6.2 code tasks (33 files) |
| `testing/` | State machine audit, service tests |
| `debugging/` | Runtime fixes (CFM, service user, CSRF, clientlib, …) |
| `code-review/` | Sprint 8.1.2 self-review prompt |
| `documentation/` | Module map, README, reflection, consolidation |
| `tool-specific/cursor-workflow/` | This folder |

---

## Related Files

- [session-management.md](session-management.md) — priming prompt example
- [prompt-history-mechanism.md](prompt-history-mechanism.md) — sprint log rules
- [rules/](rules/) — mirror of `.cursor/rules/`
