# AI Prompt — Task 2.1.2: Content Fragment Models (Ticket + Comment)

**Date:** 2025-08-26
**Purpose:** Record the implementation prompt, AI response summary, and developer corrections for Sprint 2.1 Tasks 2.1.2–2.1.3 (CFM creation).

**Sprint/Task:** 2.1 / 2.1.2 (also delivers 2.1.3 Comment CFM in same session)
**Category:** implementation
**Meaningful:** Yes — CFM creation task, AEM correctness constraints, reference samples provided.

---

## Prompt (verbatim)

> Task 2.1.2 (Sprint 2.1): Create the Content Fragment Models for Ticket and Comment.
>
> Follow all rules in .cursor/rules/. Read data-model.md for the exact fields, types,
> and validation. This creates AEM configuration (CFM definitions), not Java code.
>
> IMPORTANT AEM correctness:
> - CFMs are stored as JCR nodes. Create them under:
>   ui.content/src/main/content/jcr_root/conf/assessment/settings/dam/cfm/models/
> - Each model is a folder with a .content.xml defining the model node
>   (cq:Template / model structure with elements).
> - Use the CORRECT AEM CFM node structure. If you are unsure of the exact XML for
>   Content Fragment Models, SAY SO and provide the closest correct structure plus a
>   note that I should verify/create it in the AEM UI (Tools > Assets > Content Fragment Models)
>   and export back. Do NOT invent node types.
>
> Create:
>
> 1. Ticket CFM at .../models/ticket/.content.xml
>    Fields (from data-model.md):
>    - ticketId     : single-line text
>    - title        : single-line text (required)
>    - description  : multi-line text
>    - priority     : enumeration [Low, Medium, High, Critical]
>    - status       : enumeration [Open, In Progress, Resolved, Closed, Cancelled]
>    - assignedTo   : single-line text (userId — NOT a content reference)
>    - createdBy    : single-line text (userId)
>    - createdAt    : date and time
>    - updatedAt    : date and time
>
> 2. Comment CFM at .../models/comment/.content.xml
>    Fields:
>    - commentId    : single-line text
>    - ticketId     : single-line text (FK — NOT a content reference)
>    - message      : multi-line text (required)
>    - createdBy    : single-line text (userId)
>    - createdAt    : date and time
>
> 3. Ensure the parent folder nodes exist with correct jcr:primaryType
>    (sling:Folder / the appropriate types for /conf/.../cfm/models).
>
> 4. Add filter.xml entries (in ui.content META-INF/vault/filter.xml) so these
>    /conf/assessment paths are included in the content package.
>
> Constraints:
> - assignedTo, createdBy, ticketId MUST be plain text fields (store userIds/IDs as strings),
>   NEVER AEM Content References — this preserves DB-swappability.
> - Do NOT create any Content Fragment instances yet (that is a later task).
>
> After generating, list EXACTLY what I must verify in the AEM UI after deploying, and
> tell me the mvn command to deploy (mvn clean install -PautoInstallSinglePackage).
> When done, remind me to save this prompt + response summary to ai-prompts/implementation/.
>
> I have created sample content fragment and model for reference with most data types available in AEM and kept in the folder use this for referene to make compatible and correct model for AEm as cloud service AEM content and content fragment models
> @ui.content/src/main/content/jcr_root/content/dam/assessment/sample-content-fragment/
> @ui.content/src/main/content/jcr_root/conf/assessment/settings/dam/cfm/models/model1/

---

## AI response summary

Created Ticket and Comment CFM definitions under `ui.content/.../conf/assessment/settings/dam/cfm/models/` using the developer-provided `model1` export as the structural reference (`cq:Template`, scaffolding dialog, `metaType`/`valueType`/`sling:resourceType` per field). Applied `data-model.md` overrides to the prompt: priority enum `P1`–`P4` (not Low/Medium/High/Critical); `createdAt`/`updatedAt` as single-line text for ISO-8601 strings (not date pickers); all FK fields as plain `textfield` (no content/fragment references). Added explicit `filter.xml` entries for `ticket` and `comment` model paths. Updated `implementation-plan.md` — tasks 2.1.2 and 2.1.3 complete; Active Task advanced to 2.1.4. No Content Fragment instances created.

**Deploy command documented:** `mvn clean install -PautoInstallSinglePackage`

---

## Developer corrections (post-AI)

Build initially failed FileVault validation. Developer fixed two issues; see [debugging/01-cfm-vault-filter-granite-namespace.md](../debugging/01-cfm-vault-filter-granite-namespace.md).

| Issue | Fix | Files |
|-------|-----|-------|
| Missing `granite` XML namespace | Added `xmlns:granite="http://www.adobe.com/jcr/granite/1.0"` to `jcr:root` (required for `<granite:data>` nodes) | `ticket/.content.xml`, `comment/.content.xml` |
| Vault filter blocked DAM content | Commented out archetype exclude/include under `/content/dam/assessment` so CFM-related DAM paths deploy | `ui.content/.../META-INF/vault/filter.xml` |

**Result after fixes:** `mvn clean install` / package deploy **SUCCESS**; CFMs visible and working in AEM.

---

## Artifacts produced

| File | Change |
|------|--------|
| `ui.content/.../cfm/models/ticket/.content.xml` | Created (developer: +granite xmlns) |
| `ui.content/.../cfm/models/comment/.content.xml` | Created (developer: +granite xmlns) |
| `ui.content/.../META-INF/vault/filter.xml` | Updated — explicit CFM filters; DAM exclude commented (developer) |
| `implementation-plan.md` | Updated — 2.1.2, 2.1.3 complete; Active Task 2.1.4 |
| `ai-prompts/implementation/01-content-fragment-models.md` | Created (this file) |
| `ai-prompts/debugging/01-cfm-vault-filter-granite-namespace.md` | Created — correction log |

---

## AEM UI verification checklist (post-deploy)

1. **Tools → Assets → Content Fragment Models** — Ticket and Comment models appear, status **Enabled**.
2. **Ticket** — 9 fields: `ticketId`, `title`, `description`, `priority` (P1–P4), `status` (5 values), `assignedTo`, `createdBy`, `createdAt`, `updatedAt`; FK/timestamp fields are text, not references.
3. **Comment** — 5 fields: `commentId`, `ticketId`, `message`, `createdBy`, `createdAt`; `ticketId` is plain text.
4. **Target paths** — Ticket → `/content/dam/assessment/tickets`; Comment → `/content/dam/assessment/comments`.
5. **No CF instances** created yet (Task 2.1.4+).

---

## Traceability

| ID | Coverage |
|----|----------|
| Sprint 2.1 / Task 2.1.2 | Ticket CFM |
| Sprint 2.1 / Task 2.1.3 | Comment CFM |
| [data-model.md](../../data-model.md) §3–§4 | Field names, types, enums |
| DOD-2 (Sprint 2.1) | CFMs visible in AEM CF model console |
| Downstream | Repoinit/OSGi (2.1.4), DTOs (2.1.5), repository interfaces (2.1.6) |
