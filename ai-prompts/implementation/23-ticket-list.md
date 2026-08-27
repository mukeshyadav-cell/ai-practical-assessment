# AI Prompt — Task 6.1.1: Ticket List View

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.1.

**Sprint/Task:** 6.1 / 6.1.1
**Category:** implementation
**Meaningful:** Yes — ticket list fetch, card rendering, search/status filter wiring, and UI states.

---

## Prompt (verbatim)

> Task 6.1.1 (Sprint 6.1): Implement the ticket LIST view — fetch tickets from the API, render
> cards into #ticket-list-root, wire the search box and status filter, add real CSS, and handle
> loading/empty/error states.
>
> Follow all rules in .cursor/rules/ (05-frontend). Read api-contract.md (GET /bin/api/v1/tickets,
> query params ?q= and ?status=, TicketDTO JSON shape). TypeScript source lives in ui.frontend
> (under the site/ticketing entry created in 6.1.0); compiled into the assessment.ticketing clientlib.
> UI consumes the existing API only — NO backend changes.
>
> Context:
> - The list container is #ticket-list-root inside #ticket-list-view (from 6.1.0 skeleton).
> - A search <input> and a status filter <select> exist in the ticketlist component (from 6.1.0)
>   but are not wired. If options are empty, populate the status filter with:
>   All, Open, In Progress, Resolved, Closed, Cancelled.
> - TicketDTO fields: id, title, description, priority (P1-P4), status, assignedTo, createdBy,
>   createdAt, updatedAt.
>
> Implement (TypeScript):
>
> 1. A typed model + API module:
>    - interface Ticket { id, title, description, priority, status, assignedTo, createdBy,
>      createdAt, updatedAt: string } (assignedTo may be empty).
>    - async function fetchTickets(params?: { q?: string; status?: string }): Promise<Ticket[]>
>        * builds /bin/api/v1/tickets with URLSearchParams for q/status (omit blanks)
>        * throws on non-2xx; returns parsed array
>    Put API helpers in a small module (e.g., src/main/webpack/site/ticketing/api.ts) so later
>    tasks reuse them.
>
> 2. List rendering:
>    - renderTicketList(container, tickets): builds a card per ticket with:
>        * ticket id (e.g., TKT-1001)
>        * title
>        * a status badge (color-coded per status)
>        * a priority badge (P1-P4, color-coded; P1 most urgent)
>        * assignee (show "Unassigned" if blank)
>        * a short createdAt (formatted date)
>    - Each card is clickable -> navigates to ?id=<ticketId> (which the 6.1.0 view-switch stub
>      uses to show the detail view). Use anchor or click handler updating window.location.search.
>    - Escape/encode all ticket text inserted into the DOM (prevent XSS) — use textContent or an
>      escape helper, NOT innerHTML with raw data.
>
> 3. Search + filter wiring:
>    - Search input: debounce (~300ms); on input, call fetchTickets({ q, status }) and re-render.
>    - Status <select>: on change, call fetchTickets({ q, status }) and re-render.
>    - Combine q + status when both are set (pass both params).
>    - "All" status option means no status param.
>
> 4. States:
>    - Loading: show a simple loading indicator while fetching.
>    - Empty: if zero tickets, show "No tickets found." message.
>    - Error: if the fetch fails, show a friendly error message (log details to console).
>
> 5. CSS (in the clientlib):
>    - Clean, minimal, responsive card layout (CSS grid or flex).
>    - Status badge colors: Open (blue), In Progress (amber), Resolved (green),
>      Closed (grey), Cancelled (red/muted).
>    - Priority badge styling (P1 strongest emphasis).
>    - Basic page container width, spacing, typography. No external CSS frameworks.
>
> 6. Wire-up:
>    - Initialize on DOMContentLoaded (or the archetype's clientlib init pattern).
>    - Only run list logic when the list view is active (respect the 6.1.0 ?id= view switch:
>      if ?id= present, detail view is shown and list init can skip or stay hidden).
>
> Constraints:
> - TypeScript with explicit types; async/await; relative API paths only.
> - No secrets/hostnames. Handle errors gracefully. Keep functions small and reusable
>   (later tasks will import api.ts).
>
> After generating:
> - Confirm the ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify at /content/assessment/us/en/tickets.html:
>     * The 8 sample tickets render as cards
>     * Status filter narrows results (e.g., Open shows 3)
>     * Search box filters by title keyword (e.g., "login" -> TKT-1001)
>     * Clicking a card sets ?id= and switches to the (still-empty) detail view
>     * Loading/empty/error states behave
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Added `ticketing/api.ts` (`Ticket` + `fetchTickets`), `view.ts` (view switch / navigation), and `list.ts` (`renderTicketList`, debounced search, status filter, loading/empty/error). Cards use `textContent` for XSS safety; click sets `?id=` for detail view. Expanded `main.scss` with card grid and status/priority badges. `npm run prod` and `mvn clean install` succeeded; deploy profile failed when AEM was down. Active task advanced to 6.1.2.
