# Debugging — Ticket API / Service User Fixes

**Date:** 2026-08-27
**Purpose:** Record developer fixes so ticket REST endpoints can read/write Content Fragments under DAM using a dedicated service user resolver.

**Sprint/Task:** 3.1 / 5.1 — fix applied before Sprint 6.1 UI work
**Category:** debugging
**Related:** [implementation/02-repoinit-service-user-seed-users.md](../implementation/02-repoinit-service-user-seed-users.md), [implementation/05-content-fragment-ticket-repository-read.md](../implementation/05-content-fragment-ticket-repository-read.md), [implementation/06-content-fragment-ticket-repository-write.md](../implementation/06-content-fragment-ticket-repository-write.md)

---

## Symptom

Ticket REST endpoints (`GET /bin/api/v1/tickets`, create/update flows) returned empty lists, `500 INTERNAL_ERROR`, or failed to persist tickets. Content Fragment ticket assets under `/content/dam/assessment/tickets` were not accessible or writable with the repository’s resource resolver.

---

## Root cause

1. **No dedicated service user** — Repository code required a mapped `assessment-service` subservice, but ACLs or mapping were missing or insufficient on a fresh SDK install.
2. **Insufficient DAM permissions** — Service user lacked `jcr:read` / `rep:write` on `/content/dam/assessment` and ticket subfolders (`/tickets`, `/comments`).
3. **Wrong resolver context** — Ticket reads/writes must use `ResourceResolverFactory.getServiceResourceResolver()` (not an administrative resolver) and adapt DAM child resources to `ContentFragment` for CFM-backed persistence.

---

## Fix — Ticket API / Service User changes

### 1. Added `assessment-service` service user (repoinit)

**File:** `ui.config/.../org.apache.sling.jcr.repoinit.RepositoryInitializer~assessment.cfg.json`

```text
create service user assessment-service with path system/assessment
set ACL for assessment-service
    allow jcr:read on /home/users
    allow jcr:read on /conf/assessment
    allow jcr:read,rep:write on /content/dam/assessment
end
set ACL for assessment-service
  allow jcr:read,rep:write on /var/assessment
end
```

Also creates DAM paths:

- `/content/dam/assessment/tickets`
- `/content/dam/assessment/comments`
- `/var/assessment` (ID counters)

### 2. OSGi service-user mapping

**File:** `ui.config/.../org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~assessment.cfg.json`

```json
"ai-practical-assessment.core:assessment-service=[assessment-service]"
```

Maps bundle subservice `assessment-service` to JCR principal `assessment-service`.

### 3. Ticket repository uses service resolver + `ContentFragment` adapt

**File:** `core/src/main/java/com/mysite/core/repositories/impl/ContentFragmentTicketRepository.java`

Obtain resolver via subservice (never administrative):

```java
private ResourceResolver obtainServiceResolver() throws LoginException {
    Map<String, Object> authInfo = Map.of(
            ResourceResolverFactory.SUBSERVICE,
            SERVICE_SUBSERVICE);
    return resolverFactory.getServiceResourceResolver(authInfo);
}
```

List and load tickets by iterating DAM children and adapting to CF:

```java
for (Resource child : ticketsFolder.getChildren()) {
    ContentFragment contentFragment = child.adaptTo(ContentFragment.class);
    if (contentFragment == null) {
        continue;
    }
    tickets.add(TicketMapper.toDto(contentFragment));
}
```

`findContentFragmentByTicketId()` uses the same pattern: service resolver → `/content/dam/assessment/tickets` → `child.adaptTo(ContentFragment.class)` → match `ticketId` element.

All CRUD paths (`getAll`, `getById`, `create`, `update`) run inside `try (ResourceResolver resolver = obtainServiceResolver())`.

---

## Expected outcome

| Endpoint / operation | Before | After |
|----------------------|--------|-------|
| `GET /bin/api/v1/tickets` | `[]` or `500` | `200` + ticket array from CF assets |
| `POST /bin/api/v1/tickets` | Create failure / `500` | `201` + CF written under `/content/dam/assessment/tickets/{id}` |
| `GET /bin/api/v1/tickets/{id}` | `404` when CF exists | `200` when CF found via service resolver |

**Verify:**

```bash
# List tickets
curl -s -u admin:admin http://localhost:4502/bin/api/v1/tickets

# Create ticket
curl -s -u admin:admin -X POST http://localhost:4502/bin/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Test ticket","description":"Service user check","priority":"P2"}'
```

Confirm CF asset exists in CRXDE at `/content/dam/assessment/tickets/TKT-*`.

---

## Lesson

Content Fragment repositories on AEMaaCS must use a **mapped service user** with explicit ACLs on DAM + `/var` counter paths, and resolve tickets by adapting DAM child resources to `com.adobe.cq.dam.cfm.ContentFragment` — not by reading JCR properties on the folder node alone.
