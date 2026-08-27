# AI Prompt — Task 6.2.3: Client-Side Priority Filter

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.2 Task 6.2.3.

**Sprint/Task:** 6.2 / 6.2.3
**Category:** implementation
**Meaningful:** Yes — client-side priority filter integrated into unified `ListState` + `refreshList()` pipeline.

---

## Prompt (verbatim)

> Task 6.2.3 (Sprint 6.2): Add a PRIORITY filter to the ticket list (All, P1, P2, P3, P4),
> integrated into the unified list state and render pipeline from Task 6.2.2. Client-side filter
> (no backend change).
>
> Follow all rules in .cursor/rules/ (05-frontend). Reuse the currentState + refreshList()
> pipeline, fetchTickets, sortTickets, renderTicketList, and the result-count summary from 6.2.2.
> NO backend changes. TypeScript in ui.frontend -> assessment.ticketing clientlib.
>
> Context:
> - The API supports ?status= and ?q= server-side, but NOT priority. So priority filtering is
>   CLIENT-SIDE: filter the fetched tickets by priority in TS before sort + render.
> - currentState currently holds { q, status, sort }. Add a `priority` field.
>
> Implement (TypeScript):
>
> 1. Add a priority <select> alongside the status filter: All Priorities, P1, P2, P3, P4.
> 2. Extend ListState with priority (default "" = All).
> 3. Update refreshList(): fetch (q,status) -> client-filter by priority -> sort -> render + count.
> 4. Extend Clear filters to reset priority.
> 5. No regressions; document client-side vs server-side asymmetry in a code comment.
> 6. CSS consistent with status filter.
>
> After generating: confirm ui.frontend build + mvn clean install; deploy with -PautoInstallSinglePackage.

---

## AI response summary

Extended `ListState` with `priority`, added `#ticket-priority-filter` in `ticketlist.html`, and `filterTicketsByPriority()` in `list.ts` (commented as client-side; status/q remain server-side). Pipeline order: fetch → priority filter → sort → render; summary and Clear filters include priority. `npm run prod` and `mvn clean install` passed. Active task advanced to 6.2.4.
