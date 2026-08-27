# AI Prompt — Task 6.1.2: Ticket Detail View

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.2.

**Sprint/Task:** 6.1 / 6.1.2
**Category:** implementation
**Meaningful:** Yes — ticket detail fetch, comment list, back navigation, and inert placeholders for later tasks.

---

## Prompt (verbatim)

> Task 6.1.2 (Sprint 6.1): Implement the ticket DETAIL view — fetch a single ticket by id,
> render all fields into #ticket-detail-root, and load + display its comments. Also add a
> "Back to list" action. This task is DISPLAY-only; interactive controls (status change,
> reassign, add comment) come in later tasks — render clearly-labeled placeholders for them.
>
> Follow all rules in .cursor/rules/ (05-frontend). Read api-contract.md
> (GET /bin/api/v1/tickets/{id} -> TicketDTO or 404; GET /bin/api/v1/tickets/{id}/comments
> -> CommentDTO[] ascending). Reuse the api.ts module and the status/priority badge styling
> from Task 6.1.1. TypeScript in ui.frontend -> assessment.ticketing clientlib. NO backend changes.
>
> Context:
> - Detail container: #ticket-detail-root inside #ticket-detail-view (from 6.1.0).
> - The 6.1.0 view-switch stub already shows the detail view when ?id=<ticketId> is present.
> - CommentDTO fields: id, ticketId, message, createdBy, createdAt.
>
> Implement (TypeScript, extending api.ts):
>
> 1. api.ts additions:
>    - interface Comment { id, ticketId, message, createdBy, createdAt: string }
>    - async fetchTicket(id: string): Promise<Ticket>   // GET /bin/api/v1/tickets/{id}; throws on 404
>    - async fetchComments(ticketId: string): Promise<Comment[]>  // GET .../{id}/comments
>
> 2. Detail rendering (renderTicketDetail(container, ticket)):
>    - Show ALL ticket fields clearly:
>        id, title, description (preserve line breaks safely), status badge, priority badge,
>        assignee (or "Unassigned"), createdBy, createdAt, updatedAt (formatted dates).
>    - Layout: a readable detail panel (header with id + title + badges; body with description
>      and metadata).
>    - Escape/encode all text (XSS) — use textContent / safe DOM building, NOT innerHTML with raw data.
>
> 3. Comments rendering (renderComments(container, comments)):
>    - A "Comments" section listing each comment: author (createdBy), timestamp, message.
>    - Ordered ascending (API already returns ascending) — oldest first.
>    - Empty state: "No comments yet."
>    - Escape all comment text.
>
> 4. Detail view controller:
>    - On load (and when ?id= changes): read id from URL; if present:
>        * show detail view, hide list view (respect the 6.1.0 switch)
>        * fetchTicket(id) then renderTicketDetail; on 404 show "Ticket not found." message
>        * fetchComments(id) then renderComments
>    - "Back to list" button/link: clears ?id= (navigate to the page without the param),
>      which returns to the list view (re-run list init from 6.1.1 or reload).
>
> 5. Placeholders for later tasks (render, but non-functional, clearly labeled):
>    - A "Change Status" region (placeholder text: "status control – Task 6.1.5")
>    - A "Reassign" region (placeholder: "reassign control – Task 6.1.6")
>    - An "Add Comment" region (placeholder: "add comment – Task 6.1.4")
>    Keep these as empty labeled containers so the next tasks fill them in.
>
> 6. States:
>    - Loading indicator while fetching ticket/comments.
>    - 404 -> friendly "Ticket not found" with a back-to-list link.
>    - Error -> friendly message; log details to console.
>
> 7. CSS:
>    - Style the detail panel, metadata grid, and comment list (reuse badge styles from 6.1.1).
>    - Keep consistent with the list view's look.
>
> Constraints:
> - Reuse api.ts (do not duplicate fetch logic).
> - Explicit TS types; async/await; relative API paths only.
> - XSS-safe DOM building; no secrets/hostnames.
> - Do NOT implement status change / reassign / add-comment logic here (later tasks).
>
> After generating:
> - Confirm ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify:
>     * Open .../tickets.html, click TKT-1001 -> detail shows all fields + its 2 comments
>       (CMT-1001, CMT-1002 in order)
>     * Open .../tickets.html?id=TKT-1006 -> shows unassigned + its comment (CMT-1006)
>     * Open .../tickets.html?id=TKT-9999 -> "Ticket not found"
>     * "Back to list" returns to the list view
>     * Placeholder regions for status/reassign/add-comment are visible but inert
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Added `fetchTicket`, `fetchComments`, and `detail.ts` for XSS-safe detail/comment rendering, back-to-list navigation, loading/404/error states, and inert placeholders for 6.1.4–6.1.6. Shared helpers moved to `dom.ts`. Build passed; active task advanced to 6.1.3.
