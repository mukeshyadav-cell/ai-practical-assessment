# AI Prompt — Task 3.1.4: AEM UserManager UserRepository Adapter

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 3.1 Task 3.1.4.

**Sprint/Task:** 3.1 / 3.1.4
**Category:** implementation
**Meaningful:** Yes — UserManager-backed read adapter, impl.type=aem, profile mapping, system-user filtering.

---

## Prompt (verbatim)

> Task 3.1.4 (Sprint 3.1): Implement the AEM UserManager-backed UserRepository adapter
> (getById, getAll, search).
>
> Follow all rules in .cursor/rules/. Read data-model.md (UserDTO: userId, displayName, email;
> users come from AEM UserManager, NOT Content Fragments), the UserRepository interface, and UserDTO.
> Target: com.mysite.core.repositories.impl.
>
> IMPORTANT AEM correctness (do NOT invent APIs; if unsure, say so and give closest-correct):
> - Obtain a service ResourceResolver via subservice "assessment-service", try-with-resources
>   (service user must have read on /home/users — granted in Sprint 2.1.4).
> - UserManager via resolver.adaptTo(org.apache.jackrabbit.api.security.user.UserManager.class)
> - Look up: userManager.getAuthorizable(String id) -> org.apache.jackrabbit.api.security.user.Authorizable
> - Filter: include only instances of org.apache.jackrabbit.api.security.user.User
>   AND NOT user.isSystemUser(). Exclude groups.
> - Read profile props via authorizable.getProperty("profile/<name>") which returns javax.jcr.Value[]
>   (guard null/empty; take first value's getString()).
>
> Create class:
>   com.mysite.core.repositories.impl.AemUserRepository
>
> Requirements:
>
> 1. OSGi component:
>    @Component(service = UserRepository.class, property = "impl.type=aem")
>    @Reference private ResourceResolverFactory resolverFactory;
>    Constant SERVICE_SUBSERVICE = "assessment-service".
>    (Note: use impl.type=aem here, since users come from AEM, not content fragments.)
>
> 2. Optional<UserDTO> getById(String userId):
>    - getAuthorizable(userId); if null / not a User / system user -> Optional.empty()
>    - else map to UserDTO via toDto(user)
>
> 3. List<UserDTO> getAll():
>    - Enumerate non-system Users. Prefer userManager.findAuthorizables with a query selecting
>      User class; if that API is uncertain, iterate a known approach and filter in code.
>    - Skip system users and groups. Return empty list if none (never null).
>    - (For a learning scope, it is acceptable to focus on returning the seeded users
>      agent-1, agent-2; do NOT hardcode them — return all real non-system users.)
>
> 4. List<UserDTO> search(String query):
>    - Case-insensitive match of query against displayName or email (or userId).
>    - If query is null/blank, return getAll() (or empty list — pick one and Javadoc it;
>      prefer returning empty list for blank to avoid dumping all users in UI autocomplete).
>    - Filter over getAll() for simplicity (correctness first).
>
> 5. Private helper UserDTO toDto(User user):
>    - userId    = user.getID()
>    - displayName = profile/givenName + " " + profile/familyName (trim; fall back to userId
>      if both blank)
>    - email     = profile/email (may be null)
>    Handle RepositoryException with SLF4J logging; skip problematic users gracefully.
>
> 6. Javadoc on class + methods. Never leak Authorizable/User/Value outside the class
>    (return only UserDTO/Optional).
>
> After generating:
> - Confirm mvn clean install compiles and the component becomes satisfied in OSGi console.
> - Tell me how to smoke test: getAll() should include agent-1 and agent-2 (seeded in 2.1.4);
>   getById("agent-1") returns their UserDTO; getById("admin") should be excluded if admin is
>   treated as system, or included if it's a regular user (explain the behavior).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Created `AemUserRepository` in `com.mysite.core.repositories.impl` as OSGi `@Component` with `impl.type=aem` (not `contentfragment`). Uses service resolver (`assessment-service`) and `UserManager.getAuthorizable` / `findAuthorizables("rep:principalName", "%", SEARCH_TYPE_USER)` to enumerate users. Filters groups and system users via `isAssignableUser`. `toDto` maps `profile/givenName`, `profile/familyName`, and `profile/email` from `Authorizable.getProperty`. `search` returns empty list for blank query; matches `userId`, `displayName`, or `email` case-insensitively over `getAll()`. Verified `mvn clean install -pl core -am`. Updated `implementation-plan.md` — 3.1.4 complete; Active Task → 3.1.5. Documented that `admin` is a regular user (`isSystemUser()` false) and is included in results.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| OSGi property | `impl.type=aem` — users from UserManager, not CF |
| User enumeration | `findAuthorizables("rep:principalName", "%", UserManager.SEARCH_TYPE_USER)` |
| Filtering | `User` instances only; exclude groups (`isGroup()`) and `isSystemUser()` |
| Blank search | Returns **empty list** (not `getAll()`) — avoids full user dump in autocomplete |
| Search fields | `userId`, `displayName`, `email` (case-insensitive contains) |
| displayName | `givenName + " " + familyName` trimmed; fallback to `userId` |
| `admin` user | Regular `rep:User` — **included** in `getAll()` and returned by `getById("admin")` |
| Service reference (downstream) | Sprint 4.1 services need `@Reference(target="(impl.type=aem)")` for `UserRepository` |

---

## Artifacts produced

| File | Change |
|------|--------|
| `core/src/main/java/com/mysite/core/repositories/impl/AemUserRepository.java` | Created |
| `implementation-plan.md` | Updated — 3.1.4 complete; Active Task 3.1.5 |
| `ai-prompts/implementation/08-aem-user-repository.md` | Created (this file) |

---

## Build verification

| Check | Result |
|-------|--------|
| `mvn clean install -pl core -am` | SUCCESS |

---

## AEM APIs verified

| Class | Method | Source |
|-------|--------|--------|
| `org.apache.jackrabbit.api.security.user.UserManager` | `getAuthorizable(String)` | `aem-sdk-api` javap |
| `org.apache.jackrabbit.api.security.user.UserManager` | `findAuthorizables(String, String, int)` | `aem-sdk-api` javap |
| `org.apache.jackrabbit.api.security.user.User` | `isSystemUser()` | `aem-sdk-api` javap |
| `org.apache.jackrabbit.api.security.user.Authorizable` | `getProperty(String)` | Task spec / javap |

---

## Smoke test notes

1. Deploy: `mvn clean install -PautoInstallBundle -pl core`.
2. OSGi Console → `AemUserRepository` → Active / Satisfied; `impl.type=aem`.
3. `getAll()` includes `agent-1`, `agent-2` (repoinit seed users).
4. `getById("agent-1")` → `UserDTO` with displayName and email from profile.
5. `getById("admin")` → included (regular user, not system user).

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 3.1 / Task 3.1.4 | Complete |
| FR-16, FR-17 | User list/get persistence path |
| [data-model.md](../../data-model.md) §5 | UserManager, profile props, skip system users |
| Downstream | Task 3.1.5 (mappers); Sprint 4.1 (`@Reference(target="(impl.type=aem)")`) |
