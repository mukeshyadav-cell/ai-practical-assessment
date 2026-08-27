# AI Prompt — Task 6.1.3: Create/Edit Ticket Form

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.3.

**Sprint/Task:** 6.1 / 6.1.3
**Category:** implementation
**Meaningful:** Yes — create/edit ticket form, API write operations, assignee dropdown, validation, and terminal-ticket edit blocking.

---

## Prompt (verbatim)

> Task 6.1.3 (Sprint 6.1): Implement the create/edit ticket FORM. One reusable form handles
> CREATE (POST /bin/api/v1/tickets) and EDIT (PUT /bin/api/v1/tickets/{id}), with the assignee
> dropdown populated from GET /bin/api/v1/users. Introduce UI write operations.
>
> Follow all rules in .cursor/rules/ (05-frontend). Read api-contract.md:
> - POST /bin/api/v1/tickets (create; status is forced to Open server-side; do NOT send status;
>   fields: title, description, priority, assignedTo, createdBy) -> 201 + TicketDTO
> - PUT /bin/api/v1/tickets/{id} (edit; only title/description/priority honored) -> 200 + TicketDTO;
>   terminal ticket -> 400 TICKET_NOT_EDITABLE; validation -> 400 VALIDATION_ERROR
> - GET /bin/api/v1/users -> UserDTO[] (for assignee dropdown)
> Reuse api.ts; TypeScript in ui.frontend -> assessment.ticketing clientlib. NO backend changes.
>
> Context:
> - The ticketform component shell (#ticket-form-root, hidden) exists from 6.1.0.
> - List view (6.1.1) and detail view (6.1.2) exist. Detail has an "Edit" placeholder region.
>
> Implement (TypeScript, extending api.ts):
>
> 1. api.ts additions:
>    - interface User { userId, displayName, email: string }
>    - async fetchUsers(): Promise<User[]>                        // GET /bin/api/v1/users
>    - async createTicket(payload): Promise<Ticket>               // POST; returns created ticket
>        payload = { title, description, priority, assignedTo?, createdBy }
>    - async updateTicket(id, payload): Promise<Ticket>           // PUT /{id}
>        payload = { title, description, priority }  (only editable fields)
>    All throw on non-2xx; parse and surface the API error {error, code} for the UI to display.
>
> 2. Form UI (renderTicketForm):
>    - Fields: title (text, required), description (textarea, required per data-model),
>      priority (<select> P1-P4), assignedTo (<select> populated from fetchUsers(); include an
>      "Unassigned" option mapping to empty).
>    - createdBy: for create, set to the current user. Since AEM auth is in play, use a sensible
>      approach: if a current-user value is available client-side use it, otherwise default to a
>      known seed user (e.g., "agent-1") and add a TODO note that createdBy should come from the
>      authenticated user. Do NOT hardcode secrets. State the choice.
>    - Two modes:
>        * CREATE mode: empty form; submit -> createTicket -> on 201, close form and refresh the
>          list (or navigate to the new ticket's detail via ?id=).
>        * EDIT mode: pre-fill from an existing ticket; submit -> updateTicket -> on 200, close
>          form and refresh the detail view.
>    - Present as a modal or inline panel over the current view; include Save and Cancel.
>
> 3. Triggers:
>    - Add a "+ New Ticket" button to the LIST view -> opens form in CREATE mode.
>    - Add an "Edit" button in the DETAIL view (replace the 6.1.2 edit placeholder) -> opens form
>      in EDIT mode pre-filled with the current ticket.
>    - IMPORTANT: If the ticket status is terminal (Closed or Cancelled), DISABLE/HIDE the Edit
>      button and show a note ("Closed/Cancelled tickets cannot be edited"). Also gracefully
>      handle a 400 TICKET_NOT_EDITABLE from the API (show the error message).
>
> 4. Validation + errors:
>    - Client-side: require non-blank title and description; valid priority selected.
>    - On submit, disable the Save button to prevent double-submit.
>    - Surface API errors: if the response is 400 with {error, code}, show the error message
>      near the form (e.g., VALIDATION_ERROR, TICKET_NOT_EDITABLE). Do not leave the user guessing.
>
> 5. States:
>    - Loading state on submit; success closes form + refreshes; error keeps form open with message.
>
> 6. CSS:
>    - Style the form/modal (overlay, form fields, buttons, error text). Consistent with existing UI.
>
> Constraints:
> - Reuse api.ts; explicit TS types; async/await; relative API paths only.
> - CREATE must NOT send a status field (server forces Open).
> - EDIT must send only title/description/priority.
> - XSS-safe; no secrets/hostnames.
>
> After generating:
> - Confirm ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify:
>     * "+ New Ticket" -> fill form -> Save -> new ticket appears (TKT-1009+) with status Open;
>       the CF is created (check CRXDE)
>     * Assignee dropdown lists agent-1, agent-2 (+ Unassigned)
>     * Edit an Open ticket (e.g., TKT-1002) -> change title/priority -> Save -> detail updates
>     * Edit button hidden/disabled for a Closed ticket (TKT-1004); if forced, API returns 400
>       and the UI shows the message
>     * Blank title -> client validation blocks (or API 400 shown)
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## Follow-up prompt (verbatim)

> use free version of ai models when ever possible to fix and analyze the issue and coding and all task and logging
> log this prompt and response summary to ai-prompts/implementation/25-ticket-form.md (follow the pattern from 23-ticket-list.md / 24-ticket-detail.md).

---

## AI response summary

Extended `api.ts` with `User`, `fetchUsers`, `createTicket`, `updateTicket`, and `ApiRequestError` (parses `{ error, code }` from non-2xx responses). Added `form.ts` with a modal create/edit form: title, description, priority, assignee (create only), client validation, disabled Save during submit, and inline API error display. `createdBy` uses `document.body.dataset.currentUser` when set, otherwise defaults to `agent-1` with a TODO for real AEM auth. List view: `+ New Ticket` opens CREATE mode; on success navigates to `?id=<newId>`. Detail view: `Edit` opens EDIT mode; terminal tickets (Closed/Cancelled) show a note instead of the button. `main.scss` adds modal overlay, form fields, and button styles. `npm run prod` and `mvn clean install` succeeded. Active task advanced to 6.1.4 (add comment).

**Files changed:** `ui.frontend/src/main/webpack/ticketing/api.ts`, `form.ts`, `list.ts`, `detail.ts`, `main.ts`, `main.scss`; `implementation-plan.md` (6.1.3 marked complete).

**Developer note:** Prefer free-tier / lower-cost models for analysis, debugging, and prompt logging where full capability is not required; use higher-capability models for complex implementation tasks.
