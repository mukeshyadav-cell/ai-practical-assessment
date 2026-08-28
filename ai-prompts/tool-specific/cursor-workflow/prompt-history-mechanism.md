# Prompt History Mechanism

**Purpose:** Explain how `.cursor/rules/prompt-history-during-task.mdc` and `prompt-history-sprint-log.mdc` capture prompts and response summaries across the project.

**Date:** 2026-08-27  
**Sprint/Task:** 8.1 / 8.1.5

**Authoritative rules:** `.cursor/rules/prompt-history-during-task.mdc`, `.cursor/rules/prompt-history-sprint-log.mdc` (mirrored in [`rules/`](rules/))

---

## Two-Layer Capture

| Layer | When | Output | Who triggers |
|-------|------|--------|--------------|
| **During task** | After each meaningful task | Developer saves to `ai-prompts/<category>/` | Developer (+ AI reminder in task summary) |
| **Sprint Quality Gate** | End of sprint | `prompt-history/sprint-X.X.md` | Rule applied at QG; transcript from Cursor |

Both layers are required for a complete audit trail.

---

## During-Task Rule (`prompt-history-during-task.mdc`)

### When it applies

After every task completion, when Cursor stops for developer review.

### What the AI does

Appends a **prompt log notice** to the task summary:

```
---
> **Prompt log:** This prompt will be captured in the sprint log when the Quality Gate passes.
> Meaningful: Yes / No — reason
> Response summary: One sentence — concrete artifact produced
---
```

### Meaningful vs not

| Meaningful **Yes** | Meaningful **No** |
|--------------------|-------------------|
| Task instruction or task ID | Single word: "proceed", "ok", "approved" |
| Architectural decision | Status check only |
| Iteration on prior AI output | Acknowledgement with no decision |

### Developer action

Save the prompt + response summary to `ai-prompts/<category>/` (planning, implementation, testing, debugging, code-review, documentation, design).

**Typical file format** (see `ai-prompts/implementation/10-ticket-state-machine.md`):

- Title, date, purpose, Sprint/Task, category
- `## Prompt (verbatim)`
- `## AI response summary`
- Optional: artifacts table, traceability

> **Note:** Not every early prompt file includes a "What I accepted / changed" section — sprint logs (`prompt-history/`) are the richer source for accept/reject detail.

---

## Sprint Log Rule (`prompt-history-sprint-log.mdc`)

### When it applies

At the **end of each sprint**, when the Quality Gate passes and exit criteria are met.

### Process

1. **Locate transcript** — Cursor conversation at `agent-transcripts/<uuid>/<uuid>.jsonl`
2. **Filter** — meaningful user prompts only (length, task ID, decision words, role persona)
3. **Write** `prompt-history/sprint-X.X.md` — structured markdown with:
   - Goal, tasks completed table
   - Prompts log (verbatim prompt + AI output bullets + accepted/rejected)
   - Files changed, requirements traced, Quality Gate result
4. **Update** `prompt-history/README.md` Session Index

### Honesty rule

- Never fabricate prompts
- Preserve typos in verbatim prompts
- AI responses summarized, not pasted in full
- **Do not create sprint log mid-sprint** — only at Quality Gate

---

## How They Work Together

```
Task prompt → AI implements → during-task notice
                    ↓
         Developer saves ai-prompts/<category>/NN-task.md
                    ↓
         (repeat per task in sprint)
                    ↓
         Quality Gate passes
                    ↓
         prompt-history/sprint-X.X.md  (verbatim from transcript)
                    ↓
         prompt-history/README.md updated
```

---

## Current Project Status (Task 8.1.5)

| Sprint log | Status |
|------------|--------|
| sprint-1.1.md … sprint-7.1.md | Present |
| **sprint-8.1.md** | **MISSING** — create at Sprint 8.1 Quality Gate from Cursor transcript |

---

## Related

- [prompt-history/README.md](../../../prompt-history/README.md) — Session Index
- [session-management.md](session-management.md) — fresh session priming
- [README.md](README.md) — Cursor workflow overview
