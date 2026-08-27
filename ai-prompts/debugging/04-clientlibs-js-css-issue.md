# Debugging — Clientlibs JS/CSS Not Loaded on Tickets Page

**Date:** 2026-08-27
**Purpose:** Record diagnosis and fix when ticket list UI did not fetch API data because `assessment.ticketing` clientlib was missing from the page.

**Sprint/Task:** 6.1 / 6.1.1 — ticket list not rendering; API call never fired
**Category:** debugging
**Related:** [implementation/22-ui-scaffold.md](../implementation/22-ui-scaffold.md), [implementation/23-ticket-list.md](../implementation/23-ticket-list.md)

---

## Prompt (verbatim)

> its not listing any things because api call itself not happening i cross checked from networ tab
> seems clientlib is not added on the page you may added at component level for now
> or where have you created js and css? is it added
> **use free modal to debug this js and css issue**
> is js and css written?

---

## AI response summary

DevTools showed only `clientlib-base` loaded — not `assessment.ticketing`. JS/CSS existed in `clientlib-ticketing` but page policy did not inject them. Fixed by adding `assessment.ticketing` CSS/JS to `ticketapp.html`; list API calls and cards worked after redeploy.

---

## Symptom

At `/content/assessment/us/en/tickets.html`, the list skeleton rendered (heading, search input, empty status `<select>`) but `#ticket-list-root` stayed empty. Network tab showed **no** `GET /bin/api/v1/tickets`. DevTools listed only `clientlib-base.js` at page bottom — no `clientlib-ticketing` script or stylesheet.

---

## Root cause

1. **Clientlib not on page** — `assessment.ticketing` was defined in page policy (`assessment/components/page/ticketing`) but was not emitted on the live page; only `assessment.base` from `customheaderlibs` / `customfooterlibs` loaded.
2. **No component-level include** — `ticketapp.html` (6.1.0) did not call the clientlib template; JS never executed, so status options and `fetchTickets` never ran.
3. **Not a missing build** — `ticketing.js` and `ticketing.css` were present under `ui.apps/.../clientlibs/clientlib-ticketing/` after `npm run prod`.

---

## Fix

**File:** `ui.apps/src/main/content/jcr_root/apps/assessment/components/ticketapp/ticketapp.html`

Load ticketing clientlib when the app component renders:

```html
<sly data-sly-use.clientlib="core/wcm/components/commons/v1/templates/clientlib.html">
    <sly data-sly-call="${clientlib.css @ categories='assessment.ticketing'}"/>
    <sly data-sly-call="${clientlib.js @ categories='assessment.ticketing'}"/>
</sly>
```

**Artifact locations (for reference):**

| Layer | Path |
|-------|------|
| TS source | `ui.frontend/src/main/webpack/ticketing/` |
| Built clientlib | `ui.apps/.../clientlibs/clientlib-ticketing/` |
| Category | `assessment.ticketing` |

Redeploy: `mvn clean install -pl ui.apps,all -am -PautoInstallSinglePackage`

---

## Lesson

For app-specific clientlibs on a locked editable template, include the category from the **component** that owns the UI (`ticketapp`) if page-policy clientlibs do not appear in rendered HTML — otherwise TypeScript never runs and REST calls never fire.
