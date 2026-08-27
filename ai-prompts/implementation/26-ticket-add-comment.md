# AI Prompt — Task 6.1.4: Add Comment UI

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.4.

**Sprint/Task:** 6.1 / 6.1.4
**Category:** implementation
**Meaningful:** Yes — add-comment form, POST comment API, validation, comment list refresh, terminal-ticket support.

---

## Prompt (verbatim)

> Task 6.1.4 (Sprint 6.1): Implement the ADD COMMENT control in the ticket detail view.
> Replace the "add comment" placeholder from Task 6.1.2 with a working form that posts a comment
> and refreshes the comment list.
>
> Follow all rules in .cursor/rules/ (05-frontend). Read api-contract.md:
> - POST /bin/api/v1/tickets/{id}/comments  body { message, createdBy } -> 201 + CommentDTO;
>   empty message -> 400 VALIDATION_ERROR; missing ticket -> 404.
> - Comments are allowed even when the ticket status is Closed or Cancelled (do NOT hide the
>   add-comment control for terminal tickets).
> Reuse api.ts and the existing renderComments from Task 6.1.2. TypeScript in ui.frontend ->
> assessment.ticketing clientlib. NO backend changes.
>
> Context:
> - Detail view (6.1.2) already fetches + renders comments and has an add-comment placeholder region.
> - CommentDTO fields: id, ticketId, message, createdBy, createdAt.
>
> Implement (TypeScript, extending api.ts):
>
> 1. api.ts addition:
>    - async addComment(ticketId: string, payload: { message: string; createdBy: string })
>        : Promise<Comment>
>      // POST /bin/api/v1/tickets/{ticketId}/comments; throws on non-2xx; parse {error, code} on error.
>
> 2. Add-comment UI (in the detail view's comment region):
>    - A <textarea> for the message + an "Add Comment" button.
>    - createdBy: use the same current-user approach chosen in Task 6.1.3 (client value if
>      available, else default seed user with a TODO to use the authenticated user). Be consistent
>      with 6.1.3. Do NOT hardcode secrets.
>    - On submit:
>        * client validation: message must be non-blank (trim); if blank, show inline message and
>          do not call the API.
>        * disable the Add button during the request (prevent double-submit).
>        * call addComment(ticketId, {message, createdBy}).
>        * on 201: clear the textarea, re-fetch comments (fetchComments) and re-render the list so
>          the new comment appears (ascending order, at the bottom).
>        * on error (e.g., 400 VALIDATION_ERROR): show the API error message near the form; keep
>          the entered text.
>
> 3. Terminal-ticket behavior:
>    - The add-comment control MUST remain enabled/visible for Closed and Cancelled tickets
>      (comments are allowed on terminal tickets). Do not disable it based on status.
>
> 4. States:
>    - Loading indicator on submit; success clears + refreshes; error keeps text + shows message.
>
> 5. CSS:
>    - Style the comment form (textarea, button, inline error) consistent with the existing UI.
>
> Constraints:
> - Reuse api.ts and renderComments (do NOT duplicate comment-fetch/render logic).
> - Explicit TS types; async/await; relative API paths only; XSS-safe (escape message on render).
>
> After generating:
> - Confirm ui.frontend build + mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Tell me how to verify:
>     * Open TKT-1001 detail -> add a comment -> it appears at the bottom of the list; CF created
>       in CRXDE (/content/dam/assessment/comments/CMT-xxxx)
>     * Empty message -> blocked with inline error (no API call) OR API 400 shown
>     * Add a comment on a CLOSED ticket (TKT-1004) -> succeeds (201) and appears
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Added `addComment` and `AddCommentPayload` in `api.ts` (POST via `fetchWithCsrf`). Shared `resolveCreatedBy` in `userContext.ts` (used by `form.ts` and detail). `detail.ts`: `renderAddCommentForm` replaces placeholder; validation, disabled submit, inline errors; on success re-fetches with `fetchComments` + `renderComments`. Add-comment stays enabled for Closed/Cancelled tickets. CSS in `main.scss`. Build passed; active task advanced to 6.1.5.
