# Project Reflection — AEM Support Ticket Management System

**Date:** 2026-08-27  
**Purpose:** Honest reflection on what was built, what worked, what was hard, and what was learned — for developer review and personal editing.

**Sprint/Task:** 8.1 / 8.1.4

---

## What Was Built & Overall Outcome

Over nine sprints (1.1 planning through 8.1 documentation), this project delivered a **working mandatory-core** ticketing application on AEM as a Cloud Service:

- **Backend:** Content Fragment persistence, repository adapters, pure `TicketStateMachine`, `TicketService` / `CommentService`, ten REST endpoints at `/bin/api/v1/*`, domain exception → HTTP mapping
- **Frontend:** TypeScript SPA-style UI at `/content/assessment/us/en/tickets.html` with list, detail, create/edit, comments, status change, reassign, and Sprint 6.2 enhancements (sort, priority filter, `/me`, toasts)
- **Tests:** ~96 Java unit tests in `core` (state machine + services)
- **Process artifacts:** Planning docs, lifecycle documentation, eight sprint prompt-history files, 50+ categorized prompts in `ai-prompts/`

The outcome is **functionally complete for assessment scope**: a reviewer can deploy to local SDK, seed tickets via curl or UI, exercise the state machine, and trace requirements to code. It is **not production-hardened** — non-atomic ID counters, in-memory JCR scans, no integration/E2E automation, and no custom authorization layer remain documented limitations.

---

## What Went Well

### Rules-first Cursor setup reduced AEM hallucination

Loading `.cursor/rules/` (architecture, state machine, documentation, sprint context) at the start of each sprint gave the AI concrete constraints: package `com.mysite.core`, paths under `/apps/assessment`, **no GraphQL**, exact transition table, `InvalidTransitionException` → 409. When prompts referenced task IDs from `implementation-plan.md`, scope stayed bounded.

### Pure state machine made testing trivial

`TicketStateMachine` has zero AEM dependencies. Sprint 4.1 produced 54 tests; Sprint 7.1 audited and expanded to **62** without rewriting passing tests. Service tests use Mockito for repositories but exercise the **real** state machine inside `TicketServiceImpl`. This was the highest-confidence layer in the project.

### Repository Pattern kept layers clean

DTOs never leaked `ContentFragment` above `.repositories.impl`. OSGi `@Reference(target="(impl.type=contentfragment)")` made the swappability story real, not theoretical. When Sprint 5.1 added servlets, they only called services — no persistence logic in HTTP handlers.

### Planning reconciliation caught drift early

Sprint 1.1 task 1.1.5 explicitly reconciled `implementation-plan.md` against planning docs (removed ticket delete, merged search into `GET /tickets`, split status/assignee servlets). That prevented building the wrong API surface.

### Sprint Quality Gates forced verification

Each implementation sprint ended with a concrete gate: `mvn test`, curl on all endpoints, or browser E2E. AI could generate code quickly, but **gates** caught issues like suffix servlet 404s and missing clientlibs before moving on.

---

## Challenges & How They Were Overcome

| Challenge | When | What happened | Resolution |
|-----------|------|---------------|------------|
| **CFM XML / Vault filter** | Sprint 2.1 | `mvn clean install` failed; CFMs blocked by missing `xmlns:granite` and archetype DAM filter exclude | Developer fixed namespace + `filter.xml` (`ai-prompts/debugging/01-…`) |
| **Service user / OSGi mapping** | Sprint 3.1 / 5.1 | `GET /tickets` returned `[]` or 500; CF writes failed | Repoinit ACLs on DAM + `/var/assessment`; `ServiceUserMapperImpl` mapping for `assessment-service` |
| **User API empty/noisy** | Sprint 5.1 | Assignee picker broken | `AemUserRepository` tightened to `rep:User` + `profile/email` filter |
| **Suffix servlet routing** | Sprint 5.1 | `GET /tickets/{id}` returned HTML 404; `suffix=null` on local SDK | Rejected pure suffix approach after live failure; added `*RoutingFilter` dispatchers |
| **Clientlib not on page** | Sprint 6.1 | Network tab showed no API calls; only `clientlib-base` loaded | Added `assessment.ticketing` include to `ticketapp.html` |
| **CSRF on browser POST** | Sprint 6.1 | `POST /tickets` → 403; AEM log: empty CSRF token | Added `csrf.ts` / `fetchWithCsrf` in `api.ts` |
| **Create → detail navigation** | Sprint 6.1 | POST succeeded but detail did not init | Full page reload via `window.location.href` after create |
| **Doc fabrication risk** | Sprint 8.1 | AI could invent features in lifecycle docs | Prompts required "base on ACTUAL project" + explicit verification flags |

[ADD PERSONAL NOTE: Which of these issues cost you the most time, and what was your debugging process?]

---

## Where AI Helped vs Where Human Judgment Was Essential

### AI helped most (concrete examples)

| Area | Example |
|------|---------|
| **Boilerplate scaffolding** | DTOs, repository interfaces, servlet skeletons, HTL component shells, `ServletResponseUtil` error mapping |
| **Planning doc drafts** | FR/AC tables, api-contract endpoint list, data-model field tables (Sprint 1.1) |
| **State machine + tests** | `TicketStateMachine` `EnumMap` table, parameterized invalid-transition matrix when rules were explicit |
| **UI TypeScript** | `api.ts`, list/detail/form modules, client-side sort/filter pipeline (Sprint 6.2) |
| **Debugging write-ups** | Structured symptom → cause → fix logs after developer reported issues |

### Human judgment was essential

| Area | Why AI alone was insufficient |
|------|-------------------------------|
| **State machine transition table** | Developer/planning pass defined the exact five allowed transitions; AI had to be constrained by `02-state-machine.mdc` — wrong transitions in early drafts would have broken AC-22–AC-35 |
| **CFM XML on real AEM** | AI omitted `xmlns:granite`; only `mvn clean install` + developer diff against UI export caught it |
| **Servlet routing on local SDK** | AI recommended suffix servlets per api-contract; **live 404** forced filter-dispatch redesign — not discoverable from code review alone |
| **Service user ACLs** | Empty ticket list looked like "no data" but was permissions; required CRXDE + error.log + repoinit knowledge |
| **Scope control** | Keeping `it.tests`/`ui.tests` unused, not implementing DB adapter, comments-on-terminal asymmetry — product/architecture decisions |
| **Documentation honesty** | Sprint 8.1 docs required cross-check against code and prompt history; AI flagged uncertainties (README curl CSRF, template structure for `ticketapp`) instead of silent invention |

[ADD PERSONAL NOTE: Describe one moment where you rejected or significantly rewrote AI output — what was wrong and how you knew?]

---

## Key Trade-offs & Rationale

| Trade-off | Chosen | Why | Cost |
|-----------|--------|-----|------|
| **Repository Pattern** | Interface + CF adapter (`impl.type=contentfragment`) | Mandatory swappability story; clean service layer | Extra interfaces, mappers, OSGi wiring |
| **Servlets vs GraphQL** | Sling Servlets at `/bin/api/v1/*` | Write operations + state machine map cleanly to PUT sub-resources; GraphQL on AEM CF is read-biased | More servlet/filter classes |
| **CF persistence** | Content Fragments in DAM | Native AEMaaCS headless storage for assessment | Verbose write API; in-memory query scans |
| **Client-side priority/sort** | Sprint 6.2 UI-only | Ship polish without backend sprint | Priority filter not server-side; large lists won't scale |
| **Unit-only tests** | `core/src/test` only | Fast feedback on business rules; no AEM test harness setup | No automated regression for servlets, repos, or UI |
| **Comments on terminal tickets** | Allowed; edits blocked | FR-13 + support audit trail on closed tickets | Asymmetric rules — easy to misunderstand |
| **Routing filters vs suffix servlets** | Filters on local SDK | Reliability over api-contract purity | Doc/runtime mismatch |
| **`/me` endpoint** | Added in 6.2 | Real `createdBy` from session | Extra servlet; still no full authZ |

---

## What I Would Do Differently Next Time

1. **Wire clientlib in the same task as UI scaffold** — Sprint 6.1 lost time when the list rendered but JS never ran; include clientlib verification in the scaffold Definition of Done.
2. **Prove servlet routing on SDK before documenting suffix paths** — Task 5.1.2 iteration on filters could have been anticipated with a spike curl on `{id}` paths on day one.
3. **Atomic ID generation from the start** — Document the JCR counter limitation early; consider Oak counter or UUID if concurrency matters.
4. **Server-side `?priority=` when adding UI filters** — Avoid client/server asymmetry that confuses reviewers.
5. **Minimal `it.tests` smoke suite** — Even one HTTP test for `POST /tickets` + `PUT /status` 409 would catch service-user and routing regressions without full Cypress.
6. **Align api-contract with filter implementation** — Update contract doc when runtime diverges from suffix registration.
7. **Overwrite `createdBy` from session on comments** — Match ticket create behavior; reduce spoofing risk noted in `code-review-notes.md`.

[ADD PERSONAL NOTE: What would you prioritize if you had one more day on this project?]

---

## What I Learned

### Technical

- Content Fragment repositories need a **mapped service user** with explicit DAM + `/var` ACLs; administrative resolvers are not acceptable.
- AEM **CSRFFilter** affects browser POST/PUT to custom servlets — plan for Granite token fetch in any custom UI.
- **Editable template structure** vs page `jcr:content` — understanding where `ticketapp` lives matters for troubleshooting empty pages.
- Keeping domain logic **pure** (state machine) pays off disproportionately in test speed and confidence.

### Working with AI effectively

- **Rules + task IDs** beat long ad-hoc prompts for AEM work — the model still invents APIs without guardrails.
- **Quality Gates on real AEM** are non-negotiable; generated code that compiles can still fail at runtime (OSGi, routing, CSRF).
- **Save prompts and summaries** (`ai-prompts/`, `prompt-history/`) made Sprint 8.1 documentation possible without reconstructing memory.
- **Ask AI to flag uncertainty** in docs ("if unsure, say so") produced better README than asking for polished prose alone.

---

## Personal Voice — Edit These Sections

The following markers are intentionally left for developer editing:

1. **[ADD PERSONAL NOTE: Which of these issues cost you the most time…]** — Challenges section
2. **[ADD PERSONAL NOTE: Describe one moment where you rejected…]** — AI vs human section
3. **[ADD PERSONAL NOTE: What would you prioritize if you had one more day…]** — Do differently section

---

## Related Documents

- [tool-workflow.md](tool-workflow.md) — AI workflow and README checklist
- [final-ai-usage-summary.md](final-ai-usage-summary.md) — Consolidated AI usage across SDLC
- [debugging-notes.md](debugging-notes.md) — Issue index
- [code-review-notes.md](code-review-notes.md) — Self review with improvement areas
