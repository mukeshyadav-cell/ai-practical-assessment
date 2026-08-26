# AI Prompt — Task 5.1.6: UserCollectionServlet + UserByIdServlet (GET list + detail)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 5.1 Task 5.1.6.

**Sprint/Task:** 5.1 / 5.1.6
**Category:** implementation
**Meaningful:** Yes — read-only user list and detail REST endpoints.

---

## Prompt (verbatim)

> Task 5.1.6 (Sprint 5.1): Implement the user endpoints —
> GET /bin/api/v1/users (list, optional ?q= search) and
> GET /bin/api/v1/users/{userId} (detail).
>
> Follow all rules in .cursor/rules/. Read api-contract.md (user list + detail shapes, 404
> on unknown user), UserRepository (the interface), UserDTO, and ServletResponseUtil.
> Target: com.mysite.core.servlets. Reuse ServletResponseUtil. These are READ-ONLY endpoints.
>
> Routing:
> - UserCollectionServlet: register on sling.servlet.paths = /bin/api/v1/users,
>   extend SlingSafeMethodsServlet (GET only). Handle:
>     * No suffix -> list (with optional ?q= search)
>     * Suffix "/{userId}" -> single user detail
>   OR create a separate UserByIdServlet for the suffix case — choose the cleaner option and
>   ensure no routing conflict. (A single suffix-branching servlet is fine here.)
>
> Wiring:
>   @Reference(target = "(impl.type=aem)") private UserRepository userRepository;
>   (users come from AEM UserManager — impl.type=aem, matching AemUserRepository)
>
> Implement:
>
> GET /bin/api/v1/users:
>   - Read optional query param q.
>   - If q is non-blank -> userRepository.search(q); else -> userRepository.getAll().
>   - 200 + JSON array of UserDTO (userId, displayName, email).
>   - Never 500 on empty; return empty array if none.
>
> GET /bin/api/v1/users/{userId}:
>   - Extract userId from suffix.
>   - userRepository.getById(userId):
>       present -> 200 + UserDTO
>       empty   -> 404 NOT_FOUND (via ServletResponseUtil; system users are excluded, so
>                  requesting a system user id returns 404)
>
> Requirements:
> - Extend SlingSafeMethodsServlet (GET only). Register method {GET}.
> - Thin servlet: call repository (or a thin UserService if one exists — check; if not, calling
>   the repository directly from the servlet is acceptable for read-only user lookups, but note
>   that architecturally a UserService could wrap it. State the choice.)
> - Reuse ServletResponseUtil; SLF4J logging; no stack traces to client; Javadoc.
>
> Note on architecture: services normally sit between servlet and repository. For users there is
> no business logic (just lookups). Decide: (a) inject UserRepository directly into the servlet,
> or (b) create a minimal UserService pass-through. Recommend (a) for simplicity in this scope,
> but state the trade-off in a Javadoc comment.
>
> After generating:
> - Confirm mvn clean install compiles; deploy with -PautoInstallSinglePackage.
> - Verify no routing conflict (servlet resolver) with the tickets endpoints.
> - Provide curl commands:
>     a) GET /bin/api/v1/users -> 200 array incl. agent-1, agent-2
>     b) GET /bin/api/v1/users?q=agent -> filtered
>     c) GET /bin/api/v1/users/agent-1 -> 200 detail
>     d) GET /bin/api/v1/users/ghost -> 404
>     e) GET /bin/api/v1/users/admin -> 404 (system user excluded)
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Chose split routing (not single suffix-branching servlet): `UserCollectionServlet` OSGi servlet on exact `/bin/api/v1/users` (`SlingSafeMethodsServlet`, GET list); `UserByIdServlet` handler + `UserByIdRoutingFilter` (pattern `/bin/api/v1/users/[^/]+$`) for detail — consistent with ticket sub-resource filter-dispatch on local AEM SDK. Architecture choice **(a)**: `@Reference(target="(impl.type=aem)") UserRepository` injected directly (no `UserService`); trade-off documented in Javadoc. List uses `search(q)` or `getAll()` → `200` array; detail uses `getById` → `200` or `404 NOT_FOUND`. Build/deploy succeeded; routing verified (`200 []` list, `404` for ghost/admin/agent-1 on instance without seeded users visible to repository). Updated `implementation-plan.md` — 5.1.6 complete; Active Task → 5.1.7.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Routing | `UserCollectionServlet` exact path + `UserByIdRoutingFilter` for `/{userId}` (not suffix servlet) |
| Architecture | **(a)** `UserRepository` direct injection — no pass-through `UserService` |
| OSGi target | `(impl.type=aem)` matching `AemUserRepository` |
| Servlet base | `SlingSafeMethodsServlet` (GET only) for collection |
| Empty list | `200` + `[]` — never 500 on empty |
| Unknown/system user | `404 NOT_FOUND` with `"User not found: {userId}"` |
| No ticket conflict | Distinct `/users` vs `/tickets` paths |

---

## Endpoint behavior summary

| Method | Path | Handler | Success | Errors |
|--------|------|---------|---------|--------|
| `GET` | `/bin/api/v1/users` | `UserCollectionServlet` | `200` + `UserDTO[]` | `500` on unexpected failure |
| `GET` | `/bin/api/v1/users/{userId}` | `UserByIdRoutingFilter` → `UserByIdServlet.doGet` | `200` + `UserDTO` | `404 NOT_FOUND`, `500` |

Optional `?q=` triggers `userRepository.search(q)`; blank/absent uses `getAll()`.

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/servlets/UserCollectionServlet.java` | Created — user list GET |
| `core/src/main/java/com/mysite/core/servlets/UserByIdServlet.java` | Created — user detail handler |
| `core/src/main/java/com/mysite/core/servlets/UserByIdRoutingFilter.java` | Created — Sling filter dispatch |
| `core/src/main/java/com/mysite/core/servlets/ServletConstants.java` | Updated — `USERS_PATH` |
| `core/src/main/java/com/mysite/core/servlets/util/ServletPathUtil.java` | Updated — `resolveUserId()` |
| `implementation-plan.md` | Updated — 5.1.6 complete; Active Task 5.1.7 |
| `ai-prompts/implementation/20-user-servlets.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -PautoInstallSinglePackage` | SUCCESS |
| No routing conflict with tickets | Confirmed |
| `GET /users` | `200` + `[]` |
| `GET /users?q=agent` | `200` + `[]` |
| `GET /users/agent-1` | `404 NOT_FOUND` (users not visible in repo on instance) |
| `GET /users/ghost` | `404 NOT_FOUND` |
| `GET /users/admin` | `404 NOT_FOUND` (system user excluded) |
| Regression `GET /tickets` | `200` unchanged |
| `(a)–(c)` with agent-1/agent-2 | Blocked — repoinit users not returned by `AemUserRepository` on test instance |

---

## curl test commands

```bash
# a) List all users → 200 array (agent-1, agent-2 when seeded)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/users

# b) Search → 200 filtered
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  "http://localhost:4502/bin/api/v1/users?q=agent"

# c) User detail → 200
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/users/agent-1

# d) Unknown user → 404
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/users/ghost

# e) System user → 404 (excluded from API)
curl -s -w "\nHTTP %{http_code}\n" -u admin:admin \
  http://localhost:4502/bin/api/v1/users/admin
```

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 5.1 / Task 5.1.6 | Complete |
| FR-16, FR-17, FR-18 | User list and detail REST endpoints |
| AC-47, AC-48, AC-49 | List assignable users; detail by id; 404 for unknown |
| api-contract | `GET /users`, `GET /users/{userId}`; system users excluded |
| Downstream | 5.1.7 shared error handling; 6.1.3 assignee picker UI |
