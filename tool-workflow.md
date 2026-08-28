# Tool Workflow — AI-Assisted Development

**Purpose:** Document how AI tools were used across the SDLC for the AEM Support Ticket Management System assessment.

**Date:** 2026-08-27

---

## Tools Used

| Tool | Role |
|------|------|
| **Cursor** | Primary IDE and implementation assistant; governed by `.cursor/rules/` (architecture, state machine, documentation, sprint context, prompt history) |
| **External planning assistant** | Early architecture and requirements shaping; outputs captured in `ai-prompts/planning/` and formalized in Sprint 1.1 docs |
| **Maven / AEM SDK** | Build, deploy, and manual verification (human-driven) |
| **Local AEM author** | Runtime validation of repoinit, servlets, UI |

No separate AI tool wrote production code without developer review. All AI output was treated as a draft.

---

## Workflow Overview

```
.cursor/rules/  +  implementation-plan.md  (source of truth)
        │
        ▼
Fresh Cursor session per sprint  →  priming prompt (read rules + plan + codebase)
        │
        ▼
Task-by-task implementation  →  ai-prompts/<category>/  (prompt + summary saved)
        │
        ▼
Quality Gate  →  mvn test / manual verify  →  prompt-history/sprint-X.X.md
```

### Rules-first setup

Before coding, `.cursor/rules/` established non-negotiables:

- Repository Pattern and DTO boundary (`01-architecture.mdc`)
- State machine transitions and `InvalidTransitionException` (`02-state-machine.mdc`)
- Documentation and lifecycle artifacts (`07-documentation.mdc`)
- Sprint/task tracking via `implementation-plan.md` (`sprint-context.mdc`)

### Sprint-based development

Work followed `implementation-plan.md` sprints **1.1 → 8.1**, each ending in a **Quality Gate**:

| Sprint | Deliverable |
|--------|-------------|
| 1.1 | Planning docs (FR, AC, data model, API contract) |
| 2.1 | CFMs, repoinit, DTOs, repository interfaces |
| 3.1 | CF repository adapters |
| 4.1 | State machine, services, domain exceptions |
| 5.1 | REST servlets |
| 6.1 / 6.2 | TypeScript UI + enhancements |
| 7.1 | Java unit tests |
| 8.1 | Lifecycle documentation |

### Prompt history

- **Per sprint:** `prompt-history/sprint-X.X.md` generated at Quality Gate (verbatim user prompts, summarized AI responses)
- **Per task:** `ai-prompts/<category>/` — developer-saved prompts and response summaries during work

---

## Context Management

| Technique | How it was used |
|-----------|-----------------|
| **Fresh session per sprint** | New Cursor chat at sprint start with a priming prompt: read rules, `implementation-plan.md` Current Status, relevant planning docs, and summarize back before coding |
| **`implementation-plan.md`** | Single source of truth for Active Sprint / Active Task; updated when tasks complete |
| **Planning docs** | `requirements-analysis.md`, `acceptance-criteria.md`, `data-model.md`, `api-contract.md` referenced by AI and developer for FR/AC traceability |
| **Task-scoped prompts** | Narrow prompts per task ID (e.g., `5.1.4 TicketStatusServlet`) to limit scope creep |
| **No silent guessing** | Documentation tasks explicitly require flagging uncertainty (see Sprint 8.1 README) |

---

## How AI Output Was Validated & Corrected

Real examples from the project where human review caught or fixed AI output:

| Issue | AI output problem | Human correction | Recorded in |
|-------|-------------------|------------------|-------------|
| CFM XML | Missing `xmlns:granite` on CFM `.content.xml` | Developer added namespace; build passed | `ai-prompts/debugging/01-cfm-vault-filter-granite-namespace.md` |
| Vault filter | DAM exclude blocked CF deployment | Developer adjusted `ui.content/.../filter.xml` | Same |
| Service user | Empty ticket list / 500 on API | Repoinit ACLs + OSGi mapping fixed | `ai-prompts/debugging/03-service-user-issue-fixes.md` |
| User API | Noisy/empty assignee list | `AemUserRepository` query tightened to `rep:User` + email | `ai-prompts/debugging/02-user-api-fixes.md` |
| Servlet routing | Suffix servlet 404 on local SDK | Switched to routing filters | `prompt-history/sprint-5.1.md` |
| Clientlib | API never called — JS not on page | Added clientlib include to `ticketapp.html` | `ai-prompts/debugging/04-clientlibs-js-css-issue.md` |
| CSRF | Browser POST 403 | Added `csrf.ts` + `fetchWithCsrf` | `ai-prompts/debugging/05-csrf-token-post.md` |
| State machine tests | Sprint 7.1 audit found label/gap issues | Expanded tests; did not rewrite passing suite | `prompt-history/sprint-7.1.md` |
| Documentation accuracy | Risk of inventing features in Sprint 8.1 | Explicit “base on ACTUAL project” + verification flags | `ai-prompts/design/02-task-8.1.1-lifecycle-docs.md` |

**Fabrication checks:** Lifecycle docs (8.1) required cross-check against code, prompt history, and debugging logs — not AI memory alone.

---

## Prompt Categories & Locations

| Category | Folder | Examples |
|----------|--------|----------|
| Planning | `ai-prompts/planning/` | Requirements, AC, data model, API contract |
| Design | `ai-prompts/design/` | Task 8.1.1 lifecycle doc prompt |
| Implementation | `ai-prompts/implementation/` | Repositories, servlets, UI tasks (01–33) |
| Testing | `ai-prompts/testing/` | State machine audit, service tests |
| Debugging | `ai-prompts/debugging/` | CFM vault, service user, CSRF, clientlib |
| Code review | `ai-prompts/code-review/` | Task 8.1.2 self-review prompt |
| Documentation | `ai-prompts/documentation/` | Module map (2.1.1) |

Sprint transcripts: `prompt-history/sprint-1.1.md` … `sprint-7.1.md` (index in `prompt-history/README.md`).

---

## What AI Did Well vs Where Human Judgment Was Essential

### AI did well

- Scaffolding boilerplate: DTOs, repository interfaces, servlet skeletons, HTL component structure
- Generating planning doc drafts with FR/AC tables and api-contract endpoint lists
- State machine implementation and exhaustive unit test matrices when rules were explicit
- Documenting debugging sessions when prompted with logs/symptoms
- Consistent package naming and layering when `.cursor/rules/` were loaded

### Human judgment was essential

- **Verifying on running AEM SDK** — suffix servlet failure, CSRF, clientlib not loading, service-user ACLs
- **Repoinit and security** — least-privilege ACLs, demo password placeholders, not committing secrets
- **Rejecting wrong approaches** — suffix-only servlet routing after live 404 evidence
- **Scope control** — keeping `it.tests`/`ui.tests` out of scope; not implementing DB adapter
- **Acceptance decisions** — terminal-ticket comment asymmetry, client-side sort vs server filter
- **Documentation honesty** — flagging uncertain README steps, real improvement areas in code review
- **Quality Gates** — `mvn test`, curl verification, browser E2E before marking sprints complete

---

## README Verification Checklist (clean setup)

Run on a **fresh AEM SDK author** before submission and check each item:

| # | Step | Confirmed? |
|---|------|------------|
| 1 | AEM author running at `localhost:4502` | ☐ |
| 2 | `mvn clean install -PautoInstallSinglePackage` → SUCCESS | ☐ |
| 3 | OSGi bundle `ai-practical-assessment.core` Active | ☐ |
| 4 | Repoinit: `agent-1`, `agent-2` exist; password `changeme-local-dev-only` works | ☐ |
| 5 | CFM models ticket + comment visible in AEM UI | ☐ |
| 6 | `curl GET /bin/api/v1/tickets` → `200` (empty `[]` OK) | ☐ |
| 7 | `curl POST /bin/api/v1/tickets` → `201` (no 403/500) | ☐ |
| 8 | Tickets page loads UI + Network shows API call | ☐ |
| 9 | `mvn test -pl core` → all assessment tests pass | ☐ |

---

## Related Documents

- [README.md](README.md) — setup and run
- [implementation-plan.md](implementation-plan.md) — sprint tree
- [final-ai-usage-summary.md](final-ai-usage-summary.md) — *(Sprint 8.1.4 — pending)*
