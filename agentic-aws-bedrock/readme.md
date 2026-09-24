# AWS Bedrock connector

This document describes the Radiant Logic custom connector for AWS Bedrock, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector is built using the Radiant Logic Connector SDK and integrates with Amazon Bedrock Agents and Amazon Bedrock AgentCore through the AWS control-plane APIs. It virtualizes every AI agent in the configured AWS accounts and region—classic Bedrock Agents and AgentCore Runtimes—as a single object type. Each entry carries its foundation model, guardrails, tools, reachable resources, IAM permission graph, ownership history, and invocation recency. The connector provides search across all discovered agents and one opt-in write: quarantine remediation.

[Download the AWS Bedrock connector JAR file](builds/aws-bedrock-connector-1.0.1.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>AWS Bedrock</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>1.0.1</td>
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

The following sections provide a walkthrough for configuring AWS and Identity Data Platform to use the AWS Bedrock connector.

### Preconditions

- An AWS account and region containing Amazon Bedrock Agents or Bedrock AgentCore Runtimes, reachable from RadiantOne over HTTPS.
- An AWS access key (`AKIA…` long-term is recommended) for an IAM principal holding the connector's read permissions. Ambient or passwordless credentials aren't supported.
- Optional, for multi-account or cross-account scanning: a reader role in each target AWS account that the connector's principal can assume.

### Configure AWS

1. Create a dedicated least-privilege IAM user for the connector and attach the read-only policy. Two grants are commonly missing on stock read-only roles, so add them explicitly: `bedrock-agentcore:GetAgentCard` and `bedrock:ListTagsForResource`. For more information, see [Minimum read-only policy (default-on enrichment)](user-guide.md#341-minimum-read-only-policy-default-on-enrichment).
2. Create an access key for that user (`AKIA…` plus its secret).
3. Optional: To scan several AWS accounts from one data source, create the reader role in each target account, grant it the same read-only policy, trust the connector's IAM user in its trust policy, and grant `sts:AssumeRole` on those role ARNs to the connector's user. For more information, see [Cross-account role assumption (STS `AssumeRole`)](user-guide.md#352-cross-account-role-assumption-sts-assumerole).
4. Optional: To enable quarantine remediation, additionally grant the four tag-write actions (`bedrock:TagResource`, `bedrock:UntagResource`, `bedrock-agentcore:TagResource`, `bedrock-agentcore:UntagResource`), scoped to the quarantine tag keys, and deploy the tag-protection guardrail. For more information, see [Write-back — quarantine remediation](user-guide.md#8-write-back--quarantine-remediation).

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the AWS Bedrock connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file. If a template named **AWS Bedrock** already exists, don't upload the new JAR file to it. Follow [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **AWS Bedrock** template. The system automatically generates a new schema.
3. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section.
4. Run **Test Connection** to confirm the connector reaches AWS. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source. You'll see one entry per agent.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection**, **Agent sources and filtering**, **Enrichment**, and **Performance** sections. For more information, see [Configuration reference — all parameters](user-guide.md#4-configuration-reference--all-parameters).

### Connection

| Property                | Required | Type       | Default                          | Allowed values                               | Description                                                                                                                                  |
|-------------------------|----------|------------|----------------------------------|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `awsRegion`             | Yes      | `string`   | `us-east-1`                      | Any AWS region ID                            | Region to scan, for example `us-east-2`. One region per data source.                                                                         |
| `awsAccessKeyId`        | Yes      | `string`   | None                             | `AKIA…` or `ASIA…`                           | Access key ID. The connector rejects an empty value or a partial key pair.                                                                   |
| `awsSecretAccessKey`    | Yes      | `password` | None                             | Any                                          | Secret access key that pairs with `awsAccessKeyId`.                                                                                          |
| `awsSessionToken`       | No       | `password` | None                             | Any                                          | Session token. Required only when the access key ID starts with `ASIA…` (temporary credentials).                                             |
| `assumeRoleArn`         | No       | `string`   | None                             | An IAM role ARN                              | Single target account: role assumed on top of the base credentials. Every AWS call then runs as the target account.                          |
| `assumeRoleExternalId`  | No       | `string`   | None                             | Any                                          | External ID (confused-deputy guard). Set only when the target role's trust policy carries an `sts:ExternalId` condition.                     |
| `assumeRoleSessionName` | No       | `string`   | `radiantlogic-bedrock-connector` | Any valid session name                       | Role session name, visible in the target account's CloudTrail.                                                                               |
| `accountIds`            | No       | `string`   | None                             | Comma-separated 12-digit account IDs         | Multi-account crawl: accounts scanned in one pass, each assumed through `assumeRoleName`. Empty means single-account.                        |
| `assumeRoleName`        | No       | `string`   | None                             | An IAM role name                             | Role name assumed in each `accountIds` account.                                                                                              |
| `assumeRoleTargets`     | No       | `string`   | None                             | JSON array `[{roleArn,externalId?,region?}]` | Explicit multi-account targets for heterogeneous accounts. Overrides `accountIds` and `assumeRoleName`.                                      |
| `repositoryName`        | No       | `string`   | None                             | Any                                          | Display name of the scanned AWS account, surfaced as `repositoryDisplayName`. Empty falls back to the account alias, then to the account ID. |

### Agent sources and filtering

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `enableBedrockAgents` | No | `boolean` | `true` | `true`, `false` | Scan classic Bedrock Agents. Off means no `bedrock:*` calls. |
| `enableAgentCoreRuntimes` | No | `boolean` | `true` | `true`, `false` | Scan AgentCore Runtimes. Off means no `bedrock-agentcore:*` calls. |
| `tagFilter` | No | `string` | None | Comma-separated <code><var>KEY</var>=<var>VALUE</var></code> pairs | Enumerate agents server-side by tag (all tags must match) instead of listing everything. Returns only matching agents and captures their tags at no extra cost. |
| `maxAgents` | No | `number` | `1000` | `0` or greater (`0` = no cap) | Hard ceiling on agents collected and enriched per scan. When exceeded, the connector truncates the candidate set before enrichment and logs a warning. Raise it or set `0` for accounts with more than 1,000 agents. |

### Enrichment

| Property                   | Required | Type      | Default                   | Allowed values        | Description                                                                                                                                                                                        |
|----------------------------|----------|-----------|---------------------------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enableActionGroups`       | No       | `boolean` | `true`                    | `true`, `false`       | Populate `tools` from action groups.                                                                                                                                                               |
| `enableKnowledgeBases`     | No       | `boolean` | `true`                    | `true`, `false`       | Populate knowledge-base `resources`.                                                                                                                                                               |
| `enableAgentCardFetch`     | No       | `boolean` | `true`                    | `true`, `false`       | Fetch the AgentCore _agent-to-agent (A2A)_ AgentCard to populate `provider`, `skills`, and endpoint URLs.                                                                                          |
| `enableIamPolicyWalk`      | No       | `boolean` | `true`                    | `true`, `false`       | Walk the agent's IAM role to build `permissionFlow` and IAM-discovered `resources`. Heaviest enrichment stage.                                                                                     |
| `enableTags`               | No       | `boolean` | `true`                    | `true`, `false`       | Populate the `tags` object (and the quarantine flag it carries).                                                                                                                                   |
| `enableHistory`            | No       | `boolean` | `true`                    | `true`, `false`       | Populate ownership and change history from one account-wide CloudTrail sweep per scan (90-day horizon).                                                                                            |
| `enableInvocationHistory`  | No       | `boolean` | `true`                    | `true`, `false`       | Populate `lastInvokedAt` for AgentCore runtimes from one batched CloudWatch query per scan.                                                                                                        |
| `enableSubagents`          | No       | `boolean` | `false`                   | `true`, `false`       | Populate `subagents` for Bedrock multi-agent supervisors, plus the matching permission-flow edges.                                                                                                 |
| `enableDeepPermissionFlow` | No       | `boolean` | `false`                   | `true`, `false`       | Extend `permissionFlow` through action-group Lambdas and their execution roles. Requires `enableIamPolicyWalk` and `enableActionGroups`.                                                           |
| `enableToolDetail`         | No       | `boolean` | `false`                   | `true`, `false`       | Fill each tool's type, executor, and API schema reference. Requires `enableActionGroups`.                                                                                                          |
| `enableInstruction`        | No       | `boolean` | `false`                   | `true`, `false`       | Emit the raw agent instruction (system prompt). Off keeps only its hash and length, which is privacy-preserving.                                                                                   |
| `enableDependencyIndex`    | No       | `boolean` | `false`                   | `true`, `false`       | Emit the multivalued `dependsOn` index used for reverse change lookup. No extra AWS call and no new permission.                                                                                    |
| `quarantineTagKey`         | No       | `string`  | `radiantlogic:quarantine` | Any valid AWS tag key | Marker-tag key reflected onto, and written by, `actionQuarantined`.                                                                                                                                |
| `enableRemediation`        | No       | `boolean` | `false`                   | `true`, `false`       | The connector's only write path. When on, an LDAP modify of `actionQuarantined` writes the marker tag. When off, the connector refuses that modify and writes nothing.                             |
| `quarantineMethod`         | No       | `string`  | `tag`                     | `tag`, `block`        | How a quarantine is enforced. `tag` writes the marker only and delegates enforcement to your AWS automation; `block` also blocks invocation directly. Needs extra permissions; see the user guide. |

### Performance

| Property                      | Required | Type     | Default | Allowed values     | Description                                                                                                                                             |
|-------------------------------|----------|----------|---------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enrichmentThreads`           | No       | `number` | `10`    | `1-50` (inclusive) | Per-agent enrichment concurrency. `1` is sequential. Values above about 15 rarely help because of AWS quotas.                                           |
| `accountThreads`              | No       | `number` | `4`     | `1-16` (inclusive) | Multi-account crawl only: accounts scanned in parallel. Peak worker threads are roughly `accountThreads × enrichmentThreads`.                           |
| `awsMaxRetries`               | No       | `number` | `3`     | `0-10` (inclusive) | Retry budget per AWS call. All clients use adaptive retry with client-side rate limiting.                                                               |
| `awsApiCallTimeoutSeconds`    | No       | `number` | `60`    | `0-600`            | Maximum seconds for a whole AWS API call including retries. A value of `0` means the connector imposes no timeout.                                      |
| `awsApiAttemptTimeoutSeconds` | No       | `number` | `20`    | `0-600`            | Maximum seconds for a single attempt before retry. Shouldn't exceed `awsApiCallTimeoutSeconds`. A value of `0` means the connector imposes no timeout.  |

## Supported operations

The following table summarizes the operations that the connector supports for its object type. Each cell shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type       | Search | Create | Modify  | Delete |
|-------------------|--------|--------|---------|--------|
| `vdAgentIdentity` | Yes    | No     | Limited | No     |

The [Operation details](#operation-details) section explains cells marked _No_ or _Limited_. For more information about attribute-level detail, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `vdAgentIdentity` | Search | Yes | Subtree search returns every agent, subject to `maxAgents`. Base search on <code>agentKey=<var>AGENT_ARN</var></code> returns exactly that entry. Paging isn't supported: bound result size with attribute narrowing, `tagFilter`, or `maxAgents`. |
| `vdAgentIdentity` | Modify | Limited | Only `actionQuarantined` is writable, and only when `enableRemediation` is `true`; the connector supports all three sub-operations (add, replace, delete value), and deleting the value releases the quarantine. Modifying it with remediation off returns `UNWILLING_TO_PERFORM (53)`. Modifying any other attribute returns `OPERATIONS_ERROR (1)` and writes nothing. |
| `vdAgentIdentity` | Create, Delete | No | The connector never creates or deletes AWS agents. |

## Schema overview

The schema defines one object type: `vdAgentIdentity`. The primary key and _relative distinguished name (RDN)_ is `agentKey`. This value is the agent's AWS ARN. The ARN is globally unique, so distinguished names never collide when one data source crawls several AWS accounts. The short AWS agent ID is available in the `agentId` attribute.

The connector exposes both classic Bedrock Agents and AgentCore Runtimes through this single object type and uses the `platform` attribute to tell them apart. Many attributes might be absent from a given entry: they don't apply to that agent's platform, AWS hasn't populated them yet, or the enrichment they need is off. The identity data management service drops these null-valued attributes from its response. This is normal, not an error.

The connector supports automatic schema authoring: the system generates the schema when you create the data source. For more information about each attribute's data type and write support, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **One region per data source.** One data source can crawl several AWS accounts through role assumption, but a single crawl never spans regions. Configure one data source per region. |
| 2 | **Quarantine write-back targets the primary account.** In a multi-account crawl, the modify operation writes to the first configured account, because a modify distinguished name carries no account selector. |
| 3 | **No paging.** The connector must materialize and return the whole result set at once, so bound result size with attribute narrowing, `tagFilter`, or `maxAgents` rather than by paging. |
| 4 | **`maxAgents` truncates silently by default.** The default cap of 1,000 agents truncates larger accounts (with a warning in the log). Raise it or set `0` when the account holds more agents and entry counts must match AWS exactly. |
| 5 | **Invocation recency is AgentCore-only.** Classic Bedrock Agents expose no per-agent invocation metric, so `lastInvokedAt` is always absent for them. The last invoking identity (`lastInvokedBy`) isn't populated at all: it would require CloudTrail data events, which are off by default, charged, and high-volume. |
| 6 | **History has a 90-day horizon.** Ownership and change history come from CloudTrail, so agents whose last relevant event is older than 90 days carry no history attributes. |
| 7 | **Some lifecycle attributes have no AWS source.** `publishedBy`, `suspendedAt` and `suspendedBy`, `blockedAt` and `blockedBy`, and `deletedAt` and `deletedBy` are part of the data model but are always absent for AWS. |
| 8 | **Enforcing quarantine by tag requires your own AWS automation.** With the default `tag` method, the connector only writes the marker tag; AWS doesn't block a tagged agent by itself, and a tag condition can't deny the invoke actions. Use `quarantineMethod=block` for connector-enforced blocking, or build the automation described in the user guide. |
| 9 | **Credentials are read once, at load time.** Editing a data source's credentials or flags takes effect only after you reload the connector or restart the identity data management service. Static temporary keys (`ASIA…`) expire within hours and are never refreshed; prefer a long-term key with role assumption. |
| 10 | **Throughput is bounded by AWS quotas.** Full enrichment saturates at roughly 10 agents per second because of non-adjustable AWS rate limits, so a 10,000-agent account takes about 17 minutes per full scan. See the user guide for capacity planning. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 1.0.1 | September 9, 2026 | Bug fix: deleting an agent in AWS now removes the agent from Identity Data Platform. A failed read does not delete the entry. No schema, property, or permission changes. |
| 1.0.0 | August 31, 2026 | First official release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns, so the following table focuses on write support. The connector doesn't support creating new entries, so this table omits the 'Create' column. The 'Modify' column marks whether an attribute can be modified on an existing object.

The 'Platform' column indicates whether an attribute applies to classic Bedrock Agents, to AgentCore Runtimes, or to both; an attribute belonging to the other platform is absent from that entry. The 'Nullable' column marks the attributes that are present on every entry (a blank cell) versus those that can be null (✔), and therefore legitimately missing from an entry.

Some attributes represent a nested object or a list, but the connector returns them to Identity Data Platform as strings, so they're declared as `string` in the schema. To help you identify the original type, this document adds a qualifier in the 'Data type' column:

- `string` (JSON object)
- `string` (JSON array)

The connector emits attribute names in LDAP-valid `camelCase`; a downstream mapping converts them to the canonical `snake_case` model.

For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

### Object type: `vdAgentIdentity`

| Attribute name | Data type | Modify | Platform | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `agentKey` | `string` | | Both | | Primary key and RDN. The agent's AWS ARN, identical to `externalId`. |
| `agentId` | `string` | | Both | | Short AWS agent ID. Unique only within one account. |
| `externalId` | `string` | | Both | ✔ | The agent's AWS ARN: the cross-platform correlation key. Absent only if the connector couldn't resolve the account ID. |
| `name` | `string` | | Both | | Agent or runtime name. |
| `description` | `string` | | Both | ✔ | Absent when AWS holds none. |
| `intent` | `string` | | Both | | Reserved for Identity Data Platform to author; emitted blank. |
| `platform` | `string` | | Both | | Discriminator: `BEDROCK_AGENT` or `AGENTCORE_RUNTIME`. |
| `version` | `string` | | Both | ✔ | Agent or runtime version. |
| `instruction` | `string` | | Bedrock | ✔ | Raw system prompt. Emitted only when `enableInstruction` is on. |
| `repositoryId` | `string` | | Both | ✔ | Owning AWS account ID. |
| `repositoryDisplayName` | `string` | | Both | ✔ | Owning account display name, prefixed with `AWS` plus a space. |
| `status` | `string` | | Both | ✔ | Normalized lifecycle status. `QUARANTINED` when the agent is quarantined. |
| `statusReason` | `string` | | Both | | Blank when the status mapping is lossless; otherwise, carries the original AWS status or the pre-quarantine one. |
| `statusChangedAt` | `string` | | Both | ✔ | Time of the latest publish. Requires `enableHistory`. |
| `statusChangedBy` | `string` (JSON object) | | Both | ✔ | Identity that performed the latest publish, with session-governance signals. Requires `enableHistory`. |
| `createdAt` | `string` | | Both | ✔ | Creation time. Requires per-agent enrichment. |
| `createdBy` | `string` (JSON object) | | Both | ✔ | Creating identity. Requires `enableHistory`. |
| `lastUpdatedAt` | `string` | | Both | ✔ | Last modification time. Available even on a skeleton scan. |
| `lastUpdatedBy` | `string` (JSON object) | | Both | ✔ | Identity that last modified the agent. Requires `enableHistory`. |
| `publishedAt` | `string` | | Both | ✔ | First publish time. Requires `enableHistory`. |
| `publishedBy` | `string` (JSON object) | | Both | ✔ | No AWS source. Always absent. |
| `suspendedAt` | `string` | | Both | ✔ | No AWS source. Always absent. |
| `suspendedBy` | `string` (JSON object) | | Both | ✔ | No AWS source. Always absent. |
| `blockedAt` | `string` | | Both | ✔ | No AWS source. Always absent. |
| `blockedBy` | `string` (JSON object) | | Both | ✔ | No AWS source. Always absent. |
| `deletedAt` | `string` | | Both | ✔ | No AWS source. Always absent. |
| `deletedBy` | `string` (JSON object) | | Both | ✔ | No AWS source. Always absent. |
| `lastInvokedAt` | `string` | | AgentCore | ✔ | Most recent invocation, from CloudWatch. Requires `enableInvocationHistory`. |
| `lastInvokedBy` | `string` (JSON object) | | Both | ✔ | Not populated. |
| `metadata` | `string` (JSON object) | | Both | ✔ | Region, account ID, idle session TTL, prepare time, instruction hash and length, and more. |
| `tags` | `string` (JSON object) | | Both | ✔ | AWS resource tags. Requires `enableTags`. |
| `model` | `string` (JSON array) | | Bedrock | ✔ | Foundation models the agent reasons with. Absent until the agent has been prepared in AWS. |
| `guardrails` | `string` (JSON array) | | Bedrock | ✔ | Attached content and safety policy. |
| `features` | `string` (JSON array) | | Both | ✔ | Positive-signal capability flags. The connector omits an empty set rather than emitting an empty array. |
| `tools` | `string` (JSON array) | | Bedrock | ✔ | Callable tools, from action groups. Requires `enableActionGroups`. |
| `resources` | `string` (JSON array) | | Both | ✔ | Knowledge bases and the resources reachable through the agent's IAM permissions. |
| `runtimeIdentity` | `string` (JSON object) | | Both | ✔ | The agent's execution role and its trust chain. |
| `permissionFlow` | `string` (JSON object) | | Both | ✔ | Acyclic permission graph from the agent through its roles and policies to the resources it can reach. Requires `enableIamPolicyWalk`. |
| `subagents` | `string` (JSON array) | | Bedrock | ✔ | Collaborators a supervisor delegates to. Requires `enableSubagents`. |
| `dependsOn` | `string` | | Both | ✔ | Multivalued. Typed <code><var>TYPE</var>:<var>IDENTIFIER</var></code> tokens for every object the agent depends on, for reverse change lookup. Requires `enableDependencyIndex`. The connector's only multivalued attribute. |
| `agentCardUrl` | `string` | | AgentCore | ✔ | URL of the agent's A2A AgentCard. |
| `url` | `string` (JSON object) | | AgentCore | ✔ | Primary, invocation, health-check, and metrics URLs. |
| `provider` | `string` (JSON object) | | Both | ✔ | Defaults to AWS; a fetched AgentCard overrides it. |
| `skills` | `string` (JSON array) | | AgentCore | ✔ | Declared skills from the AgentCard. Requires `enableAgentCardFetch`. |
| `actionQuarantined` | `boolean` | ✔ | Both | | `true` only when the marker tag equals `true`. Writable only when `enableRemediation` is on. |
| `connectorBedrockSourceAvailable` | `boolean` | | Both | | Diagnostic, scan-level: whether listing Bedrock Agents succeeded this scan. |
| `connectorAgentcoreSourceAvailable` | `boolean` | | Both | | Diagnostic, scan-level: whether listing AgentCore Runtimes succeeded this scan. |
| `connectorSourcesUnavailable` | `string` | | Both | | Diagnostic, per entry: comma-separated AWS sources that failed for this entry. Blank when the entry is complete. |
