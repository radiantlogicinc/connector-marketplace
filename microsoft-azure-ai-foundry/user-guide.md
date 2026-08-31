# Microsoft Azure AI Foundry Connector for RadiantOne IDDM — User Guide

**Product:** Microsoft Azure AI Foundry Connector (IDDM custom connector)
**Version:** 0.9.2 — **Beta**
**Author / Publisher:** Radiant Logic

> **Beta.** This connector has not yet completed official validation. Deploy it to a non-production
> environment first. Beta versions are numbered `0.9.x`, with the patch digit incremented on every
> change; `1.0.0` is reserved for the first officially validated release.

This is the complete, self-contained guide to the Microsoft Azure AI Foundry Connector — a RadiantOne IDDM
custom connector that brings **agentic AI** under Identity Observability (IDO) governance by exposing Azure
AI Foundry agents, and optionally Microsoft Copilot Studio agents, as LDAP identities.

It is written for two audiences:

- **Operators** — install, configure, run, troubleshoot: sections 3, 4, 9, 10, 11.
- **Architects & security** — data model, mapping, refresh model, write-back model: sections 5, 6, 7, 8.

The connector's output conforms to the **agent data model version 1.6** — a provider-agnostic schema shared
with Radiant Logic's other agentic-AI connectors, so an Azure AI Foundry agent and an agent from another
cloud land in the same shape.

## Table of contents

1. [Introduction — agentic AI identity for IDO](#1-introduction--agentic-ai-identity-for-ido)
2. [Background — Azure AI Foundry](#2-background--azure-ai-foundry)
3. [Installation & deployment](#3-installation--deployment)
4. [Configuration reference — all parameters](#4-configuration-reference--all-parameters)
5. [The agent data model](#5-the-agent-data-model)
6. [Data model ↔ Azure mapping](#6-data-model--azure-mapping)
7. [Keeping IDDM in sync](#7-keeping-iddm-in-sync)
8. [Write-back — quarantine remediation](#8-write-back--quarantine-remediation)
9. [Performance & scale](#9-performance--scale)
10. [Capacity planning — memory](#10-capacity-planning--memory)
11. [Logs & troubleshooting](#11-logs--troubleshooting)
12. [Appendices](#12-appendices)

---

## 1. Introduction — agentic AI identity for IDO

### 1.1 What this connector is

A RadiantOne IDDM **custom connector** that discovers AI agents in **Azure AI Foundry** — and, when enabled,
in **Microsoft Copilot Studio** — and presents each as an LDAP entry conforming to a provider-agnostic
**agent data model** (defined in full in section 5). It targets the same data model as Radiant Logic's other
agentic-AI connectors; only the source provider differs.

### 1.2 Why — non-human identity governance for agentic AI

Agents are a fast-growing class of **non-human identity (NHI)**: they run under managed identities, hold
permissions, call tools, reach resources, and invoke each other. Identity Observability needs them as
first-class, correlatable objects. This connector brings Foundry agents — and, optionally, the principals
they run as — into IDDM so they can be searched, correlated, and governed alongside every other identity.

### 1.3 What it produces at a glance

Per agent, a uniform LDAP entry (object class `vdAgentIdentity`) carrying: core identity (`agentId`,
`externalId`, `name`, `version`, `platform`, `kind`, `status`), the **model**, **content-safety guardrails**,
behavioural **features**, **tools**, **resources** (project connections), **subagents** (workflow children),
the **runtime identity** (`runtimeIdentity` — the managed identity, directory-enriched), a **permission-flow
graph** (`permissionFlow`), **`lastInvokedAt`** (invocation recency), project **governance** metadata, the
project's **`tags`** (Azure resource tags), and the mandatory repository-linking fields `repositoryId` /
`repositoryDisplayName` (from which Identity Observability resolves a Repository object).

Optionally, a second object type (`vdIdentity`) emits one row per distinct principal the agents reference.

### 1.4 Posture — read-only by default, one opt-in write

**The connector is read-only by default.** It supports search, test connection and modify, but the **sole**
write path is the opt-in quarantine write-back: an LDAP `modify` of `actionQuarantined` quarantines or
releases the agent through the platform's own reversible lifecycle action. That path is gated by
`enableRemediation` (default **off**) — with it off the connector is **strictly read-only** and rejects any
inbound write. See section 8 for the mechanism, the gate, the attribution model and the required role.

**`modify` result codes.** The two refusal codes mean different things and are deliberately distinct:

| Code | Name | Returned when |
|---|---|---|
| **0** | `SUCCESS` | The quarantine or release action succeeded. |
| **1** | `OPERATIONS_ERROR` | The modification targets a **read-only attribute** — anything other than `actionQuarantined`. 45 of the 46 `vdAgentIdentity` attributes, and all 17 `vdIdentity` attributes, are read-only. A `modify` carrying no modifications at all is treated the same way. |
| **32** | `NO_SUCH_OBJECT` | The target distinguished name carries no usable `agentId`, or the platform reports the agent does not exist. |
| **53** | `UNWILLING_TO_PERFORM` | `enableRemediation` is **off**. The attribute *is* writable; the connector is declining to leave its read-only posture. Nothing is sent to Azure. |
| **80** | `OTHER` | The write was attempted and failed — for example the service principal lacks agent write access (the Reader used for listing is not sufficient), or the agent kind does not support the lifecycle action. |

> **1 vs 53.** `1` says *"you cannot write this attribute, ever."* `53` says *"you could write this
> attribute, but the connector is configured not to."* Testing read-only behaviour therefore requires an
> attribute other than `actionQuarantined`, and the expected code is **1**.

### 1.5 Scope & non-goals

- **In scope:** one Azure **subscription** per data source, optionally scoped to one resource group or
  project, or (opt-in) a multi-project walk across the resource group or the whole subscription.
- **Also in scope (opt-in):** Microsoft Copilot Studio / Power Platform agents (discovery only in this
  release) and quarantine write-back (section 8).
- **Non-goals:** Foundry Local offline agents and Microsoft 365 Copilot built-ins.

### 1.6 Connector type, target application & supported operations

| Question | Answer |
|---|---|
| **Connector type** | **SDK connector** — built on the IDDM Connector SDK 1.2.0. It is **not** a legacy plugin: there is no fully-qualified class name to enter, and its configuration **auto-populates** on import. |
| **Target application** | **IDDM.** The connector is deployed into IDDM and nowhere else. The data it produces is consumed **downstream by Identity Observability**, whose connector mapping translates the connector's `camelCase` LDAP attribute names to its own `snake_case` storage names on ingest. It is **not** installed into Identity Observability. |
| **Schema authoring** | **Used.** The schema is auto-generated by IDDM from the connector's declarations when the data source is created. **Do not hand-build the schema** — generate it, then compare the result against the reference tables in section 12.5. There is no ORX file. |
| **Object types exposed** | **Two.** `vdAgentIdentity` (46 attributes, always present) and `vdIdentity` (17 attributes, **opt-in**, single-project only). |
| **Naming attributes (RDN)** | `agentId` for `vdAgentIdentity`; `principalId` for `vdIdentity`. |

**Supported LDAP operations**

| Operation | Supported | Notes |
|---|---|---|
| **Search — sub-tree** | Yes | Both object types. An unscoped browse returns both in one response. |
| **Search — base** | Yes | Returns the single entry for a known distinguished name. |
| **Test connection** | Yes | Validates every required property the configured feature set uses — see 3.6. |
| **Modify** | Yes — **opt-in, one attribute** | `actionQuarantined` only, and only when `enableRemediation` is on. All three sub-operations (add / replace / delete) are supported. See 1.4 for result codes and section 8 for the mechanism. |
| **Add** | No | Not implemented. No attribute is ever required or optional at create time. |
| **Delete** | No | Not implemented. |
| **Paging** | No | Structural, not an omission: a search returns an eager, single-shot result set with no cursor to suspend on (9.6). |

---

## 2. Background — Azure AI Foundry

### 2.1 Azure AI Foundry & the Agent Service

Foundry is Azure's platform for building and hosting AI agents. Agents live under a **project**, which lives
under a Foundry **account** (an Azure `Microsoft.CognitiveServices/accounts` resource). Agents themselves are
**data-plane objects** reached through the project endpoint — they are **not** management-plane tracked
resources. This is the single most important architectural fact about Foundry: it shapes identity, tagging,
history and write-back alike (2.5).

### 2.2 Agent kinds

Foundry agents all carry `platform = AZURE_FOUNDRY` and are discriminated by `kind`:

- **`prompt`** — a prompt/instructions agent over a model deployment (the common case).
- **`workflow`** — orchestrates other agents; its children become the entry's **subagents**.
- **`hosted`** / **`container_app`** — a deployed container agent.

Copilot Studio agents, when that source is enabled, carry `platform = COPILOT_STUDIO` and `kind = copilot`.

### 2.3 Where agent data comes from — the Azure surfaces the connector reads

| Plane | Endpoints | Supplies | Token audience |
|---|---|---|---|
| **Data plane** | `GET {projectEndpoint}/agents` (cursor-paged) | core identity, `model`, `tools`, `features`, the instruction fingerprint, `kind`, `status`, workflow children | `https://ai.azure.com/.default` |
| **Azure Resource Manager** | account / project / `connections` / `deployments` / `raiPolicies` / `roleAssignments` / `roleDefinitions` | `runtimeIdentity`, `resources`, `permissionFlow`, `guardrails`, `tags`, subscription display name | `https://management.azure.com/.default` |
| **Microsoft Graph** | `directoryObjects`, `servicePrincipals` and their `appRoleAssignments` / `oauth2PermissionGrants` / `memberOf` / `owners` | identity display name and application id, then API permissions, group memberships and owners; resolution of an opaque governance actor | `https://graph.microsoft.com/.default` |
| **Application Insights** | `api.applicationinsights.io/v1/apps/{appId}/query` | `lastInvokedAt` and the 30-day invocation count | `https://api.applicationinsights.io/.default` |
| **Azure Monitor Activity Log** | `Microsoft.Insights/eventtypes/management/values` | project governance: the last control-plane write actor | (Resource Manager) |
| **Power Platform** *(optional)* | environment enumeration through the administration API, then each environment's Dataverse `/api/data/v9.2/bots` | Copilot Studio agent discovery | `https://api.bap.microsoft.com/.default` and, per environment, `https://{org}.crm.dynamics.com/.default` |

A fully-enriched Foundry scan therefore spans **four token audiences** and five Azure surfaces; enabling
Copilot Studio adds two more audiences. Each enrichment plane is independently gated and degrades gracefully
(sections 4 and 11).

### 2.4 Agent identifiers

- **`agentId`** (RDN) — the data-plane agent id (short, human-readable). In a multi-project walk it is
  **qualified** as `{account}__{project}__{id}` to stay unique across projects; Copilot Studio agents are
  qualified as `{environmentId}__{botId}`.
- **`externalId`** — the agent's stable platform GUID: the cross-provider correlation key. It survives a
  rename, which `agentId` does not.

### 2.5 The data-plane vs management-plane split

Because Foundry agents are data-plane objects under a project rather than management-plane resources:

- They are **not taggable as Azure resources** — the only per-agent key/value surface is the data-plane
  metadata bag, and it is version-immutable. The `tags` attribute therefore carries the **project's** Azure
  resource tags.
- They are **not individually audited** — the Azure Activity Log records control-plane writes on the
  account, project and application, **not** per-agent create or update. Per-agent `createdBy`, and most of
  the lifecycle actor set, are therefore **unobtainable**; the project-level governance actor is surfaced as
  `lastUpdatedBy` instead (best-effort).
- **Per-agent role assignment is not a thing** — access is granted at the project or account scope.

This split is why the refresh model (section 7), the write-back mechanism (section 8) and the history fields
look the way they do.

---

## 3. Installation & deployment

### 3.1 Prerequisites

- **RadiantOne IDDM 8.2.0 or later.**
- **An Azure subscription** containing at least one Foundry account and project with agents.
- **A Microsoft Entra service principal** (app registration) that the data source authenticates as, holding
  the role assignments in 3.4, plus a **client secret** for it.
- **The connector JAR** — `microsoft-azure-ai-foundry-connector-0.9.2.jar` (about 13 MB), which embeds all of
  its dependencies. This is the file IDDM loads; nothing is built on site and no source tree is required.

### 3.2 Deploying the JAR into IDDM

1. Copy the JAR into the IDDM custom-connector directory (per your IDDM version's documented location for
   custom connector JARs).
2. Restart or reload so IDDM discovers the connector and registers its configuration.
3. Confirm the connector type **Microsoft Azure AI Foundry** appears in the IDDM Control Panel's data-source wizard.

> **One JAR only.** The published JAR is self-contained. Do not also load a thin or partial JAR alongside it.

### 3.3 Creating the data source

1. In the IDDM Control Panel, create a new data source of type **Microsoft Azure AI Foundry**.
2. Fill in the configuration form (section 4 documents every field). Minimum: `azureTenantId`,
   `azureClientId`, `azureClientSecret`, `azureSubscriptionId`, and `foundryProjectEndpoint` for a
   single-project data source — plus `azureResourceGroup` if you want management-plane enrichment
   (recommended).
3. Save. The connector's object types map to the object classes **`vdAgentIdentity`** (agents) and, when
   `enableIdentityEntity` is on, **`vdIdentity`** (principals).

> **Schema is locked at data-source creation.** Any change to the attribute set — including turning on
> `enableIdentityEntity` — requires **deleting and recreating the data source**, or hand-editing the schema
> in the Control Panel. See 3.7.

### 3.4 Entra and Azure role setup

The service principal needs read access on the planes you enable. All enrichment is best-effort: a missing
grant disables that slice and logs a warning — it does not fail the scan (section 11).

**Minimum — data plane only (core identity, model, tools, features):**

| Grant | Scope | Enables |
|---|---|---|
| **Azure AI User** (or any role granting read access to agents) | the Foundry **project** | data-plane agent listing |

**Recommended — full read-only enrichment:**

| Grant | Scope | Enables |
|---|---|---|
| **Azure AI User** | project | data-plane listing |
| **Reader** | the resource group (or the account and project) | `runtimeIdentity`, `resources`, `permissionFlow`, `guardrails`, `tags`, the role-assignment walk, **and** the Activity Log |
| **`Directory.Read.All`** (Microsoft Graph application permission, **admin-consented**) | tenant | directory enrichment: display name and application id, then API permissions, group memberships and owners |
| **Monitoring Reader** (or equivalent query access) | the Application Insights resource connected to the project | invocation recency (`lastInvokedAt`) |

**Additional grants for the opt-in features:**

| Grant | Scope | Enables |
|---|---|---|
| **Azure AI Developer** or **Contributor** (agent **write** access) | the Foundry **project** | Quarantine write-back. A deliberate step up from read-only Reader — grant it only when write-back is turned on. |
| **Power Platform Administrator** (tenant-wide) **and** a **Dataverse application user** with a security role in each in-scope environment | tenant, then per environment | Copilot Studio discovery: environment enumeration needs the tenant-wide administrator role; listing each environment's agents needs the application user and security role in that environment. |

Notes:

- **Reader** at the resource-group scope covers both management-plane enrichment and the Activity Log — one
  grant, two features.
- The data plane uses **Entra ID authentication and Azure role assignments**, not an API key. An API key
  cannot list or manage agents.
- Section 12.2 lists the full call-to-grant cross-reference.

### 3.5 Azure credentials

The connector authenticates with a **service-principal client secret**:

- `azureTenantId` — the Entra tenant id.
- `azureClientId` — the app registration's application (client) id.
- `azureClientSecret` — a client secret (password-typed in the form).

To create a secret: Entra ID → App registrations → your app → Certificates & secrets → New client secret.
Capture the **value** immediately; it is shown only once.

> **Secret lifecycle.** A client secret **expires**. When it does, update `azureClientSecret` on the data
> source and reload — the credential is read once, when the connector is constructed (section 11). Managed
> identity and other passwordless credential modes are not supported in this release.

### 3.6 First smoke test

1. Use **Test Connection** in the data-source wizard. It probes every plane the configured feature set
   actually uses: a one-agent data-plane listing (or a management-plane account listing in multi-project
   mode), the management scope when management-plane enrichment is on, and the Power Platform
   administration API when Copilot Studio is on. The failure message names the probe that failed.
2. Browse the data source or run a search such as `(objectClass=vdAgentIdentity)`. You should see your
   agents with `platform=AZURE_FOUNDRY` and the core fields populated; enrichment fields populate according
   to the grants in 3.4.
3. Check the connector log for the scan summary line, which reports agents, projects, enrichment state, call
   counts per plane, and the number of responses with an HTTP status of 400 or above.

### 3.7 Upgrading & the schema-recreate rule

- A **code-only** upgrade (new JAR, same attribute set) is a JAR swap and a reload.
- Any change to the **attribute set** — including turning on `enableIdentityEntity` — changes the LDAP
  schema and requires **recreating the data source**.

---

## 4. Configuration reference — all parameters

### 4.1 How the form is organized

Properties are grouped in setup order: **Connection** (who and where) → **Scope** (what to crawl) →
**Copilot Studio** (the opt-in second source) → **Enrichment** (what data to pull) → **Remediation** (the
opt-in write-back) → **Advanced** (rarely-touched tuning). IDDM displays each property by its uppercased
name; its description is the tooltip.

### 4.2 Connection

| Property | Required | Default | Notes |
|---|---|---|---|
| `azureTenantId` | **yes** | — | Entra tenant id. |
| `azureClientId` | **yes** | — | Service-principal application (client) id. |
| `azureClientSecret` | **yes** | — | Password-typed client secret. |
| `azureSubscriptionId` | **yes** | — | Subscription to scan. Also emitted as `repositoryId` on every entry. |
| `azureResourceGroup` | **yes for management-plane enrichment and for a resource-group-scoped multi-project walk** | — | Resource group of the Foundry account. The account and project are parsed from the endpoint; the resource group is not. Empty means management-plane enrichment is disabled — data-plane discovery still works. |
| `foundryProjectEndpoint` | **yes unless `enableMultiProject` is on, or the data source is Copilot-only** | — | `https://{account}.services.ai.azure.com/api/projects/{project}`. Left optional at schema level so multi-project and Copilot-only modes can omit it without recreating the data source; a single-project Foundry data source fails fast at load time if it is empty. |

### 4.3 Scope — what to crawl

| Property | Default | Notes |
|---|---|---|
| `enableMultiProject` | `false` | Scan **all** Foundry projects in scope rather than only `foundryProjectEndpoint`. The endpoint becomes optional and projects are discovered on the management plane, so this **requires `enableArmEnrichment`**. Agent RDNs are qualified `{account}__{project}__{id}`. `vdIdentity` is single-project only and is **not** populated in this mode. |
| `multiProjectScope` | `resourceGroup` | `resourceGroup` (needs `azureResourceGroup`) or `subscription` (every Foundry account and project in the subscription). An unrecognized value logs a warning and falls back to `resourceGroup`. |
| `maxProjects` | `50` | Cap on projects per multi-project walk (`0` = no cap). Discovery stops at the cap and logs a warning. |
| `maxAgents` | `1000` | Cap on agents collected and enriched **per project** (`0` = no cap). Paging stops at the cap and the set is truncated **before** enrichment, with a warning — never a silent cut. In a multi-project walk the cap applies to each project independently. |

### 4.4 Copilot Studio — the opt-in second source

| Property | Default | Notes |
|---|---|---|
| `enableCopilotStudio` | `false` | **Also** discover Microsoft Copilot Studio agents. Power Platform environments are enumerated through the administration API, then each environment's Dataverse agents are listed (fully paged) and emitted as agents with `platform=COPILOT_STUDIO` and environment-qualified RDNs, merged into the same result set. Lifecycle state, created and updated actors, and environment provenance are captured; `model` and `guardrails` are **not applicable** — Copilot Studio exposes no such surface. **Independent of Foundry**: a Copilot-only data source is valid. Best-effort — a discovery failure records the source and still returns Foundry results. Needs the extra grants in 3.4. |
| `copilotEnvironmentFilter` | — (all) | Optional comma-separated allowlist of Power Platform environment ids. Empty means every environment the service principal can see. Use it to bound cost on large tenants. |

### 4.5 Enrichment depth

| Property | Default | Notes |
|---|---|---|
| `enableArmEnrichment` | `true` | `runtimeIdentity`, `resources`, `permissionFlow`, `guardrails` and `tags` from the management plane. Needs `azureResourceGroup` and management-plane read access. **Prerequisite** for multi-project discovery, for `vdIdentity`, and for both history properties below. Degrades gracefully when absent or forbidden. |
| `enableGraphEnrichment` | `true` | Directory enrichment: resolve the runtime identity to a display name and application id, then its API permissions and group memberships; also resolve an opaque governance actor to a name and principal type. Active only when management-plane enrichment is on. Needs `Directory.Read.All`. Best-effort. |
| `enableInstructionText` | `false` | Emit the raw system prompt in the `instruction` attribute. **Off by default for content safety** — the prompt can carry sensitive business logic. The instruction's cryptographic fingerprint and length are emitted **always**, regardless of this flag. Turn it on only when your governance policy permits prompt text in the directory. |
| `enableInvocationHistory` | `true` | `lastInvokedAt` and the 30-day invocation count from the Application Insights resource connected to the project. One **content-safe** aggregate query per project — timestamps, counts and agent names only; conversation and prompt content are never read. Needs a connected Application Insights and query access. Best-effort. |
| `enableActivityHistory` | `true` | Project governance — the most recent successful control-plane write (`lastWriteBy`, `lastWriteAt`, `lastWriteOperation`) from the Azure Activity Log, surfaced inside `metadata` and as the top-level `lastUpdatedBy`. **Project-level, not per-agent.** A capped, newest-first walk over a 90-day window. Needs Activity Log read access. Best-effort. |
| `enableIdentityEntity` | `false` | Expose the optional second object type `vdIdentity` — one row per distinct principal the agents reference. Adds a directory owners lookup on the identity path only. Single-project only. **Schema-affecting: recreate the data source to enable it.** |

### 4.6 Remediation — the opt-in write-back

| Property | Default | Notes |
|---|---|---|
| `enableRemediation` | `false` | Turns `actionQuarantined` into a **functional write-back control** — the connector's **only** write to Azure. When on, an LDAP `modify` of that attribute quarantines (`true`) or releases (`false`) the agent through the platform's reversible lifecycle action; a quarantined agent reads back as `status=SUSPENDED`. Needs agent **write** access (Azure AI Developer or Contributor — 3.4). **Off (default):** the connector is strictly read-only and a `modify` returns `UNWILLING_TO_PERFORM` without contacting Azure. On read, `actionQuarantined` is always `false`. See section 8. |

### 4.7 Advanced

| Property | Default | Notes |
|---|---|---|
| `foundryApiVersion` | `v1` | Data-plane API version. Overridable because the preview value has changed across platform releases; pin the value your tenant expects without a new JAR. |
| `enrichmentThreads` | `10` | Bounds **concurrent project scans** during a multi-project walk (clamped 1–50; `1` = sequential). No effect on a single-project data source. Per-agent enrichment maps over a context fetched once per project and is not threaded, because it is CPU-bound rather than network-bound. |
| `azureMaxRetries` | `3` | Retry budget per Azure call on throttling (HTTP 429) and transient errors (clamped 0–10). Exponential backoff honours the service's `Retry-After` hint. The initial attempt is not counted as a retry. |

### 4.8 Recommended configurations

- **Fast inventory (core only):** `enableArmEnrichment=false`. Data-plane listing only — fastest, gives
  identity plus model, tools and features, and needs no permission beyond project read.
- **Balanced governance (default):** all enrichment on, single project. The full data model including
  history.
- **Estate-wide:** `enableMultiProject=true`, `multiProjectScope=subscription`, `enrichmentThreads=10–25`,
  `maxProjects=0`. Watch for throttling (section 9).
- **Identity-centric:** add `enableIdentityEntity=true` (single project) for the `vdIdentity` non-human
  identity rows.
- **Copilot Studio inventory:** `enableCopilotStudio=true` with `copilotEnvironmentFilter` set to the
  environments you govern; leave `foundryProjectEndpoint` empty for a Copilot-only data source.

---

## 5. The agent data model

### 5.1 What it is and why

The connector's output conforms to the **agent data model version 1.6** — a provider-agnostic schema for AI
agents. The point is normalization: an Azure AI Foundry agent and an agent from another platform land in the
**same** shape, so IDDM can govern a heterogeneous agent fleet without per-provider special-casing. The
connector's job is to map Azure's APIs onto this shared vocabulary; the field-level mapping is section 6.

Every top-level LDAP attribute name is **camelCase** (`externalId`, `statusReason`, `runtimeIdentity`,
`createdAt`, `lastUpdatedAt`, `lastInvokedAt`, `permissionFlow`, `agentCardUrl`, …) because LDAP attribute
descriptors cannot contain underscores; Identity Observability maps these names to its own `snake_case`
storage names on ingest. Keys **inside** JSON attributes stay camelCase, with one exception: the
`permissionFlow` graph internals (`agent_id`, and each node's `external_id`) are snake_case, because that
graph shape is consumed as-is.

Every `*By` actor is a uniform **identity reference** `{principalId, principalType, displayName, metadata}`.
The `*By`-to-account edge (`accountRef`) is configured in Identity Observability and is never emitted by the
connector.

The model is structured around **nine concepts**. The Agent is the core entity; the other eight are
sub-entities carried on the agent as JSON.

### 5.2 The nine model concepts

| # | Concept | Cardinality | What it captures | Azure fill status |
|---|---|---|---|---|
| 1 | **Agent** (core) | one | Identity, hosting context, lifecycle, governance, runtime posture | Full |
| 2 | **Model** | one | The foundation model powering the agent | From the agent definition |
| 3 | **GuardRail** | many | Safety / content policy enforced on input and output | The model deployment's Responsible-AI policy |
| 4 | **Features** | many | Positive-signal capability flags | Synthesized from the declared tools |
| 5 | **Skills** | many | A2A skills (named capabilities from the agent card) | Not populated — requires a published agent exposing an agent endpoint |
| 6 | **Tools** | many | Invokable adapters (function / OpenAPI / MCP / connector) | From the agent definition |
| 7 | **Resources** | many | External assets the agent can reach | Project connections |
| 8 | **Subagents** | many | Other agents this agent may invoke | Workflow children |
| 9 | **Permission Flow** | one (graph) | Versioned graph of principals to resources | Agent → managed identity → connections |

Concepts 2–8 ride on the agent core as JSON arrays; concept 9 rides as a single JSON graph object.

#### 5.2.1 Agent (core)

| Attribute | Description | Azure |
|---|---|---|
| `externalId` | Stable cross-cloud correlation key. **Primary key for matching across providers.** | The Foundry agent GUID |
| `name` | Human-readable agent name | Yes |
| `description` | Free-text description (editable downstream) | Yes, nullable |
| `intent` | The agent's purpose or goal | No Azure source — always absent; authored downstream |
| `instruction` | Raw system prompt | Opt-in (`enableInstructionText`); the fingerprint and length always ride in `metadata` |
| `version` | Agent version string | Yes |
| `platform` | Hosting taxonomy | `AZURE_FOUNDRY` or `COPILOT_STUDIO` |
| `kind` | Sub-taxonomy within the platform | `prompt` / `workflow` / `hosted` / `container_app`; `copilot` for Copilot Studio |
| `tags` | Key/value labels | The **project's** Azure resource tags; needs management-plane enrichment |
| `status` | Lifecycle state: `CREATED`, `PUBLISHED`, `SUSPENDED`, `BLOCKED`, `DEPRECATED`, `DELETED`, `QUARANTINED` | Normalized from the platform-native state; a disabled agent reads `SUSPENDED` |
| `statusReason` | Coded or free-text reason; carries the lossy native value | The raw native status |
| `actionQuarantined` | Boolean write-back remediation control | Functional write-back when `enableRemediation` is on (section 8); **always `false` on read** |
| `runtimeIdentity` | `{displayName, principalId, principalType, trustChain, allowedAssumers, sessionConstraints, metadata}` | The project managed identity, directory-enriched |
| `createdAt` | Creation timestamp | Yes |
| `createdBy` | Creating identity | Copilot Studio only — Foundry agents are not individually audited |
| `lastUpdatedAt` | Most-recent modification timestamp | Yes |
| `lastUpdatedBy` | Last-write actor (identity reference) | The **project-level** governance actor, best-effort; absent without `enableActivityHistory` |
| `lastInvokedAt` | Most-recent invocation timestamp | From Application Insights, best-effort |
| `metadata` | Platform-specific extras as a JSON object | Yes — see 5.6 |
| `repositoryId` | Repository linkage — Identity Observability resolves a Repository from it | The Azure subscription id; present on every record |
| `repositoryDisplayName` | Repository linkage display label | `"Azure " + subscription name`, falling back to the subscription id |
| `agentCardUrl`, `url`, `provider`, `skills` | A2A card URL, endpoint object, owning organization, declared skills | `provider` defaults to the owning organization; the rest require a published agent endpoint and are not populated |
| `statusChangedAt`/`By`, `publishedAt`/`By`, `suspendedAt`/`By`, `blockedAt`/`By`, `deletedAt`/`By`, `lastInvokedBy` | Full per-transition lifecycle audit actors and timestamps | **Declared but absent** — Foundry data-plane agents are not in the Activity Log, so per-agent audit actors are unobtainable. The full set is declared for schema completeness; project-level governance is surfaced through `lastUpdatedBy` and `metadata` instead |

#### 5.2.2 Model (JSON array — typically one entry)

| Attribute | Description | Azure |
|---|---|---|
| `provider` | `AZURE_OPENAI`, `OPENAI`, `ANTHROPIC`, … | `AZURE_OPENAI` |
| `modelId` | Stable provider model id (for example `gpt-4.1`) | From the agent definition |
| `version` | Specific version; empty when only a floating alias is pinned | Yes, often empty |
| `metadata` | Extras such as context window, hosting mode, region | Best-effort |

#### 5.2.3 GuardRail (JSON array)

| Attribute | Description | Azure |
|---|---|---|
| `externalId` | Platform-specific id | The Responsible-AI policy name |
| `name`, `description` | Name and description | Yes |
| `status` | `enabled` / `disabled` | Yes |
| `enforcementMode` | `block` / `allow with log` / human-in-the-loop | Inferred from the policy mode |
| `version` | Version | Best-effort |
| `metadata` | Policy bodies, applied filters, blocked topics | Content filters, policy type, mode, deployment model |

> The concept also defines an id and created/updated actors; Azure sources the policy from the model
> deployment, which carries no per-policy audit actors, so those stay absent.

#### 5.2.4 Features (JSON array)

| Attribute | Description | Azure |
|---|---|---|
| `featureName` | Capability name: `streaming`, `pushNotifications`, `stateTransitionHistory`, `knowledgeEnabled`, `actionsEnabled`, `guardrailsEnabled`, `a2aCompliant`, `mcpCompliant` | Derived from the declared tools, positive signals only |
| `featureDescription` | Description | Yes |
| `metadata` | Feature type, value, where it was declared | Yes |

#### 5.2.5 Skills (JSON array) — not currently sourced

| Attribute | Description |
|---|---|
| `id`, `externalId` | Canonical and platform id |
| `name`, `description`, `tags` | Display name, what it does, searchable labels |
| `metadata` | Examples, parameters, response schema, input and output modes, agent card URL, backing tool references |

#### 5.2.6 Tools (JSON array)

| Attribute | Description | Azure |
|---|---|---|
| `toolExternalId` | Platform-specific id | Tool type or id |
| `toolName`, `toolDescription` | Display name and description | Yes |
| `toolType` | `OPEN_API`, `FUNCTION`, `MCP`, `CONNECTOR`, `KNOWLEDGE_BASE`, `RETURN_CONTROL`, … | Mapped from the native type |
| `state` | `ENABLED` / `DISABLED` / `DRAFT` | Best-effort |
| `principalDisplayName` | Friendly label of the executing principal | The project managed identity's display name, when resolved |
| `metadata` | The native tool block, carried losslessly with secrets redacted | Yes |

> The concept additionally defines an executor reference, API schema reference, target resource references,
> required permissions, principal id and type, credential reference and material type, OAuth configuration,
> and created/updated/invoked actors. Azure populates what the agent definition exposes and carries the rest
> opaquely in `metadata`.

#### 5.2.7 Resources (JSON array)

| Attribute | Description | Azure |
|---|---|---|
| `resourceExternalId` | Platform-specific id | The connection target or resource id |
| `resourceType` | `SEARCH_INDEX`, `AI_SERVICE`, `MONITORING`, `MCP`, `CONNECTOR`, `KEY_VAULT_SECRET`, … | Mapped from the connection category |
| `displayName`, `description` | Name and description | Yes |
| `accessLevel` | `READ`, `WRITE`, `FULL`, `RETRIEVE`, `INVOKE`, … | Best-effort |
| `grantedThrough` | `PROJECT_CONNECTION`, `MANAGED_IDENTITY`, `PROJECT_POLICY`, … | `PROJECT_CONNECTION` |
| `principalId` / `principalType` / `principalDisplayName` | The non-human identity used to reach the resource | The project managed identity |
| `credentialMaterialType` | `API_KEY`, `MANAGED_IDENTITY_TOKEN`, `OAUTH_DELEGATED`, … | From the connection's authentication type |
| `metadata` | Category, authentication type, connection metadata | Yes |

> The concept also defines a resource id, location, owning account, policy statements, credential reference,
> trust chain, sensitivity and tags. Foundry access is **connection-centric** — the project managed identity
> typically holds no Azure role assignments at all — so access is modelled through the connection rather
> than through policy statements.

#### 5.2.8 Subagents (JSON array)

| Attribute | Description | Azure |
|---|---|---|
| `subAgentExternalId` | Platform id of the callee | The resolved child agent GUID |
| `subAgentName`, `subAgentDescription` | Name and description | From the workflow node |
| `subAgentProvider` | `azure-foundry`, `external-a2a`, `internal-http`, … | `azure-foundry` |
| `relationshipType` | `delegate` / `supervisor` / `peer` / `toolLike` | `delegate` |
| `invocationProtocol` | `a2a` / `mcp` / `http` / `providerNative` / `internalSdk` | `providerNative` |
| `endpoint`, `authMethod`, `credentialRef`, `delegationMode`, `dataSharingPolicy` | Reachability and trust | Best-effort |
| `metadata` | Invocation method, trust level, how it was discovered | Yes |

> Sourced from a workflow agent's definition, whose invoke-agent nodes name a child by name; the connector
> resolves that name to the child's stable GUID over the same scan. Absent for non-workflow agents.

#### 5.2.9 Permission Flow (single JSON graph object)

A versioned, acyclic graph of every intermediate principal the agent traverses to reach the assets it can
access. Consumed by the IDDM interface to render the reachable-resource graph.

**Top level:** `version` (currently `1`), `agent_id` (the id of the node whose `type` is `agent`), `nodes[]`,
`edges[]`.

**Node:** `id` (document-scoped, stable across scans), `external_id` (the native Azure resource id or GUID),
`name`, `description`, `type` (drives icon and colour — for example `agent`, `managed_identity`, `resource`),
`data` (free-form JSON, rendered as-is and not interpreted by IDDM).

**Edge:** `from` / `to` (must reference node ids), `relation` (for example `runs_as`, `connects_to`),
`description`, `data` (free-form).

**Validation rules the connector honours:** every edge endpoint resolves to a declared node; exactly one
`agent`-type node, referenced by `agent_id`; the graph is acyclic; node ids are stable and repeatable across
scans so diffs stay meaningful. For Azure the shape is **agent → project managed identity → project
connections** — one `runs_as` edge, then one `connects_to` edge per connection.

### 5.3 The object type `vdAgentIdentity`

Every emitted entry carries the same **uniform key set**, with a null for absent values. Grouped by purpose:

- **Identity and lifecycle:** `agentId` (RDN), `externalId`, `name`, `description`, `intent` (always absent),
  `version`, `platform`, `kind`, `tags`, `status`, `statusReason`, `actionQuarantined`, `createdAt`,
  `lastUpdatedAt`.
- **Lifecycle actors (identity references, mostly absent):** `lastUpdatedBy` (project-level governance
  actor), `createdBy` (Copilot Studio only), plus the declared-but-absent `statusChangedAt`/`By`,
  `publishedAt`/`By`, `suspendedAt`/`By`, `blockedAt`/`By`, `deletedAt`/`By`, `lastInvokedBy`.
- **Invocation history:** `lastInvokedAt`.
- **Runtime identity:** `runtimeIdentity`.
- **Repository linkage (always present):** `repositoryId`, `repositoryDisplayName`.
- **Instruction (opt-in):** `instruction`.
- **Enrichment sub-entities (JSON):** `metadata`, `model`, `guardrails`, `features`, `tools`, `resources`,
  `subagents`, `permissionFlow`.
- **A2A / agent card (declared, not populated in this release):** `agentCardUrl`, `url`, `skills`;
  `provider` is populated.
- **Connector diagnostics:** `_connectorFoundrySourceAvailable` (scan-level — did the agent listing succeed
  this scan?) and `_connectorSourcesUnavailable` (per entry — which enrichment sources failed for *this*
  entry; blank when complete).

> **Why the A2A and per-transition audit fields are declared but absent.** The schema is locked at
> data-source creation, so the connector declares the full model-aligned attribute set up front and
> populates it as sources become available. A field that is structurally unsourceable on Azure — the A2A
> card on unpublished agents, per-agent audit actors — stays absent and is dropped at the LDAP wire layer.
> That is not a defect: declaring the full set means no *second* data-source recreate is needed if a source
> later appears.

### 5.4 The optional object type `vdIdentity`

Opt-in via `enableIdentityEntity` (default off), object class `vdIdentity`, RDN `principalId`. **One row per
distinct principal** (managed identity, service principal, user or group) that the project's agents
reference — deduplicated across the scan and back-referenced to the agents that use it. Principals are
*derived from* the agent scan: the management plane finds them and the directory describes them. The
connector never enumerates the directory.

> This object type is a deliberate **extension** beyond the agent data model, which is agent-centric and
> treats the identity as part of `runtimeIdentity` and the permission graph. It does not carry the
> repository-linking fields.

| Attribute | Type | Description |
|---|---|---|
| `principalId` | string (RDN) | Directory object id of the principal. |
| `externalId` | string | Cross-connector correlation key: the directory application id for a service principal or managed identity, otherwise the principal id. The inverse of each agent's `runtimeIdentity.principalId`. |
| `displayName` | string | Directory name. Absent when directory enrichment is off or forbidden. |
| `principalType` | string | `MANAGED_IDENTITY` / `SERVICE_PRINCIPAL` / `USER` / `GROUP`. |
| `appId` | string | Directory application (client) id for service principals and managed identities. |
| `accountEnabled` | boolean | Directory enablement state. |
| `identityType` | string | `SystemAssigned` / `UserAssigned` for managed identities; absent otherwise. |
| `tenantId` | string | Owning Entra tenant id. |
| `apiPermissions` | JSON array | API permissions and application-role grants held **by** this identity — the access plane Azure role assignments cannot see. |
| `groupMemberships` | JSON array | Groups and directory roles this identity belongs to. |
| `roleAssignments` | JSON array | Azure role assignments. Usually empty for Foundry, whose access is connection-centric. |
| `owners` | JSON array | Directory owners of this identity — the accountability signal for a non-human identity. |
| `referencedByAgents` | JSON array | The agents in this scan that **run as** this identity. |
| `agentFanout` | integer | Number of agents that run as this identity. |
| `isShared` | boolean | `true` when more than one agent runs as this identity — the over-shared identity signal. |
| `metadata` | JSON object | Provenance: the scope the identity was found at and which planes described it. |
| `_connectorSourcesUnavailable` | string | Per-row failed sources; blank when the row is complete. |

> `vdIdentity` is single-project only and is **not** populated during a multi-project walk. Adding the
> object type changes the schema, so enabling it requires recreating the data source.

### 5.5 Extensions beyond the agent data model

A few emitted constructs are deliberate connector extensions rather than parts of the shared model:

- **`vdIdentity`** — promotes the principal to a first-class, cross-connector-correlatable object with its
  own grants, group memberships, owners, role assignments, and the "N agents share one identity"
  relationship.
- **`metadata.projectGovernance`** — `{lastWriteBy, lastWriteAt, lastWriteOperation}`, the most recent
  control-plane write actor from the Azure Activity Log. Project-level, and therefore shared by every agent
  in the project; surfaced inside `metadata` and **also** as the top-level `lastUpdatedBy`. `lastWriteBy` is
  an identity reference: `principalId` is always present; when it is an opaque identifier (a service
  principal or managed identity) **and** directory enrichment is on, `displayName` and `principalType` are
  resolved from the directory, while a user-principal-name caller is already legible. Its `metadata`
  sub-object carries best-effort connection-context signals from the event claims (`ipAddress`, `appId`,
  `mfaAuthenticated`).
- **Attributes prefixed `_connector`** — connector-private diagnostics; the leading underscore signals that
  they are not part of the shared model.

### 5.6 Output mechanics that matter to consumers

- **Uniform key set.** Every `vdAgentIdentity` entry carries the same 46 keys and every `vdIdentity` entry
  the same 17. Attributes that do not apply are present as null.
- **Null-dropping at the LDAP wire layer.** Null-valued attributes are **not shown** in the LDAP entry.
  Empty strings *do* appear with a blank value — which is why some diagnostic fields use a blank string
  rather than null, and why a missing attribute in the browser usually means "null this scan", not "schema
  error".
- **JSON-bearing string attributes.** `metadata`, `runtimeIdentity`, `model`, `guardrails`, `features`,
  `url`, `provider`, `skills`, `tools`, `resources`, `subagents`, `permissionFlow`, `tags` and every
  populated `*By` identity reference ride on the entry as **JSON-serialized strings**. On `vdIdentity` the
  JSON-bearing attributes are `apiPermissions`, `groupMemberships`, `roleAssignments`, `owners`,
  `referencedByAgents` and `metadata`. Plain strings: `agentCardUrl`, `intent`, `instruction`,
  `repositoryId`, `repositoryDisplayName`, and all timestamps. Booleans: `accountEnabled`, `isShared`,
  `actionQuarantined`, `_connectorFoundrySourceAvailable`. Integer: `agentFanout`.
- **Empty positive-signal arrays become absent.** `features`, `skills`, `tools`, `resources` and
  `subagents` that would be empty are suppressed so LDAP drops them entirely — absence reads as a negative
  signal rather than as an empty positive one.
- **Every timestamp shares one wire format — no exceptions.** All dates the connector emits are
  **ISO-8601 UTC with exactly nine fractional-second (nanosecond) digits and a `Z` suffix**, for example
  `2026-05-26T22:54:23.626338932Z`. Lower-precision sources are zero-padded, offsets are converted to UTC, a
  source that trims trailing zeros is restored to full precision, and space-separated or zone-less shapes
  are accepted. This applies to **every** top-level date attribute **and** to every timestamp nested inside
  a JSON attribute, so raw Azure timestamps carried verbatim conform too.

### 5.7 Representative JSON

The shapes below are taken from a live extract with identifiers replaced by placeholders.

**`runtimeIdentity`** — the project managed identity, directory-enriched:

```json
{
  "displayName": "example-foundry-account/projects/proj-prod",
  "principalId": "<managed-identity-guid>",
  "principalType": "MANAGED_IDENTITY",
  "metadata": {
    "scope": "project",
    "identityType": "SystemAssigned",
    "tenantId": "<tenant-guid>",
    "accountPrincipalId": "<account-identity-guid>",
    "displayName": "example-foundry-account/projects/proj-prod",
    "appId": "<app-id>",
    "accountEnabled": true
  }
}
```

**`model`**:

```json
[ { "provider": "AZURE_OPENAI", "modelId": "gpt-4.1", "version": "" } ]
```

**`guardrails`** — the model deployment's Responsible-AI policy, filters trimmed:

```json
[
  {
    "externalId": "Microsoft.DefaultV2",
    "name": "Microsoft.DefaultV2",
    "status": "enabled",
    "description": "Azure AI content-safety policy (SystemManaged)",
    "enforcementMode": "block",
    "metadata": {
      "deploymentModel": "gpt-4.1",
      "raiPolicyType": "SystemManaged",
      "mode": "Blocking",
      "contentFilters": [
        { "name": "Hate", "severityThreshold": "Medium", "blocking": true, "enabled": true, "source": "Prompt", "action": "NONE" },
        { "name": "Jailbreak", "blocking": true, "enabled": true, "source": "Prompt", "action": "NONE" }
      ]
    }
  }
]
```

**`tools`** — `principalDisplayName` is the executing principal's friendly label:

```json
[ { "toolExternalId": "web_search", "toolName": "web_search", "toolType": null, "state": null,
    "principalDisplayName": "example-foundry-account/projects/proj-prod",
    "metadata": { "type": "web_search" } } ]
```

**`resources`** — one project connection:

```json
[
  {
    "resourceExternalId": "https://example-search.search.windows.net/",
    "resourceType": "SEARCH_INDEX",
    "displayName": "example-search-index",
    "grantedThrough": "PROJECT_CONNECTION",
    "principalDisplayName": "example-foundry-account/projects/proj-prod",
    "metadata": {
      "category": "CognitiveSearch",
      "authType": "ProjectManagedIdentity",
      "credentialMaterialType": "MANAGED_IDENTITY_TOKEN",
      "isSharedToAll": false,
      "principalId": "<managed-identity-guid>",
      "principalType": "MANAGED_IDENTITY"
    }
  }
]
```

**`metadata`** — the agent-level extras object, including the project governance block:

```json
{
  "projectEndpoint": "https://example-foundry-account.services.ai.azure.com/api/projects/proj-prod",
  "subscriptionId": "<subscription-guid>",
  "resourceGroup": "example-resource-group",
  "instructionHash": "<sha-256 hex>",
  "instructionLength": 68,
  "invocationCount30d": 12,
  "projectGovernance": {
    "lastWriteBy": {
      "principalId": "admin@example.com",
      "principalType": "USER",
      "metadata": { "ipAddress": "203.0.113.10", "appId": "<client-app-id>", "mfaAuthenticated": true }
    },
    "lastWriteAt": "2026-06-24T05:29:13.150015400Z",
    "lastWriteOperation": "microsoft.insights/diagnosticSettings/write"
  }
}
```

> `lastWriteBy` is an identity reference and is also surfaced as the top-level `lastUpdatedBy`. A service
> principal or managed identity writer yields a resolved actor instead, for example
> `{ "principalId": "<sp-guid>", "principalType": "SERVICE_PRINCIPAL", "displayName": "agent-deployer", "metadata": { … } }`.

**`permissionFlow`** — agent → managed identity → connections, abbreviated. Note that the graph internals
stay snake_case:

```json
{
  "version": 1,
  "agent_id": "node:agent",
  "nodes": [
    { "id": "node:agent", "external_id": "hr-support-agent", "name": "hr-support-agent", "type": "agent",
      "data": { "platform": "AZURE_FOUNDRY", "kind": "prompt" } },
    { "id": "node:identity", "external_id": "<managed-identity-guid>",
      "name": "example-foundry-account/projects/proj-prod", "type": "managed_identity",
      "data": { "principalType": "MANAGED_IDENTITY", "scope": "project" } },
    { "id": "node:res:4", "external_id": "https://example-search.search.windows.net/",
      "name": "example-search-index", "type": "resource",
      "data": { "category": "CognitiveSearch", "authType": "ProjectManagedIdentity" } }
  ],
  "edges": [
    { "from": "node:agent", "to": "node:identity", "relation": "runs_as",
      "description": "agent runs under the project managed identity", "data": {} },
    { "from": "node:identity", "to": "node:res:4", "relation": "connects_to",
      "description": "identity authenticates to the connection target",
      "data": { "authType": "ProjectManagedIdentity" } }
  ]
}
```

**`vdIdentity`** — one emitted row, the shared project managed identity:

```json
{
  "principalId": "<managed-identity-guid>",
  "externalId": "<app-id>",
  "displayName": "example-foundry-account/projects/proj-prod",
  "principalType": "MANAGED_IDENTITY",
  "appId": "<app-id>",
  "accountEnabled": true,
  "identityType": "SystemAssigned",
  "tenantId": "<tenant-guid>",
  "referencedByAgents": ["hr-support-agent", "it-helpdesk-agent", "…"],
  "agentFanout": 38,
  "isShared": true,
  "metadata": { "scope": "project", "sourcePlane": "ARM+GRAPH" }
}
```

---

## 6. Data model ↔ Azure mapping

| Data model attribute | Azure source | Notes |
|---|---|---|
| core identity | data-plane agent listing (`id`, `name`, version block) | `externalId` is the agent GUID; `platform=AZURE_FOUNDRY` |
| `status` / `statusReason` | the agent version's native status | `statusReason` carries the native value verbatim; a disabled agent reads `SUSPENDED` |
| `actionQuarantined` | not read from Azure | Write-back control, default `false`, always `false` on read (section 8) |
| `intent` | no Azure source | Always absent; authored downstream |
| `instruction` (opt-in) | the agent definition's instructions | Only when `enableInstructionText` is on; the fingerprint always rides in `metadata` |
| `tags` | the project's Azure resource tags | Needs management-plane enrichment |
| `model` | the agent definition's model | Provider reported as `AZURE_OPENAI` |
| `tools` / `features` | the agent definition's tools | Positive-signal features only |
| `guardrails` | model deployment policy name → the account's Responsible-AI policy and its content filters | Needs management-plane enrichment |
| `runtimeIdentity` | project managed identity, plus directory enrichment | Needs management-plane enrichment |
| `resources` | the project's **connections** | Connection-centric access model |
| `permissionFlow` | management-plane identity, connections and role grants, plus the directory | Node/edge graph |
| `subagents` | a workflow agent's definition | Invoke-agent nodes resolved to the child agent's GUID |
| `lastInvokedAt` and `metadata.invocationCount30d` | Application Insights query by agent name | Content-safe: no conversation or prompt content |
| `lastUpdatedBy` and `metadata.projectGovernance` | Azure Activity Log, last control-plane write; opaque actors resolved through the directory | **Project-level, not per-agent** |
| `repositoryId` / `repositoryDisplayName` | subscription id and `"Azure " + subscription name` | Present on every record |
| `createdBy` | Copilot Studio agent record | Copilot Studio only |

**Business-rule callouts**

- **No per-agent `createdBy` or lifecycle actors.** Data-plane agents are not in the Activity Log; only
  project- and account-level governance is obtainable, surfaced as `lastUpdatedBy`. The full lifecycle actor
  set is declared but absent.
- **`status` normalization is conservative.** The data-plane object exposes a small set of native states;
  the raw value is always preserved in `statusReason` so nothing is lost.
- **Skills and A2A data are absent** until a published agent exposes an agent endpoint.
- **Multi-project provenance** — the account and project are added to `metadata`, and the `agentId` RDN is
  qualified, only in multi-project mode.
- **Copilot Studio entries** carry core identity, lifecycle state, created and updated actors, and
  environment provenance in `metadata`. They have no `model` and no `guardrails`.

---

## 7. Keeping IDDM in sync

### 7.1 The model today

The connector is **scan-on-search**: each LDAP search triggers a fresh scan (data-plane listing plus
per-project enrichment), and IDDM's own cache layer fronts it — the connector performs no internal caching
of its own, so IDDM's cache settings are the single place freshness is controlled.

A **request-aware** plan keeps cheap searches cheap:

- an **attributes-only** request (types-only search) returns data-plane core fields with no management-plane
  or directory calls at all;
- a **single-agent** request (a filter on `agentId` or `externalId`) stops at the first project holding the
  match instead of walking every project;
- a client-supplied **size limit** caps the result set.

### 7.2 Event-driven refresh

This release has **no event-driven refresh path** and emits no dependency index. Azure's change signals for
data-plane agents are structurally weaker than a per-resource audit trail: the Activity Log does not see
per-agent writes (2.5), so a change feed cannot be built on it without additional platform support.

**Practical guidance:** rely on IDDM's cache time-to-live and periodic re-scan. For large estates, schedule
periodic re-scans rather than relying on per-query freshness, and use the request-aware fast paths above to
keep targeted lookups cheap.

---

## 8. Write-back — quarantine remediation

### 8.1 What quarantine is

The agent data model defines a single write-back remediation control, **`actionQuarantined`** (boolean) —
the attribute Identity Observability pushes back to a platform to quarantine an agent. The effective
`status` becomes `QUARANTINED`, with the pre-quarantine state kept in `statusReason`.

### 8.2 The mechanism

An LDAP `modify` of `actionQuarantined` on a `vdAgentIdentity` entry invokes the platform's own agent
lifecycle action:

- `actionQuarantined=true` → the agent is **disabled**, taking its endpoint offline;
- `actionQuarantined=false` → the agent is **enabled** again.

The action is **reversible and non-destructive**: the agent's definition and version history are untouched.
It has been verified against both prompt and workflow agents. A disabled agent then **reads back as
`status=SUSPENDED`**. This is the single write path in the connector — everything else is a read.

### 8.3 The gate and the required role

- **`enableRemediation` (default off).** With it off, a `modify` returns **`UNWILLING_TO_PERFORM`** and the
  connector performs no write at all — read-only by default (1.4, 4.6).
- **Role.** The write needs agent **write** access — **Azure AI Developer** or **Contributor** on the
  project — a step up from the read-only Reader posture used for discovery (3.4). Turn the flag on only for
  a service principal that has been granted that role. Without it, the modify fails with `OTHER` (80) and
  the connector log names the HTTP status.

### 8.4 Attribution is owned by Identity Observability

Azure AI Foundry exposes **no in-place quarantine attribution** — there is no per-agent field recording
*who* quarantined an agent or *why*, and the per-agent metadata bag is version-immutable, so no marker can
be written in place either. On read the connector therefore always emits **`actionQuarantined=false`**: it
never asserts a value it cannot prove. The quarantine decision and its attribution are owned by **Identity
Observability**; the connector reflects only the *effect* of a disable, through `status=SUSPENDED`.

**Consequence for verification:** after a successful quarantine, confirm the change by re-reading `status`
(expect `SUSPENDED`), not by re-reading `actionQuarantined`. The connector also cannot distinguish an agent
it quarantined from one an administrator disabled directly in the Azure portal.

### 8.5 Enforcement is direct

Because the mechanism is the platform's own lifecycle action rather than a marker tag, **no additional Azure
automation is required** to make a quarantine take effect: a disabled agent's endpoint is offline
immediately. Nothing needs to be built on your side to enforce the decision.

---

## 9. Performance & scale

### 9.1 What bounds throughput

**Cost scales with the number of *projects*, not agents.** The per-project management-plane, directory,
monitoring and activity context is fetched **once**, and every agent in that project is enriched by mapping
over it with **zero per-agent API calls**. The dominant network cost is the **sequential, cursor-bound
data-plane agent listing**.

### 9.2 Measured per-call latency (five-sample average)

| Call | Latency |
|---|---|
| **Data-plane agent listing (100 per page)** | **~2,100 ms** ← bottleneck |
| Management-plane account / project identity read | ~1,100 ms |
| Management-plane deployments / policies / connections | ~400 ms |
| Activity Log page | ~210 ms |
| Directory call | ~300 ms |

A full-enrichment **per-project scan** is roughly **40 calls** — one data-plane listing, about 33
management-plane calls including the capped Activity Log walk, five directory calls and one monitoring
query — which is about **13–15 seconds** for a project of 35 agents.

### 9.3 CPU and throughput

Measured over the real enrich-and-serialize path:

| Agents | CPU time | Throughput | Per agent |
|---|---|---|---|
| 10,000 | ~1.0 s | ~10,400/s | ~0.1 ms |
| 50,000 | ~3.7 s | ~13,600/s | ~0.07 ms |

CPU is **never** the bottleneck.

### 9.4 Extrapolation to 10,000 agents

| Scenario | Total (ideal) |
|---|---|
| 10,000 agents in **one project** (paging-bound, sequential) | **~3.7 min** |
| 100 projects × 100 agents, `enrichmentThreads=10` | **~2.5 min** |
| The same, `enrichmentThreads=50` | ~30–60 s ideal |
| Data plane only, one project | ~3.5 min (still paging-bound) |

**Realistic band:** three to four minutes for one or a few large projects; one to three minutes distributed
across many projects. A high `enrichmentThreads` fan-out invites management-plane and directory
**throttling**, which is handled reactively through retry and the service's `Retry-After` hint; there is no
proactive rate limiter, so allow a factor of two to five under heavy load.

### 9.5 Scale levers

- **`enableMultiProject` with `enrichmentThreads`** turns per-project serial cost into parallel work.
- **Cross-project deduplication** means projects sharing an account, a role or a principal fetch it once.
- **Disable enrichment you do not need** — turning off `enableActivityHistory` saves roughly five seconds
  per project; `enableArmEnrichment=false` drops everything but the data plane.
- **Request-aware search** (7.1) collapses attributes-only and single-agent searches.
- **`maxAgents` and `maxProjects`** bound the worst case deterministically.

### 9.6 No streaming — a structural limit

The connector materializes the full result set per search, as the IDDM search contract expects. There is no
incremental or streaming emit, so capacity is bounded as described in section 10.

---

## 10. Capacity planning — memory

### 10.1 Where the memory goes

The retained set is the aggregated list of fully-serialized entries — by the end of a scan every JSON
sub-entity is a string. The per-scan deduplication caches are small and bounded by the number of distinct
accounts, roles and principals.

### 10.2 Measured footprint

**About 9.3 KB per fully-enriched agent** (runtime identity, resources, permission graph, guardrails,
metadata, model, tools and features, all serialized).

| Agents | Heap retained |
|---|---|
| 10,000 | ~88 MB |
| 50,000 | ~442 MB |

### 10.3 Recommendation

Comfortable to 10,000–20,000 agents on a default IDDM heap. Above roughly 50,000, raise the heap or split
the estate across data sources (one per project or per resource group).

### 10.4 Levers when memory-bound

- **Attributes-only and request-aware searches** carry no enrichment and drop per-agent size dramatically.
- **Split the estate** across several single-project data sources instead of one subscription-wide scan.
- **`maxAgents` per project** caps the retained set deterministically.

---

## 11. Logs & troubleshooting

### 11.1 Logging model

**WARN** for each individual failure, with a stack trace; **ERROR** when a scan completed with issues;
**INFO** for the happy path and for the per-scan metrics line, which reports agents, projects, enrichment
state, call counts per plane and the number of responses with an HTTP status of 400 or above. High-level
diagnostic lines are prefixed for easy filtering.

### 11.2 Diagnostic attributes

- **`_connectorFoundrySourceAvailable`** (boolean, scan-level) — did the top-level agent listing succeed?
- **`_connectorSourcesUnavailable`** (string, per entry) — comma-separated names of the failures that
  affected *this* entry. Blank when the entry's data is complete.

### 11.3 Error-handling philosophy

Scan-level failures (the agent listing itself, authentication) gate the search result code: the search
reports **UNAVAILABLE** only when *everything* failed. Per-agent and per-source failures leave just that
slice absent and surface in `_connectorSourcesUnavailable`. One bad agent, or one missing grant, never
aborts a scan.

### 11.4 Troubleshooting playbook

| Symptom | Likely cause | Fix |
|---|---|---|
| Test Connection fails with an authentication error | Wrong tenant, client id or secret, or the secret expired | Re-check the three credentials; rotate the client secret and update `azureClientSecret`, then reload |
| Search returns **UNAVAILABLE** | The data-plane listing failed entirely — authentication, endpoint or the project read role | Verify `foundryProjectEndpoint` and the project read grant; read the connector log |
| Agents are listed but `runtimeIdentity`, `resources`, `permissionFlow`, `guardrails` and `tags` are all absent | Management-plane enrichment is off or forbidden | Set `azureResourceGroup`, keep `enableArmEnrichment` on, and grant **Reader** on the resource group; check `_connectorSourcesUnavailable` |
| `runtimeIdentity` is present but has no display name, application id or group data | The directory permission is not consented | Grant and admin-consent `Directory.Read.All` |
| `lastInvokedAt` is always absent | No Application Insights connected to the project, no runs in the 30-day window, or no query access | Confirm the project's Application Insights connection and the Monitoring Reader grant; absence is normal for an un-invoked agent |
| Project governance data is absent | No Activity Log read access, or no control-plane writes in the 90-day window | Grant **Reader** at resource-group or subscription scope; absence is normal on a quiet project |
| A `vdIdentity` search returns nothing | `enableIdentityEntity` is off, the schema was not recreated, or the data source is in multi-project mode | Set the flag **and recreate the data source**; this object type is single-project only |
| A newly introduced attribute is not visible | The data source predates the schema change | **Recreate the data source** (3.7) |
| `instruction` is always absent | `enableInstructionText` is off (the default) | Set `enableInstructionText=true`; the fingerprint and length are present either way |
| A multi-project scan finds nothing | `enableArmEnrichment` is off, or `azureResourceGroup` is empty with the default resource-group scope | Enable management-plane enrichment; set the resource group, or use `multiProjectScope=subscription` |
| The result is truncated with a `maxAgents` or `maxProjects` warning | A cap was hit, by design | Raise the cap or narrow the data source's scope |
| Copilot Studio agents are missing | The service principal is not a Power Platform Administrator, or it has no application user in the environment | Assign the tenant-wide role and create the Dataverse application user with a security role; check the environment allowlist |
| Slow scans, or throttling in the log | Data-plane paging plus management-plane and directory fan-out | Lower `enrichmentThreads`, disable enrichment you do not need, split the estate (section 9) |
| A quarantine modify returns 53 | `enableRemediation` is off | Turn it on — and grant the agent write role first (section 8) |
| A quarantine modify returns 80 | The service principal lacks agent write access, or the agent kind does not support the lifecycle action | Assign Azure AI Developer or Contributor on the project; check the HTTP status in the log |
| `actionQuarantined` still reads `false` after a successful quarantine | Expected behaviour, by design | Verify through `status=SUSPENDED` instead (8.4) |
| A configuration change appears to be ignored | Credentials and flags are read when the connector is constructed | Reload the data source, then re-run Test Connection |

---

## 12. Appendices

### 12.1 Glossary

- **NHI** — non-human identity (agents, managed identities, service principals).
- **IDO** — Identity Observability.
- **Foundry account / project** — the Azure Cognitive Services account resource and its project child.
- **Data plane vs management plane** — the data plane is the project endpoint that lists agents; the
  management plane is Azure Resource Manager.
- **Responsible-AI policy** — the content-safety policy on a model deployment, mapped to `guardrails`.
- **Permission flow** — the node/edge graph of how an agent reaches resources through its identity.
- **Project governance** — the project-level last-control-plane-write actor.
- **Identity reference** — the uniform `{principalId, principalType, displayName, metadata}` shape every
  `*By` field uses. `principalId` is always present; the rest are best-effort.

### 12.2 Call → grant cross-reference

| Surface / call | Grant | Gated by |
|---|---|---|
| Data-plane agent listing | Azure AI User (project) | always (core) |
| Management-plane account, project, connections, deployments, policies | Reader (resource group or account) | `enableArmEnrichment` |
| Management-plane role assignments and role definitions | Reader | `enableArmEnrichment` |
| Directory objects and service principals | `Directory.Read.All` | `enableGraphEnrichment` |
| Service principal owners | `Directory.Read.All` | `enableIdentityEntity` |
| Application Insights query | Monitoring Reader on the Application Insights resource | `enableInvocationHistory` |
| Activity Log management events | Reader | `enableActivityHistory` |
| Agent disable / enable lifecycle action | Azure AI Developer or Contributor (project) | `enableRemediation` |
| Power Platform environment enumeration | Power Platform Administrator (tenant) | `enableCopilotStudio` |
| Dataverse agent listing per environment | Application user with a security role in that environment | `enableCopilotStudio` |

### 12.3 Known limitations

1. **One subscription per data source.** Several projects can be crawled in one data source, but a single
   crawl never spans subscriptions or tenants.
2. **`vdIdentity` is single-project only** and requires management-plane enrichment.
3. **No paging.** The whole result set is materialized and returned at once; bound it with attribute
   narrowing, the caps, or a client size limit.
4. **`maxAgents` truncates by default** at 1000 agents per project, with a warning in the log.
5. **No per-agent authorship or lifecycle actors.** Foundry data-plane agents are not individually audited;
   governance is project-level and therefore shared by every agent in the project.
6. **Project governance has a 90-day horizon** and covers control-plane writes only.
7. **The A2A agent card is not populated** — `agentCardUrl`, `url` and `skills` require a published agent
   endpoint.
8. **`intent` has no platform source** and is authored downstream.
9. **Invocation recency requires a connected Application Insights** and covers a 30-day window.
10. **Quarantine always reads back as `false`**; verify through `status=SUSPENDED` (8.4).
11. **Copilot Studio support is discovery-only** in this release: no model, no guardrails.
12. **Credentials and flags are read once, at load time**; client secrets expire and are never refreshed
    automatically.
13. **Throughput is bounded by the sequential data-plane listing**, and there is no proactive rate limiter.

### 12.4 Worked example — an extracted agent

A prompt agent in a Foundry project, fully enriched.

> **Provenance and fidelity.** The structure and every field name and shape are verbatim from a live
> extract. **Account, project, subscription, identity and user identifiers, and resource names have been
> replaced with placeholders**; nothing else has been altered.

```json
{
  "agentId": "hr-support-agent",
  "externalId": "<agent-guid>",
  "name": "hr-support-agent",
  "description": "Answers employee HR policy questions.",
  "intent": null,
  "version": "4",
  "platform": "AZURE_FOUNDRY",
  "kind": "prompt",
  "tags": { "env": "prod", "owner": "data-platform" },
  "status": "PUBLISHED",
  "statusReason": "active",
  "actionQuarantined": false,
  "createdAt": "2026-06-12T15:09:45.000000000Z",
  "lastUpdatedAt": "2026-06-12T15:09:44.000000000Z",
  "lastUpdatedBy": {
    "principalId": "admin@example.com",
    "principalType": "USER"
  },
  "lastInvokedAt": null,
  "instruction": null,
  "repositoryId": "<subscription-guid>",
  "repositoryDisplayName": "Azure Production Subscription",
  "runtimeIdentity": {
    "displayName": "example-foundry-account/projects/proj-prod",
    "principalId": "<managed-identity-guid>",
    "principalType": "MANAGED_IDENTITY",
    "metadata": {
      "scope": "project",
      "identityType": "SystemAssigned",
      "displayName": "example-foundry-account/projects/proj-prod",
      "appId": "<app-id>",
      "accountEnabled": true
    }
  },
  "model": [ { "provider": "AZURE_OPENAI", "modelId": "gpt-4o", "version": "" } ],
  "guardrails": [
    {
      "externalId": "Microsoft.DefaultV2",
      "status": "enabled",
      "enforcementMode": "block",
      "metadata": {
        "raiPolicyType": "SystemManaged",
        "mode": "Blocking",
        "contentFilters": [ "… Hate / Sexual / Violence / SelfHarm / Jailbreak …" ]
      }
    }
  ],
  "features": [
    { "featureName": "knowledgeEnabled", "featureDescription": "Agent can retrieve external knowledge" }
  ],
  "tools": [
    {
      "toolExternalId": "web_search",
      "toolName": "web_search",
      "principalDisplayName": "example-foundry-account/projects/proj-prod",
      "metadata": { "type": "web_search" }
    }
  ],
  "resources": [
    { "resourceType": "MONITORING",   "displayName": "appinsights-connection",
      "grantedThrough": "PROJECT_CONNECTION",
      "principalDisplayName": "example-foundry-account/projects/proj-prod" },
    { "resourceType": "SEARCH_INDEX", "displayName": "example-search-index",
      "grantedThrough": "PROJECT_CONNECTION" },
    { "resourceType": "MCP",          "displayName": "kb-knowledgebase",
      "grantedThrough": "PROJECT_CONNECTION" }
  ],
  "permissionFlow": {
    "version": 1,
    "agent_id": "node:agent",
    "nodes": [
      { "id": "node:agent", "type": "agent" },
      { "id": "node:identity", "type": "managed_identity",
        "name": "example-foundry-account/projects/proj-prod" },
      { "id": "node:res:0", "type": "resource", "data": { "category": "AppInsights" } }
    ],
    "edges": [
      { "from": "node:agent", "to": "node:identity", "relation": "runs_as" },
      { "from": "node:identity", "to": "node:res:0", "relation": "connects_to" }
    ]
  },
  "metadata": {
    "projectEndpoint": "https://example-foundry-account.services.ai.azure.com/api/projects/proj-prod",
    "subscriptionId": "<subscription-guid>",
    "resourceGroup": "example-resource-group",
    "instructionLength": 68,
    "instructionHash": "fb76887de60b…",
    "projectGovernance": {
      "lastWriteBy": {
        "principalId": "admin@example.com",
        "principalType": "USER",
        "metadata": { "ipAddress": "203.0.113.10", "mfaAuthenticated": true }
      },
      "lastWriteAt": "2026-06-12T15:09:54.000000000Z",
      "lastWriteOperation": "Microsoft.CognitiveServices/accounts/projects/applications/agentdeployments/write"
    }
  },
  "_connectorFoundrySourceAvailable": true,
  "_connectorSourcesUnavailable": ""
}
```

This single entry demonstrates the data model end to end: identity → managed identity (directory-enriched) →
project connections as resources, with a permission graph; content-safety guardrails from the model
deployment's policy; the model and tools from the agent definition; the instruction fingerprint but never
the raw text; invocation history (absent — the agent has not been invoked in the window); and project-level
governance — all from one read-only scan across four Azure surfaces.

### 12.5 Schema reference

The authoritative attribute reference for **both** object types — the table to compare the IDDM-generated
schema against.

**Column meanings**

| Column | Meaning |
|---|---|
| **Type** | The wire type IDDM generates. `JSON` means a string whose value is a serialized JSON object or array — IDDM stores and returns it as an opaque string; Identity Observability parses it. |
| **Write** | `RO` = read-only, `RW` = writable. A `modify` against a read-only attribute returns LDAP status **1** — see 1.4. |
| **Create** | Whether the attribute is required or optional when adding an entry. **`n/a` on every row**: the connector does not implement add, so no attribute is ever required *or* optional at create time. |
| **Platform** | Which agent surface populates it: `Both`, `Foundry`, `Copilot`, or a dash for attributes declared in the schema that have no Azure source on either. |
| **May be absent** | `no` = always present on every entry. `yes` = may legitimately be missing. |

> **No entry ever carries all 46 attributes, and that is by design.** Null values are dropped at the LDAP
> wire layer (5.6), and whole groups of attributes are structurally unsourceable on Azure: the A2A agent
> card block, most lifecycle actors, `instruction` (opt-in), and `model` and `guardrails` on Copilot Studio
> entries. Treat the **May be absent** column as the expected shape, not the full attribute list.

#### 12.5.1 Object type `vdAgentIdentity` — 46 attributes

RDN / naming attribute: **`agentId`**. Structural object class: `vdAgentIdentity`.

| # | Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|---|
| 1 | `agentId` | STRING | RO | n/a | Both | **no** | RDN. The platform agent id; qualified `{account}__{project}__{id}` in multi-project mode and `{environmentId}__{botId}` for Copilot Studio. |
| 2 | `externalId` | STRING | RO | n/a | Both | yes | Cross-provider correlation key: the Foundry agent GUID, or a synthetic provider path for Copilot Studio. |
| 3 | `name` | STRING | RO | n/a | Both | **no** | Human-readable agent name. |
| 4 | `description` | STRING | RO | n/a | Both | yes | Free text. Locally overridable downstream. |
| 5 | `intent` | STRING | RO | n/a | — | yes | **Always absent** — no Azure source; authored downstream. |
| 6 | `instruction` | STRING | RO | n/a | Foundry | yes | Raw system prompt. **Only when `enableInstructionText=true`.** |
| 7 | `version` | STRING | RO | n/a | Both | yes | Agent version string. |
| 8 | `platform` | STRING | RO | n/a | Both | **no** | `AZURE_FOUNDRY` or `COPILOT_STUDIO`. |
| 9 | `kind` | STRING | RO | n/a | Both | yes | `prompt` / `workflow` / `hosted` / `container_app`; `copilot` for Copilot Studio. |
| 10 | `tags` | JSON | RO | n/a | Foundry | yes | The project's Azure resource tags. Requires management-plane enrichment. |
| 11 | `status` | STRING | RO | n/a | Both | yes | Normalized lifecycle value. A disabled agent reads `SUSPENDED`. |
| 12 | `statusReason` | STRING | RO | n/a | Both | yes | The Azure-native status, carried verbatim when the mapping is lossy. |
| 13 | `actionQuarantined` | BOOLEAN | **RW** | n/a | Both | **no** | **The only writable attribute.** Writable only when `enableRemediation=true`; **always reads back `false`** (8.4). |
| 14 | `statusChangedAt` | STRING | RO | n/a | — | yes | Declared; no Azure source. |
| 15 | `statusChangedBy` | JSON | RO | n/a | — | yes | Declared; no Azure source. |
| 16 | `runtimeIdentity` | JSON | RO | n/a | Foundry | yes | The project managed identity, directory-enriched. Requires management-plane enrichment. |
| 17 | `createdAt` | STRING | RO | n/a | Both | yes | ISO-8601, fixed nanosecond precision. |
| 18 | `createdBy` | JSON | RO | n/a | Copilot | yes | Identity reference. Foundry agents are not audited, so this is Copilot-only. |
| 19 | `publishedAt` | STRING | RO | n/a | — | yes | Declared; no Azure source. |
| 20 | `publishedBy` | JSON | RO | n/a | — | yes | Declared; no Azure source. |
| 21 | `suspendedAt` | STRING | RO | n/a | — | yes | Declared; no Azure source. |
| 22 | `suspendedBy` | JSON | RO | n/a | — | yes | Declared; no Azure source. |
| 23 | `blockedAt` | STRING | RO | n/a | — | yes | Declared; no Azure source. |
| 24 | `blockedBy` | JSON | RO | n/a | — | yes | Declared; no Azure source. |
| 25 | `deletedAt` | STRING | RO | n/a | — | yes | Declared; no Azure source. |
| 26 | `deletedBy` | JSON | RO | n/a | — | yes | Declared; no Azure source. |
| 27 | `lastUpdatedAt` | STRING | RO | n/a | Both | yes | Per-agent modification time. |
| 28 | `lastUpdatedBy` | JSON | RO | n/a | Both | yes | Identity reference. For Foundry, the **project-level** Activity Log actor, shared by every agent in the project. |
| 29 | `lastInvokedAt` | STRING | RO | n/a | Foundry | yes | From Application Insights. Requires management-plane enrichment and a connected Application Insights. |
| 30 | `lastInvokedBy` | JSON | RO | n/a | — | yes | Declared; not populated in this release. |
| 31 | `metadata` | JSON | RO | n/a | Both | yes | Provenance, instruction fingerprint and length, invocation count, project governance, Copilot environment details. |
| 32 | `repositoryId` | STRING | RO | n/a | Both | **no** | Repository linkage: the subscription id. |
| 33 | `repositoryDisplayName` | STRING | RO | n/a | Both | **no** | `"Azure " + subscription display name`. |
| 34 | `model` | JSON | RO | n/a | Foundry | yes | Model array. **Not applicable to Copilot Studio.** |
| 35 | `guardrails` | JSON | RO | n/a | Foundry | yes | Content safety from the deployment's policy. Requires management-plane enrichment. **Not applicable to Copilot Studio.** |
| 36 | `features` | JSON | RO | n/a | Foundry | yes | Positive-signal behavioural flags synthesized from the declared tools. |
| 37 | `agentCardUrl` | STRING | RO | n/a | — | yes | A2A — declared; not populated in this release. |
| 38 | `url` | JSON | RO | n/a | — | yes | A2A — declared; not populated in this release. |
| 39 | `provider` | JSON | RO | n/a | Both | **no** | Owning organization of the agent. |
| 40 | `skills` | JSON | RO | n/a | — | yes | A2A — declared; not populated in this release. |
| 41 | `tools` | JSON | RO | n/a | Foundry | yes | From the agent definition, secrets redacted. |
| 42 | `resources` | JSON | RO | n/a | Foundry | yes | Project connections. Requires management-plane enrichment. |
| 43 | `subagents` | JSON | RO | n/a | Foundry | yes | Workflow children. Only on workflow agents. |
| 44 | `permissionFlow` | JSON | RO | n/a | Foundry | yes | Permission graph. Requires management-plane enrichment. |
| 45 | `_connectorFoundrySourceAvailable` | BOOLEAN | RO | n/a | Both | **no** | Connector diagnostic, **scan-level**: did the agent listing succeed this scan? |
| 46 | `_connectorSourcesUnavailable` | STRING | RO | n/a | Both | **no** | Connector diagnostic, **per entry**: comma-separated failed sources. Present but blank when the entry is complete. |

**Guaranteed present on every entry:** `agentId`, `name`, `platform`, `actionQuarantined`, `repositoryId`,
`repositoryDisplayName`, `provider`, `_connectorFoundrySourceAvailable`, `_connectorSourcesUnavailable`.

#### 12.5.2 Object type `vdIdentity` — 17 attributes

RDN / naming attribute: **`principalId`**. Structural object class: `vdIdentity`. **Opt-in**
(`enableIdentityEntity=true`, default off), **single-project only**, and entirely derived from the
management plane and the directory — with `enableArmEnrichment=false` this object type returns an empty
result set (a success, not a failure). **Every attribute is read-only; modify is not supported for this
object type at all.**

| # | Attribute | Type | Write | Create | May be absent | Notes |
|---|---|---|---|---|---|---|
| 1 | `principalId` | STRING | RO | n/a | **no** | RDN. Directory object id of the principal. |
| 2 | `externalId` | STRING | RO | n/a | yes | Directory application id for a service principal or managed identity, otherwise the principal id. |
| 3 | `displayName` | STRING | RO | n/a | yes | Directory name. Absent when directory enrichment is off or forbidden. |
| 4 | `principalType` | STRING | RO | n/a | yes | `MANAGED_IDENTITY` / `SERVICE_PRINCIPAL` / `USER` / `GROUP`. |
| 5 | `appId` | STRING | RO | n/a | yes | Directory application (client) id for service principals and managed identities. |
| 6 | `accountEnabled` | BOOLEAN | RO | n/a | yes | Directory enablement state. |
| 7 | `identityType` | STRING | RO | n/a | yes | `SystemAssigned` / `UserAssigned`; absent for non-managed identities. |
| 8 | `tenantId` | STRING | RO | n/a | yes | Owning Entra tenant id. |
| 9 | `apiPermissions` | JSON | RO | n/a | yes | API permissions and application-role grants held **by** this identity. |
| 10 | `groupMemberships` | JSON | RO | n/a | yes | Groups and directory roles this identity belongs to. |
| 11 | `roleAssignments` | JSON | RO | n/a | yes | Azure role assignments. Usually empty for Foundry. |
| 12 | `owners` | JSON | RO | n/a | yes | Directory owners of this identity — the accountability signal. |
| 13 | `referencedByAgents` | JSON | RO | n/a | yes | The agents in this scan that **run as** this identity. |
| 14 | `agentFanout` | INTEGER | RO | n/a | **no** | Number of agents running as this identity. |
| 15 | `isShared` | BOOLEAN | RO | n/a | **no** | `true` when more than one agent runs as this identity — the over-sharing signal. |
| 16 | `metadata` | JSON | RO | n/a | yes | Provenance: scope and source planes. |
| 17 | `_connectorSourcesUnavailable` | STRING | RO | n/a | **no** | Per-row failed sources; blank when the row is complete. |

### 12.6 Acceptance-test configuration

Configure the data source as below before running functional acceptance tests, or several will fail for
configuration reasons rather than defects.

| Setting | Value | Why |
|---|---|---|
| `maxAgents` | **`0`** | The default `1000` caps agents **per project** and truncates with only a warning, so entry counts cannot match the platform on a larger estate. The default stays at 1000 deliberately as a production scale guard, so this is a test-configuration change only. |
| `enableIdentityEntity` | `true` | Otherwise the second object type returns zero rows and cannot be exercised. |
| `enableArmEnrichment` | `true` | Required for `vdIdentity` to be populated at all, and for the subscription and resource-group validation in the connection test. |
| `foundryProjectEndpoint` | a **single** project | `vdIdentity` is single-project only and is not populated during a multi-project walk. Leave `enableMultiProject` off. |
| `enableRemediation` | `true` | Otherwise every modify returns 53 and the write-operations section is untestable. Requires a service principal with agent **write** access — the Reader used for listing is **not** sufficient. |
| `enableCopilotStudio` | **decide up front** | If on, Copilot Studio agents merge into the *same* result set, so the independently-verified expected entry count must cover **both** surfaces. Simplest is to leave it off and certify that source separately. |
| IDDM server and connector log level | `DEBUG` | How page-size and entry-count claims are verified. |

**Notes for the test engineer**

- **Object types:** there are **two**, so every "repeat for each object type" instruction is two passes —
  and the second one needs `enableIdentityEntity`.
- **Expected entry counts:** determine the expected `vdAgentIdentity` count from the Foundry project
  directly, and the expected `vdIdentity` count as the number of **distinct principals** the agents
  reference — not the number of agents.
- **"All attributes populated":** no entry ever carries all 46 attributes, by design. Use the **May be
  absent** and **Platform** columns in 12.5 as the expectation — an absent Copilot-only attribute on a
  Foundry agent is correct behaviour, not a defect.
- **Read-only attribute test:** every attribute except `actionQuarantined` is read-only. Modifying one
  returns `OPERATIONS_ERROR` (**1**) and writes nothing. Do **not** use `actionQuarantined` with
  `enableRemediation=false` as the read-only representative: that correctly returns **53**, because there
  the attribute *is* writable and the connector is declining rather than refusing. See the table in 1.4.
- **Equivalence classes for modify:** `actionQuarantined` is the only writable attribute. It is
  single-valued, optional, boolean (non-string) and connector-transformed. The multi-value, string and
  pass-through classes therefore have **no writable representative** and are not applicable. `vdIdentity`
  has no writable attribute at all, so modify is not applicable for that object type.
- **Verifying a persisted quarantine:** after a successful quarantine, `actionQuarantined` deliberately
  still reads **`false`** — the platform exposes no in-place attribution, so the connector never asserts an
  unprovable value (8.4). The observable change is **`status` becoming `SUSPENDED`**. Re-reading
  `actionQuarantined` expecting `true` records a false failure.
- **"Incorrect property value"** in the test-connection cases means **malformed or unauthorized**. The
  connector validates each required property and names the failing probe. Note that `azureSubscriptionId`
  and `azureResourceGroup` are validated by a **single** management-plane probe, whose URL embeds both as
  path segments. This is deliberate: probing the subscription directly would require a role assignment at
  *subscription* scope, which a least-privilege service principal (Reader on only the Foundry resource
  group) does not hold, so it would fail the connection test for a correctly configured data source. With
  `enableArmEnrichment=false` a management-plane probe failure is warned rather than failed, because that
  mode is documented as needing no management-plane access at all.

| Property | Made incorrect | Test connection |
|---|---|---|
| `azureTenantId` / `azureClientId` / `azureClientSecret` | Any wrong value | **Fails** — token acquisition |
| `foundryProjectEndpoint` | Wrong host or project | **Fails** — data-plane probe |
| `azureSubscriptionId` | A wrong but well-formed id | **Fails** — management-plane scope probe (requires `enableArmEnrichment=true`) |
| `azureResourceGroup` | A non-existent resource group | **Fails** — management-plane scope probe (management-plane enrichment on) |
| Power Platform access | A principal lacking the Power Platform administrator role | **Fails** — Power Platform probe (only when `enableCopilotStudio=true`) |

- **Add, delete and paging** are not supported (1.6) — mark those items not applicable.

---

© Radiant Logic. Azure, Azure AI Foundry, Microsoft Entra ID, Microsoft Graph, Copilot Studio, Power
Platform and Dataverse are trademarks of Microsoft Corporation.
