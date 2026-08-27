# AI Prompt — Task 6.1.6: Reassign UI

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.6.

**Sprint/Task:** 6.1 / 6.1.6
**Category:** implementation
**Meaningful:** Yes — reassign control, assignee dropdown (FR-19), terminal-ticket blocking; final Sprint 6.1 UI feature task.

---

## Prompt (verbatim)

> Task 6.1.6 (Sprint 6.1): Implement the REASSIGN control in the ticket detail view (FR-19).
> Provide an assignee dropdown (from GET /bin/api/v1/users) and reassign via
> PUT /bin/api/v1/tickets/{id}/assignee. Replace the "reassign control" placeholder from Task 6.1.2.
> This is the final UI feature task.
>
> Follow all rules in .cursor/rules/ (05-frontend). Read api-contract.md
> (PUT /bin/api/v1/tickets/{id}/assignee body { "assignedTo": "<userId>" } -> 200 + TicketDTO;
> unknown user -> 400 UNKNOWN_USER; terminal ticket -> 400 TICKET_NOT_EDITABLE; missing -> 404).
> Reuse api.ts (fetchUsers already exists from Task 6.1.3). TypeScript in ui.frontend ->
> assessment.ticketing clientlib. NO backend changes.
>
> Context:
> - Detail view (6.1.2) has a "reassign" placeholder region and fetches the ticket.
> - Users come from GET /bin/api/v1/users (agent-1, agent-2, ...). fetchUsers() exists in api.ts.
>
> Implement (TypeScript, extending api.ts):
>
> 1. api.ts addition:
>    - async reassignTicket(id: string, assignedTo: string): Promise<Ticket>
>        // PUT /bin/api/v1/tickets/{id}/assignee with { assignedTo }; throws on non-2xx;
>        // parse {error, code} on error.
>      (Confirm the exact body field name from api-contract.md; match it.)
>
> 2. Reassign control UI (renderReassignControl):
>    - A <select> populated from fetchUsers() showing displayName (value = userId).
>    - Include an "Unassigned" option (value = "") IF the api-contract/service allows explicit
>      unassign; if the service requires a valid user (from Task 4.1.3 decision), OMIT the
>      Unassigned option and note that in a comment. Match the actual backend behavior — check
>      TicketService.reassignTicket semantics and api-contract.
>    - Pre-select the ticket's current assignedTo (or "Unassigned" if blank and allowed).
>    - A "Reassign" button.
>    - On action:
>        * disable during request (prevent double-submit)
>        * call reassignTicket(id, selectedUserId)
>        * on 200: re-fetch the ticket and re-render detail (assignee updates; updatedAt changes)
>        * on 400 UNKNOWN_USER / 400 TICKET_NOT_EDITABLE: show the API error message near the control
>
> 3. Terminal-ticket behavior:
>    - If the ticket status is Closed or Cancelled, DISABLE/HIDE the reassign control and show a
>      note ("Closed/Cancelled tickets cannot be reassigned") — consistent with edit (Task 6.1.3).
>    - Also handle a 400 TICKET_NOT_EDITABLE from the API gracefully as a safety net.
>
> 4. States: loading on submit; success refreshes detail; error keeps selection + shows message.
>
> 5. CSS: style the reassign control consistent with the status control and the rest of the UI.
>
> Constraints:
> - Reuse api.ts (fetchUsers, and the detail re-render from 6.1.2). Do NOT duplicate logic.
> - Explicit TS types; async/await; relative API paths; XSS-safe.
> - The UI restricts to valid users via the dropdown; the SERVER remains authoritative (handle 400).
>
> After generating:
> - Confirm ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify (use sample tickets):
>     * TKT-1006 (Open, unassigned): dropdown lists agent-1/agent-2; reassign to agent-1 -> 200 ->
>       assignee updates to agent-1
>     * Reassign TKT-1002 (In Progress) from agent-2 to agent-1 -> 200 -> updates
>     * TKT-1004 (Closed): reassign control disabled/hidden with a note; forced PUT -> 400
>       TICKET_NOT_EDITABLE handled
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/

---

## AI response summary

Added `reassignTicket` in `api.ts` (PUT `{ assignedTo }` via `fetchWithCsrf`). `detail.ts`: `renderReassignControl` in `#ticket-reassign-root` — user dropdown from `fetchUsers`, no Unassigned option (MVP requires valid user per `TicketService.reassignTicket`). Terminal tickets show read-only note. On success calls `loadDetail()`. CSS in `main.scss`. Build passed; all Sprint 6.1 UI tasks (6.1.0–6.1.6) complete; active task → 6.1 Quality Gate.
