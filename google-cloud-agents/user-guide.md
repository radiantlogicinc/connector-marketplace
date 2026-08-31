# Google Cloud Agents Connector for RadiantOne — User Guide

**Product:** Google Cloud Agents Connector (custom connector)
**Version:** 0.9.2 — **beta**
**Author / Publisher:** Radiant Logic

> **Beta.** This connector has not yet completed official validation. Deploy it to a non-production
> environment first. Versions in beta are numbered `0.9.x`, with the patch digit incremented on every
> change; `1.0.0` is reserved for the first officially validated release.

This is the complete, self-contained guide to the Google Cloud Agents Connector — a RadiantOne custom
connector that brings **agentic AI** under Identity Observability (IDO) governance by exposing the AI
agents hosted in your Google Cloud projects as LDAP identities.

It is written for two audiences:

- **Operators** — install, configure, run, troubleshoot: sections 3, 4, 7, 9.
- **Architects & security** — data model, Google Cloud mapping, read-only posture, scale: sections 5,
  6, 8.

## Table of contents

1. [Introduction — agentic AI identity for IDO](#1-introduction--agentic-ai-identity-for-ido)
2. [Background — the Google Cloud agent services](#2-background--the-google-cloud-agent-services)
3. [Installation & deployment](#3-installation--deployment)
4. [Configuration reference — all 26 properties](#4-configuration-reference--all-26-properties)
5. [The agent data model](#5-the-agent-data-model)
6. [Data model ↔ Google Cloud mapping](#6-data-model--google-cloud-mapping)
7. [Keeping IDDM in sync](#7-keeping-iddm-in-sync)
8. [Performance & scale](#8-performance--scale)
9. [Logs & troubleshooting](#9-logs--troubleshooting)
10. [Appendices](#10-appendices)

---

## 1. Introduction — agentic AI identity for IDO

### 1.1 What this connector is

A custom connector that makes the **AI agents hosted in your Google Cloud projects** visible inside
IDDM as ordinary LDAP entries, so they can be governed like any other identity. It discovers agents
across five Google Cloud services, normalizes them to one provider-agnostic shape, and enriches each
with the things a governance team actually needs: which model it uses, which tools it can call, which
data it can reach, which identity it runs as, and who can invoke it.

It is one of a family of connectors that emit the **same** output schema — the AWS and Azure agentic
connectors are its siblings. Only the source cloud differs, so an IDO deployment can consume all of
them side by side and compare like with like.

### 1.2 Why — non-human identity governance for agentic AI

An AI agent is a non-human identity with real privilege: it runs as a service account, it calls tools,
it reads knowledge bases, and it can reach whatever its identity is allowed to reach. Left outside the
identity programme, that privilege is invisible — nobody can answer "which agents can read this
bucket?", "which agents have no guardrail?", or "who can invoke this agent?".

This connector answers those questions by turning each agent into an inventoried, attributed identity
with an explicit permission graph.

### 1.3 What it produces at a glance

One LDAP object type, `vdAgentIdentity`, with **46 attributes**
([§10.5](#105-schema-reference--vdagentidentity)). Each entry is one Google Cloud agent. The `platform`
attribute says which service it came from:

| Google Cloud service | `platform` value | Notes |
|---|---|---|
| Vertex AI Agent Engine (Reasoning Engines) | `VERTEX_REASONING_ENGINE` | custom-coded agents built on agent frameworks |
| Dialogflow CX | `DIALOGFLOW_CX` | conversational agents; per-location endpoints |
| Dialogflow ES | `DIALOGFLOW_ES` | one legacy agent per project |
| Vertex AI Agent Builder (Discovery Engine) | `VERTEX_AGENT_BUILDER` | search and chat engines over data stores |
| CX Agent Studio (Customer Engagement Suite) | `CX_AGENT_STUDIO` | **opt-in** — see `enableCesAgentStudio` |

A sixth value, `VERTEX_ADK_CLOUD_RUN`, is declared in the schema for agents self-hosted on Cloud Run;
the connector does not discover those in this release.

### 1.4 Posture — read-only

**The connector never writes to Google Cloud.** It implements no create, modify or delete operation,
requests only `list`- and `get`-shaped permissions, and treats every value it reads as authoritative.
There is no remediation write-back, no tagging and no quarantine enforcement. The `actionQuarantined`
attribute exists in the shared data model as a write-back control, and this connector always reports it
as `false`.

Two further protections are deliberate and visible in the output:

- **Prompt and instruction text is not emitted by default.** A generative prompt is fingerprinted
  (SHA-256 plus length) rather than emitted, unless you opt in with `enableInstructionText`.
- **Secrets are stripped from the native payload copies.** Values of credential-shaped keys are
  replaced with a redaction marker, and embedded documents — an OpenAPI specification carried as one
  string, or any string over 4096 characters — are replaced by a fingerprint, because a credential
  inside such a document cannot be found by key name.

### 1.5 LDAP result codes

| Situation | Result code | Meaning |
|---|---|---|
| Scan succeeded, including "zero agents found" | `SUCCESS` (0) | Normal. An empty result is a success, not a failure. |
| Scan succeeded but some sources failed | `SUCCESS` (0) | **Partial by design.** Check the diagnostic attributes — see [§9.2](#92-diagnostic-attributes). |
| Every agent source failed and nothing was returned | `UNAVAILABLE` (52) | Credentials, connectivity or permissions. |
| Unexpected error | `OTHER` (80) | A defect or an unhandled Google Cloud response; the server log carries the detail. |
| Any add, modify or delete request | *not applicable* | Those operations are not implemented — see [§1.7](#17-connector-type-target-application--supported-operations). |

### 1.6 Scope & non-goals

- **One Google Cloud credential per data source.** Multiple credentials mean multiple data sources.
- **Many projects and locations per data source** — either listed explicitly or auto-discovered.
- **Not a runtime monitor.** A scan is a point-in-time inventory; IDDM's own refresh schedule decides
  how current it is.
- **Not an invocation log.** The connector reads control-plane metadata, not conversations.

### 1.7 Connector type, target application & supported operations

| Fact | Value |
|---|---|
| **Connector type** | **SDK connector** — built on the IDDM Connector SDK 1.2.0. Not a legacy plugin: there is no fully-qualified class name to enter by hand. Configuration is **auto-populated on import** from the connector's bundled configuration descriptor. |
| **Target application** | **IDDM.** The connector is deployed into IDDM and exposes agents as LDAP entries. The data it produces is designed to be consumed downstream by **IDO** (Identity Observability), whose connector mapping bridges the connector's `camelCase` attribute names to IDO's `snake_case` model. It is **not** installed into IDO directly. |
| **Schema authoring** | **Used.** The connector declares its schema, so IDDM **auto-generates the data-source schema** when the data source is created. Do not hand-build the schema in the Control Panel, and do not import a schema file — there isn't one. The generated schema must match [§10.5](#105-schema-reference--vdagentidentity). |
| **Object types exposed** | **One:** `vdAgentIdentity` (46 attributes; IDDM auto-prefixes entity names with `vd`). All five agent services are exposed as this single type, discriminated by `platform`. |
| **Deployable artifact** | `google-cloud-agents-connector-0.9.2.jar` (about 11.8 MB), with all dependencies embedded. It is the only artifact; no thin JAR and no source distribution are published. |

**Supported LDAP operations:**

| Operation | Supported | Detail |
|---|---|---|
| **Search** — sub-tree | Yes | Returns every agent from every enabled source. |
| **Search** — base | Yes | Base DN `agentId=<value>,<container-dn>` returns exactly that one entry. The connector resolves the target itself and stops scanning once it finds it. |
| **Test connection** | Yes | Validates credentials **and** the configured scope — see [§9.3](#93-what-test-connection-checks). |
| **Add** | No | The connector cannot create Google Cloud agents. |
| **Modify** | No | **All 46 attributes are read-only.** |
| **Delete** | No | The connector cannot delete Google Cloud agents. |
| **Paging** | No | Structural, not an omission: the SDK search contract is eager and single-shot, so there is no paging hook. Bound result size with `gcpMaxAgents` or by narrowing the scope instead. |

---

## 2. Background — the Google Cloud agent services

Google Cloud has no single "list all agents" API. Agents are project-scoped resources spread across
separate services, each with its own API surface, its own location model and its own IAM. The
connector's job is to hide that.

**Discovery flow.** For each data source scan:

1. **Resolve projects** — the explicit `gcpProjectIds` list, or auto-discovery of every `ACTIVE`
   project the credential can see, optionally narrowed to one organization.
2. **Resolve locations** per project — the explicit `gcpLocations` list, or the enabled Vertex AI
   locations for that project, always including `global`.
3. **Scan each enabled service** per (project, location) and map every agent to `vdAgentIdentity`.
4. **Enrich each agent** — tools, model, guardrails, resources, runtime identity and permission graph,
   audit history, agent card — according to the enrichment options.
5. **Finalize** — derive the capability flags, normalize timestamps, serialize the JSON-bearing
   attributes.

Two Google Cloud quirks show up in configuration and in logs:

- **Dialogflow CX uses per-location endpoints.** The `global` location is served by the base Dialogflow
  host; a regional agent must be read from that region's own host. The connector handles this
  automatically, and the same applies to resource-scoped Vertex AI operations.
- **Discovery Engine and CES use multi-regions** (`global`, `us`, `eu`), not ordinary regions. Agent
  Builder is scanned at `global`; CES multi-regions are derived from the configured locations (for
  example `us-central1` maps to `us`).

### 2.1 The five services side by side

| | Reasoning Engines | Dialogflow CX | Dialogflow ES | Agent Builder | CX Agent Studio |
|---|---|---|---|---|---|
| Agents per project | many | many, per location | at most one | many | many, per app |
| Creation timestamp from the API | yes | **no** | **no** | yes | yes |
| Model exposed | **no** (lives in deployed code) | yes, when generative settings exist | no | no | no |
| Guardrails exposed | no | yes, when configured | no | no | no |
| Tools exposed | yes, inline | yes, extra call | no | no | no |
| Runtime identity derivable | **no** | yes | yes | no | no |
| Knowledge data stores | no | yes, via tools | no | yes | no |
| Enabled by default | yes | yes | yes | yes | **no** |

---

## 3. Installation & deployment

### 3.1 Prerequisites

- **IDDM 8.2.0 or later.**
- Administrative access to the **IDDM Main Control Panel**.
- One or more **Google Cloud projects** containing the agents you want to govern, reachable from IDDM
  over HTTPS.
- Permission in Google Cloud to enable APIs and create a service account, or a colleague who has it.

### 3.2 Enable the Google Cloud APIs

Enable these on **every project you intend to scan**. A disabled API returns a permission error that
the connector isolates per source — the scan continues, but that source contributes nothing.

| API | Needed for |
|---|---|
| `cloudresourcemanager.googleapis.com` | project auto-discovery, project numbers, project IAM fallback |
| `aiplatform.googleapis.com` | Reasoning Engines, location discovery |
| `dialogflow.googleapis.com` | Dialogflow CX and ES, tools, generative settings |
| `discoveryengine.googleapis.com` | Agent Builder engines, data stores |
| `cloudasset.googleapis.com` | the outbound permission walk (`enableOutboundPermissions`) |
| `logging.googleapis.com` | audit history (`enableAuditHistory`) |
| `ces.googleapis.com` | CX Agent Studio (`enableCesAgentStudio`) |

```bash
gcloud services enable cloudresourcemanager.googleapis.com aiplatform.googleapis.com \
  dialogflow.googleapis.com discoveryengine.googleapis.com --project MY_PROJECT
# add cloudasset / logging / ces only if you enable the matching connector options
```

### 3.3 Create the service account & role

The connector needs **read-only** access. Create a custom role with the permissions in
[§10.2](#102-complete-iam-reference-grant--option) — every permission is tied to the option that needs
it, so you can grant only what you switch on.

```bash
# 1. the service account
gcloud iam service-accounts create iddm-agent-reader \
  --display-name "IDDM agent inventory (read-only)" --project MY_PROJECT

# 2. a least-privilege custom role (edit the permission list to match your enabled options)
cat > iddm-agent-reader-role.yaml <<'YAML'
title: "IDDM Agent Reader"
description: "Read-only discovery of Google Cloud AI agents for the IDDM connector"
stage: "GA"
includedPermissions:
  - resourcemanager.projects.get
  - aiplatform.locations.list
  - aiplatform.reasoningEngines.list
  - aiplatform.reasoningEngines.get
  - dialogflow.agents.list
  - dialogflow.agents.get
  - dialogflow.tools.list
  - discoveryengine.engines.list
  - discoveryengine.dataStores.get
YAML
gcloud iam roles create iddmAgentReader --project MY_PROJECT --file iddm-agent-reader-role.yaml

# 3. bind it
gcloud projects add-iam-policy-binding MY_PROJECT \
  --member "serviceAccount:iddm-agent-reader@MY_PROJECT.iam.gserviceaccount.com" \
  --role "projects/MY_PROJECT/roles/iddmAgentReader"

# 4. a key (see §3.4 for the alternative)
gcloud iam service-accounts keys create sa-key.json \
  --iam-account iddm-agent-reader@MY_PROJECT.iam.gserviceaccount.com
```

> **Least privilege and project listing.** `resourcemanager.projects.list` is required **only** for
> auto-discovery. If you list your projects explicitly in `gcpProjectIds` — the recommended production
> setup — you do not need it, and test connection will not ask for it.

> **Missing permissions degrade, they do not break.** A permission you did not grant produces an
> isolated per-source or per-entry failure recorded in the diagnostics
> ([§9.2](#92-diagnostic-attributes)); the rest of the scan still completes. The Cloud Asset permission
> in particular is commonly ungranted and has a low quota — see
> [§10.3](#103-known-limitations--deferred-items).

### 3.4 Authentication modes

Provide **exactly one** of these. If both are set, the key wins.

| Mode | Property | When to use |
|---|---|---|
| **Service-account key JSON** *(recommended)* | `gcpServiceAccountKey` | Normal production. Paste the full contents of the downloaded key file. The connector acquires and **auto-refreshes** OAuth2 tokens itself. |
| **Raw OAuth2 access token** | `gcpAccessToken` | Keyless and token-injection setups, and short-lived testing. The token is used **verbatim with no refresh**, so it expires after roughly an hour and every scan then fails until you supply a fresh one. Test connection says so explicitly when it happens. |

Application Default Credentials and the compute metadata server are **not** supported modes in this
release.

The token scope in both modes is `https://www.googleapis.com/auth/cloud-platform`.

> **Pasting a key.** Copying key JSON out of a browser, PDF or document can silently replace
> indentation with non-breaking spaces or prepend a byte-order mark — neither is valid JSON. The
> connector normalizes both before parsing, so a mangled paste still works. If a key is rejected
> anyway, the message names the reason: invalid JSON, or a missing client email.

> **Credentials are read once, at load time.** Editing them takes effect only after the connector is
> reloaded or the IDDM service is restarted.

### 3.5 Deploy the JAR into IDDM

1. Download `google-cloud-agents-connector-0.9.2.jar` from the marketplace package.
2. In the **IDDM Main Control Panel**, create a custom template from that JAR file. There is no need to
   fill in template details: IDDM reads them from the connector itself.
3. Reload or restart as your IDDM version requires.

### 3.6 Create the data source

1. **Main Control Panel → New Data Source**, choose the imported **Google Cloud Agents** template. The
   configuration form **auto-populates** with all 26 properties, grouped as **Connection**,
   **Agent sources**, **Enrichment** and **Performance**.
2. Fill in `gcpServiceAccountKey` (or `gcpAccessToken`).
3. **Strongly recommended:** set `gcpProjectIds` to the projects you actually want, and `gcpLocations`
   to the regions you actually use. Both narrow the scan dramatically and remove the need for the
   project-listing permission.
4. Switch off any agent source you do not use, and any enrichment whose permission you did not grant.
5. Save. IDDM generates the schema at this point ([§1.7](#17-connector-type-target-application--supported-operations)).

> IDDM displays each property by its **uppercased name** (for example `GCPPROJECTIDS`) and shows the
> description as a **tooltip**. There is no separate display label.

### 3.7 First smoke test

1. **Test Connection** — expect success. If it fails, the message names the cause; see
   [§9.3](#93-what-test-connection-checks).
2. Browse the data source. Each agent appears as a `vdAgentIdentity` entry with `agentId` as its RDN.
3. Spot-check one entry: `platform`, `repositoryId` and a populated `metadata` JSON string.
4. Confirm `_connectorGcpSourceAvailable` is true and `_connectorSourcesUnavailable` is empty. Anything
   in the latter tells you which enrichment lacked a permission.

### 3.8 Upgrading & the schema-recreate rule

> **The schema is read from the JAR once, when the data source is created, and is NOT refreshed when
> you upgrade the connector.** Any release that changes the attribute set or the property list requires
> the administrator to **delete and re-create the data source** (or hand-edit the schema in the Control
> Panel). This guide flags every such release explicitly. A release that
> changes neither is a drop-in JAR replacement.

The same one-shot reading applies to the connector's **configuration descriptor**, which IDDM parses when
a **template** is created — including the marker that tells Identity Observability this connector is an
agent source. A release that changes the descriptor therefore needs the **template** re-imported from the
new JAR, even when it needs no data-source recreate. The release notes say so per
version in their upgrade notes.

---

## 4. Configuration reference — all 26 properties

Every property is **optional** except that **one of** `gcpServiceAccountKey` / `gcpAccessToken` must be
supplied. An empty value always means "use the default".

### 4.1 Connection

| Property | Type | Default | Purpose / allowed values |
|---|---|---|---|
| `gcpServiceAccountKey` | password | *(empty)* | The full service-account key JSON. Provide **either** this or `gcpAccessToken`; the key wins if both are set. Rejected at data-source creation if it is not parseable JSON carrying a client email. |
| `gcpAccessToken` | password | *(empty)* | A raw OAuth2 bearer token scoped to `https://www.googleapis.com/auth/cloud-platform`. Used verbatim, **never refreshed** — expect about an hour of validity. |
| `gcpProjectIds` | string | *(empty)* | Comma-separated project ids to scan, for example `proj-a,proj-b`. Empty means auto-discover every `ACTIVE` project the credential can see, which needs `resourcemanager.projects.list`. **Setting this explicitly is the recommended production configuration.** |
| `gcpLocations` | string | *(empty)* | Comma-separated locations per project, for example `us-central1,europe-west1,global`. Empty means auto-discover the project's enabled Vertex AI locations, always adding `global`. |
| `gcpOrganizationId` | string | *(empty)* | Numeric organization id that narrows **auto-discovery** to projects parented by that organization. Ignored when `gcpProjectIds` is set. **Limitation:** matches only projects parented *directly* by the organization — a project inside a folder is skipped. Must be numeric; a non-numeric value fails test connection. |

### 4.2 Agent sources

Switch off what you do not use: each disabled source removes its API calls and its permission
requirement entirely.

| Property | Type | Default | Purpose |
|---|---|---|---|
| `enableReasoningEngines` | boolean | `true` | Scan Vertex AI Agent Engine (Reasoning Engines). |
| `enableDialogflowCx` | boolean | `true` | Scan Dialogflow CX agents across the project's locations. |
| `enableDialogflowEs` | boolean | `true` | Scan the project's single Dialogflow ES agent. A not-found response simply means none exists. |
| `enableAgentBuilder` | boolean | `true` | Scan Vertex AI Agent Builder (Discovery Engine) engines at `global`. |
| `enableCesAgentStudio` | boolean | **`false`** | Scan CX Agent Studio (Customer Engagement Suite) agents. **Opt-in:** CES is a separate product needing its own API and permissions; enabling it where CES is unused produces isolated, non-fatal source failures. |

### 4.3 Enrichment

All of these add per-agent API calls. Switching one off makes the attributes it fills absent, which is
a legitimate, documented state — not a defect.

| Property | Type | Default | Purpose | Fills |
|---|---|---|---|---|
| `enableToolEnrichment` | boolean | `true` | One tool-listing call per Dialogflow CX agent. Reasoning Engine tools are derived inline and unaffected. | `tools`, CX knowledge `resources` |
| `enableModelEnrichment` | boolean | `true` | One generative-settings call per CX agent — fills **both** model and guardrails. CX only: Reasoning Engines expose no model. | `model`, `guardrails` |
| `enableFeatureDerivation` | boolean | `true` | **No API call.** Derives positive-signal flags from data already collected. | `features` |
| `enableInstructionText` | boolean | **`false`** | Emit the **raw** generative prompt. Off by default because prompts often contain sensitive content; the SHA-256 fingerprint and length are available either way. | `instruction` |
| `enableAuditHistory` | boolean | **`false`** | One Cloud Audit Logs query per agent. **The only source of created and last-modified for Dialogflow CX and ES**, whose APIs omit those timestamps. | `createdAt`/`createdBy`, `lastUpdatedAt`/`lastUpdatedBy` |
| `gcpHistoryDays` | number | `30` | Audit lookback window in days, 1–400 (400 is the API maximum). Larger windows recover older events at more cost. | — |
| `enableAgentCard` | boolean | **`false`** | Fetch the agent-to-agent card from `{host}/.well-known/agent.json` for agents with a host configured below. Fetched **unauthenticated first**, retried with the connector token only if the host challenges, so the Google Cloud token is never volunteered to a third party. | `agentCardUrl`, `url`, `provider`, `skills` |
| `gcpAgentCardHostsJson` | string | *(empty)* | JSON object mapping an agent's `externalId` to the base host serving its card, for example `{"projects/p/locations/l/reasoningEngines/1":"https://my-agent.example.run.app"}`. Required for native cards, because Google Cloud list APIs expose no serving endpoint. A malformed value fails test connection. | — |
| `enablePermissionAnalysis` | boolean | `true` | One IAM-policy call per Reasoning Engine and CX agent — who can invoke the agent, and the identity it runs as. | `runtimeIdentity`, `permissionFlow` |
| `enableOutboundPermissions` | boolean | `true` | A Cloud Asset policy analysis per unique runtime service account — what that identity can reach. **Requires `enablePermissionAnalysis`.** | `resources` IAM fields, outbound graph edges |
| `enableProjectIamFallback` | boolean | `true` | When a Dialogflow agent's resource-level policy is empty — Dialogflow IAM is project-scoped — fall back to the project policy and surface the bindings that actually grant agent access. **Requires `enablePermissionAnalysis`.** | inbound `permissionFlow` edges, allowed assumers |

### 4.4 Performance

| Property | Type | Default | Purpose |
|---|---|---|---|
| `enrichmentThreads` | number | `10` | Worker threads for per-agent enrichment, 1–50. This is the main scan-time lever: enrichment is per-agent and I/O-bound. **Set `1` for fully sequential enrichment** when a Google Cloud per-minute quota is the binding constraint. Shared lookups are de-duplicated across threads, so raising this does not multiply them. |
| `gcpMaxRetries` | number | `3` | Retries per API call on HTTP 429 and 5xx, 0–10, with bounded exponential backoff. The first attempt is not a retry. |
| `gcpMaxAgents` | number | `0` | Cap on agents emitted per scan; **`0` means unlimited**. A heap guard for very large estates. When it bites, the scan stops and records a diagnostic, so a truncated result can never look complete. **Leave at `0` whenever an exact entry count matters.** |
| `gcpCallTimeoutSeconds` | number | `120` | Ceiling on a single API call, 10–600. Distinct from the connect (30 s) and read (60 s) timeouts, which bound individual socket phases: a slow-drip response satisfies both indefinitely. |
| `gcpScanTimeoutMinutes` | number | `30` | Ceiling on a whole scan, 0–1440; **`0` means no deadline**. On expiry the partial result is returned and a diagnostic is recorded. Raise it for very large estates. |

### 4.5 Recommended configurations

| Goal | Settings |
|---|---|
| **First deployment / smoke test** | key plus `gcpProjectIds` plus `gcpLocations`; leave everything else at its default. |
| **Least-privilege production** | explicit `gcpProjectIds`; disable unused sources; disable `enableOutboundPermissions` if the Cloud Asset permission is not granted. |
| **Fullest picture** | enable `enableAuditHistory` (grant the log-reading permission) and `enableCesAgentStudio` if CES is in use; keep the permission options on. |
| **Quota-constrained project** | `enrichmentThreads=1`, `gcpMaxRetries=5`, consider `enableOutboundPermissions=false`. |
| **Acceptance testing** | see [§10.6](#106-acceptance-test-configuration). |

---

## 5. The agent data model

### 5.1 What it is and why

The output follows a **provider-agnostic canonical model** shared with the AWS and Azure agentic
connectors, so a governance consumer writes one integration and gets every cloud. The model is
organized as nine concepts:

| Concept | Carried by | What it answers |
|---|---|---|
| **Agent** | the entry's core attributes | what and where is this agent, who owns it, when did it change |
| **Model** | `model` | which model does it reason with, from which provider |
| **GuardRail** | `guardrails` | what safety policy constrains it, and is that policy enforcing |
| **Features** | `features` | what is it capable of — actions, knowledge, guardrails |
| **Skills** | `skills` | what does it declare it can do, in agent-to-agent terms |
| **Tools** | `tools` | what can it call |
| **Resources** | `resources` | what data and services can it reach, and how was that granted |
| **Subagents** | `subagents` | what does it delegate to (no Google Cloud source in this release) |
| **Permission Flow** | `permissionFlow` | the graph: who may invoke it, what identity it runs as, what that identity reaches |

Attribute names on the wire are LDAP-valid `camelCase`; IDO's connector mapping bridges them to its own
`snake_case` model.

### 5.2 Output mechanics that matter to consumers

Four behaviors will otherwise look like bugs:

1. **Null attributes are absent, not empty.** LDAP has no null, so an attribute the connector could not
   populate simply does not appear on the entry. **No entry ever carries all 46 attributes** — see the
   *May be absent* column in [§10.5](#105-schema-reference--vdagentidentity).
2. **Sub-entities ride as JSON strings.** `tools`, `resources`, `model`, `guardrails`, `features`,
   `permissionFlow`, `runtimeIdentity`, `metadata`, `tags`, `provider`, `url`, `skills`, `subagents`
   and every `*By` attribute are single-valued LDAP strings containing JSON. `agentCardUrl` is a plain
   string.
3. **Empty arrays are omitted entirely** rather than emitted as `[]`.
4. **Timestamps are normalized** to a fixed nine-digit-nanosecond ISO form
   (`2026-05-26T22:54:23.626338932Z`), regardless of the precision Google Cloud returned, so the
   connectors in this family are byte-comparable.

A fifth behavior matters to change detection: **output is byte-stable across scans.** The embedded
copies of native Google Cloud payloads are serialized with canonical key ordering, so an unchanged
agent produces the same value every time and downstream change detection does not see modifications
that never happened.

### 5.3 Identifiers

| Attribute | Value |
|---|---|
| `agentId` | **RDN.** The last segment of the Google Cloud resource name — a numeric id for Reasoning Engines, a UUID for Dialogflow CX, `{project}-es` for the ES agent. |
| `externalId` | The **full Google Cloud resource name**, for example `projects/p/locations/us-central1/reasoningEngines/123`. The stable cross-provider key. |
| `repositoryId` / `repositoryDisplayName` | The project id, and `GCP <project>`. Always present. |

---

## 6. Data model ↔ Google Cloud mapping

### 6.1 Mapping table

| Attribute | Google Cloud source | Notes |
|---|---|---|
| `agentId` | last segment of the resource name | Synthesized as `{project}-es` for Dialogflow ES, whose API returns no resource id. |
| `externalId` | the full resource name | — |
| `name` | the agent's display name | Falls back to the id when the service has no display name. |
| `description` | the agent's description | — |
| `intent` | none | Authored by IDO, never provider-sourced. |
| `instruction` | the CX generative prompt | Raw text only when `enableInstructionText` is on; otherwise fingerprinted under `model`. |
| `version` | Dialogflow ES API version | No other service exposes a version. |
| `platform` / `kind` | constant per service | The discriminator and its sub-type. |
| `tags` | Google Cloud labels | Promoted to a top-level attribute. |
| `status` / `statusReason` | none | No service exposes a lifecycle state field. |
| `actionQuarantined` | none | Always `false`: a write-back control this read-only connector never sets. |
| `statusChangedAt` / `statusChangedBy` | none | No status-transition signal exists. |
| `createdAt` | creation time (Reasoning Engines, Agent Builder, CES); the create audit event (Dialogflow CX and ES) | Dialogflow needs `enableAuditHistory`. |
| `createdBy` | the create audit event's principal | Requires `enableAuditHistory`. |
| `lastUpdatedAt` | update time (Reasoning Engines, Agent Builder, CES); the latest update audit event (Dialogflow CX and ES) | Same caveat. |
| `lastUpdatedBy` | the latest update audit event's principal | Requires `enableAuditHistory`. |
| `publishedAt`/`By`, `suspendedAt`/`By`, `blockedAt`/`By`, `deletedAt`/`By` | none | Google Cloud has no publish, suspend, block or soft-delete signal to map. |
| `lastInvokedAt` / `lastInvokedBy` | none | Invocation events are deliberately excluded to bound audit volume. |
| `metadata` | project, location, resource name, entity tag, labels, service-specific fields, and a secret-redacted copy of the native object | Also carries the agent-card provenance and the prompt fingerprint. |
| `repositoryId` / `repositoryDisplayName` | the scan's project context | Mandatory linkage; never absent. |
| `model` | the CX generative settings' model configuration | Provider is reported as Google Vertex. The prompt is fingerprinted into the model's metadata, never emitted there raw. |
| `guardrails` | the CX generative settings' safety configuration | One guardrail describing the banned-phrase policy, with enforcement mode; the phrase list itself is not emitted. |
| `features` | derived from `tools`, `resources` and `guardrails` | No API call. Positive signals only: actions enabled, knowledge enabled, guardrails enabled. |
| `agentCardUrl`, `url`, `skills` | a fetched agent-to-agent card | Requires `enableAgentCard` and a host in `gcpAgentCardHostsJson`. |
| `provider` | constant, overridden by a fetched card | Always populated. |
| `tools` | the deployed Reasoning Engine's declared methods; the CX tool list | CX tools require `enableToolEnrichment`. |
| `resources` | Agent Builder data stores and CX data-store tool connections; plus the Cloud Asset outbound walk | Knowledge data stores are free of extra calls; the IAM fields come from the outbound walk. |
| `subagents` | none | No discovery API is wired. |
| `permissionFlow` | resource-level IAM policies, the project policy fallback, and the Cloud Asset outbound walk | A graph of nodes and edges — see [§6.3](#63-the-permission-graph). |
| `runtimeIdentity` | the derived runtime service account, plus the principals in the agent's IAM policy | Dialogflow only: a Reasoning Engine's runtime service account is not API-exposed. |
| `_connectorGcpSourceAvailable`, `_connectorSourcesUnavailable` | the connector itself | Diagnostics — see [§9.2](#92-diagnostic-attributes). |

### 6.2 Business-rule callouts

- **Uniform key set.** Every entry is built with all 46 keys; keys that do not apply to that agent's
  platform are null and therefore dropped at the wire.
- **Every `*By` attribute is an identity reference** — a principal id, a principal type, a display name
  and metadata. Account references are never emitted: IDO reconciles them.
- **The prompt is never emitted raw except on request.** A fingerprint and a length always ride under
  the model's metadata, so you can detect that a prompt changed without ever storing it.
- **Dialogflow IAM is project-scoped.** A CX agent usually has no resource-level policy, which is why
  the project-policy fallback exists: it surfaces only the bindings whose roles actually grant agent
  access, rather than the whole project policy.
- **The outbound walk is cached per unique runtime identity per scan,** not per agent, so many agents
  sharing one service account cost one analysis.
- **The agent's own project is filtered out of the outbound results** as noise: a bare project-level
  binding reflects the agent's own home, not a reachable resource. Cross-project bindings are kept.

### 6.3 The permission graph

`permissionFlow` is an acyclic graph — a version, the agent's node id, a list of nodes and a list of
edges. Node types are `agent`, `service_account`, `iam_role`, the principal types of role members, and
`knowledge_base`. Edge relations are:

| Relation | Meaning | Direction |
|---|---|---|
| `runs_as` | the agent runs as this service account | agent → identity |
| `iam_binding` | this role is bound on the agent | agent → role |
| `member` | this principal holds the role | role → principal |
| `uses_knowledge` | the agent is wired to this knowledge resource | agent → resource |
| `can_access` | the runtime identity can reach this resource | identity → resource |

The first three answer "who can invoke this agent"; the last two answer "what can this agent reach".

---

## 7. Keeping IDDM in sync

The connector is a **pull-based inventory**. A scan is a point-in-time snapshot, and IDDM's own cache
refresh decides how current the view is.

**How to keep it current**

| Approach | How | Trade-off |
|---|---|---|
| **Periodic refresh** *(recommended)* | Configure IDDM's periodic refresh on the data source. | Simple and predictable. Freshness equals the refresh interval. |
| **On-demand re-read** | Trigger a refresh of the data source when your own automation learns that agents changed. | Fresher, but each refresh is a full scan — size the interval against the scan cost in [§8](#8-performance--scale). |

There is **no** connector-side incremental mode, no change stream and no per-entry refresh hook in this
release: every scan reads the full configured scope. Two consequences worth planning for:

- **Concurrent searches each run their own scan.** Two simultaneous IDDM searches double the API
  volume; there is no shared in-flight result.
- **Scan cost scales with scope, not with change.** Narrowing `gcpProjectIds` and `gcpLocations` is the
  single most effective way to make frequent refreshes affordable.

Because output is byte-stable for an unchanged agent ([§5.2](#52-output-mechanics-that-matter-to-consumers)),
a refresh that finds nothing changed produces no spurious modifications downstream.

---

## 8. Performance & scale

### 8.1 What bounds a scan

The dominant cost is **per-agent enrichment**: a single agent can cost up to about five API calls
(tools, generative settings, IAM policy, outbound analysis, audit history). Discovery itself is cheap —
one call per (project, location, service).

That makes the fan-out shape `projects × locations × services` for discovery, plus `agents × enabled
enrichments` for enrichment. Auto-discovery inflates the first term before a single agent is read.

### 8.2 The levers, in order of effect

1. **`gcpProjectIds` and `gcpLocations`** — the biggest win by far.
2. **Source options** — every disabled source removes its whole listing pass.
3. **`enrichmentThreads`** — enrichment is I/O-bound, so this scales close to linearly until a Google
   Cloud quota pushes back. Moving from sequential to ten threads roughly halves wall-clock time on a
   few dozen agents, and the gain grows with fleet size.
4. **Enrichment options** — `enableAuditHistory` and `enableOutboundPermissions` are the most
   expensive; `enableFeatureDerivation` is free.

### 8.3 Quota-aware tuning

Some Google Cloud APIs the connector touches have low per-minute quotas — the Cloud Asset policy
analysis most of all. Under throttling the connector does the right thing: the affected call is
retried within its budget, then isolated as a per-entry diagnostic, and the scan still completes with
every agent emitted and its inbound data intact.

If you see that diagnostic often, either lower `enrichmentThreads` (down to `1`), raise `gcpMaxRetries`,
or switch `enableOutboundPermissions` off and accept that `resources` will carry no IAM fields.

### 8.4 Capacity planning — memory

Every entry embeds a lossless copy of its native Google Cloud object, secret-redacted and with large
embedded documents fingerprinted. Entries are therefore larger than their canonical fields suggest —
tens of kilobytes each is normal, and an agent with a rich tool set or a large permission graph can be
larger.

**No streaming.** The SDK contract is eager: the whole result set is built in memory and returned in
one response. That is also why paging cannot be supported. For very large estates:

- set `gcpMaxAgents` as a heap guard — and remember it truncates, visibly;
- split the estate across several data sources, by project or by location;
- switch off the enrichments you do not govern on, which shrinks each entry as well as the scan.

---

## 9. Logs & troubleshooting

### 9.1 Logging model

| Level | Used for |
|---|---|
| `INFO` | scan start and end (`Listed N GCP agents (M source failures)`), configuration summary at startup |
| `WARN` | one source or one agent's enrichment failed — the scan continues |
| `ERROR` | scan-level failure (everything failed, so the search returns `UNAVAILABLE`), or a **truncated** result (pagination backstop, agent cap, scan deadline) |
| `DEBUG` | per-request URLs, retries, page tokens, enrichment fan-out |

Set both the IDDM server log level **and** the connector log level to `DEBUG` when diagnosing, or when
running acceptance tests.

### 9.2 Diagnostic attributes

Two connector-private attributes, on every entry:

| Attribute | Scope | Meaning |
|---|---|---|
| `_connectorGcpSourceAvailable` | **scan-level** (same value on every entry) | True when at least one agent-listing source succeeded. False means the result set is not trustworthy. |
| `_connectorSourcesUnavailable` | **per-entry** | Comma-separated names of the failures that affected *this* entry. **Empty means this entry is complete.** |

Source names you may see, and what each means:

| Name | Cause / fix |
|---|---|
| `gcp:ListProjects` | Auto-discovery failed — grant `resourcemanager.projects.list`, or set `gcpProjectIds`. **Scan-fatal.** |
| `gcp:ListLocations` | Location discovery failed for a project — grant `aiplatform.locations.list` or set `gcpLocations`. |
| `gcp:ListReasoningEngines` · `gcp:ListDialogflowCxAgents` · `gcp:GetDialogflowEsAgent` · `gcp:ListAgentBuilderEngines` · `gcp:ListCxAgentStudio` | That source failed for a (project, location). Usually the API is not enabled, or the list permission is missing. |
| `gcp:ListDialogflowCxTools` | The tool-listing permission is missing for that agent. |
| `gcp:GetDialogflowCxGenerativeSettings` | The Dialogflow get permission is missing. A not-found response is *not* a failure — it means the agent has no generative settings. |
| `gcp:GetReasoningEngineIamPolicy` · `gcp:GetDialogflowCxIamPolicy` | The matching policy-read permission is missing. |
| `gcp:GetProjectIamPolicy` | The project policy-read permission is missing (project IAM fallback). |
| `gcp:AnalyzeIamPolicy` | The Cloud Asset permission is missing **or throttled** — its per-minute quota is low, so this appears intermittently even when granted. Re-scan, or set `enableOutboundPermissions=false`. |
| `gcp:ListAuditLogs` | The log-reading permission is missing. Data-access events additionally need the private-log-viewer grant. |
| `gcp:FetchAgentCard` | The configured card host did not serve a valid card. |
| `gcp:Enrichment(<platform>)` | **An unexpected connector-side error** while enriching that entry. The entry is emitted with partial data. Report this — it indicates a defect, and the log carries the detail. |
| `gcp:ListingTruncated` · `gcp:MaxAgentsReached` · `gcp:ScanDeadlineExceeded` | **The result set is incomplete.** Raise `gcpMaxAgents` or `gcpScanTimeoutMinutes`, or narrow the scope. |

### 9.3 What test connection checks

Test connection validates the configuration **as entered**, not a fixed endpoint:

- Wrong-valued properties collected at startup — a malformed `gcpAgentCardHostsJson`, a non-numeric
  `gcpOrganizationId` — **fail**, naming the property.
- No agent source enabled → **failure** (the scan could never return anything).
- **With `gcpProjectIds` set:** each enabled source is probed in the configured projects and locations,
  and at least one must answer. A nonexistent project or a bogus location therefore fails.
  `resourcemanager.projects.list` is **not** required.
- **Without `gcpProjectIds`:** project listing is required and is the fatal probe; the source probe in
  the first discovered project is advisory only — logged, not failed — because a large estate
  legitimately contains projects with no agent APIs enabled.
- An expired `gcpAccessToken` is reported as such, not as a bare HTTP 401.

A **valid but different** project id — one that exists and is readable but holds no agents —
correctly **succeeds**: the configuration works, and the scan legitimately finds nothing.

### 9.4 Troubleshooting playbook

| Symptom | Likely cause |
|---|---|
| Test connection fails naming a property | That property's value is wrong. The message says which and why. |
| Test connection succeeds, browse returns nothing | Scope is right but empty, or every source is disabled. Check the `INFO` line `Listed 0 GCP agents`. |
| Fewer entries than expected | `enableCesAgentStudio` is off, so CES agents are invisible; or a source failed (check the diagnostics); or the agent cap or scan deadline truncated the scan. |
| `UNAVAILABLE` (52) | Credentials, or the project-listing permission. With `gcpProjectIds` set, check each project id. |
| Attributes missing on some entries | Expected. See the *May be absent* column in [§10.5](#105-schema-reference--vdagentidentity). |
| `model` absent on Reasoning Engines | Expected — Google Cloud does not expose it. |
| `gcp:AnalyzeIamPolicy` appears intermittently | Cloud Asset throttling. Expected; re-scan or disable the outbound walk. |
| Scan takes far too long | Narrow `gcpProjectIds` and `gcpLocations`, raise `enrichmentThreads`, disable audit history. |
| Schema does not match this document after an upgrade | The data source must be **deleted and re-created** ([§3.8](#38-upgrading--the-schema-recreate-rule)). |
| Credential change appears to have no effect | Configuration is read at load time; reload the connector or restart IDDM. |

---

## 10. Appendices

### 10.1 Glossary

| Term | Meaning |
|---|---|
| **Agent** | A non-human identity that reasons with a model and acts through tools. |
| **Reasoning Engine** | A custom-coded agent deployed to Vertex AI Agent Engine. |
| **Dialogflow CX / ES** | Google Cloud's conversational agent services; ES is the legacy generation. |
| **Agent Builder** | Discovery Engine search and chat engines built over data stores. |
| **CX Agent Studio (CES)** | Customer Engagement Suite agents; a separate product, opt-in here. |
| **Runtime identity** | The service account an agent runs as — the privilege it actually wields. |
| **Inbound permission** | Who may invoke or administer the agent. |
| **Outbound permission** | What the agent's runtime identity can reach. |
| **Agent card** | The agent-to-agent (A2A) descriptor served at `/.well-known/agent.json`. |
| **Identity reference** | The shape every `*By` attribute uses: principal id, principal type, display name, metadata. |

### 10.2 Complete IAM reference (grant → option)

| Permission | Required by |
|---|---|
| `resourcemanager.projects.list` | project auto-discovery (**only** when `gcpProjectIds` is empty) |
| `resourcemanager.projects.get` | project-number resolution → Dialogflow runtime identity |
| `resourcemanager.projects.getIamPolicy` | `enableProjectIamFallback` |
| `aiplatform.locations.list` | location auto-discovery (only when `gcpLocations` is empty) |
| `aiplatform.reasoningEngines.list` / `.get` | `enableReasoningEngines` |
| `aiplatform.reasoningEngines.getIamPolicy` | `enablePermissionAnalysis` (Reasoning Engines) |
| `dialogflow.agents.list` / `.get` | `enableDialogflowCx`, `enableDialogflowEs`, `enableModelEnrichment` |
| `dialogflow.agents.getIamPolicy` | `enablePermissionAnalysis` (Dialogflow CX) |
| `dialogflow.tools.list` | `enableToolEnrichment` |
| `discoveryengine.engines.list` | `enableAgentBuilder` |
| `discoveryengine.dataStores.get` | knowledge-base `resources` (Agent Builder and CX data stores) |
| `cloudasset.assets.analyzeIamPolicy` | `enableOutboundPermissions` |
| `logging.logEntries.list` (plus `logging.privateLogViewer` for data-access events) | `enableAuditHistory` |
| `ces.apps.list`, `ces.agents.list` | `enableCesAgentStudio` |

### 10.3 Known limitations & deferred items

Everything an administrator or integrator should know before drawing conclusions from the data.

**Attributes Google Cloud does not expose (permanently absent):**

- **`status` / `statusReason`** — absent on **every** entry. No agent service exposes a lifecycle state
  field, so there is nothing to map. The same goes for `statusChangedAt` / `statusChangedBy`.
- **`lastInvokedAt` / `lastInvokedBy`** — never populated. Invocation events are deliberately excluded
  from the audit query to bound its volume and cost.
- **`intent`** — authored by IDO, never provider-sourced.
- **`subagents`** — no subagent-relationship API is wired.
- **`publishedAt`/`By`, `suspendedAt`/`By`, `blockedAt`/`By`, `deletedAt`/`By`** — Google Cloud has no
  publish, suspend, block or soft-delete signal.
- **`model` and `runtimeIdentity` on Reasoning Engines** — the model lives inside the deployed code, and
  the runtime service account is not exposed by the API.
- **`version`** — only Dialogflow ES supplies one.

**Behavioural limitations:**

- **Created and last-modified for Dialogflow CX and ES require `enableAuditHistory`.** Their APIs omit
  those timestamps entirely; Cloud Audit Logs are the only source. Events older than `gcpHistoryDays`
  (maximum **400**, an API limit) cannot be recovered, so a long-lived agent may show no creation date
  at all.
- **Native agent cards need operator-supplied hosts.** Google Cloud list APIs expose no serving
  endpoint, so without `gcpAgentCardHostsJson` every agent's card is reported as synthesized
  (Dialogflow, Agent Builder) or absent (Reasoning Engines) in the entry's metadata.
- **The Cloud Asset policy analysis has a low per-minute quota.** On a fleet of any size the outbound
  walk is expected to be throttled intermittently, producing isolated diagnostics. This is graceful
  degradation, not a defect.
- **`gcpOrganizationId` matches direct children only** — a project nested in a folder is skipped. The
  folder-hierarchy walk is not implemented.
- **Agent Builder is scanned at `global` only.** The other Discovery Engine multi-regions are deferred.
- **CX Agent Studio is opt-in and REST-only.** The streaming transport is deferred, and only the `us`
  multi-region has been exercised.
- **A partial scan still returns `SUCCESS`.** Failure isolation is deliberate — check the diagnostics
  ([§9.2](#92-diagnostic-attributes)) before treating a count as authoritative.
- **Embedded documents are fingerprinted, not emitted.** An OpenAPI specification carried as one
  string, and any string value over 4096 characters, is replaced by a SHA-256 fingerprint and a length,
  because a credential embedded inside such a document cannot be redacted by key name.
- **No paging, no writes, no streaming** ([§1.7](#17-connector-type-target-application--supported-operations)).
- **Concurrent searches each run their own scan.** There is no shared in-flight result.
- **Configuration is read once, at load time.** Reload the connector or restart IDDM after editing a
  data source.

### 10.4 Worked example — an extracted agent

Below is a Dialogflow CX agent as the connector emits it, with the default-on enrichment plus
`enableAuditHistory`. It exercises most of the enrichment paths: model, guardrails, a tool, a knowledge
data store, the runtime identity, the inbound and outbound permission graph, and audit-sourced history.

> **Provenance & fidelity.** This example is **composed for documentation** from the field shapes the
> connector emits — it is not a capture from any customer environment, and all project ids, agent
> names, service accounts, user handles and resource names are placeholders. Two arrays are shown with
> a representative subset plus an explicit marker. Attribute values shown as `null` here are **absent**
> from the real LDAP entry; they are listed so the shape is complete.

**Identity, lifecycle, model, guardrails, features:**

```json
{
  "agentId": "1f2e3d4c-5b6a-7890-abcd-ef1234567890",
  "externalId": "projects/example-agents-prod/locations/us-central1/agents/1f2e3d4c-5b6a-7890-abcd-ef1234567890",
  "name": "support-triage-agent",
  "description": "Triages inbound support conversations and routes them to the right queue.",
  "platform": "DIALOGFLOW_CX",
  "kind": "dialogflow_cx",
  "repositoryId": "example-agents-prod",
  "repositoryDisplayName": "GCP example-agents-prod",
  "tags": { "env": "prod", "owner": "support-platform" },
  "actionQuarantined": false,
  "status": null,
  "statusReason": null,
  "createdAt": "2026-03-11T09:14:02.000000000Z",
  "createdBy": {
    "principalId": "platform-eng-1@example.com",
    "principalType": "USER",
    "displayName": "platform-eng-1@example.com",
    "metadata": { "provider": "GCP", "method": "google.cloud.dialogflow.cx.v3.Agents.CreateAgent" }
  },
  "lastUpdatedAt": "2026-07-22T16:48:37.000000000Z",
  "lastUpdatedBy": {
    "principalId": "platform-eng-2@example.com",
    "principalType": "USER",
    "displayName": "platform-eng-2@example.com",
    "metadata": { "provider": "GCP", "method": "google.cloud.dialogflow.cx.v3.Agents.UpdateAgent" }
  },
  "model": [
    {
      "provider": "GOOGLE_VERTEX",
      "modelId": "gemini-2.5-flash",
      "version": null,
      "metadata": {
        "provider": "GCP",
        "source": "dialogflow.cx.generativeSettings",
        "languageCode": "en",
        "instructionHash": "9f2b7c1e4a6d8b0f3c5e7a9d1b3f5c7e9a1d3b5f7c9e1a3d5b7f9c1e3a5d7b9f",
        "instructionLength": 1462
      }
    }
  ],
  "guardrails": [
    {
      "id": "generative-safety-settings",
      "name": "Generative Safety Settings",
      "description": "Dialogflow CX generative safety configuration (banned phrases)",
      "status": "enabled",
      "enforcementMode": "block",
      "metadata": {
        "provider": "GCP",
        "guardrailType": "BANNED_PHRASES",
        "bannedPhraseCount": 12,
        "languageCodes": ["en"]
      }
    }
  ],
  "features": [
    {
      "featureName": "actionsEnabled",
      "featureDescription": "The agent can invoke tools.",
      "metadata": { "provider": "GCP", "value": true, "derivedFrom": "tools", "count": 2 }
    },
    {
      "featureName": "knowledgeEnabled",
      "featureDescription": "The agent is wired to a knowledge resource.",
      "metadata": { "provider": "GCP", "value": true, "derivedFrom": "resources", "count": 1 }
    },
    {
      "featureName": "guardrailsEnabled",
      "featureDescription": "A safety policy constrains the agent.",
      "metadata": { "provider": "GCP", "value": true, "derivedFrom": "guardrails", "count": 1 }
    }
  ]
}
```

**Tools, resources, runtime identity:**

```json
{
  "tools": [
    {
      "toolExternalId": "ticket-lookup",
      "toolName": "ticket-lookup",
      "toolDescription": "Look up a support ticket by id.",
      "toolType": "OPEN_API"
    },
    {
      "toolExternalId": "kb-search",
      "toolName": "kb-search",
      "toolDescription": "Search the support knowledge base.",
      "toolType": "DATA_STORE"
    }
  ],
  "resources": [
    {
      "resourceId": "support-kb",
      "resourceExternalId": "projects/example-agents-prod/locations/global/collections/default_collection/dataStores/support-kb",
      "resourceType": "KNOWLEDGE_BASE",
      "displayName": "support-kb",
      "location": "global",
      "ownerAccount": "example-agents-prod",
      "accessLevel": "RETRIEVE",
      "grantedThrough": "RUNTIME_IDENTITY",
      "principalId": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
      "principalType": "SERVICE_ACCOUNT",
      "policyStatements": [
        {
          "roles": ["roles/discoveryengine.viewer"],
          "resource": "//discoveryengine.googleapis.com/projects/example-agents-prod/locations/global/collections/default_collection/dataStores/support-kb"
        }
      ],
      "metadata": { "provider": "GCP", "linkedVia": "dataStoreTool" }
    },
    {
      "resourceId": "support-api-key",
      "resourceExternalId": "projects/example-shared-secrets/secrets/support-api-key",
      "resourceType": "SECRET",
      "displayName": "support-api-key",
      "ownerAccount": "example-shared-secrets",
      "accessLevel": "READ",
      "grantedThrough": "RUNTIME_IDENTITY",
      "principalId": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
      "principalType": "SERVICE_ACCOUNT",
      "policyStatements": [
        {
          "roles": ["roles/secretmanager.secretAccessor"],
          "resource": "//secretmanager.googleapis.com/projects/example-shared-secrets/secrets/support-api-key"
        }
      ]
    }
  ],
  "runtimeIdentity": {
    "principalId": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
    "principalType": "SERVICE_ACCOUNT",
    "displayName": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
    "allowedAssumers": [
      "user:platform-eng-1@example.com",
      "group:support-platform@example.com"
    ],
    "metadata": {
      "provider": "GCP",
      "runtimeIdentitySource": "dialogflow-service-agent",
      "resourceLevelBindings": [
        { "role": "roles/dialogflow.admin", "members": ["group:support-platform@example.com"] }
      ]
    }
  }
}
```

**Permission graph (subset):**

```json
{
  "permissionFlow": {
    "version": 1,
    "agent_id": "node:agent",
    "nodes": [
      {
        "id": "node:agent",
        "external_id": "projects/example-agents-prod/locations/us-central1/agents/1f2e3d4c-5b6a-7890-abcd-ef1234567890",
        "name": "support-triage-agent",
        "description": "DIALOGFLOW_CX agent",
        "type": "agent",
        "data": { "platform": "DIALOGFLOW_CX", "project": "example-agents-prod", "location": "us-central1" }
      },
      {
        "id": "node:service_account:service_000000000000_gcp_sa_dialogflow_iam_gserviceaccount_com",
        "external_id": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
        "name": "service-000000000000@gcp-sa-dialogflow.iam.gserviceaccount.com",
        "description": "Runtime service account the agent runs as",
        "type": "service_account",
        "data": {}
      },
      {
        "id": "node:role:roles_dialogflow_admin",
        "external_id": "roles/dialogflow.admin",
        "name": "dialogflow.admin",
        "description": "IAM role bound on the agent",
        "type": "iam_role",
        "data": { "role": "roles/dialogflow.admin" }
      }
    ],
    "edges": [
      { "from": "node:agent", "to": "node:service_account:service_000000000000_gcp_sa_dialogflow_iam_gserviceaccount_com", "relation": "runs_as", "description": "Agent runtime identity", "data": {} },
      { "from": "node:agent", "to": "node:role:roles_dialogflow_admin", "relation": "iam_binding", "description": "Principals granted this role on the agent", "data": {} },
      { "from": "node:agent", "to": "node:resource:projects_example_agents_prod_locations_global_collections_default_collection_datastores_support_kb", "relation": "uses_knowledge", "description": "Agent's configured knowledge resource", "data": {} }
    ]
  },
  "_connectorGcpSourceAvailable": true,
  "_connectorSourcesUnavailable": ""
}
```

*(the real graph for this agent carries several more member and resource nodes, all of the same shape)*

### 10.5 Schema reference — `vdAgentIdentity`

The connector exposes **one** object type, `vdAgentIdentity`, with **46 attributes**. The schema is
auto-generated by IDDM when the data source is created
([§1.7](#17-connector-type-target-application--supported-operations)); **this table is the contract that
generated schema must match.**

**Column meanings**

- **Type** — the LDAP wire type. `JSON` attributes are single-valued strings containing JSON; the
  column notes the top-level JSON shape.
- **Write** — `RO` means read-only. **All 46 are `RO`**: the connector implements no write operation.
- **Create** — required or optional when creating a new entry. **Add is not supported**, so no attribute
  is ever required or optional for create and the column reads `n/a` throughout.
- **Platform** — which sources populate it: `All`, or a subset — `RE` (Reasoning Engines), `CX`
  (Dialogflow CX), `ES` (Dialogflow ES), `AB` (Agent Builder), `CES` (CX Agent Studio) — or `None` when
  Google Cloud exposes no source.
- **May be absent** — whether the attribute can be missing from a returned entry. **This is the normal
  case, not an error.** Only the rows marked **Never** are guaranteed present on every entry.

#### Identity & naming

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `agentId` | STRING | RO | n/a | All | **Never** | **RDN / naming attribute.** Last segment of the resource name. |
| `externalId` | STRING | RO | n/a | All | Rarely | Full resource name — the cross-provider key. |
| `name` | STRING | RO | n/a | All | **Never** | Display name, falling back to the id. |
| `description` | STRING | RO | n/a | All | Often | Not all agents have one. |
| `intent` | STRING | RO | n/a | None | **Always** | IDO-authored; no Google Cloud source. |
| `instruction` | STRING | RO | n/a | CX | **Usually** | Only when `enableInstructionText` is on **and** the agent has a generative prompt. |
| `version` | STRING | RO | n/a | ES | **Usually** | Only Dialogflow ES exposes a version. |
| `platform` | STRING | RO | n/a | All | **Never** | One of the five values in [§1.3](#13-what-it-produces-at-a-glance). |
| `kind` | STRING | RO | n/a | All | **Never** | Sub-type: `reasoning_engine`, `dialogflow_cx`, `dialogflow_es`, `agent_builder`, `cx_agent_studio`. |
| `tags` | JSON object | RO | n/a | All | Often | Google Cloud labels, where present. |

#### Lifecycle & governance

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `status` | STRING | RO | n/a | None | **Always** | No lifecycle-state field exists. |
| `statusReason` | STRING | RO | n/a | None | **Always** | Same. |
| `actionQuarantined` | BOOLEAN | RO | n/a | All | **Never** | Always `false`: a write-back control IDO sets; this connector never writes. |
| `statusChangedAt` | STRING (ISO) | RO | n/a | None | **Always** | No status-transition signal. |
| `statusChangedBy` | JSON object | RO | n/a | None | **Always** | Identity reference. |
| `createdAt` | STRING (ISO) | RO | n/a | RE, AB, CES; CX/ES via audit | Sometimes | CX and ES require `enableAuditHistory`, within `gcpHistoryDays`. |
| `createdBy` | JSON object | RO | n/a | Audit only | Usually | Identity reference from the create audit event. |
| `publishedAt` / `publishedBy` | STRING / JSON | RO | n/a | None | **Always** | No publish signal. |
| `suspendedAt` / `suspendedBy` | STRING / JSON | RO | n/a | None | **Always** | No suspend signal. |
| `blockedAt` / `blockedBy` | STRING / JSON | RO | n/a | None | **Always** | No block signal. |
| `deletedAt` / `deletedBy` | STRING / JSON | RO | n/a | None | **Always** | No soft-delete signal. |
| `lastUpdatedAt` | STRING (ISO) | RO | n/a | RE, AB, CES; CX/ES via audit | Sometimes | Same caveat as `createdAt`. |
| `lastUpdatedBy` | JSON object | RO | n/a | Audit only | Usually | Identity reference. |
| `lastInvokedAt` / `lastInvokedBy` | STRING / JSON | RO | n/a | None | **Always** | Invocation events excluded to bound audit volume. |
| `metadata` | JSON object | RO | n/a | All | **Never** | Platform extras: project, location, resource name, entity tag, labels, a secret-redacted native copy, the agent-card provenance, the prompt fingerprint. |

#### Repository linkage

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `repositoryId` | STRING | RO | n/a | All | **Never** | The Google Cloud project id. |
| `repositoryDisplayName` | STRING | RO | n/a | All | **Never** | `GCP <project>`. |

#### Model, guardrails & features

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `model` | JSON array | RO | n/a | CX | **Usually** | Needs `enableModelEnrichment`; only CX agents with generative settings have one. |
| `guardrails` | JSON array | RO | n/a | CX | **Usually** | Same call; only where a safety policy is configured. |
| `features` | JSON array | RO | n/a | All | Sometimes | Derived from `tools`, `resources` and `guardrails`; absent when an agent has no positive signal. |

#### Agent card / A2A

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `agentCardUrl` | STRING | RO | n/a | All | **Usually** | Native cards only — needs `enableAgentCard` **and** `gcpAgentCardHostsJson`. |
| `url` | JSON object | RO | n/a | All | **Usually** | Same. |
| `provider` | JSON object | RO | n/a | All | **Never** | Defaults to Google Cloud regardless of `enableAgentCard`. |
| `skills` | JSON array | RO | n/a | All | **Usually** | Native cards only. |

#### Tools, resources & permissions

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `tools` | JSON array | RO | n/a | RE, CX | Sometimes | Reasoning Engine tools are derived inline; CX tools need `enableToolEnrichment`. |
| `resources` | JSON array | RO | n/a | AB, CX, plus outbound | Often | Knowledge data stores, plus anything the runtime identity can reach when `enableOutboundPermissions` is on. |
| `subagents` | JSON array | RO | n/a | None | **Always** | Not wired. |
| `permissionFlow` | JSON object | RO | n/a | RE, CX, ES | Sometimes | Nodes and edges — see [§6.3](#63-the-permission-graph). Needs `enablePermissionAnalysis`. |
| `runtimeIdentity` | JSON object | RO | n/a | CX, ES | Often | The execution identity and who may assume it. A Reasoning Engine's is not API-exposed. |

#### Connector diagnostics

| Attribute | Type | Write | Create | Platform | May be absent | Notes |
|---|---|---|---|---|---|---|
| `_connectorGcpSourceAvailable` | BOOLEAN | RO | n/a | All | **Never** | Scan-level health — see [§9.2](#92-diagnostic-attributes). |
| `_connectorSourcesUnavailable` | STRING | RO | n/a | All | **Never** | Per-entry failures; an **empty string** when the entry is complete. |

### 10.6 Acceptance-test configuration

Configure the data source as below before running functional acceptance tests, or several will fail for
configuration reasons rather than defects.

| Setting | Value | Why |
|---|---|---|
| `gcpProjectIds` | the specific project(s) whose agents you counted | Makes the expected entry count knowable and deterministic. |
| `gcpLocations` | the specific location(s) those agents live in | Same. |
| `gcpMaxAgents` | **`0`** (the default) | Any cap would truncate the entry count. |
| `gcpScanTimeoutMinutes` | large enough for the scan to finish (`0` = no deadline) | A deadline hit mid-scan truncates the count. |
| Agent source options | **match what exists in the backend** — in particular set `enableCesAgentStudio=true` if the project has CES agents, or exclude them from your count | The expected count is the union of the **enabled** sources. |
| All other enrichment options | leave at their defaults, plus `enableAuditHistory` if you want history attributes populated | So every attribute that *can* be populated is. |
| IDDM server + connector log level | `DEBUG` | How entry-count and behavior claims are verified. |

**Notes for the test engineer**

- **Object types:** there is exactly **one** (`vdAgentIdentity`), so every "repeat for each object
  type" instruction is a single pass.
- **Before accepting any entry count**, confirm `_connectorGcpSourceAvailable` is true on the entries
  **and** `_connectorSourcesUnavailable` is empty on every one. A partial scan deliberately returns
  `SUCCESS`, so a silent permission gap would otherwise look like a wrong count. Also confirm that none
  of `gcp:ListingTruncated`, `gcp:MaxAgentsReached` or `gcp:ScanDeadlineExceeded` appears.
- **"All attributes populated"**: no entry ever carries all 46 attributes, by design. Use the **May be
  absent** and **Platform** columns in [§10.5](#105-schema-reference--vdagentidentity) as the
  expectation — an absent CX-only attribute on a Reasoning Engine is correct behavior, not a defect.
- **Base search**: use `agentId=<value>,<container-dn>`. The connector resolves the single target
  itself, so exactly one entry comes back. An unknown target returns zero entries with `SUCCESS`, not
  `UNAVAILABLE`.
- **Read-only attribute test**: every attribute is read-only and modify is not supported, so there is
  no writable representative attribute and no equivalence class to sample.
- **"Incorrect property value"** means **malformed or unauthorized**. Test connection detects a
  nonexistent project id, a bogus location, a non-numeric organization id, a malformed agent-card host
  map, bad credentials, and an expired access token. A *valid but different* project id — one that
  exists and is readable but holds no agents — correctly **succeeds**.
- **Add, delete and paging** are not supported, so mark those items not applicable.

---

© Radiant Logic. Google Cloud, Vertex AI, and Dialogflow are trademarks of Google LLC.
