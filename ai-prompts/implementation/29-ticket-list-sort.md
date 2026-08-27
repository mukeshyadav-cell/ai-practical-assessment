# AI Prompt — Task 6.2.1: Client-Side Ticket List Sorting

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.2 Task 6.2.1.

**Sprint/Task:** 6.2 / 6.2.1
**Category:** implementation
**Meaningful:** Yes — client-side sort dropdown, pure `sortTickets()` function, pipeline wiring.

---

## Prompt (verbatim)

> Task 6.2.1 (Sprint 6.2): Add client-side SORTING to the ticket list. A sort dropdown with:
> Newest (createdAt desc), Oldest (createdAt asc), Recently Updated (updatedAt desc),
> Priority (P1→P4), Ticket ID (asc). Sorting must combine with the existing search + status filter.
>
> Follow all rules in .cursor/rules/ (05-frontend). This is CLIENT-SIDE only — sort the tickets
> already fetched from GET /bin/api/v1/tickets. Reuse the existing list module + renderTicketList.
> NO backend changes. TypeScript in ui.frontend -> assessment.ticketing clientlib.
>
> Context:
> - The list view (6.1.1) fetches tickets and renders cards; it has a search input and a status
>   filter <select>. Add a sort <select> alongside them.
> - Ticket fields: createdAt, updatedAt (ISO-8601 strings), priority (P1-P4), id (e.g., TKT-1001).
>
> Implement (TypeScript):
>
> 1. Add a sort <select> to the list controls with options: Newest, Oldest, Recently Updated,
>    Priority, Ticket ID (default = Newest).
> 2. A pure sort function: sortTickets(tickets: Ticket[], sortKey: string): Ticket[]
> 3. Wire into existing flow: fetch -> sortTickets -> renderTicketList; combine with q/status.
> 4. No regressions to search and status filter.
> 5. CSS consistent with existing filter control.
>
> After generating: confirm ui.frontend build + mvn clean install; deploy with -PautoInstallSinglePackage.

---

## AI response summary

Added `ticketing/sort.ts` with pure `sortTickets()` (dates, P1–P4 priority rank, numeric TKT-ID compare; non-mutating). Wired sort `<select>` in `list.ts` and `ticketlist.html`; pipeline is fetch → sort → `renderTicketList`. Default sort Newest. `npm run prod` and `mvn clean install` passed. Active task advanced to 6.2.2.
