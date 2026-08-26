# Debugging — User API Fixes (AemUserRepository)

**Date:** 2026-08-27
**Purpose:** Record developer fixes to user listing/query logic so `GET /bin/api/v1/users` returns only assignable seeded users.

**Sprint/Task:** 5.1 / 5.1.6 (UserCollectionServlet) — fix applied before Sprint 6.1 UI work
**Category:** debugging
**Related:** [implementation/08-aem-user-repository.md](../implementation/08-aem-user-repository.md), [implementation/20-user-servlets.md](../implementation/20-user-servlets.md)

---

## Symptom

`GET /bin/api/v1/users` did not return the expected assignable users for the ticketing UI (assignee picker / `UNKNOWN_USER` on create/reassign). User enumeration in `AemUserRepository.getAll()` was too broad or included non-assignable authorizables.

---

## Root cause

1. **Broad authorizable search** — `findAuthorizables("rep:principalName", "%", UserManager.SEARCH_TYPE_USER)` could return unwanted entries; query did not target `rep:User` nodes explicitly.
2. **Groups and incomplete profiles** — Groups and users without a `profile/email` value were not filtered out before mapping to `UserDTO`.

---

## Fix — User API changes

**File:** `core/src/main/java/com/mysite/core/repositories/impl/AemUserRepository.java`

### 1. Query `rep:User` authorizables explicitly

Replace broad principal-name search with `jcr:primaryType = rep:User`:

```java
Iterator<Authorizable> authorizables =
        userManager.findAuthorizables(
                "jcr:primaryType",
                "rep:User",
                UserManager.SEARCH_TYPE_USER);
```

### 2. Exclude groups and users without profile email

In the `getAll()` loop, skip groups and users missing a non-blank `profile/email` before mapping:

```java
if (authorizable.isGroup()) {
    continue;
}
User user = (User) authorizable;
String email = user.getProperty("profile/email") != null
        ? user.getProperty("profile/email")[0].getString()
        : null;

if (email == null || email.isBlank()) {
    continue;
}
```

### 3. Return shape (unchanged contract)

Continue mapping assignable users to `UserDTO` with:

- `userId`
- `displayName` (from `profile/givenName` + `profile/familyName`, or `userId` fallback)
- `email`

Existing `isAssignableUser()` still excludes system users after the email filter.

---

## Expected outcome

| Endpoint | Before | After |
|----------|--------|-------|
| `GET /bin/api/v1/users` | Empty, noisy, or non-assignable entries | Only users with `rep:User`, valid email, not a group/system user |
| `GET /bin/api/v1/users/{userId}` | Same assignability rules via `isAssignableUser()` | Consistent with list filtering |

**Verify:**

```bash
curl -s -u admin:admin http://localhost:4502/bin/api/v1/users
# Expect 200 + JSON array with agent-1, agent-2 (seeded via repoinit with profile/email)
```

---

## Lesson

When listing AEM users for UI assignee pickers, query `rep:User` explicitly, skip groups, and require `profile/email` so the REST layer only exposes assignable directory entries aligned with `UserDTO` and api-contract.
