# AI Prompt — Task 2.1.4: Repoinit, Service User & Seed Users

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt and AI response summary for Sprint 2.1 Task 2.1.4.

**Sprint/Task:** 2.1 / 2.1.4
**Category:** implementation
**Meaningful:** Yes — repoinit, OSGi service user mapping, DAM folders, seed users, least-privilege ACLs.

---

## Prompt (verbatim)

> Task 2.1.4 (Sprint 2.1): Create repoinit + OSGi config for DAM folders, service user, and seed users.
>
> Follow all rules in .cursor/rules/. Read data-model.md and implementation-plan.md.
> Target module: ui.config. This is AEM configuration, not Java code.
>
> IMPORTANT: repoinit syntax is strict. If unsure of exact repoinit grammar, SAY SO and
> provide the closest-correct statements with a note to verify. Do NOT invent directives.
>
> Create the following in ui.config (jcr_root/apps/assessment/osgiconfig/config or the
> appropriate config folder the archetype uses — check the existing ui.config structure first):
>
> 1. RepositoryInitializer factory config (repoinit) — a file like:
>    org.apache.sling.jcr.repoinit.RepositoryInitializer~assessment.cfg.json
>    With repoinit "scripts" that:
>
>    a) Create DAM folder structure (sling:OrderedFolder or nt:folder as appropriate):
>       - /content/dam/assessment/tickets
>       - /content/dam/assessment/comments
>       (create parent /content/dam/assessment only if it does not already exist —
>        note: it may already exist from the archetype sample)
>
>    b) Create a system service user:
>       - create service user assessment-service with path system/assessment
>
>    c) Grant the service user ACLs:
>       - allow read + write (jcr:read, rep:write) on /content/dam/assessment
>       - allow read (jcr:read) on /home/users  (to resolve assignees)
>
>    d) Optional Create two seed  only if possible as it is AEM as cloud service and with password might not be possible(regular) users for assignees:
>       - create user agent-1 with password (use a placeholder; note it must NOT be a real secret)
>       - create user agent-2 with password placeholder
>       - set their profile properties: profile/givenName, profile/familyName, profile/email
>       NOTE: For AEMaaCS, seeding regular users via repoinit is acceptable for a learning
>       project. If passwords in repoinit are a concern, explain the safer alternative
>       (seed via a one-time script or AEM UI) and still provide the repoinit version.
>
> 2. Service User Mapping OSGi config:
>    org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~assessment.cfg.json
>    Mapping the core bundle subservice to the service user, e.g.:
>    "user.mapping": [ "com.mysite.core:assessment-service=assessment-service" ]
>    (Confirm the exact bundle symbolic name from core/pom.xml or the generated bnd config;
>     if unknown, state the assumption and how to verify.)
>
> 3. Ensure ui.config filter.xml includes the config path so it deploys.
>
> Constraints:
> - No real secrets. Use obvious placeholder passwords and add a comment/// note.
> - Do not grant admin/all — least privilege only.
> - The subservice name used here (assessment-service) MUST match what the repositories
>   will request via getServiceResourceResolver later (Sprint 3.1).
>
> After generating: tell me EXACTLY how to verify after deploy
> (OSGi console service user mapping, /system/console/configMgr, and that the users/folders
> exist in CRXDE), and the mvn deploy command.
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.

---

## AI response summary

Updated `RepositoryInitializer~assessment.cfg.json` in `ui.config` with repoinit scripts: DAM root (idempotent), `tickets` and `comments` folders as `sling:OrderedFolder`, service user `assessment-service` at `system/assessment`, principal ACLs (`jcr:read` + `rep:write` on `/content/dam/assessment`; `jcr:read` on `/home/users`), and seed users `agent-1` / `agent-2` with placeholder password `changeme-local-dev-only` and profile properties per `data-model.md`. Created `ServiceUserMapperImpl.amended~assessment.cfg.json` with verified bundle symbolic name `ai-practical-assessment.core` (from built core JAR manifest), mapping `ai-practical-assessment.core:assessment-service=[assessment-service]`. Confirmed `ui.config` filter.xml already includes `/apps/assessment/osgiconfig`. Documented deploy command, AEM UI/CRXDE verification steps, and safer alternative (create users via Security UI on shared/cloud author). Updated `implementation-plan.md` — 2.1.4 complete; Active Task 2.1.5.

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Bundle symbolic name | **`ai-practical-assessment.core`** (verified via `mvn -pl core package` → `META-INF/MANIFEST.MF`) — not `com.mysite.core` |
| Service user mapping syntax | Bracket form: `ai-practical-assessment.core:assessment-service=[assessment-service]` |
| ACL style | `set principal ACL for assessment-service` (principal-based, least privilege) |
| Seed user password | `changeme-local-dev-only` — obvious placeholder, not a production secret |
| Subservice name | `assessment-service` — must match Sprint 3.1 `ResourceResolverFactory.SUBSERVICE` |
| filter.xml | No change — `/apps/assessment/osgiconfig` already in workspace filter |

---

## Artifacts produced

| File | Change |
|------|--------|
| `ui.config/.../RepositoryInitializer~assessment.cfg.json` | Updated — folders, service user, ACLs, seed users |
| `ui.config/.../ServiceUserMapperImpl.amended~assessment.cfg.json` | Created |
| `implementation-plan.md` | Updated — 2.1.4 complete; Active Task 2.1.5 |
| `ai-prompts/implementation/02-repoinit-service-user-seed-users.md` | Created (this file) |

---

## Deploy & verification (from response)

**Deploy:** `mvn clean install -PautoInstallSinglePackage` (or `mvn clean install -pl ui.config -PautoInstallPackage`)

**Verify:**
1. **configMgr** — RepositoryInitializer~assessment scripts present; no repoinit errors in slinglog
2. **configMgr** — ServiceUserMapperImpl.amended~assessment mapping correct
3. **CRXDE** — `/content/dam/assessment/tickets`, `/comments` exist; `cq:conf` on DAM root
4. **User Admin / CRXDE** — `assessment-service` system user; `agent-1`, `agent-2` with profile props

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 2.1 / Task 2.1.4 | Complete |
| DOD-3 (Sprint 2.1) | DAM folders; `cq:conf` → `/conf/assessment` |
| DOD-4 (Sprint 2.1) | Service user mapping active |
| [data-model.md](../../data-model.md) §5 | Seed users agent-1, agent-2; service user assessment-service |
| Downstream | DTOs (2.1.5), repository interfaces (2.1.6), CF adapters use subservice (3.1) |
