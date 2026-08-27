# Debugging — Create Ticket: Page Refresh After Save

**Date:** 2026-08-27
**Purpose:** Record fix when new ticket was not fetched after create modal save.

**Sprint/Task:** 6.1 / 6.1.3 — create ticket form
**Category:** debugging
**Related:** [implementation/25-ticket-form.md](../implementation/25-ticket-form.md), [debugging/05-csrf-token-post.md](05-csrf-token-post.md)

---

## Prompt (verbatim)

> Its working as expected but need one more fix like after click sav button of create modal it should refresh page so that it fetches newly created ticket as well

---

## AI response summary

Create success now uses `window.location.href` to `?id=<newId>` for a full page reload so `initTicketDetail()` runs and fetches the new ticket.

---

## Symptom

After **+ New Ticket** → Save, POST succeeded but the UI did not show the newly created ticket reliably (detail not initialized; list stale on back).

---

## Root cause

`navigateToTicketDetail()` only updated `window.location.search` without a full reload. `main.ts` had already run `initTicketList()` on first load; `initTicketDetail()` never ran when `?id=` was added client-side.

---

## Fix

**File:** `ui.frontend/src/main/webpack/ticketing/form.ts`

On create success, replace query-only navigation with full page load:

```typescript
const detailUrl = `${window.location.pathname}?id=${encodeURIComponent(ticket.id)}`;
window.location.href = detailUrl;
```

Removed unused `navigateToTicketDetail` import from `form.ts`.

---

## Lesson

When view init is split by URL (`?id=` → detail vs list), use `window.location.href` (full reload) after create so the correct init path runs and REST data is fresh.
