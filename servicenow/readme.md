# ServiceNow connector

This document describes the Radiant Logic custom connector for ServiceNow, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector uses the Radiant Logic Connector SDK and integrates with ServiceNow through its REST API. It virtualizes ServiceNow users, groups, and roles, and provides search across all three object types with limited write support.

[Download the ServiceNow connector JAR file](builds/servicenow-connector-1.0.0.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>ServiceNow</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>1.0.0</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector SDK version</th><td>1.1.1</td>
  </tr>
</table>

### Identity Data Management support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>8.3.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic</td>
  </tr>
</table>

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>2.4.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Consuming configuration</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure ServiceNow and RadiantOne, complete the steps in the following sections.

### Preconditions

- A ServiceNow instance (Zurich release) must be accessible with administrative rights.

### Configure ServiceNow

These steps follow the ServiceNow Zurich release. A later release might relabel or reorganize some screens; see the ServiceNow documentation for the current process.

1. Enable the OAuth Client Credentials grant type system property:
   1. Navigate to the System Properties list by opening <code>https://<var>INSTANCE_NAME</var>.service-now.com/sys_properties_list.do</code>.
   2. Click **New**, fill in the following fields, and then click **Submit**:

      | Field | Value |
      | --- | --- |
      | **Name** | `glide.oauth.inbound.client.credential.grant_type.enabled` |
      | **Type** | `true \| false` |
      | **Value** | `true` |

2. Create a REST API Auth Scope:
   1. In the ServiceNow portal, use the global search to find **REST API Auth Scope**, and then click **New**.
   2. Provide a descriptive name, and ensure **Active** is selected.
   3. Under **REST API**, select **Table API**.
   4. Under **Auth Scope**, either create a new OAuth scope or select an existing one, noting the scope name for the Application Registry setup in step 4.
   5. Leave all other default selections unchanged, and then save the record.
3. In the ServiceNow portal, create a new dedicated user with the `admin` role. For least-privilege environments, you can use the combination of `rest_service`, `user_admin`, `ai_user_admin`, and `usage_admin` roles instead. However, the full `admin` role is required if the connector must update the `InternalIntegrationUser` property or assign elevated roles such as `admin` to a user.
4. Register an OAuth client application:
   1. Navigate to **Application Registry \> New \> New Inbound Integration Experience \> New Integration \> OAuth - Client Credentials Grant**.
   2. Provide a name, and select the admin user created in step 3 as the OAuth application user.
   3. Ensure **Active** is selected.
   4. Select the OAuth scope configured in step 2.
   5. Under **Limit authorization to the following APIs**, select **Table API** with **Allow access only to APIs in selected scope**.
   6. Under **Advanced Options**, set the token format to **JWT**.
   7. Note the generated **client ID** and **client secret**.
5. To protect the admin user, create a business rule in the ServiceNow portal that restricts updates and deletions of the OAuth user account (created in step 3) to the admin user itself. This prevents accidental modification and ensures uninterrupted connector operations.
6. Enforce non-blank usernames with business rules:
   1. Because the system uses `username` as the RDN for the user object, create a business rule in ServiceNow that prevents any insert or update that would result in a blank (or whitespace-only) username.
   2. Create similar rules for group names and role names.
7. Remediate existing blank usernames (one-time):
   1. Run a background script in ServiceNow to identify and replace any existing blank usernames with a unique value, for example the user's email address (if unique and mandatory) or `sys_id` as a fallback. The connector fetches records with blank usernames, but the system discards them.
   2. Run a similar script for group names and role names.

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the ServiceNow connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file. If a template named **ServiceNow** already exists, don't upload the new JAR file to it. Follow [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **ServiceNow** template. The system automatically generates a new schema.
3. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches ServiceNow. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties:

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `instance_name` | Yes | `string` | None | Any valid ServiceNow instance name | The ServiceNow instance identifier: the subdomain portion of the instance URL (for example, `devxxxxxx`). |
| `client_id` | Yes | `string` | None | Any | The OAuth client ID generated when registering the integration application in the ServiceNow Application Registry. |
| `client_secret` | Yes | `password` | None | Any | The OAuth client secret generated alongside the client ID in the ServiceNow Application Registry. |
| `page_size` | Yes | `number` | `100` | `100-1000` (inclusive) | The number of records to retrieve per API page when fetching various objects. |
| `batch_size` | Yes | `number` | `25` | `1-50` (inclusive) | The number of parent objects whose related data (user roles, group users, group roles, role child-roles) is fetched in a single batch. |

## Supported operations

The following table summarizes the operations that the connector supports for its object types. Each cell shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `user` | Yes | Yes | Yes | Yes |
| `group` | Yes | No | Limited | No |
| `role` | Limited | No | Limited | No |

The [Operation details](#operation-details) section explains cells marked _No_ or _Limited_. For more
information about attribute-level detail, see the
[Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `group` | Modify | Limited | Only the `groupUsers` membership attribute can be modified, using the `ADD`, `REPLACE`, or `DELETE` modification types. All other group attributes are read-only, including `groupRoles`. |
| `role` | Search | Limited | The connector doesn't return inherited roles when fetching a role's child roles. |
| `role` | Modify | Limited | Only the `childRoles` attribute can be modified, using the `ADD`, `REPLACE`, or `DELETE` modification types. All other role attributes are read-only. |

## Schema overview

The schema defines three object types:

- `group`
- `role`
- `user`

The primary key and _relative distinguished name (RDN)_ is `name` for `group` and `role` entries, and `userName` for `user` entries.

To create an object, supply its required attributes:

| Object type | Required for create |
| --- | --- |
| `group` | Not applicable (read-only) |
| `role` | Not applicable (read-only) |
| `user` | `userName` |

The connector supports automatic schema authoring: the system generates the schema when you create the data source. For more information about each attribute's data type and write support, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **Certain ServiceNow system roles aren't accessible.** Roles reserved for ServiceNow internal use can't be retrieved through the REST API and don't appear when you assign roles in the ServiceNow portal. These roles include `maint`, `nobody`, `security_admin`, `sn_kmf.admin`, `sn_kmf.cryptographic_auditor`, `sn_kmf.cryptographic_integrator`, `sn_kmf.cryptographic_manager`, `sn_kmf.cryptographic_operator`, `sn_secrets.admin`, `sn_secrets.secret_manager`, and `sn_secrets.viewer`. As a result, even with the page size set to 100, a page can return fewer roles (for example, 98 or 96) because ServiceNow filters these roles out. |
| 2 | **Comma in multivalued attribute values.** If a value to link contains a comma, submit it on its own rather than as part of a list; otherwise the connector associates the wrong value. For example, submit a role named `role1,` as a single value, not inside a list such as `[role1,,role2]`. In a list, the connector misreads the trailing comma and links `role1` instead of `role1,`. |
| 3 | **Multivalued attribute format.** The allowed format for a multivalued attribute is a list, `[value1, value2]`, or a single value, `value3`. Don't pass `null` as a value, either on its own or inside a list such as `[value1,null]`. |
| 4 | **Partial success on multivalued operations is non-transactional.** When linking or unlinking multiple values, if the API returns a non-success response partway through the operation, the operation is partially applied and can't be automatically rolled back. You must retry the failed portion manually. For example, a request to link roles `r1`, `r2`, and `r3` might link `r1` and `r2` but fail on `r3` with an internal server error. The connector returns `OPERATION_ERROR`, even though `r1` and `r2` are already linked. As another example, a `REPLACE` of user roles (replacing `r1` with `r2`) first deletes all existing roles, then tries to link `r2`. If an internal server error occurs at that point, the connector returns `OPERATION_ERROR`, but the existing roles are already deleted. Because the operation is non-transactional, you must retry with the full set of roles to link. |
| 5 | **Spurious `sys_id` generation on invalid multivalued links.** ServiceNow might generate a `sys_id` even when you provide an invalid value for a multivalued attribute. The connector treats a returned `sys_id` as a success indicator, so it raises no error. Provide a valid value to link. For example, when you link an invalid user such as `invalid_user` to a valid group such as `group1`, ServiceNow might still return a `sys_id` for the linkage. On receiving the `sys_id`, the connector returns success, even though nothing was linked. |
| 6 | **The connector doesn't return inherited roles.** When fetching user roles, the connector excludes inherited roles from the results. |
| 7 | **Constrained fields might silently accept invalid values.** For fields such as `identityType`, `country`, `gender`, and `introduction`, ServiceNow returns an HTTP `200` status code even when you provide an invalid value, but might not update the field. The connector treats a `200` response as success without verifying the returned value. |
| 8 | **`name` (full name) is computed and can't be set directly.** ServiceNow derives `name` from `firstName` and `lastName`; any attempt to set it directly has no effect. |
| 9 | **ServiceNow applies no default format validation on certain fields.** Fields such as `zip`, `phone`, `mobilePhone`, and `email` accept arbitrary strings unless a ServiceNow administrator has added a business rule to enforce format validation. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 1.0.0 | September 2, 2026 | First official release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns, so the following tables focus on write support. In the 'Create' and 'Modify' columns, a checkmark (✔) marks an attribute that you can write during that operation, and a blank cell marks one that you can't:

- **Create**: the attribute can be set when the object is created.
- **Modify**: the attribute can be changed on an existing object.

An attribute with a blank cell in both columns is read-only.

The 'Nullable' column indicates whether the attribute must have a value. An attribute marked with a checkmark is allowed to have a null value; one with a blank cell must have a value on every entry.

Some attributes represent a boolean, but the connector returns them to Identity Data Platform as strings, so the schema declares them as `string`. To help you identify the original type, this document adds a qualifier in the 'Data type' column:

- `string` (boolean)

For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

### Object type: `group`

| Attribute | Data type | Nullable | Create | Modify | Notes |
| --- | --- | --- | --- | --- | --- |
| active | `string` (boolean) | | | | |
| description | `string` | ✔ | | | |
| groupRoles | `string` | ✔ | | | Multivalued attribute. |
| groupUsers | `string` | ✔ | | ✔ | Multivalued attribute. Supports `ADD`, `REPLACE`, and `DELETE`. If a value contains a comma, it must be submitted individually, not as part of a list. |
| includeMembers | `string` | | | | |
| name | `string` | | | | Used for primary key and RDN. Immutable after creation. |
| source | `string` | ✔ | | | |
| sysCreatedBy | `string` | ✔ | | | |
| sysId | `string` | | | | System-generated identifier. Read-only. |
| type | `string` | ✔ | | | |

### Object type: `role`

| Attribute | Data type | Nullable | Create | Modify | Notes |
| --- | --- | --- | --- | --- | --- |
| canDelegate | `string` (boolean) | ✔ | | | |
| childRoles | `string` | ✔ | | ✔ | Multivalued attribute. Supports `ADD`, `REPLACE`, and `DELETE`. Lists only directly assigned child roles, not inherited ones. |
| description | `string` | ✔ | | | |
| elevatedPrivilege | `string` (boolean) | ✔ | | | |
| federatedId | `string` | ✔ | | | |
| grantable | `string` (boolean) | ✔ | | | |
| includesRoles | `string` | ✔ | | | |
| name | `string` | | | | Used for primary key and RDN. Immutable after creation. |
| scopedAdmin | `string` (boolean) | ✔ | | | |
| suffix | `string` | ✔ | | | |
| sysCreatedBy | `string` | ✔ | | | |
| sysId | `string` | | | | System-generated identifier. |
| sysPolicy | `string` | ✔ | | | |

### Object type: `user`

| Attribute | Data type | Nullable | Create | Modify | Notes |
| --- | --- | --- | --- | --- | --- |
| active | `string` (boolean) | ✔ | ✔ | ✔ | Accepted values: `true`, `false`. Default: `false`. |
| city | `string` | ✔ | ✔ | ✔ | |
| country | `string` | ✔ | ✔ | ✔ | Default allowed values: `GB`, `IT`, `DE`, `BR`, `FR`, `JP`, `US`, `ES`, `CN`. Default: empty string. ServiceNow silently discards invalid codes. An administrator can extend the allowed values through a business rule. |
| email | `string` | ✔ | ✔ | ✔ | ServiceNow applies no format validation by default. An administrator can add a business rule to enforce email format. |
| employeeNumber | `string` | ✔ | ✔ | ✔ | |
| federatedId | `string` | ✔ | | | |
| firstName | `string` | ✔ | ✔ | ✔ | ServiceNow may reject blanking `firstName` if `lastName` is also blank. |
| gender | `string` | ✔ | ✔ | ✔ | Default allowed values: `male`, `female`, `not specified`. Default: empty string. ServiceNow silently discards invalid values. |
| identityType | `string` | ✔ | ✔ | ✔ | Default allowed values: `human`, `ai_agent`, `machine`, `unclassified`. Default: `unclassified`. ServiceNow business rules may block certain transitions, such as from `human` back to `ai_agent`. |
| internalIntegrationUser | `string` (boolean) | ✔ | ✔ | ✔ | Accepted values: `true`, `false`. Default: `false`. Requires `admin` role on the service account. |
| introduction | `string` | ✔ | ✔ | ✔ | Default allowed values: `Mr.`, `Mrs.`, `Ms.`, `Dr.`. Default: empty string. ServiceNow silently discards invalid values. |
| lastName | `string` | ✔ | ✔ | ✔ | ServiceNow may reject blanking `lastName` if `firstName` is also blank. |
| lockedOut | `string` (boolean) | ✔ | ✔ | ✔ | Accepted values: `true`, `false`. Default: `false`. |
| middleName | `string` | ✔ | ✔ | ✔ | |
| mobilePhone | `string` | ✔ | ✔ | ✔ | ServiceNow applies no format validation by default. |
| name | `string` | ✔ | | | ServiceNow computes it from `firstName` and `lastName`. You can't set or modify it directly. |
| phone | `string` | ✔ | ✔ | ✔ | ServiceNow applies no format validation by default. |
| source | `string` | ✔ | ✔ | ✔ | |
| state | `string` | ✔ | ✔ | ✔ | |
| street | `string` | ✔ | ✔ | ✔ | |
| sysId | `string` | | | | System-generated identifier. Read-only. |
| title | `string` | ✔ | ✔ | ✔ | |
| userName | `string` | | ✔ | | Used for primary key and RDN. Default max length is 40 characters. You can't update it after creation. |
| userRoles | `string` | ✔ | | ✔ | Multivalued attribute. Supports `ADD`, `REPLACE`, and `DELETE`. If a value contains a comma, it must be submitted individually, not as part of a list. |
| zip | `string` | ✔ | ✔ | ✔ | ServiceNow applies no format validation by default. |
