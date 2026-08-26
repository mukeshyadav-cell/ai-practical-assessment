# AI Prompt — Task 6.1.0: UI Scaffold (HTL + TypeScript)

**Date:** 2026-08-27
**Purpose:** Record the implementation prompt and AI response summary for Sprint 6.1 Task 6.1.0.

**Sprint/Task:** 6.1 / 6.1.0
**Category:** implementation
**Meaningful:** Yes — scaffold static ticketing page, HTL mount components, clientlib pipeline, and API wiring stub.

---

## Prompt (verbatim)

> Task 6.1.0 (Sprint 6.1): Scaffold the UI infrastructure — a static template, a single tickets
> page, the clientlib pipeline, and empty component skeletons. NO feature logic yet — just the
> mount points and wiring so later tasks fill them in.
>
> Follow all rules in .cursor/rules/ (05-frontend, 04-aem-correctness). The REST API is complete;
> the UI only consumes /bin/api/v1/*. App URL target: /content/assessment/us/en/tickets.html.
>
> FIRST, inspect the existing project:
> - Check what the archetype already generated: existing templates under
>   /conf/assessment/settings/wcm/templates, existing page(s) under /content/assessment,
>   the ui.frontend webpack setup, and existing clientlib structure.
> - Decide the SIMPLEST path: reuse an existing static/content page template if suitable, or
>   create a minimal static template. State what you find and your decision before creating files.
>
> Then create/scaffold:
>
> 1. Static template (or reuse existing) for the ticketing app:
>    - A minimal template at /conf/assessment/settings/wcm/templates/ticketing-page
>      (or reuse the archetype's content-page template if it's simpler).
>    - Include head with the clientlib and a body region for the app components. Keep it static.
>    - Ensure the template loads the clientlib category "assessment.ticketing".
>
> 2. Tickets page:
>    - Create a page at /content/assessment/us/en/tickets using the template (single app page).
>
> 3. Components (skeletons only) under /apps/assessment/components/ (node names no hyphens):
>    a) ticketapp (wrapper): renders <div class="ticket-app" id="ticket-app-root"> with
>       placeholders for list + detail views.
>    b) ticketlist: <section id="ticket-list-view"><div id="ticket-list-root"> + a search input
>       and a status filter <select> (empty options for now).
>    c) ticketdetail: <section id="ticket-detail-view" hidden><div id="ticket-detail-root">
>       (+ placeholder regions for comments, status, reassign).
>    d) ticketform: hidden <div id="ticket-form-root"> (modal/inline shell).
>    Each: proper .content.xml (jcr:primaryType, jcr:title, componentGroup "Assessment - Ticketing").
>    HTL only; no logic. Compose them into the page/template statically so they render.
>
> 4. Clientlib (assessment.ticketing) compiled from ui.frontend TypeScript:
>    - Ensure category "assessment.ticketing" exists and is loaded by the template.
>    - In ui.frontend, add a TS entry (e.g., src/main/webpack/site/ticketing/main.ts) that logs
>      "ticketing UI loaded" and calls GET /bin/api/v1/tickets, console.logging the ticket count
>      (proves wiring end-to-end).
>    - Follow the archetype's existing clientlib build mechanism; do NOT invent a new pipeline.
>    - Include minimal CSS (container widths, a hidden utility class).
>
> 5. View-switching stub in main.ts:
>    - If URL has ?id=<ticketId>, show #ticket-detail-view and hide #ticket-list-view; else show
>      list. Just show/hide skeleton — no data rendering yet.
>
> Constraints:
> - HTL components: markup + mount points only, NO business logic.
> - TS talks ONLY to /bin/api/v1/* with relative paths; handle fetch errors gracefully.
> - No secrets, no hardcoded hostnames.
> - Reuse the archetype's clientlib/webpack conventions; if uncertain, SAY SO and show the
>   closest-correct config plus how to verify.
>
> After generating:
> - Confirm mvn clean install (and ui.frontend build) compiles; deploy with -PautoInstallSinglePackage.
> - Tell me EXACTLY how to verify at /content/assessment/us/en/tickets.html:
>     * Page loads with the static template
>     * Clientlib loads (console shows "ticketing UI loaded")
>     * TS calls GET /bin/api/v1/tickets (console shows ticket count)
>     * View-switching stub works (?id= toggles detail vs list)
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Scaffolded locked `ticketing-page` template, `/content/assessment/us/en/tickets` page, HTL skeletons (`ticketapp`, `ticketlist`, `ticketdetail`, `ticketform`), separate Webpack entry → `assessment.ticketing` clientlib, API proof call (`GET /bin/api/v1/tickets`), and `?id=` view-switching stub. `mvn clean install` and `-PautoInstallSinglePackage` succeeded. Active task advanced to 6.1.1.
