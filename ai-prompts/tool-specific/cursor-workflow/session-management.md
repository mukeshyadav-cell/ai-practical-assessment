# Session Management — Fresh Session Per Sprint

**Purpose:** Document the fresh-session-per-sprint approach and provide an example priming prompt used in this project.

**Date:** 2026-08-27  
**Sprint/Task:** 8.1 / 8.1.5

---

## Approach

| Practice | Rationale |
|----------|-----------|
| **New Cursor chat each sprint** | Avoids context bleed (e.g., Sprint 5 servlet details polluting Sprint 6 UI) |
| **Priming prompt before coding** | AI reads rules + plan + codebase summary; developer confirms Active Task |
| **Task-scoped follow-ups** | Narrow prompts with task ID (e.g., `Task 5.1.4`) limit scope creep |
| **"Summarize back — do NOT write yet"** | Catches misunderstanding before code generation |
| **Save prompts after meaningful tasks** | `ai-prompts/<category>/` preserves evidence for Sprint 8.1 docs |

---

## Workflow Per Sprint

```
1. Open new Cursor chat
2. Send priming prompt (below) — wait for summary
3. Developer confirms or corrects Active Sprint/Task
4. Send task prompt(s) one at a time
5. Review AI output; run build/tests/browser checks
6. Save prompt + summary to ai-prompts/ when meaningful
7. Mark task complete in implementation-plan.md
8. At Quality Gate → prompt-history/sprint-X.X.md
```

---

## Example Priming Prompt (Sprint 8.1 — verbatim pattern)

Used at the start of Sprint 8.1 (adapt sprint number and file list):

```markdown
Starting a fresh Cursor session for Sprint 8.1 (Documentation & Reflection — final sprint).

READ before we begin:
- .cursor/rules/ (ESPECIALLY 07-documentation.mdc)
- implementation-plan.md (Sprint 8.1 tasks + full project history; confirm Active Task = 8.1.1)
- All planning docs: requirements-analysis.md, acceptance-criteria.md, data-model.md, api-contract.md
- The full codebase (core services, servlets, repositories, state machine, UI in ui.frontend)
- prompt-history/ (sprint-1.1 through sprint-7.1) and ai-prompts/ folders

Project summary for context:
- AEM Support Ticket Management System (AEMaaCS, archetype 57, Java com.mysite.core,
  content /assessment). Content Fragments for persistence, Sling Servlets REST at /bin/api/v1/*,
  Repository Pattern (swappable to DB), TypeScript UI, state machine enforced + unit tested.
- Sprints completed: 1.1 planning, 2.1 foundation, 3.1 repositories, 4.1 state machine+services,
  5.1 REST API, 6.1 UI, 6.2 UI enhancements, 7.1 unit tests.

Summarize back to confirm:
1. Active Sprint / Active Task (8.1.1)
2. The list of lifecycle documentation artifacts still to create
3. Which design decisions were made across the project (...)

Do NOT write docs yet — wait for my Task 8.1.1 prompt.
```

---

## Example Task Prompt (after priming)

```markdown
Task 8.1.1 (Sprint 8.1): Create design-notes.md, ui-flow.md, test-strategy.md, debugging-notes.md
— all at the repo root.

Follow all rules in .cursor/rules/ (07-documentation). Base the content on the ACTUAL project...
```

---

## What Did Not Work Well Without Fresh Sessions

- Carrying servlet routing debugging context into UI tasks caused unnecessary backend suggestions
- Long chats increased risk of AI "remembering" wrong api-contract details from earlier iterations

---

## Related

- [README.md](README.md) — Cursor workflow overview
- [prompt-history-mechanism.md](prompt-history-mechanism.md) — sprint log capture
- [../../../tool-workflow.md](../../../tool-workflow.md) — full AI SDLC workflow
