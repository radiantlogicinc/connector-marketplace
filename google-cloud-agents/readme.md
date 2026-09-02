# Google Cloud Agents connector

This document describes the Radiant Logic custom connector for Google Cloud agents, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector uses the Radiant Logic Connector SDK and integrates with Google Cloud through its control-plane REST APIs. It virtualizes the AI agents in the configured Google Cloud projects across five services—Vertex AI Agent Engine (Reasoning Engines), Dialogflow CX, Dialogflow ES, Vertex AI Agent Builder, and CX Agent Studio—as a single object type. Each entry carries the agent's model, guardrails, tools, reachable resources, runtime identity, _Identity and Access Management (IAM)_ permission graph, and ownership history. The connector is strictly read-only.

[Download the Google Cloud Agents connector JAR file](builds/google-cloud-agents-connector-0.9.2.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Google Cloud Agents</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>0.9.2 (beta)</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector SDK version</th><td>1.2.0</td>
  </tr>
</table>

### Identity Data Management support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>8.2.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic</td>
  </tr>
</table>

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>3.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td><a href="user-guide.md">User guide</a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td><a href="../shared-resources/agent-canonical-model-mapping-v1.yaml">Agent model v1 YAML</a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Consuming configuration</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>Yes</td>
  </tr>
</table>

## Configuration walkthrough

To configure Google Cloud and RadiantOne, complete the steps in the following sections.

### Preconditions

- One or more Google Cloud projects containing AI agents, reachable from RadiantOne over HTTPS.
- Permission in Google Cloud to enable APIs and create a service account, or a colleague who has it.
- A service-account key JSON file, or a raw OAuth 2.0 access token scoped to `https://www.googleapis.com/auth/cloud-platform`. This release doesn't support Application Default Credentials or the compute metadata server.

### Configure Google Cloud

1. Enable the required APIs on every project that you intend to scan: `cloudresourcemanager.googleapis.com`, `aiplatform.googleapis.com`, `dialogflow.googleapis.com`, and `discoveryengine.googleapis.com`. Add `cloudasset.googleapis.com`, `logging.googleapis.com`, and `ces.googleapis.com` only if you turn on the matching connector options. If an API isn't turned on, the connector isolates the failure to the source that needs it: the scan continues, but that source contributes nothing.
2. Create a dedicated least-privilege service account for the connector and bind it to a custom read-only role. Every permission is tied to the option that needs it, so you can grant only what you turn on. For more information, see [Complete IAM reference (grant → option)](user-guide.md#102-complete-iam-reference-grant--option).
3. Create a key for that service account, or plan to supply a short-lived access token instead. For more information about the tradeoffs between the two modes, see [Authentication modes](user-guide.md#34-authentication-modes).
4. Optional: Grant `resourcemanager.projects.list` only if you want the connector to auto-discover projects. Radiant Logic recommends that you list your projects explicitly in `gcpProjectIds` for production, which makes that permission unnecessary.

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the Google Cloud Agents connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **Google Cloud Agents** template. The system automatically generates a new schema.
3. Fill in the data source properties. At a minimum, supply one credential; Radiant Logic also recommends that you set `gcpProjectIds` and `gcpLocations` explicitly. For more information, see the [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches Google Cloud. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source. You'll see one entry per agent.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection**, **Agent sources**, **Enrichment**, and **Performance** sections. Every property is optional except that you must supply either `gcpServiceAccountKey` or `gcpAccessToken`. An empty value always means the connector uses the default. For more information, see [Configuration reference — all 26 properties](user-guide.md#4-configuration-reference--all-26-properties).

### Connection

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `gcpServiceAccountKey` | One of the two credentials | `password` | None | Service-account key JSON | The full contents of the downloaded key file. The connector acquires and refreshes OAuth 2.0 tokens itself. The connector rejects the key at data source creation if it isn't parseable JSON that carries a `client_email` field. This property takes precedence if you set both credentials. |
| `gcpAccessToken` | One of the two credentials | `password` | None | OAuth 2.0 bearer token | A raw access token scoped to `https://www.googleapis.com/auth/cloud-platform`. The connector uses the token verbatim and never refreshes it, so it expires within about an hour. For keyless setups and short-lived testing. |
| `gcpProjectIds` | No | `string` | None | Comma-separated project IDs | Projects to scan, for example, `agents-production,agents-staging`. Empty means auto-discover every `ACTIVE` project that the credential can see, which requires `resourcemanager.projects.list`. Radiant Logic recommends that you set this property explicitly in production. |
| `gcpLocations` | No | `string` | None | Comma-separated locations | Locations to scan per project, for example, `us-central1,europe-west1,global`. Empty means auto-discover the project's enabled Vertex AI locations, always adding `global`. |
| `gcpOrganizationId` | No | `string` | None | Numeric organization ID | Narrows auto-discovery to projects parented by that organization. Ignored when `gcpProjectIds` is set. Matches direct children only. If the value isn't numeric, **Test Connection** fails. |

### Agent sources

Turning off a source removes its API calls and its permission requirement entirely.

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableReasoningEngines` | No | `boolean` | `true` | `true`, `false` | Scan Vertex AI Agent Engine (Reasoning Engines). |
| `enableDialogflowCx` | No | `boolean` | `true` | `true`, `false` | Scan Dialogflow CX agents across the project's locations. |
| `enableDialogflowEs` | No | `boolean` | `true` | `true`, `false` | Scan the project's single Dialogflow ES agent. An HTTP `404 Not Found` status code means none exists. |
| `enableAgentBuilder` | No | `boolean` | `true` | `true`, `false` | Scan Vertex AI Agent Builder (Discovery Engine) engines at `global`. |
| `enableCesAgentStudio` | No | `boolean` | `false` | `true`, `false` | Scan CX Agent Studio (Customer Engagement Suite, or CES) agents. Opt-in: CES is a separate product needing its own API and permissions, and turning it on where CES is unused produces isolated, non-fatal source failures. |

### Enrichment

Turning off an option leaves the attributes that it fills absent, which is a documented state, not a defect.

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableToolEnrichment` | No | `boolean` | `true` | `true`, `false` | One tool-listing call per Dialogflow CX agent. Fills `tools` and CX knowledge `resources`. The connector derives Reasoning Engine tools inline, so this option doesn't affect them. |
| `enableModelEnrichment` | No | `boolean` | `true` | `true`, `false` | One generative-settings call per Dialogflow CX agent, filling both `model` and `guardrails`. CX only: Reasoning Engines expose no model. |
| `enableFeatureDerivation` | No | `boolean` | `true` | `true`, `false` | Derives the positive-signal `features` flags from data already collected. This derivation makes no API call. |
| `enableInstructionText` | No | `boolean` | `false` | `true`, `false` | Emit the raw generative prompt in `instruction`. Turned off by default because prompts often carry sensitive content; the prompt's SHA-256 fingerprint and length are available either way. |
| `enableAuditHistory` | No | `boolean` | `false` | `true`, `false` | One Cloud Audit Logs query per agent, filling `createdAt`, `createdBy`, `lastUpdatedAt`, and `lastUpdatedBy`. This option is the only source of creation and last-modification timestamps for Dialogflow CX and ES. |
| `gcpHistoryDays` | No | `number` | `30` | `1-400` (inclusive) | Audit lookback window in days. The API maximum is 400. Larger windows recover older events at more cost. |
| `enableAgentCard` | No | `boolean` | `false` | `true`, `false` | Fetch the agent-to-agent card from a configured host to fill `agentCardUrl`, `url`, `provider`, and `skills`. The connector requests the card without authentication first, and sends the Google Cloud token only if the host responds with an HTTP `401` or `403` status code. |
| `gcpAgentCardHostsJson` | No | `string` | None | JSON object | Maps an agent's `externalId` to the base host serving its card. Required for fetched cards, because Google Cloud list APIs expose no serving endpoint. If the value is malformed, **Test Connection** fails. |
| `enablePermissionAnalysis` | No | `boolean` | `true` | `true`, `false` | One IAM-policy call per Reasoning Engine and Dialogflow CX agent, filling `runtimeIdentity` and the inbound `permissionFlow`: the identity that the agent runs as, and who can invoke it. |
| `enableOutboundPermissions` | No | `boolean` | `true` | `true`, `false` | Resolve what each unique runtime identity can reach, filling the IAM fields of `resources` and the outbound permission-graph edges. Requires `enablePermissionAnalysis`. Depends on an API with a low quota; see the known limitations. |
| `enableProjectIamFallback` | No | `boolean` | `true` | `true`, `false` | When a Dialogflow agent's resource-level policy is empty (Dialogflow IAM is project-scoped), fall back to the project policy and report the bindings that actually grant agent access. Requires `enablePermissionAnalysis`. |

### Performance

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enrichmentThreads` | No | `number` | `10` | `1-50` (inclusive) | Worker threads for per-agent enrichment. `1` is fully sequential. Set it to `1` when a Google Cloud per-minute quota, rather than connector throughput, limits the scan. The connector de-duplicates shared lookups, so raising this value doesn't multiply them. |
| `gcpMaxRetries` | No | `number` | `3` | `0-10` (inclusive) | Retries per API call on an HTTP `429` or `5xx` status code, with bounded exponential backoff. The first attempt isn't a retry. |
| `gcpMaxAgents` | No | `number` | `0` | `0-1000000` (inclusive) | Hard ceiling on agents emitted per scan, as a heap guard for deployments with a large number of agents. When the cap is reached, the scan stops and records a diagnostic that marks the result set as truncated. `0` means no cap. Leave this property at `0` whenever an exact entry count matters. |
| `gcpCallTimeoutSeconds` | No | `number` | `120` | `10-600` (inclusive) | Ceiling on a single API call, including retries. Distinct from the connect and read timeouts, which bound individual socket phases only. |
| `gcpScanTimeoutMinutes` | No | `number` | `30` | `0-1440` (inclusive) | Ceiling on a whole scan. On expiry the connector returns the partial result and records a diagnostic. Raise it for deployments with a large number of agents. `0` means no deadline. |

## Supported operations

The following table summarizes the operations that the connector supports for its object type. Each cell shows one of two support levels:

- **Yes**: fully supported
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `vdAgentIdentity` | Yes | No | No | No |

For more information about attribute-level detail, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `vdAgentIdentity` | Search | Yes | Subtree search returns every agent from every enabled source, subject to `gcpMaxAgents`. Base search on <code>agentId=<var>AGENT_ID</var></code>, where <var>AGENT_ID</var> is the agent's `agentId` value, returns exactly that entry: the connector resolves the single target itself and stops scanning after it finds the entry. The connector doesn't support paging: it returns all results in one response. Bound the result size either by narrowing the search scope or by setting `gcpMaxAgents`. |
| `vdAgentIdentity` | Create, Modify, Delete | No | The connector is strictly read-only. It implements no write operation, requests only list- and get-shaped permissions, and never writes to Google Cloud. All 46 attributes are read-only. |

## Schema overview

The schema defines one object type: `vdAgentIdentity`. The primary key and _relative distinguished name (RDN)_ is `agentId`. This value is the last segment of the agent's Google Cloud resource name. The full resource name is available in the `externalId` attribute. It's globally unique and serves as the cross-provider correlation key.

The connector exposes all five Google Cloud agent services as this single object type and uses the `platform` attribute to tell them apart. The values are `VERTEX_REASONING_ENGINE`, `DIALOGFLOW_CX`, `DIALOGFLOW_ES`, `VERTEX_AGENT_BUILDER`, and `CX_AGENT_STUDIO`. The schema declares a sixth value, `VERTEX_ADK_CLOUD_RUN`, for agents self-hosted on Cloud Run; the connector doesn't discover those agents.

Many attributes might be absent from a given entry: they don't apply to that agent's platform, Google Cloud doesn't expose them, or the enrichment they need is off. The identity data management service drops these null-valued attributes from its response. No entry ever carries all 46 attributes. This is normal, not an error.

The connector supports automatic schema authoring: the system generates the schema when you create the data source. For more information about each attribute's data type and availability, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **Read-only.** There is no write path of any kind: no create, no modify, no delete, no tagging, and no remediation. `actionQuarantined` exists in the data model as a write-back control, and the connector always reports it as `false`. |
| 2 | **No lifecycle status.** No Google Cloud agent service exposes a lifecycle state field, so `status` and `statusReason` are always absent, as are `statusChangedAt` and `statusChangedBy`. |
| 3 | **Several lifecycle attributes have no Google Cloud source.** `publishedAt`, `publishedBy`, `suspendedAt`, `suspendedBy`, `blockedAt`, `blockedBy`, `deletedAt`, and `deletedBy` are part of the data model but are always absent. So are `subagents`: the connector doesn't call a discovery API for them. |
| 4 | **The connector doesn't populate invocation recency.** `lastInvokedAt` and `lastInvokedBy` are always absent: the connector deliberately excludes invocation events from the audit query, to bound its volume and cost. |
| 5 | **Google Cloud doesn't expose a Reasoning Engine's model or runtime identity.** The model lives inside the deployed agent's code, and the API doesn't return the runtime service account, so `model` and `runtimeIdentity` are absent for that platform. |
| 6 | **Created and last-modified for Dialogflow CX and ES require audit history.** Their APIs omit those timestamps entirely; Cloud Audit Logs are the only source, and the connector can't recover events older than `gcpHistoryDays` (400 days maximum, an API limit). A long-lived agent might therefore show no creation date at all. |
| 7 | **Fetched agent cards need operator-supplied hosts.** Google Cloud list APIs expose no serving endpoint, so without `gcpAgentCardHostsJson` the connector reports every agent's card as synthesized or absent rather than fetching it. |
| 8 | **The outbound permission walk depends on a low-quota API.** In a deployment of any size, expect intermittent throttling. This produces isolated per-entry diagnostics, but the rest of the entry stays intact, including the inbound permission graph. This is graceful degradation, not a defect. Re-scan or turn off `enableOutboundPermissions`. |
| 9 | **Organization-scoped auto-discovery matches direct children only.** The connector skips a project nested inside a folder; it doesn't implement the folder-hierarchy walk. Listing projects explicitly avoids the issue entirely. |
| 10 | **Limited multi-region and CES coverage.** The connector scans Agent Builder at `global` only, and CX Agent Studio is opt-in and REST-only. The connector doesn't scan the other Discovery Engine multi-regions and doesn't use the CES streaming transport. |
| 11 | **A partial scan still returns success.** Failure isolation is deliberate, so a permission gap or a throttled API produces a complete-looking result set with per-entry diagnostics. Check `_connectorGcpSourceAvailable` and `_connectorSourcesUnavailable` before treating an entry count as authoritative. |
| 12 | **No paging and no streaming.** The connector builds the whole result set in memory and returns it in one response. Bound the result size either by narrowing the search scope or by setting `gcpMaxAgents`. |
| 13 | **The connector reads its configuration once, at load time.** Editing a data source's credentials or options takes effect only after you reload the connector or restart the identity data management service. A raw access token expires within about an hour and the connector never refreshes it, so prefer a service-account key for production. |
| 14 | **Concurrent searches each run their own scan.** Two simultaneous Identity Data Platform searches double the API volume; there's no shared in-flight result. |
| 15 | **The connector fingerprints embedded documents rather than emitting them.** It replaces the following with a SHA-256 fingerprint and a length: an OpenAPI specification carried as a single string, and any string value over 4,096 characters. Fingerprinting is necessary because the connector can't redact a credential by key name when that credential is embedded inside such a document. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 0.9.2 | August 31, 2026 | First beta release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns. The connector doesn't support write operations, so the following table omits the 'Create', 'Modify', and 'Delete' columns and covers availability instead. For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

The 'Platform' column indicates which sources populate an attribute. Its value is _All_, _None_ (meaning Google Cloud doesn't expose a source), or a combination of the following:

- **RE**: Reasoning Engines
- **CX**: Dialogflow CX
- **ES**: Dialogflow ES
- **AB**: Agent Builder
- **CES**: CX Agent Studio

When a source depends on a condition rather than on the platform alone, the cell carries a qualifier.

Two columns describe whether a value is present, but they answer different questions. The 'Nullable' column indicates whether the attribute must have a value. Attributes marked with a checkmark (✔) are allowed to have a null value; those with a blank cell must carry a value on every entry. The 'Absent in practice' column describes how often the attribute is actually missing from a returned entry. The scale runs from _Never_ through _Rarely_, _Sometimes_, and _Usually_ to _Always_. Six attributes are marked nullable yet are never absent: the schema permits null defensively, but the connector populates them on every entry.

Some attributes represent a nested object or a list, but the connector returns them to Identity Data Platform as strings, so the schema declares them as `string`. To help you identify the original type, this document adds a qualifier in the 'Data type' column:

- `string` (JSON object)
- `string` (JSON array)

The connector emits attribute names in LDAP-valid `camelCase`; a downstream mapping converts them to the canonical `snake_case` model.

### Object type: `vdAgentIdentity`

| Attribute | Data type | Platform | Nullable | Absent in practice | Description |
| --- | --- | --- | --- | --- | --- |
| `agentId` | `string` | All | | Never | Primary key and RDN. Last segment of the Google Cloud resource name. |
| `externalId` | `string` | All | ✔ | Rarely | The full Google Cloud resource name—the cross-provider correlation key. |
| `name` | `string` | All | | Never | Display name, falling back to the ID. |
| `description` | `string` | All | ✔ | Sometimes | Absent when the agent has none. |
| `intent` | `string` | None | ✔ | Always | Reserved for Identity Data Platform to author; no Google Cloud source. |
| `instruction` | `string` | CX | ✔ | Usually | The raw generative prompt. Emitted only when `enableInstructionText` is on and the agent has a prompt. |
| `version` | `string` | ES | ✔ | Usually | Only Dialogflow ES exposes a version. |
| `platform` | `string` | All | | Never | Discriminator across the five agent services. |
| `kind` | `string` | All | ✔ | Never | Subtype: `reasoning_engine`, `dialogflow_cx`, `dialogflow_es`, `agent_builder`, `cx_agent_studio`. |
| `tags` | `string` (JSON object) | All | ✔ | Sometimes | Google Cloud labels, where present. |
| `status` | `string` | None | ✔ | Always | No Google Cloud lifecycle-state field exists. |
| `statusReason` | `string` | None | ✔ | Always | Same. |
| `actionQuarantined` | `boolean` | All | | Never | Always `false`: a write-back control that this read-only connector never sets. |
| `statusChangedAt` | `string` | None | ✔ | Always | No status-transition signal. |
| `statusChangedBy` | `string` (JSON object) | None | ✔ | Always | Identity reference; no source. |
| `runtimeIdentity` | `string` (JSON object) | CX, ES | ✔ | Sometimes | The service account that the agent runs as, and who can assume or invoke it. Requires `enablePermissionAnalysis`. Not exposed for Reasoning Engines. |
| `createdAt` | `string` | RE, AB, CES, plus CX and ES through audit history | ✔ | Sometimes | Creation time. For Dialogflow CX and ES it requires `enableAuditHistory` and an event inside the lookback window. |
| `createdBy` | `string` (JSON object) | Audit history only | ✔ | Usually | Identity that created the agent. Requires `enableAuditHistory`. |
| `publishedAt` | `string` | None | ✔ | Always | No publish signal. |
| `publishedBy` | `string` (JSON object) | None | ✔ | Always | No publish signal. |
| `suspendedAt` | `string` | None | ✔ | Always | No suspend signal. |
| `suspendedBy` | `string` (JSON object) | None | ✔ | Always | No suspend signal. |
| `blockedAt` | `string` | None | ✔ | Always | No block signal. |
| `blockedBy` | `string` (JSON object) | None | ✔ | Always | No block signal. |
| `deletedAt` | `string` | None | ✔ | Always | No soft-delete signal. |
| `deletedBy` | `string` (JSON object) | None | ✔ | Always | No soft-delete signal. |
| `lastUpdatedAt` | `string` | RE, AB, CES, plus CX and ES through audit history | ✔ | Sometimes | Last modification time. Same caveat as `createdAt`. |
| `lastUpdatedBy` | `string` (JSON object) | Audit history only | ✔ | Usually | Identity that last modified the agent. Requires `enableAuditHistory`. |
| `lastInvokedAt` | `string` | None | ✔ | Always | The connector excludes invocation events to bound audit volume. |
| `lastInvokedBy` | `string` (JSON object) | None | ✔ | Always | Same. |
| `metadata` | `string` (JSON object) | All | ✔ | Never | Platform extras: project, location, resource name, entity tag, labels, a secret-redacted copy of the raw Google Cloud object, the agent-card provenance, and the prompt fingerprint. |
| `repositoryId` | `string` | All | ✔ | Never | The owning Google Cloud project ID. |
| `repositoryDisplayName` | `string` | All | ✔ | Never | The owning project's display name, in the form <code>GCP <var>PROJECT_DISPLAY_NAME</var></code>. |
| `model` | `string` (JSON array) | CX | ✔ | Usually | The model that the agent reasons with. Requires `enableModelEnrichment`; only CX agents with generative settings have one. |
| `guardrails` | `string` (JSON array) | CX | ✔ | Usually | The configured safety policy and its enforcement mode. Same call as `model`. |
| `features` | `string` (JSON array) | All | ✔ | Sometimes | Positive-signal capability flags derived from `tools`, `resources`, and `guardrails`. The connector omits an empty set rather than emitting an empty array. |
| `agentCardUrl` | `string` | All | ✔ | Usually | URL of the agent-to-agent card. Fetched cards only: requires `enableAgentCard` and `gcpAgentCardHostsJson`. |
| `url` | `string` (JSON object) | All | ✔ | Usually | Endpoint URLs from a fetched card. Same requirement. |
| `provider` | `string` (JSON object) | All | ✔ | Never | Defaults to Google Cloud; a fetched card overrides it. |
| `skills` | `string` (JSON array) | All | ✔ | Usually | Declared skills from a fetched card. Same requirement. |
| `tools` | `string` (JSON array) | RE, CX | ✔ | Sometimes | Callable tools. The connector derives Reasoning Engine tools inline; CX tools require `enableToolEnrichment`. |
| `resources` | `string` (JSON array) | AB, CX, plus anything that the runtime identity reaches | ✔ | Sometimes | Knowledge data stores, plus every resource reachable through the runtime identity when `enableOutboundPermissions` is on. |
| `subagents` | `string` (JSON array) | None | ✔ | Always | The connector doesn't call a Google Cloud subagent-discovery API. |
| `permissionFlow` | `string` (JSON object) | RE, CX, ES | ✔ | Sometimes | Acyclic permission graph: who can invoke the agent, the identity that it runs as, and what that identity can reach. Requires `enablePermissionAnalysis`. |
| `_connectorGcpSourceAvailable` | `boolean` | All | | Never | Diagnostic, scan-level: whether at least one agent-listing source succeeded during this scan. `false` means the result set isn't trustworthy. |
| `_connectorSourcesUnavailable` | `string` | All | ✔ | Never | Diagnostic, entry-level: comma-separated names of the failures that affected this entry. Empty when the entry is complete. |
