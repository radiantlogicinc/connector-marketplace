# SailPoint ISC V3 connector

This document describes the Radiant Logic custom connector for SailPoint Identity Security Cloud, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector uses the Radiant Logic Connector SDK and integrates with SailPoint Identity Security Cloud through the SailPoint V3 and Beta REST APIs. It virtualizes the tenant's accounts, access profiles, entitlements, public identities, roles, and sources as six object types. It supports search for every object type, create, modify, and delete for `AccessProfile`, `Account`, `Role`, and `Source`, and create and modify for `Entitlement`. `PublicIdentity` is read-only.

[Download the SailPoint ISC V3 connector JAR file](builds/sailpoint-isc-v3-connector-1.0.1.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>SailPoint ISC V3</td>
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
    <th scope="row" align="left">Supported versions</th><td>8.4.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic</td>
  </tr>
</table>

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>2.2.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td><a href="user-guide.md">User guide</a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td><a href="resources/sailpoint-identity-security-cloud-mapping-profile-v1.yaml"><code>sailpoint-identity-security-cloud-mapping-profile-v1.yaml</code></a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure SailPoint and RadiantOne, complete the steps in the following sections.

### Preconditions

- A SailPoint Identity Security Cloud tenant, reachable from RadiantOne over HTTPS on port 443 at <code>https://<var>TENANT</var>.api.<var>DOMAIN</var>.com</code>, or at the tenant's custom base URL.
- Administrator access to the tenant, so that you can create an API client.

### Configure SailPoint

1. Sign in to your SailPoint Identity Security Cloud tenant as an administrator.
2. Go to **Admin > Security Settings > API Management**, and then click **Add New**.
3. Enter a descriptive name for the API client.
4. Under **Grant Types**, select **Client Credentials**.
5. Assign the `sp:scopes:all` scope. The connector requests this scope when it authenticates.
6. Save the client, and then record the **Client ID** and **Client Secret**. SailPoint shows them only once.
7. Record the tenant name and the domain from your tenant's API URL. For example, for `https://example-org.api.identitynow.com`, the tenant name is `example-org` and the domain is `identitynow`. If your tenant uses a nonstandard hostname, also record its full HTTPS base URL. The connector still requires the tenant name and domain.

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the SailPoint ISC V3 connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file. If a template named **SailPoint ISC V3** already exists, don't upload the new JAR file to it. Follow [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **SailPoint ISC V3** template. The system automatically generates a new schema.
3. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches SailPoint. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties:

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `sailPointTenant` | Yes | `string` | None | Any | The tenant name: the subdomain of the tenant's API URL. For example, for `https://example-org.api.identitynow.com`, the tenant name is `example-org`. The connector requires it even when you set `baseUrl`. |
| `sailPointDomain` | Yes | `string` | None | Any | The domain segment that follows `api.` in the tenant's API URL, without `.com`. For example, for `https://example-org.api.identitynow.com`, the domain is `identitynow`. The connector requires it even when you set `baseUrl`. |
| `clientId` | Yes | `password` | None | Any | The client ID of the SailPoint API client. The connector uses it to request access tokens through the OAuth 2.0 client credentials grant. |
| `clientSecret` | Yes | `password` | None | Any | The client secret that's paired with `clientId`. |
| `baseUrl` | No | `string` | None | A URL that starts with `https://` | The API host root, for example `https://example-org.api.identitynow.com`. Set it only when your tenant uses a nonstandard hostname. When it's empty, the connector builds the URL as <code>https://<var>TENANT</var>.api.<var>DOMAIN</var>.com</code> from `sailPointTenant` and `sailPointDomain`. A URL that doesn't start with `https://` stops the connector from initializing. Supply the host only, with no path. The connector joins `baseUrl` to each request path as written, so a value that already includes `/v3` is requested as `/v3/v3/...` and the call fails. A trailing slash produces a double slash in the path. On a standard Identity Security Cloud host, that double slash still reaches the API. |
| `accessTokenExpirationHours` | No | `number` | `1` | `1-24` (inclusive) | The maximum number of hours that the connector reuses an access token. The connector caches each token for the shorter of this value and the lifetime that SailPoint states, minus one minute. A missing, non-numeric, or out-of-range value falls back to `1`. |
| `pageSize` | No | `number` | `250` | `1-250` (inclusive) | The number of entries that the connector requests from SailPoint in each page of a one-level or subtree search. A missing, non-numeric, or out-of-range value falls back to `250`. |

## Supported operations

The following table summarizes the operations that the connector supports for its object types. Each cell shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `AccessProfile` | Yes | Yes | Yes | Limited |
| `Account` | Yes | Limited | Limited | Limited |
| `Entitlement` | Yes | Limited | Limited | No |
| `PublicIdentity` | Limited | No | No | No |
| `Role` | Yes | Yes | Yes | Yes |
| `Source` | Yes | Yes | Limited | Limited |

The [Operation details](#operation-details) section explains cells marked _No_ or _Limited_. For more information about attribute-level detail, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `AccessProfile` | Delete | Limited | SailPoint deletes an access profile only when nothing uses it. It rejects the delete of a profile that a role references, and the connector returns `OPERATIONS_ERROR (1)`. |
| `Account` | Create | Limited | The create request carries only the `attributes` value and `sourceId`. The connector ignores every other attribute and names them in one `INFO` log message. SailPoint queues the create as a task, and the connector returns the task ID without waiting for it to finish. The account isn't readable by ID until the task completes. The connector returns `OPERATIONS_ERROR (1)` when SailPoint's response carries no task ID. |
| `Account` | Modify | Limited | SailPoint accepts changes to `identityId`, `manuallyCorrelated`, and, for flat-file accounts, the keys inside `attributes`. It rejects changes to other attributes. SailPoint queues the change, and the connector doesn't wait for it, so a read right after a modify might return the old value. |
| `Account` | Delete | Limited | SailPoint deletes flat-file accounts, and deletes other accounts only when their source supports delete. SailPoint queues the delete as a task, and the connector returns the task ID without waiting for it, so the account might stay readable for a short time. The connector returns `OPERATIONS_ERROR (1)` when SailPoint's response carries no task ID. |
| `Entitlement` | Create | Limited | The connector creates entitlements through the SailPoint Beta API (`POST /beta/entitlements`). SailPoint allows this request for the `ORG_ADMIN` and `SOURCE_ADMIN` roles, but it isn't a general-purpose create API. |
| `Entitlement` | Modify | Limited | SailPoint accepts changes to seven attributes only: `name`, `description`, `owner`, `privileged`, `requestable`, `segments`, and `manuallyUpdatedFields`. It rejects changes to other attributes. |
| `Entitlement` | Delete | No | The connector sends <code>DELETE /beta/entitlements/<var>ID</var></code>, which SailPoint doesn't provide, so the delete fails with `OPERATIONS_ERROR (1)`. |
| `PublicIdentity` | Search | Limited | SailPoint provides no request that reads one public identity by ID. For a base search, the connector reads the public identity list without paging and looks for the ID in that response. When the ID is not in that response, including an identity that SailPoint would return on a later page, the search returns `OPERATIONS_ERROR (1)`. |
| `PublicIdentity` | Create, Modify, Delete | No | SailPoint provides only a list request for public identities. The connector sends the create, modify, or delete request, and it fails with `OPERATIONS_ERROR (1)`. |
| `Role` | Search | Yes | The connector fills `roleAssignedIdentities` with one extra request for each role that it returns. |
| `Source` | Modify | Limited | SailPoint rejects changes to the fields that it treats as fixed, such as `id`, `type`, `authoritative`, `connector`, `connectorClass`, and `passwordPolicies`, and to the source's status and connector metadata. |
| `Source` | Delete | Limited | SailPoint deletes every account on the source before it deletes the source itself. It queues the delete as a task, and the connector returns the task ID without waiting for it. The connector returns `OPERATIONS_ERROR (1)` when SailPoint's response carries no task ID. |

## Schema overview

The schema defines six object types:

- `AccessProfile`
- `Account`
- `Entitlement`
- `PublicIdentity`
- `Role`
- `Source`

The primary key and _relative distinguished name (RDN)_ is `id` for every object type. SailPoint assigns the ID when it creates an object, so the connector ignores the `id` value of an add request. LDAP still needs a value to form the RDN, so supply any placeholder. The add response carries the ID that SailPoint assigns, or for `Account`, the ID of the queued task. The new entry appears under SailPoint's ID, not under the placeholder, so a read of the DN that you added doesn't find it.

To create an object, supply its required attributes:

| Object type | Required for create |
| --- | --- |
| `AccessProfile` | `name`, `owner` with an `id`, `source`, and, when `enabled` is `true`, a non-empty `entitlements` value |
| `Account` | `attributes`, which must include `sourceId`. You can also supply `sourceId` as a separate attribute, and the connector copies it into `attributes`. |
| `Entitlement` | `name`, `source` with an `id`, `sourceSchemaObjectType`, and `attribute` |
| `PublicIdentity` | Not applicable (read-only) |
| `Role` | `name` and `owner` with an `id` |
| `Source` | `name`, `owner`, and `connector` |

The connector checks these attributes before it calls SailPoint and returns `OPERATIONS_ERROR (1)` with the name of the missing attribute. On create, boolean attributes accept only `true` or `false`, and the connector rejects any other value. If SailPoint accepts a create but returns no ID, the connector returns `OPERATIONS_ERROR (1)`, and the object might still exist.

A base search for an id that does not exist returns `OPERATIONS_ERROR (1)` for every object type.

On modify, the connector sends one LDAP value as text. It sends `true` and `false` as booleans. It sends several LDAP values as a list of strings. It parses a JSON object only for a nested attribute, such as `owner`, when the value starts with `{`. It sends a JSON array in a single LDAP value as text, and it sends each object in a list of values as text rather than as an object.

To change a multivalued string attribute, such as `features` or `segments`, replace the attribute with the complete list, one LDAP value per element. This needs at least two values, because the connector can't send a one-element list. An add sends only the supplied values for the whole attribute and does not append them. On source `features`, an add of several names replaces the current list with those names, and an add of one name is rejected. Deleting one value removes the attribute rather than that one element. On an access profile, that delete clears every entitlement. On source `features`, a delete leaves the current list in place.

For `accessProfiles`, `entitlements`, `additionalOwners`, and `schemas`, SailPoint expects objects. The connector sends each value as a JSON string, and SailPoint rejects the request.

The connector supports automatic schema authoring: the system generates the schema when you create the data source. For more information about each attribute's data type and write support, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **The connector translates search filters in part.** The connector sends SailPoint the clauses that it can translate: equality, approximate match (as equality), `>=`, `<=`, prefix (`value*`), and contains (`*value*`, where SailPoint supports it) on the filterable attributes that the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section lists for each object type. The connector supports only some of these operators on each attribute. Some filterable names aren't schema attributes. Names such as `ownerId` filter on a field inside a nested attribute, and names such as `firstname`, `lastname`, and `dimensional` filter on SailPoint fields that the connector doesn't return. It drops other clauses, such as `NOT` and other substring shapes, and applies the rest, so a search can return more or fewer entries than its filter asks for. When the connector can't translate any clause, the search returns no entries. The connector ignores `objectClass` clauses, so a filter such as `(objectClass=Account)` on its own returns no entries. Use `(objectClass=*)` instead. |
| 2 | **The connector evaluates presence filters itself.** When a filter contains a presence clause, such as `(objectClass=*)`, the connector sends no filter, reads every page of the collection, and evaluates the filter itself. Inside an `AND` or `OR` clause, it ignores clauses on attributes that aren't filterable. When it can't evaluate any clause, for example `(description=*)` on `AccessProfile`, the search returns no entries. For `Account`, `sourceName` and `sourceOwner` match no entries in this mode, because the account record has no `source.displayableName` and no `source.authoritative`. This adds API requests, and local matching is case-sensitive. |
| 3 | **Some search failures return no entries instead of an error.** When the request that counts the results fails, or when a request that reads a page for presence-filter evaluation fails, the search returns an empty result. The connector only logs the failure. |
| 4 | **The connector uses a fixed sort order.** The connector sorts `Account` lists by `id` and the other object types by `name`. You can't change the sort order. |
| 5 | **Entitlements use the SailPoint Beta API.** The connector reads and writes entitlements through `/beta/entitlements`. Radiant Logic recommends that you retest entitlement operations after SailPoint upgrades your tenant. |
| 6 | **Role searches send one extra request for each role.** To fill `roleAssignedIdentities`, the connector reads each role's assigned identities with a request that isn't paged, so the value might hold only SailPoint's first page of identities. |
| 7 | **The connector reads an object before it modifies a nested attribute.** For `AccessProfile`, `Account`, `Entitlement`, `Role`, and `Source`, when the new value of a nested attribute, such as `owner`, is a JSON object, the connector first reads the current object and then sends a `replace` operation for each changed subfield. Subfields that the new value leaves out keep their current values. When no subfield changed, the connector returns success and does not call SailPoint. |
| 8 | **The connector adds no retry logic.** A `429` or most `5xx` responses from SailPoint fail the operation with `OPERATIONS_ERROR (1)`, except in the search cases that limitation 3 describes. |
| 9 | **The connector uses fixed timeouts.** The connect and read timeouts are 30 seconds each, and no data source property changes them. |
| 10 | **Modify operation acts on whole multivalued attributes.** It doesn't append values. It replaces the current values with the ones you supply. Deleting one value can remove every value. For example, deleting one entitlement from an access profile removes all of its entitlements. To change a multivalued string attribute, such as `segments` or `features`, replace it with the complete list, using at least two values. For more information, see the [Schema overview](#schema-overview) section of this document. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 1.0.1 | Unreleased | First official release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns, so the following tables focus on write support. In the 'Create' and 'Modify' columns, a checkmark (✔) marks an attribute that you can write during that operation, and a blank cell marks one that you can't:

- **Create**: the attribute can be set when the object is created.
- **Modify**: the attribute can be changed on an existing object.

An attribute with a blank cell in both columns is read-only.

The 'Nullable' column indicates whether the attribute must have a value. An attribute marked with a checkmark (✔) is allowed to have a null value; one with a blank cell must have a value on every entry.

Some attributes represent a number or a nested object, but the schema declares them as `string`. To help you identify the original type, this document adds a qualifier in the 'Data type' column:

- `string` (number)
- `string` (JSON object)

An attribute that can carry several values says "Multivalued attribute." in its description. The two are independent: a multivalued attribute still takes the qualifier that its individual values need.

For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

### Object type: `AccessProfile`

Search filters can use `id`, `name`, `created`, `modified`, `requestable`, `ownerId` (or `owner.id`), and `sourceId` (or `source.id`).

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. SailPoint assigns the ID. On create, supply any placeholder value; the connector ignores it. |
| `name` | `string` | ✔ | ✔ | | |
| `description` | `string` | ✔ | ✔ | ✔ | |
| `created` | `string` | | | ✔ | |
| `modified` | `string` | | | ✔ | |
| `enabled` | `boolean` | ✔ | ✔ | ✔ | Whether the access profile is enabled. SailPoint sets it to `false` when you leave it unset. Setting it to `true` requires a non-empty `entitlements` value. |
| `owner` | `string` (JSON object) | ✔ | ✔ | | The owner, which must carry an `id`. On create, you can supply `ownerId`, `ownerName`, and `ownerType` instead; `ownerType` defaults to `IDENTITY`. When both forms are present, the connector uses the JSON value. SailPoint checks a supplied owner name against the owner's display name and doesn't use the name to find the owner. |
| `source` | `string` (JSON object) | ✔ | ✔ | | The access profile's source. On create, you can supply `sourceId`, `sourceName`, and `sourceType` instead; `sourceType` defaults to `SOURCE`. When both forms are present, the connector uses the JSON value. The connector sends a source that carries only a name, but Radiant Logic recommends that you supply `sourceId`. |
| `entitlements` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. Required when `enabled` is `true`. On modify, the connector sends each value as a JSON string, not an object, and SailPoint rejects the request. Deleting one entitlement removes the attribute, and the access profile then has no entitlements. |
| `requestable` | `boolean` | ✔ | ✔ | ✔ | Whether users can request the access profile through an access request. SailPoint sets it to `true` when you leave it unset, and rejects `false` unless the tenant is enabled for the new Request Center. |
| `accessRequestConfig` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `revocationRequestConfig` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `segments` | `string` | ✔ | ✔ | ✔ | Multivalued attribute. On modify, two or more LDAP values are sent as a list. One value, and a JSON array in a single value, are sent as text. Deleting one value removes the attribute. |
| `accessModelMetadata` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `provisioningCriteria` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `additionalOwners` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. On modify, the connector sends each value as a JSON string, not an object, and SailPoint rejects the request. Deleting one value removes the attribute. |

### Object type: `Account`

Search filters can use `id`, `name`, `created`, `modified`, `identityId`, `nativeIdentity`, `sourceId`, `sourceName` (or `source.displayableName`), `sourceOwner`, `sourceConnectionType`, `hasEntitlements`, `uncorrelated`, `manuallyCorrelated`, `entitlements`, `origin`, `identityName` (or `identity.name`), `identity.correlated`, and `identity.identityState`. Without a presence clause, `sourceName` filters on the source's display name, and `sourceOwner` filters on whether the source is authoritative, not on the owner. In a filter that also contains a presence clause, such as `(objectClass=*)`, `sourceName` and `sourceOwner` match no accounts.

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. SailPoint assigns the ID. On create, supply any placeholder value; the connector ignores it. |
| `name` | `string` | | | ✔ | SailPoint derives the name from the source's schema attributes. |
| `created` | `string` | | | ✔ | |
| `modified` | `string` | | | ✔ | |
| `sourceId` | `string` | ✔ | | ✔ | The ID of the account's source. On create, the connector copies it into `attributes` unless `attributes` already carries it. |
| `sourceName` | `string` | | | ✔ | The source's display name. Without a presence clause, a filter on `sourceName` selects accounts by that display name. A filter that also contains a presence clause matches no accounts on `sourceName`. |
| `identityId` | `string` | | ✔ | ✔ | |
| `cloudLifecycleState` | `string` | | | ✔ | |
| `identityState` | `string` | | | ✔ | |
| `connectionType` | `string` | | | ✔ | |
| `isMachine` | `boolean` | | | ✔ | |
| `recommendation` | `string` (JSON object) | | | ✔ | |
| `attributes` | `string` (JSON object) | ✔ | ✔ | | The source's own schema attributes, whose keys vary by source. On create, the connector sends this value as the request's `attributes` object. It accepts scalar values only: the connector sends numbers and booleans as strings and rejects a nested object or array. |
| `authoritative` | `boolean` | | | ✔ | |
| `description` | `string` | | | ✔ | |
| `disabled` | `boolean` | | | ✔ | |
| `locked` | `boolean` | | | ✔ | |
| `nativeIdentity` | `string` | | | ✔ | |
| `systemAccount` | `boolean` | | | ✔ | |
| `uncorrelated` | `boolean` | | | ✔ | |
| `uuid` | `string` | | | ✔ | |
| `manuallyCorrelated` | `boolean` | | ✔ | ✔ | |
| `hasEntitlements` | `boolean` | | | ✔ | |
| `identity` | `string` (JSON object) | | | ✔ | |
| `sourceOwner` | `string` (JSON object) | | | ✔ | The source owner. Without a presence clause, a filter on `sourceOwner` selects accounts by whether the source is authoritative, not by the owner. A filter that also contains a presence clause matches no accounts on `sourceOwner`. |
| `features` | `string` | | | ✔ | |
| `origin` | `string` | | | ✔ | |
| `ownerIdentity` | `string` (JSON object) | | | ✔ | |

### Object type: `Entitlement`

Search filters can use `id`, `name`, `created`, `modified`, `requestable`, `type`, `attribute`, `value`, `ownerId` (or `owner.id`), and `sourceId` (or `source.id`).

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. SailPoint assigns the ID. On create, supply any placeholder value; the connector ignores it. |
| `name` | `string` | ✔ | ✔ | | |
| `created` | `string` | | | ✔ | |
| `modified` | `string` | | | ✔ | |
| `attribute` | `string` | ✔ | | | |
| `value` | `string` | ✔ | | ✔ | SailPoint requires a value unless the source schema object type is a permission type. The connector can't tell which types those are, so it leaves the check to SailPoint. Supply a value unless the entitlement is a permission. |
| `sourceSchemaObjectType` | `string` | ✔ | | | |
| `privileged` | `boolean` | ✔ | ✔ | ✔ | |
| `cloudGoverned` | `boolean` | | | ✔ | SailPoint sets this value. |
| `description` | `string` | ✔ | ✔ | ✔ | |
| `requestable` | `boolean` | ✔ | ✔ | ✔ | |
| `attributes` | `string` (JSON object) | ✔ | | ✔ | |
| `source` | `string` (JSON object) | ✔ | | | The entitlement's source, which must carry an `id` because SailPoint resolves it by ID. On create, you can supply `sourceId`, `sourceName`, and `sourceType` instead; `sourceType` defaults to `SOURCE`. When both forms are present, the connector uses the JSON value. |
| `owner` | `string` (JSON object) | ✔ | ✔ | ✔ | The owner, which must carry an `id` because SailPoint resolves it by ID. On create, you can supply `ownerId`, `ownerName`, and `ownerType` instead; `ownerType` defaults to `IDENTITY`. When both forms are present, the connector uses the JSON value. |
| `directPermissions` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. |
| `segments` | `string` | ✔ | ✔ | ✔ | Multivalued attribute. On modify, two or more LDAP values are sent as a list. One value, and a JSON array in a single value, are sent as text. Deleting one value removes the attribute. |
| `manuallyUpdatedFields` | `string` (JSON object) | | ✔ | ✔ | SailPoint keeps this record of fields edited after the entitlement was aggregated. |
| `accessModelMetadata` | `string` (JSON object) | ✔ | | ✔ | |

### Object type: `PublicIdentity`

Search filters can use `id`, `alias`, `email`, `firstname`, and `lastname`.

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. |
| `name` | `string` | | | | |
| `alias` | `string` | | | ✔ | |
| `email` | `string` | | | ✔ | |
| `status` | `string` | | | ✔ | |
| `identityState` | `string` | | | ✔ | |
| `manager` | `string` (JSON object) | | | ✔ | |
| `attributes` | `string` (JSON object) | | | ✔ | Multivalued attribute. |

### Object type: `Role`

Search filters can use `id`, `name`, `created`, `modified`, `requestable`, `dimensional`, and `ownerId` (or `owner.id`).

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. SailPoint assigns the ID and rejects a create that carries one. On create, supply any placeholder value; the connector ignores it. |
| `name` | `string` | ✔ | ✔ | | |
| `description` | `string` | ✔ | ✔ | ✔ | |
| `enabled` | `boolean` | ✔ | ✔ | ✔ | SailPoint sets it to `false` when you leave it unset. |
| `owner` | `string` (JSON object) | ✔ | ✔ | | The owner, which must carry an `id` because SailPoint resolves it by ID. On create, you can supply `ownerId`, `ownerName`, and `ownerType` instead; `ownerType` defaults to `IDENTITY`. When both forms are present, the connector uses the JSON value. |
| `accessProfiles` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. On modify, the connector sends each value as a JSON string, not an object, and SailPoint rejects the request. |
| `entitlements` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. On modify, the connector sends each value as a JSON string, not an object, and SailPoint rejects the request. |
| `roleAssignedIdentities` | `string` (JSON object) | | | ✔ | Multivalued attribute. The identities that are assigned to the role. The connector reads them with a separate request for each role. |

### Object type: `Source`

Search filters can use `id`, `name`, `created`, `modified`, `description`, `type`, `status`, `category`, `authoritative`, `features`, `connectionType`, `connectorName`, `ownerId` (or `owner.id`), and `managementWorkgroupId` (or `managementWorkgroup.id`).

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | | Primary key and RDN. SailPoint assigns the ID. On create, supply any placeholder value; the connector ignores it. |
| `name` | `string` | ✔ | ✔ | | |
| `description` | `string` | ✔ | ✔ | ✔ | |
| `owner` | `string` (JSON object) | ✔ | ✔ | | The owner. On create, you can supply `ownerId`, `ownerName`, and `ownerType` instead; `ownerType` defaults to `IDENTITY`. When both forms are present, the connector uses the JSON value. Unlike `AccessProfile` and `Role`, the connector doesn't require an `id` for a source's owner. It sends an owner that carries only a name, and SailPoint decides whether to accept it. Radiant Logic recommends that you supply `ownerId`. |
| `cluster` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `accountCorrelationConfig` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `accountCorrelationRule` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `managerCorrelationMapping` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `managerCorrelationRule` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `beforeProvisioningRule` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `schemas` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. On modify, the connector sends each value as a JSON string, not an object, and SailPoint rejects the request. |
| `passwordPolicies` | `string` (JSON object) | ✔ | | ✔ | Multivalued attribute. |
| `features` | `string` | ✔ | ✔ | ✔ | Multivalued attribute. On create, supply a JSON array, for example `["PROVISIONING"]`. On modify, supply at least two feature names, one LDAP value each. A single value, including `["PROVISIONING"]`, is sent as text, and SailPoint rejects it. Several LDAP values are sent as a list, and SailPoint stores that list. An add of several names replaces the current list with those names. An add of one name is rejected. Deleting `features` leaves the current list in place. |
| `type` | `string` | ✔ | | ✔ | |
| `connector` | `string` | ✔ | | | The connector type name, for example `delimited-file`. |
| `connectorClass` | `string` | ✔ | | ✔ | |
| `connectorAttributes` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `deleteThreshold` | `string` (number) | ✔ | ✔ | ✔ | An integer with no extra characters, such as `10`. On create, the connector omits any other value, including ` 21`, `10.5`, and `abc`, and still creates the source. On modify, the connector sends the text you supply. SailPoint stores `21` and ` 21` as the integer 21, and rejects `abc` and `10.5`. |
| `authoritative` | `boolean` | ✔ | | ✔ | |
| `managementWorkgroup` | `string` (JSON object) | ✔ | ✔ | ✔ | |
| `healthy` | `boolean` | | | ✔ | SailPoint sets this from the source health check. A value sent on create is replaced with SailPoint's own value. SailPoint rejects a modify of this value. |
| `status` | `string` | | | ✔ | |
| `since` | `string` | | | ✔ | SailPoint sets this to the time of its health check. A value sent on create is not kept. A modify returns success and leaves the current timestamp in place. |
| `connectorId` | `string` | ✔ | | ✔ | |
| `connectorName` | `string` | ✔ | | ✔ | |
| `connectionType` | `string` | ✔ | | ✔ | |
| `connectorImplementationId` | `string` | ✔ | | ✔ | |
| `created` | `string` | | | ✔ | An ISO 8601 timestamp. |
| `modified` | `string` | | | ✔ | An ISO 8601 timestamp. |
| `credentialProviderEnabled` | `boolean` | ✔ | ✔ | ✔ | |
| `category` | `string` | ✔ | ✔ | ✔ | |
