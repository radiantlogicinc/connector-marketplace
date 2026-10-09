# Microsoft Azure AI Foundry connector

This document describes the Radiant Logic custom connector for Microsoft Azure AI Foundry agents, including
configuration, data source properties, supported operations, schema, known limitations, and release history.
The connector uses the Radiant Logic Connector SDK and integrates with Azure AI Foundry through the project
data plane, Azure Resource Manager, Microsoft Graph, Application Insights, and the Azure Activity Log. It
virtualizes every AI agent in the configured Azure subscription, and optionally every Microsoft Copilot
Studio agent in the tenant, as a single object type. Each entry carries the agent's model, content-safety
guardrails, tools, reachable resources, runtime managed identity, permission graph, project governance
history, and invocation recency. The connector provides search across every discovered agent and one opt-in
write: quarantine remediation.

[Download the Microsoft Azure AI Foundry connector JAR file](builds/microsoft-azure-ai-foundry-connector-0.9.3.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its
support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Microsoft Azure AI Foundry</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>0.9.3 (beta)</td>
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

To configure Azure and RadiantOne, complete the steps in the following sections.

### Preconditions

- An Azure subscription. RadiantOne must be able to reach Azure over HTTPS.
- A Microsoft Entra service principal (app registration) with a client secret, holding the connector's read
  role assignments. This release doesn't support managed identity or other passwordless credential modes.
- For Azure AI Foundry discovery, at least one Foundry account and project with agents in that subscription.
- Optional: For Copilot Studio discovery, the Power Platform Administrator role and a Dataverse application
  user in each environment that you want to scan.
- Optional: For fuller enrichment, the resource group of the Foundry account, plus management-plane read
  access to it. This adds content-safety guardrails, reachable resources, the permission graph, project
  tags, invocation recency, project governance, and multi-project scanning.

### Configure Azure

1. Register an application in Microsoft Entra ID for the connector and create a client secret for it. Record
   the tenant ID, the application (client) ID, and the secret value. Azure shows the secret value only once.
2. For Foundry discovery, assign **Azure AI User**, or any role that grants read access to agents, to the
   service principal on the Foundry project. That role is the minimum for discovering Foundry agents.
3. Recommended: For full read-only enrichment, assign **Reader** on the resource group that holds the Foundry
   account. One grant covers the management-plane enrichment, the role-assignment lookup, and the Activity Log.
4. Recommended: For directory enrichment of the identities that the agents run as, grant the Microsoft Graph
   application permission `Directory.Read.All` and have an administrator consent to it.
5. Optional: For invocation recency, assign **Monitoring Reader**, or equivalent query access, on the
   Application Insights resource connected to the Foundry project.
6. Optional: To enable quarantine remediation, also assign **Azure AI Developer** or **Contributor** on the
   Foundry project. That grant carries agent write access, which the read-only Reader doesn't include. For
   more information, see [Write-back — quarantine remediation](user-guide.md#8-write-back--quarantine-remediation).
7. Optional: To discover Copilot Studio agents, assign the tenant-wide **Power Platform Administrator** role
   to the service principal and create an application user with a security role in each Dataverse
   environment that you want to scan. For more information, see
   [Entra and Azure role setup](user-guide.md#34-entra-and-azure-role-setup).

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates)
   using the Microsoft Azure AI Foundry connector JAR file. There is no need to fill in template details; the
   system reads them automatically from the connector configuration file. If a template named
   **Microsoft Azure AI Foundry** already exists, don't upload the new JAR file to it. Follow
   [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources),
   selecting the newly created **Microsoft Azure AI Foundry** template. The system automatically generates a new
   schema.
3. Fill in the data source properties. For more information, see the
   [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches Azure. A passing test means the data source is
   ready to read. It says nothing about the identity graph.
5. **Mount the data source under a naming context, and publish every attribute.** The system wires up the
   naming attribute automatically; the rest are not published unless you say so, and an attribute that is
   not published is invisible below the naming context. A graph mapping that names it then resolves to
   nothing, with no error anywhere. This is the most-skipped step in the chain.
6. **Create an `IDO_` LDAP proxy over that naming context.** Identity Observability reads change events
   through the proxy, never through the connector data source directly.
7. **Upload the agent pipeline template and merge this connector's block into the shared mapping archive.**
   Both files ship with the connector. Every agentic connector references the same template by name, so
   merge your block into the archive rather than replacing it.
8. **Assess, apply, and read the processed-event count.** A pipeline that reaches the running state with a
   processed count matching the entry count, and no rise in the rejected count, is the first evidence that
   anything reached the graph. Stopping at step 4 leaves a green connection over an empty graph.

Browse the directory as you would any data source: you'll see one entry per agent.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the
**Connection**, **Scope**, **Copilot Studio**, **Enrichment**, **Remediation**, and **Advanced** sections.
For more information, see
[Configuration reference — all parameters](user-guide.md#4-configuration-reference--all-parameters).

### Connection

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `azureTenantId` | Yes | `string` | None | A tenant GUID | The Microsoft Entra ID tenant that the service principal belongs to. |
| `azureClientId` | Yes | `string` | None | An application GUID | Application (client) ID of the service principal that the connector authenticates as. |
| `azureClientSecret` | Yes | `password` | None | Any | Client secret of the service principal. A secret expires; update this property when it's rotated. |
| `azureSubscriptionId` | Yes | `string` | None | A subscription GUID | Azure subscription to scan. Also emitted on every entry as the repository linkage. |
| `azureResourceGroup` | Conditional | `string` | None | A resource group name | Resource group of the Foundry account. Required for management-plane enrichment and for a resource-group-scoped multi-project scan, but not for a subscription-scoped multi-project scan. When it's required and empty, the connector disables management-plane enrichment; data-plane discovery still works. |
| `foundryProjectEndpoint` | Conditional | `string` | None | <code>https://<var>ACCOUNT</var>.services.ai.azure.com/api/projects/<var>PROJECT</var></code> | The Foundry project endpoint whose agents are listed, where <code><var>ACCOUNT</var></code> is the Azure AI Foundry account name and <code><var>PROJECT</var></code> is the project name. Required unless `enableMultiProject` is on (the connector then discovers projects automatically) or the data source covers only Copilot Studio. |

### Scope

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableMultiProject` | No | `boolean` | `false` | `true`, `false` | Scan every Foundry project in scope instead of only `foundryProjectEndpoint`. Requires `enableArmEnrichment`, because the connector discovers projects on the management plane. The connector qualifies `agentId` with the account and project so that distinguished names stay unique. |
| `multiProjectScope` | No | `string` | `resourceGroup` | `resourceGroup`, `subscription` | Discovery scope for the multi-project scan. `resourceGroup` needs `azureResourceGroup`; `subscription` covers every Foundry account and project in the subscription. An unrecognized value logs a warning and falls back to `resourceGroup`. |
| `maxProjects` | No | `number` | `50` | 0 to 2147483647 | Hard ceiling on projects scanned per multi-project scan. Discovery stops at the cap and logs a warning. `0` means no cap. |
| `maxAgents` | No | `number` | `1000` | 0 to 2147483647 | Hard ceiling on agents collected and enriched per project. When exceeded, the connector truncates the candidate set before enrichment and logs a warning. `0` means no cap. Raise it or set `0` for projects with more than 1,000 agents. |

### Copilot Studio

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableCopilotStudio` | No | `boolean` | `false` | `true`, `false` | Also discover Microsoft Copilot Studio agents: the connector enumerates Power Platform environments, then lists each environment's Dataverse agents and merges them into the same result set with `platform=COPILOT_STUDIO`. Independent of Azure AI Foundry, so a data source that covers only Copilot Studio is valid. Requires extra permissions, and this release captures discovery data only: no model and no guardrails. |
| `copilotEnvironmentFilter` | No | `string` | None (all) | Comma-separated environment GUIDs | Allowlist of Power Platform environments to scan. Empty means every environment that the service principal can access. Meaningful only when `enableCopilotStudio` is on. |

### Enrichment

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableArmEnrichment` | No | `boolean` | `true` | `true`, `false` | Populate `runtimeIdentity`, `resources`, `permissionFlow`, `guardrails`, and `tags` from the Azure management plane. Needs management-plane read access, and `azureResourceGroup` except in a subscription-scoped multi-project scan. Also a prerequisite for multi-project discovery and for the `enableInvocationHistory` and `enableActivityHistory` properties. |
| `enableGraphEnrichment` | No | `boolean` | `true` | `true`, `false` | Enrich the identities that the agents run as from the directory: display name and application ID, then their API permissions and group memberships. Also resolves an opaque governance actor to a name. Active only when management-plane enrichment is on. |
| `enableInvocationHistory` | No | `boolean` | `true` | `true`, `false` | Populate `lastInvokedAt` and a 30-day invocation count from the Application Insights resource connected to the project. One aggregate query per project, reading timestamps, counts, and agent names only, never conversation or prompt content. |
| `enableActivityHistory` | No | `boolean` | `true` | `true`, `false` | Populate project governance (the most recent successful control-plane write, its actor, and its operation) from the Azure Activity Log, exposed in `metadata` and as `lastUpdatedBy`. Project-level, not per-agent. |
| `enableInstructionText` | No | `boolean` | `false` | `true`, `false` | Emit the raw agent instruction (system prompt) in the `instruction` attribute. When off, the connector keeps only the instruction's cryptographic fingerprint and length, so the prompt text stays out of the directory. |

### Remediation

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableRemediation` | No | `boolean` | `false` | `true`, `false` | The connector's only write path. When on, an LDAP modify of `actionQuarantined` quarantines or releases the agent through the platform's reversible lifecycle action. When off, the connector refuses that modify and writes nothing. Requires agent write access on the project. |

### Advanced

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `foundryApiVersion` | No | `string` | `v1` | Any Foundry agent API version value | Data-plane API version. This property is overridable because the preview value has changed across platform releases; you can set the value that your tenant expects without deploying a new JAR file. |
| `enrichmentThreads` | No | `number` | `10` | 1 to 50 | Number of projects that the connector scans concurrently during a multi-project scan. Set it to `1` for a fully sequential scan. No effect on a single-project data source. High values increase the risk of service-side throttling. |
| `azureMaxRetries` | No | `number` | `3` | 0 to 10 | Retry budget per Azure call on throttling and transient errors. Backoff honors the service's `Retry-After` hint. The first attempt isn't a retry. |

## Supported operations

The following table summarizes the operations that the connector supports for its one object type. Each cell
shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `vdAgentIdentity` | Yes | No | Limited | No |

The [Operation details](#operation-details) section explains cells marked _No_ or _Limited_. For more
information about attribute-level detail, see the
[Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `vdAgentIdentity` | Search | Yes | Subtree search returns every agent, subject to `maxAgents` and `maxProjects`. Base search on <code>agentId=<var>AGENT_ID</var></code>, where <code><var>AGENT_ID</var></code> is the agent's `agentId` value, returns exactly that entry. An unscoped browse returns every agent. The connector doesn't support paging, because the SDK search contract returns all results in one response. Bound the result size with attribute narrowing, `maxAgents`, or a client-supplied size limit. |
| `vdAgentIdentity` | Modify | Limited | Only `actionQuarantined` is writable, and only when `enableRemediation` is `true`; the connector supports all three sub-operations (add, replace, delete value), and deleting the value releases the quarantine. Modifying it with remediation off returns `UNWILLING_TO_PERFORM (53)` and writes nothing. Modifying any other attribute returns `OPERATIONS_ERROR (1)`. |
| Both | Create, Delete | No | The connector never creates or deletes Azure agents or directory objects. |

## Schema overview

The schema defines one object type:

- `vdAgentIdentity`, one entry per agent.

The connector exposes both Azure AI Foundry and Copilot Studio agents through the single `vdAgentIdentity`
object type and uses the `platform` attribute to tell them apart. Many attributes might be absent from a
given entry: they don't apply to that agent's platform, Azure doesn't expose them, or the enrichment they
need is off. The identity data management service drops these null-valued attributes from its response. No
`vdAgentIdentity` entry ever carries all 46 attributes. This is normal, not an error.

The connector supports automatic schema authoring: the system generates the schema when you create the data
source. For more information about each attribute's data type and write support, see the
[Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **One subscription per data source.** One data source can scan several Azure AI Foundry projects, but a single scan never spans subscriptions or tenants. Configure one data source per subscription. |
| 3 | **No paging.** The connector must materialize and return the whole result set at once, so bound the result size with attribute narrowing, `maxAgents` or `maxProjects`, or a client-supplied size limit rather than by paging. |
| 4 | **`maxAgents` truncates by default.** The default cap of 1,000 agents per project truncates larger projects, with a warning in the log. Raise it or set `0` when a project is larger and entry counts must match Azure exactly. |
| 5 | **No per-agent authorship or lifecycle actor history.** Foundry agents are data-plane objects and aren't individually audited, so `createdBy` and the per-transition lifecycle actors (`publishedBy`, `suspendedBy`, `blockedBy`, `deletedBy`, `statusChangedBy`, and `lastInvokedBy`) are always absent for Foundry agents. The only obtainable governance signal is the project-level last control-plane write, exposed as `lastUpdatedBy` and therefore shared by every agent in the project. |
| 6 | **Project governance has a 90-day horizon.** It covers control-plane writes only, so a project with no such write in the window carries no governance data. |
| 7 | **The connector doesn't populate the _agent-to-agent (A2A)_ card.** `agentCardUrl`, `url`, and `skills` are declared in the schema but always absent: they require a published agent that exposes an agent endpoint. |
| 8 | **`intent` has no platform source.** It's always absent, and Identity Observability authors it downstream. |
| 9 | **Invocation recency requires a connected Application Insights resource.** It also needs query access and covers a 30-day window. Agents not invoked in that window carry no `lastInvokedAt`, which is normal rather than an error. |
| 10 | **Quarantine always reads back as `false`.** The platform records no in-place per-agent quarantine attribution, so the connector never asserts an unprovable value. After a successful quarantine, the observable change is `status` becoming `SUSPENDED`; Identity Observability owns the quarantine decision and its attribution. |
| 11 | **Copilot Studio support is discovery-only in this release.** Copilot Studio entries carry core identity, lifecycle, and created and updated actors, but no model and no guardrails. Copilot Studio configures both; this release doesn't read them. |
| 12 | **The connector reads its configuration once, at load time.** Editing a data source's credentials or properties takes effect only after you reload the connector or restart the identity data management service. Client secrets expire, and the connector never refreshes them. |
| 13 | **Throughput is bounded by the sequential data-plane listing, not by the connector.** Listing runs at roughly 2 seconds per page of 100 agents, so a 10,000-agent single project takes between 3 and 4 minutes per full scan. Spreading the agents over many projects and raising `enrichmentThreads` parallelizes the work, at the cost of service-side throttling risk. For more information, see [Extrapolation to 10,000 agents](user-guide.md#94-extrapolation-to-10000-agents). |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 0.9.3 | Unreleased | Updates a bundled third-party library to close a critical vulnerability (CVE-2026-53914). Rebuilds the connector on the Radiant Logic connector development kit, so that agents now reach the identity graph. Removes the `vdIdentity` object type, so you must delete and recreate the data source. |
| 0.9.2 | August 31, 2026 | First beta release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns, so the
following tables focus on write support and availability. The connector doesn't support creating new
entries, so the tables omit the 'Create' column. In the 'Modify' column, a checkmark (✔) marks an attribute
that you can modify on an existing object, and a blank cell marks a read-only attribute. Every attribute of


The 'Platform' column indicates which agent platforms populate an attribute:

- **Both**: Azure AI Foundry and Copilot Studio
- **Foundry**: Azure AI Foundry only
- **Copilot**: Copilot Studio only
- **None**: declared in the schema, but Azure exposes no source on either platform

An attribute that belongs to only one platform is absent from entries for the other platform.

**Legend.** In the 'Modify' column, `✔` marks an attribute you can change on an existing object and an
empty cell marks a read-only attribute. In the 'Platform' column, `Both` means Azure AI Foundry and
Copilot Studio, `Foundry` and `Copilot` mean that platform only, and `None` means the attribute is
declared in the schema but Azure exposes no source for it on either platform.

The 'Nullable' column indicates whether the attribute must have a value. Attributes marked with a checkmark
(✔) are allowed to have a null value; those with a blank cell must have a value on every entry. Null values
are dropped from the response.

Some attributes represent a nested object or a list, but the connector returns them to Identity Data
Platform as strings, so the schema declares them as `string`. To help you identify the original type, this
document adds a qualifier in the 'Data type' column:

- `string` (JSON object)
- `string` (JSON array)

The connector emits attribute names in LDAP-valid `camelCase`; a downstream mapping converts them to the
canonical `snake_case` model.

For more information about object-level operation support, see the
[Supported operations](#supported-operations) section of this document.

### Object type: `vdAgentIdentity`

| Attribute | Data type | Modify | Platform | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `agentId` | `string` | | Both | | Primary key and _relative distinguished name (RDN)_. The platform agent ID, qualified with the account and project in a multi-project scan and with the environment for Copilot Studio, so distinguished names never collide. |
| `externalId` | `string` | | Both | ✔ | The agent's stable platform identifier: the cross-provider correlation key. Survives a rename, which `agentId` doesn't. |
| `name` | `string` | | Both | | Agent name. |
| `description` | `string` | | Both | ✔ | Absent when Azure holds none. Locally overridable downstream. |
| `intent` | `string` | | None | ✔ | No platform source. Reserved for Identity Observability to author. |
| `instruction` | `string` | | Foundry | ✔ | Raw system prompt. Emitted only when `enableInstructionText` is on. |
| `version` | `string` | | Both | ✔ | Agent version string. |
| `platform` | `string` | | Both | | Discriminator: `AZURE_FOUNDRY` or `COPILOT_STUDIO`. |
| `kind` | `string` | | Both | ✔ | Foundry: `prompt`, `workflow`, `hosted`, or `container_app`. Copilot Studio: `copilot`. |
| `tags` | `string` (JSON object) | | Foundry | ✔ | The project's Azure resource tags. Requires management-plane enrichment. |
| `status` | `string` | | Both | ✔ | Normalized lifecycle status. A quarantined (disabled) agent reads `SUSPENDED`. |
| `statusReason` | `string` | | Both | ✔ | The platform-native status, carried verbatim when the normalization is lossy. |
| `actionQuarantined` | `boolean` | ✔ | Both | | **The only writable attribute.** Writable only when `enableRemediation` is on, and always reads back `false` because the platform records no attribution. |
| `statusChangedAt` | `string` | | None | ✔ | Declared. No Azure source. |
| `statusChangedBy` | `string` (JSON object) | | None | ✔ | Declared. No Azure source. |
| `runtimeIdentity` | `string` (JSON object) | | Foundry | ✔ | The project managed identity that the agent runs as, enriched from the directory. Requires management-plane enrichment. |
| `createdAt` | `string` | | Both | ✔ | Creation time. |
| `createdBy` | `string` (JSON object) | | Copilot | ✔ | Creating identity. Azure AI Foundry agents aren't individually audited, so only Copilot Studio populates this attribute. |
| `publishedAt` | `string` | | None | ✔ | Declared. No Azure source. |
| `publishedBy` | `string` (JSON object) | | None | ✔ | Declared. No Azure source. |
| `suspendedAt` | `string` | | None | ✔ | Declared. No Azure source. |
| `suspendedBy` | `string` (JSON object) | | None | ✔ | Declared. No Azure source. |
| `blockedAt` | `string` | | None | ✔ | Declared. No Azure source. |
| `blockedBy` | `string` (JSON object) | | None | ✔ | Declared. No Azure source. |
| `deletedAt` | `string` | | None | ✔ | Declared. No Azure source. |
| `deletedBy` | `string` (JSON object) | | None | ✔ | Declared. No Azure source. |
| `lastUpdatedAt` | `string` | | Both | ✔ | Per-agent modification time. |
| `lastUpdatedBy` | `string` (JSON object) | | Both | ✔ | For Foundry, the project-level last control-plane write actor, shared by every agent in the project. Requires `enableActivityHistory`. |
| `lastInvokedAt` | `string` | | Foundry | ✔ | Most recent invocation, from the connected Application Insights resource. Requires `enableInvocationHistory`. |
| `lastInvokedBy` | `string` (JSON object) | | None | ✔ | Not populated in this release. |
| `metadata` | `string` (JSON object) | | Both | ✔ | Provenance (endpoint, subscription, resource group, account, and either project or environment), the instruction fingerprint and length, the 30-day invocation count, and the project governance block. |
| `repositoryId` | `string` | | Both | | Repository linkage: the owning Azure subscription ID. |
| `repositoryDisplayName` | `string` | | Both | | Repository linkage display label, prefixed with `Azure` plus a space. |
| `model` | `string` (JSON array) | | Foundry | ✔ | The model that the agent reasons with. Not populated for Copilot Studio in this release. |
| `guardrails` | `string` (JSON array) | | Foundry | ✔ | Content-safety policy from the agent's model deployment, with its filters and enforcement mode. Requires management-plane enrichment. Not populated for Copilot Studio in this release. |
| `features` | `string` (JSON array) | | Foundry | ✔ | Positive-signal capability flags. The connector omits an empty set rather than emitting an empty array. |
| `agentCardUrl` | `string` | | None | ✔ | A2A agent card URL. Declared, but not populated in this release because it requires a published agent endpoint. |
| `url` | `string` (JSON object) | | None | ✔ | A2A endpoint URLs. Declared, but not populated in this release. |
| `provider` | `string` (JSON object) | | Both | | Owning organization of the agent. |
| `skills` | `string` (JSON array) | | None | ✔ | A2A declared skills. Declared, but not populated in this release. |
| `tools` | `string` (JSON array) | | Foundry | ✔ | The agent's declared tools, secrets redacted. |
| `resources` | `string` (JSON array) | | Foundry | ✔ | The project connections that the agent can reach, with access level, grant path, and credential material type. Requires management-plane enrichment. |
| `subagents` | `string` (JSON array) | | Foundry | ✔ | The children that a workflow agent delegates to. Only on workflow agents. |
| `permissionFlow` | `string` (JSON object) | | Foundry | ✔ | Acyclic permission graph from the agent through its managed identity to the resources it can reach. Requires management-plane enrichment. |
| `_connectorFoundrySourceAvailable` | `boolean` | | Both | | Diagnostic, scan-level: whether the agent listing succeeded this scan. |
| `_connectorSourcesUnavailable` | `string` | | Both | | Diagnostic, per entry: comma-separated Azure sources that failed for this entry. Blank when the entry is complete. |

