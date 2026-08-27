# Debugging — CSRF Token Required for POST/PUT (403)

**Date:** 2026-08-27
**Purpose:** Record diagnosis and fix when ticket create/edit POST/PUT failed with HTTP 403 due to missing AEM CSRF token.

**Sprint/Task:** 6.1 / 6.1.3 — create/edit ticket form
**Category:** debugging
**Related:** [implementation/25-ticket-form.md](../implementation/25-ticket-form.md)

---

## Prompt (verbatim)

> Everything working as expected but AEM requires csrf token for post request for logged in user
> AEM logs confirm the issue:
> CSRFFilter isValidRequest: empty CSRF token - rejecting
>
> You are working on an AEM project with a custom UI that creates/saves a Content Fragment Model through a POST servlet.
> The current UI successfully opens the "New ticket" / creation dialog and submits the form, but the POST request fails with HTTP 403. The AEM environment is protected by CSRF protection, so the likely issue is that the browser POST does not include a valid AEM CSRF token.
> Please inspect the existing frontend code, servlet, and related AEM configuration before making changes. Do not rewrite unrelated functionality.
>
> Required fix: Before POST, call `/libs/granite/csrf/token.json`, read token, add `CSRF-Token: <token>` header. Reuse existing CSRF utility if present.

---

## AI response summary

Added `csrf.ts` (`fetchCsrfToken`, `fetchWithCsrf`); wired `createTicket` and `updateTicket` in `api.ts` to use it. No backend changes.

---

## Symptom

"+ New Ticket" form opened and submitted, but `POST /bin/api/v1/tickets` returned **403**. AEM log: `CSRFFilter isValidRequest: empty CSRF token - rejecting`. Edit (`PUT`) would fail the same way for logged-in users.

---

## Root cause

Authenticated AEM sessions require Granite CSRF token on mutating requests. `api.ts` used plain `fetch` for POST/PUT without calling `/libs/granite/csrf/token.json` or sending the `CSRF-Token` header. No CSRF helper existed in the ticketing frontend (Cypress tests used the pattern in `ui.tests` only).

---

## Fix

**New file:** `ui.frontend/src/main/webpack/ticketing/csrf.ts`

- `fetchCsrfToken()` — GET `/libs/granite/csrf/token.json`, parse `token`
- `fetchWithCsrf(url, options)` — adds `CSRF-Token` header before fetch

**Updated:** `ui.frontend/src/main/webpack/ticketing/api.ts`

- `createTicket()` (POST) and `updateTicket()` (PUT) call `fetchWithCsrf` instead of `fetch`
- URL, body, `Content-Type`, and business logic unchanged

Redeploy: `mvn clean install -PautoInstallSinglePackage`

---

## Lesson

Any authenticated POST/PUT/DELETE to AEM (including `/bin/api/v1/*` servlets) must fetch Granite CSRF token and send `CSRF-Token` header. Reuse `fetchWithCsrf` for future mutating API calls (comments, status, reassign in 6.1.4–6.1.6).
