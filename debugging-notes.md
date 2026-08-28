# Debugging Notes — Issues & Resolutions

**Date:** 2026-08-27  
**Purpose:** Record notable build, deployment, and runtime issues encountered during the project and how they were resolved.

**Sprint/Task:** 8.1 / 8.1.1

---

Issues below are drawn from `ai-prompts/debugging/`, sprint prompt history, and the actual implementation. Issues that were **prevented by design** (not live bugs) are called out separately.

---

## Issue Index

| # | Area | Sprint | Severity |
|---|------|--------|----------|
| 1 | CFM Vault filter / Granite namespace | 2.1 | Build blocker |
| 2 | Service user / DAM ACLs | 3.1 / 5.1 | Runtime — empty tickets / 500 |
| 3 | User API listing (`GET /users`) | 5.1 | Runtime — empty/wrong assignees |
| 4 | Suffix servlet routing on local SDK | 5.1 | Runtime — 404 on sub-paths |
| 5 | Clientlib not loaded on tickets page | 6.1 | Runtime — no API calls |
| 6 | CSRF token on POST/PUT | 6.1 | Runtime — 403 |
| 7 | Create ticket — detail not loading | 6.1 | UX — stale view |
| 8 | Toast position | 6.2 | Cosmetic |

---

## 1. CFM Vault Filter & Granite Namespace

| | |
|---|---|
| **Symptom** | `mvn clean install` failed with FileVault / XML validation errors; CFMs did not deploy to local SDK |
| **Root cause** | (a) AI-generated CFM `.content.xml` used `<granite:data>` without `xmlns:granite` on `jcr:root`. (b) Archetype `filter.xml` excluded all nodes under `/content/dam/assessment` except `jcr:content`, blocking ticket/comment CF assets |
| **Fix** | Added `xmlns:granite="http://www.adobe.com/jcr/granite/1.0"` to ticket and comment CFM models. Commented out DAM exclude/include in `ui.content/.../META-INF/vault/filter.xml` |
| **Files** | `ui.content/.../cfm/models/ticket/.content.xml`, `comment/.content.xml`, `vault/filter.xml` |
| **Lesson** | Diff CFM XML against a UI export including all namespaces; revisit `filter.xml` when adding DAM content |

---

## 2. Service User Mapping & DAM Permissions

| | |
|---|---|
| **Symptom** | `GET /bin/api/v1/tickets` returned `[]` or `500`; ticket create failed; CFs not written under `/content/dam/assessment/tickets` |
| **Root cause** | Repository used `ResourceResolverFactory.getServiceResourceResolver()` with subservice `assessment-service`, but mapping or ACLs were missing/insufficient: no `jcr:read,rep:write` on DAM paths or `/var/assessment` counter nodes |
| **Fix** | Repoinit: create `assessment-service` user; ACLs on `/content/dam/assessment`, `/conf/assessment`, `/home/users` (read), `/var/assessment` (read/write). OSGi `ServiceUserMapperImpl` amended config: `ai-practical-assessment.core:assessment-service=[assessment-service]` |
| **Files** | `ui.config/.../RepositoryInitializer~assessment.cfg.json`, `ServiceUserMapperImpl.amended~assessment.cfg.json`, `ContentFragmentTicketRepository.java` |
| **Verify** | `curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets` returns `200`; POST create persists CF in CRXDE |

---

## 3. User API — Broad Authorizable Query

| | |
|---|---|
| **Symptom** | `GET /bin/api/v1/users` returned empty, noisy, or non-assignable entries; assignee picker broken; `UNKNOWN_USER` on create/reassign |
| **Root cause** | `AemUserRepository.getAll()` used overly broad `findAuthorizables` query; groups and users without `profile/email` were included or not filtered correctly |
| **Fix** | Query `jcr:primaryType = rep:User` explicitly; skip groups; require non-blank `profile/email` before mapping to `UserDTO` |
| **Files** | `AemUserRepository.java` |
| **Verify** | `curl -s -u admin:admin http://localhost:4502/bin/api/v1/users` returns seeded `agent-1`, `agent-2` |

---

## 4. Servlet Sub-Path Routing (Suffix vs Filter)

| | |
|---|---|
| **Symptom** | `GET /bin/api/v1/tickets/TKT-1001` returned HTML 404 from `DefaultGetServlet`; `suffix=null` in servlet resolver — sub-resources (`/status`, `/assignee`, `/comments`) also failed |
| **Root cause** | Pure Sling suffix servlet registration on `/bin/api/v1/tickets/` did not match reliably on **local AEM SDK** |
| **Fix** | Added dedicated `*RoutingFilter` classes (e.g. `TicketByIdRoutingFilter`, `TicketStatusRoutingFilter`) with regex path patterns; filters dispatch to handler servlets. Documented in sprint-5.1 prompt history as accepted workaround |
| **Files** | `TicketByIdRoutingFilter.java`, `TicketStatusRoutingFilter.java`, `TicketAssigneeRoutingFilter.java`, `CommentCollectionRoutingFilter.java`, `UserByIdRoutingFilter.java` |
| **Trade-off** | api-contract still describes suffix-style registration; runtime uses filters for SDK compatibility |
| **Verify** | curl `GET/PUT` on `/tickets/{id}`, `/tickets/{id}/status`, etc. return JSON not HTML |

---

## 5. Clientlib Not Loaded — API Never Called

| | |
|---|---|
| **Symptom** | Tickets page showed static skeleton; Network tab had **no** `GET /bin/api/v1/tickets`; only `clientlib-base` loaded |
| **Root cause** | `assessment.ticketing` clientlib existed after `npm run prod` but was not emitted on the page — page policy did not inject it; `ticketapp.html` did not include the clientlib |
| **Fix** | Added explicit clientlib include in `ticketapp.html`: `clientlib.css` and `clientlib.js` for category `assessment.ticketing` |
| **Files** | `ui.apps/.../ticketapp/ticketapp.html` |
| **Verify** | DevTools shows `clientlib-ticketing.js`; Network shows ticket API calls |

---

## 6. CSRF Token Missing (403 on Mutations)

| | |
|---|---|
| **Symptom** | `POST /bin/api/v1/tickets` and `PUT` operations returned **403**; AEM log: `CSRFFilter isValidRequest: empty CSRF token - rejecting` |
| **Root cause** | Frontend `fetch()` calls did not fetch Granite CSRF token or send `CSRF-Token` header for authenticated author sessions |
| **Fix** | Added `csrf.ts` (`fetchCsrfToken`, `fetchWithCsrf`); wired `createTicket`, `updateTicket`, and subsequent mutating calls through `fetchWithCsrf` |
| **Files** | `ui.frontend/.../ticketing/csrf.ts`, `api.ts` |
| **Verify** | Create/edit ticket succeeds with 201/200 |

---

## 7. Create Ticket — Detail View Not Initialized

| | |
|---|---|
| **Symptom** | After create modal Save, POST succeeded but new ticket detail did not load; list stale on return |
| **Root cause** | `navigateToTicketDetail()` only updated `window.location.search`; `main.ts` had already run `initTicketList()` on first load — `initTicketDetail()` never ran when `?id=` added client-side |
| **Fix** | On create success, use `window.location.href = pathname + ?id={newId}` for full page reload |
| **Files** | `ui.frontend/.../ticketing/form.ts` |
| **Verify** | Create → lands on detail with ticket data loaded |

---

## 8. Toast Position (Cosmetic)

| | |
|---|---|
| **Symptom** | Action toasts appeared top-right corner instead of top-center |
| **Root cause** | CSS positioned `.ticket-toast-container` at `right: 0` |
| **Fix** | Centered with `left: 50%` + `transform: translateX(-50%)` in `main.scss`; `npm run prod` |
| **Files** | `ui.frontend/.../ticketing/main.scss` |

---

## Prevented by Design (Not Live Bugs)

| Topic | Approach | Notes |
|-------|----------|-------|
| **Jackson `Instant` serialization** | `JavaTimeModule` + `WRITE_DATES_AS_TIMESTAMPS` disabled in `ServletResponseUtil` from Task 5.1.1 | Proactive — no recorded epoch-number JSON bug in production |
| **Java 21 vs 17** | Cloud Manager uses Java 21 (`.cloudmanager/java-version`); code avoids Java 21-only preview features; Maven `release` 11 in parent POM | Compatibility constraint, not a debugging incident |
| **State machine bypass** | `PUT /tickets/{id}` rejects `status`/`assignedTo` in body; only `changeStatus` path mutates status | Prevents servlet-level bypass |

---

## Debugging Workflow Used

1. **Build first** — `mvn clean install` catches Vault/XML issues early (CFM sprint).
2. **curl before UI** — isolate servlet/service vs frontend (Sprint 5.1).
3. **Browser DevTools Network** — confirmed missing clientlib and CSRF (Sprint 6.1).
4. **AEM error.log** — CSRF filter messages, service resolver login failures.
5. **CRXDE** — verify CF assets and `/var/assessment` counters after repository changes.

---

## Source References

Detailed prompt logs saved under:

- `ai-prompts/debugging/01-cfm-vault-filter-granite-namespace.md`
- `ai-prompts/debugging/02-user-api-fixes.md`
- `ai-prompts/debugging/03-service-user-issue-fixes.md`
- `ai-prompts/debugging/04-clientlibs-js-css-issue.md`
- `ai-prompts/debugging/05-csrf-token-post.md`
- `ai-prompts/debugging/06-create-ticket-enhancement.md`
- `ai-prompts/debugging/07-toast-position-enhancement.md`
