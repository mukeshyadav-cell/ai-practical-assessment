# AI Prompt — Task 6.1.5: Status Change UI

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.5.

**Sprint/Task:** 6.1 / 6.1.5
**Category:** implementation
**Meaningful:** Yes — status change control, client transition map (FR-10), PUT status API, terminal-state read-only UX.

---

## Prompt (verbatim)

> Task 6.1.5 (Sprint 6.1): Implement the STATUS CHANGE control in the ticket detail view (FR-10).
> Show ONLY valid next statuses for the ticket's current status, and change status via
> PUT /bin/api/v1/tickets/{id}/status. Replace the "status control" placeholder from Task 6.1.2.
>
> Follow all rules in .cursor/rules/ (05-frontend, 02-state-machine). Read api-contract.md
> (PUT /bin/api/v1/tickets/{id}/status body { "status": "<label>" } -> 200 + TicketDTO;
> invalid transition -> 409 INVALID_TRANSITION; unknown label -> 400). Reuse api.ts. TypeScript
> in ui.frontend -> assessment.ticketing clientlib. NO backend changes.
>
> Context:
> - Detail view (6.1.2) has a "Change Status" placeholder region and already fetches the ticket.
> - The server enforces the state machine; the client shows only valid options for good UX.
>
> Implement (TypeScript, extending api.ts):
>
> 1. api.ts addition:
>    - async changeStatus(id: string, status: string): Promise<Ticket>
>        // PUT /bin/api/v1/tickets/{id}/status with { status }; throws on non-2xx; parse {error, code}.
>
> 2. Client-side transition map (MUST mirror the server TicketStateMachine EXACTLY):
>    const ALLOWED_TRANSITIONS: Record<string, string[]> = {
>      "Open":        ["In Progress", "Cancelled"],
>      "In Progress": ["Resolved", "Cancelled"],
>      "Resolved":    ["Closed"],
>      "Closed":      [],
>      "Cancelled":   []
>    };
>    Add a comment: "This mirrors com.mysite.core.statemachine.TicketStateMachine — keep in sync.
>    The server is the source of truth and rejects invalid transitions (409)."
>
> 3. Status control UI (renderStatusControl):
>    - Read the ticket's current status; look up allowed next statuses.
>    - If allowed list is non-empty: render a control offering ONLY those statuses
>      (e.g., a <select> with the valid options, or buttons like "Move to In Progress").
>      Include the transition action (a "Change Status" button if using a select).
>    - If allowed list is empty (Closed/Cancelled): show a read-only note like
>      "This ticket is <status> — no further status changes." Do NOT offer any transition.
>    - On action:
>        * disable the control during the request (prevent double-submit)
>        * call changeStatus(id, newStatus)
>        * on 200: re-render the detail (status badge updates; allowed transitions update;
>          updatedAt changes) — re-fetch the ticket to reflect server state
>        * on 409 INVALID_TRANSITION or 400: show the API error message near the control
>          (safety net — should be rare since UI only offers valid options)
>
> 4. Consistency with other controls:
>    - The status control lives alongside the reassign (6.1.6 placeholder) and comment sections.
>    - Keep the current status prominently shown (reuse the status badge).
>
> 5. States: loading on submit; success refreshes detail; error shows message.
>
> 6. CSS: style the status control consistent with the UI; the status badge already exists.
>
> Constraints:
> - Reuse api.ts and the detail re-render from 6.1.2 (do NOT duplicate fetch/render).
> - Explicit TS types; async/await; relative API paths; XSS-safe.
> - The UI restricts choices, but the SERVER remains the authority (handle 409 gracefully).
>
> After generating:
> - Confirm ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify (use sample tickets):
>     * TKT-1001 (Open): control offers ONLY "In Progress" and "Cancelled".
>       Move to "In Progress" -> 200 -> badge updates -> now offers "Resolved"/"Cancelled".
>     * Continue: In Progress -> Resolved -> Closed (each 200); after Closed, control shows
>       "no further status changes".
>     * TKT-1004 (Closed): shows read-only "no further status changes" (no options).
>     * (Optional safety-net check) If you force an invalid status via curl, API returns 409;
>       the UI never offers invalid options.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Added `changeStatus` in `api.ts` (PUT via `fetchWithCsrf`). Created `transitions.ts` with `ALLOWED_TRANSITIONS` mirroring `TicketStateMachine`. `detail.ts`: `renderStatusControl` in `#ticket-status-root` — select + Change Status button for valid transitions; read-only note for Closed/Cancelled; on success calls `loadDetail()` to refresh badge and options. CSS in `main.scss`. Build passed; active task advanced to 6.1.6.
