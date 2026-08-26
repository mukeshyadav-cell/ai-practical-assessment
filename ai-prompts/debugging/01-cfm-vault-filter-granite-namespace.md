# Debugging — CFM Vault Filter & Granite Namespace

**Date:** 2025-08-26
**Purpose:** Record build/package validation failures and developer fixes after Task 2.1.2 CFM generation.

**Sprint/Task:** 2.1 / 2.1.2–2.1.3
**Category:** debugging
**Related prompt:** [implementation/01-content-fragment-models.md](../implementation/01-content-fragment-models.md)

---

## Symptom

After AI-generated Ticket and Comment CFM `.content.xml` files were added, **`mvn clean install` failed** with FileVault / content-package validation errors. Package would not deploy CFMs to local AEM SDK until fixed.

---

## Root causes

### 1. Missing `xmlns:granite` on CFM model XML

**What happened:** AI copied `<granite:data>` child nodes from the reference `model1` export but omitted the granite namespace declaration on `jcr:root`.

**Error type:** Vault / XML validation (unbound `granite` prefix).

**Fix:**

```xml
xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
```

Added to `jcr:root` in:

- `ui.content/src/main/content/jcr_root/conf/assessment/settings/dam/cfm/models/ticket/.content.xml`
- `ui.content/src/main/content/jcr_root/conf/assessment/settings/dam/cfm/models/comment/.content.xml`

**Lesson:** When hand-authoring or AI-generating CFM XML from an AEM export, copy **all** namespace declarations from the reference file, not only `jcr`, `cq`, and `sling`. Any `granite:*` elements require the granite namespace.

**Reference:** Working export at `conf/assessment/settings/dam/cfm/models/model1/.content.xml` includes `xmlns:granite`.

---

### 2. Vault filter exclude blocked DAM paths needed for CFMs

**What happened:** Archetype `filter.xml` under `/content/dam/assessment` used an exclude/include pattern that only allowed `jcr:content` on the DAM root folder — blocking deployment of models, sample fragments, and future ticket/comment CF assets under `/content/dam/assessment/*`.

**Error type:** Vault filter violation / content not installed as expected when pushing models and DAM content.

**Before (archetype default):**

```xml
<filter root="/content/dam/assessment" mode="merge">
    <exclude pattern="/content/dam/assessment(/.*)?"/>
    <include pattern="/content/dam/assessment/jcr:content(/.*)?"/>
</filter>
```

**Fix (developer):** Commented out exclude/include so full DAM subtree merges:

```xml
<filter root="/content/dam/assessment" mode="merge">
   <!-- <exclude pattern="/content/dam/assessment(/.*)?"/>
    <include pattern="/content/dam/assessment/jcr:content(/.*)?"/>
    -->
</filter>
```

**File:** `ui.content/src/main/content/META-INF/vault/filter.xml`

**Lesson:** The archetype DAM filter is minimal by design (folder metadata only). Projects using DAM Content Fragments under `/content/dam/assessment/tickets` and `/comments` must allow those paths in the filter. Revisit when adding repoinit-created folders (Task 2.1.4).

**Note:** Explicit filters for CFM conf paths were already added by AI:

```xml
<filter root="/conf/assessment/settings/dam/cfm/models/ticket" mode="merge"/>
<filter root="/conf/assessment/settings/dam/cfm/models/comment" mode="merge"/>
```

Parent `/conf/assessment` merge also covers these; explicit entries aid clarity.

---

## Resolution

| Step | Action | Outcome |
|------|--------|---------|
| 1 | Add `xmlns:granite` to ticket + comment CFM XML | XML validation passes |
| 2 | Comment out DAM exclude/include in `filter.xml` | DAM + CFM paths deploy |
| 3 | `mvn clean install` / `-PautoInstallSinglePackage` | **SUCCESS** |
| 4 | Verify in AEM UI | Ticket + Comment models enabled and correct |

---

## Prevention (for future CFM / ui.content work)

1. **Always diff against a UI-exported CFM** — include every `xmlns:*` on `jcr:root`.
2. **Run package build before closing CFM tasks** — FileVault errors surface only at `mvn clean install`.
3. **Check `filter.xml` when adding under `/content/dam/assessment`** — archetype exclude pattern will silently block child nodes.
4. **When AI generates CFM XML** — prompt explicitly: "include all namespaces from reference export, especially `xmlns:granite`".

---

## Developer note (verbatim)

> you had missed vault filter and and one line i have fixed and build , its working now.
> Thanks
> added below lines as vault vilation erro message was coming
> xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
> in ticket model and comment Models
> also commented
>        <!-- <exclude pattern="/content/dam/assessment(/.*)?"/>
>         <include pattern="/content/dam/assessment/jcr:content(/.*)?"/>
>         -->
> as i need to push models

---

## Files touched (correction)

| File | Change |
|------|--------|
| `cfm/models/ticket/.content.xml` | +`xmlns:granite` |
| `cfm/models/comment/.content.xml` | +`xmlns:granite` |
| `META-INF/vault/filter.xml` | DAM exclude/include commented out |
