# CyberArk Privilege Cloud connector

This document describes the Radiant Logic custom connector for CyberArk Privilege Cloud, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector is built using the Radiant Logic IDDM Connector SDK and integrates with CyberArk Privilege Cloud and CyberArk Identity Services. It virtualizes CyberArk Users, Roles, Safes, Accounts, and Directory Services through the CyberArk REST APIs. It provides search across all object types and create, modify, and delete for Users and Roles.

[Download the latest JAR file](builds/CyberArk_Privilege_Cloud_Connector_v1.0.0.jar).

## Connector identity

This section describes how the connector fits into RadiantOne: its identity and its support for Identity Data Management (IDDM) and Identity Observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>CyberArk Privilege Cloud</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">IDDM Connector SDK version</th><td>1.1.1</td>
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

### Identity Observability support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>2.1.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td><a href="./identity/cyberark-mapping-profile.yaml">cyberark-mapping-profile.yaml</a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Consuming configuration</th><td><a href="./identity/cyberark-consuming-config.yaml">cyberark-consuming-config.yaml</a></td>
  </tr>
</table>

## Configuration walkthrough

Complete steps in the following sections to configure CyberArk and RadiantOne.

### Preconditions

- CyberArk Privilege Cloud (Shared Services) tenant, including Privileged Access Management (PAM) Service and Identity Service, reachable from IDDM over HTTPS.
- CyberArk REST APIs must be enabled with documented base URLs available for the following services:
    - Privilege Cloud: <code>https://<var>SUBDOMAIN</var>.privilegecloud.cyberark.cloud/PasswordVault/API/</code>
    - Identity: <code>https://<var>IDENTITY_TENANT_ID</var>.id.cyberark.cloud/</code>

### Configure CyberArk

1. Create a CyberArk API service user with appropriate administrative permissions: `System Administrator` for token generation and the rights to read and manage Users, Roles, Safes, Accounts, and Directory Services. If the connector must fetch Safe and Account data, the service user also needs the `Privilege Cloud Administrators` role. Select the **Is OAuth confidential client** checkbox for the service user's OAuth client.
2. Create a CyberArk API client application (client ID and client secret) associated with the service user for token generation.

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the CyberArk Privilege Cloud connector JAR file. There is no need to fill in template details—IDDM automatically reads this information from the connector configuration file. The template details appear automatically. If a template named **CyberArk Privilege Cloud** already exists, don't upload the new JAR file to it. Follow [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **CyberArk Privilege Cloud** template. IDDM automatically generates a new schema.
3. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section.

## Data source properties

The connector uses the following data source properties:

| Property             | Required | Type       | Default | Allowed values         | Description                                                                                                                                                                                                  |
|----------------------|----------|------------|---------|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `identity_tenant_id` | Yes      | `string`   | None    | Any                    | CyberArk Identity tenant identifier used in Identity API URLs (for example, `abyxxxx`).                                                                                                                      |
| `client_id`          | Yes      | `string`   | None    | Any                    | Service user username (client ID) for OAuth 2.0 `client_credentials` grant.                                                                                                                                  |
| `client_secret`      | Yes      | `password` | None    | Any                    | Service user password. Generated when creating the service user in CyberArk.                                                                                                                                 |
| `subdomain`          | No       | `string`   | None    | Any                    | Subdomain for the CyberArk Privilege Cloud instance. Used to construct the base URL (<code>https://<var>SUBDOMAIN</var>.privilegecloud.cyberark.cloud</code>). Required for `safe` and `account` operations. |
| `page_size`          | Yes      | `number`   | `100`   | `100-1000` (inclusive) | Page size for list operations such as Safes and Accounts.                                                                                                                                                    |

## Supported operations

The following table summarizes the operations that the connector supports for each object type. Each cell shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type        | Search | Create | Modify | Delete |
|--------------------|:------:|:------:|:------:|:------:|
| `user`             |  Yes   |  Yes   |  Yes   |  Yes   |
| `role`             |  Yes   |  Yes   |  Yes   |  Yes   |
| `safe`             |  Yes   |   No   |   No   |   No   |
| `account`          |  Yes   |   No   |   No   |   No   |
| `directoryservice` |  Yes   |   No   |   No   |   No   |

Cells marked _No_ are explained in the [Operation details](#operation-details) section. Attribute-level detail for each object type is in [Appendix A: Attribute reference](#appendix-a-attribute-reference).

### Operation details

Only operations with caveats are listed. Any object type or operation not shown is fully supported.

| Object type        | Operation              | Support | Details                                                     |
|--------------------|------------------------|---------|-------------------------------------------------------------|
| `safe`             | Create, Modify, Delete | No      | Exposed as read-only. The connector implements only search. |
| `account`          | Create, Modify, Delete | No      | Exposed as read-only. The connector implements only search. |
| `directoryservice` | Create, Modify, Delete | No      | Exposed as read-only. The connector implements only search. |

## Schema overview

The connector exposes five object types:

- `user`
- `role`
- `safe`
- `account`
- `directoryservice`

The primary key and _relative distinguished name (RDN)_ is `Name` for `user`, `role`, `safe`, and `directoryservice`, and `ID` for `account`.

To create an object, supply its required attributes:

| Object type        | Required for create        |
|--------------------|----------------------------|
| `user`             | `Name`                     |
| `role`             | `Name`                     |
| `safe`             | Not applicable (read-only) |
| `account`          | N/A (read-only)            |
| `directoryservice` | N/A (read-only)            |

The connector supports automatic schema authoring: IDDM generates the schema when the data source is created. See [Appendix A: Attribute reference](#appendix-a-attribute-reference) for each type's attributes, data types, and write support.

> [!CAUTION]
> Schema object (table) names are case-sensitive and must exactly match the object type names listed earlier in this section (for example, `directoryservice`). Set each object's primary key to the attribute marked _Primary key and RDN_ in its [Appendix A: Attribute reference](#appendix-a-attribute-reference) section.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation                                                                                                                                                                                                    |
|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | **Only documented attributes are editable.** Only the documented `user` and `role` attributes can be written. Attempts to set arbitrary or undocumented fields fail.                                          |
| 2 | **The connector supports only one CyberArk API version.** The connector targets one version of the CyberArk Privilege Cloud Shared Services API, so CyberArk API changes might require a connector update.    |
| 3 | **Writes depend on the service user's CyberArk permissions.** All create, modify, and delete operations require sufficient rights for the configured service user. Insufficient permissions result in errors. |

## Change log

The following table records the connector's public release history:

| Version | Date       | Description            |
|---------|------------|------------------------|
| 1.0.0   | 2026-07-13 | Initial public release |

## Appendix A: Attribute reference

IDDM provides search (read) support for every attribute that the connector returns, so the following tables focus on write support. The 'Create' and 'Modify' columns indicate whether an attribute can be written during each object-level write operation:

- **Create**: the attribute can be set when the object is created.
- **Modify**: the attribute can be changed on an existing object.
- **Neither**: the attribute is read-only and can't be written.

Some attributes represent a number, a boolean, a nested object or map, or a list, but the connector returns them to IDDM as strings. IDDM represents these attributes as a `string` in the schema. To help you identify the original type, this document adds a qualifier in the 'Data type' column:
- `string` (number)
- `string` (boolean)
- `string` (JSON)
- `string` (list)

For object-level operation support, see [Supported operations](#supported-operations).

### Object type: `user`

| Attribute name  | Data type           | Nullable | Create | Modify | Notes                             |
|-----------------|---------------------|:--------:|:------:|:------:|-----------------------------------|
| `Name`          | `string`            |    No    |   ✔    |        | Primary key and RDN               |
| `DisplayName`   | `string`            |   Yes    |   ✔    |   ✔    |                                   |
| `ID`            | `string`            |   Yes    |        |        | Backend-generated identifier      |
| `RiskLevelRank` | `string` (number)   |   Yes    |        |        |                                   |
| `Status`        | `string`            |   Yes    |        |        |                                   |
| `MobileNumber`  | `string`            |   Yes    |   ✔    |   ✔    |                                   |
| `Mail`          | `string`            |   Yes    |   ✔    |   ✔    |                                   |
| `Source`        | `string`            |   Yes    |        |        | Indicates the directory or source |

### Object type: `role`

| Attribute name | Data type       | Nullable | Create | Modify | Notes                                                     |
|----------------|-----------------|:--------:|:------:|:------:|-----------------------------------------------------------|
| `Name`         | `string`        |    No    |   ✔    |        | Primary key and RDN                                       |
| `ID`           | `string`        |   Yes    |        |        | Backend-generated role ID                                 |
| `Description`  | `string`        |   Yes    |   ✔    |   ✔    |                                                           |
| `RoleType`     | `string`        |   Yes    |   ✔    |        | Type of role as defined by CyberArk                       |
| `Members`      | `string` (list) |   Yes    |   ✔    |   ✔    | Add or remove individual member entries                   |
| `RoleRights`   | `string` (list) |   Yes    |        |        | Derived from CyberArk role and administrative rights APIs |

### Object type: `safe`

| Attribute name              | Data type          | Nullable | Create | Modify | Notes                                   |
|-----------------------------|--------------------|:--------:|:------:|:------:|-----------------------------------------|
| `Name`                      | `string`           |    No    |        |        | Primary key and RDN                     |
| `Number`                    | `string` (number)  |   Yes    |        |        | Safe number                             |
| `Description`               | `string`           |   Yes    |        |        | Safe description                        |
| `Members`                   | `string` (list)    |   Yes    |        |        | Expanded from the safe members API      |
| `Location`                  | `string`           |   Yes    |        |        | Safe location path                      |
| `ManagingCpm`               | `string`           |   Yes    |        |        |                                         |
| `IsExpiredMember`           | `string` (boolean) |   Yes    |        |        |                                         |
| `Created`                   | `string`           |   Yes    |        |        | Creation timestamp                      |
| `Modified`                  | `string`           |   Yes    |        |        | Last modification timestamp             |
| `Creator`                   | `string`           |   Yes    |        |        | Creator's name                          |
| `IsOlacEnabled`             | `string` (boolean) |   Yes    |        |        | Object-Level Access Control (OLAC) flag |
| `NumberOfVersionsRetention` | `string` (number)  |   Yes    |        |        | Version retention setting               |
| `NumberOfDaysRetention`     | `string` (number)  |   Yes    |        |        | Days retention setting                  |
| `IsAutoPurgeEnabled`        | `string` (boolean) |   Yes    |        |        | Auto purge flag                         |

### Object type: `account`

| Attribute name              | Data type       | Nullable | Create | Modify | Notes                           |
|-----------------------------|-----------------|:--------:|:------:|:------:|---------------------------------|
| `ID`                        | `string`        |    No    |        |        | Primary key and RDN             |
| `Name`                      | `string`        |   Yes    |        |        |                                 |
| `Address`                   | `string`        |   Yes    |        |        | Target system address           |
| `UserName`                  | `string`        |   Yes    |        |        |                                 |
| `PlatformId`                | `string`        |   Yes    |        |        | Platform identifier             |
| `SafeName`                  | `string`        |   Yes    |        |        | Owning safe                     |
| `SecretType`                | `string`        |   Yes    |        |        |                                 |
| `CreatedTime`               | `string`        |   Yes    |        |        | Creation timestamp              |
| `SecretManagement`          | `string` (JSON) |   Yes    |        |        | Management settings             |
| `PlatformAccountProperties` | `string` (JSON) |   Yes    |        |        | Platform-specific properties    |
| `CategoryModificationTime`  | `string`        |   Yes    |        |        | Category modification timestamp |
| `RemoteMachinesAccess`      | `string` (JSON) |   Yes    |        |        | Remote access configuration     |

### Object type: `directoryservice`

| Attribute name         | Data type          | Nullable | Create | Modify | Notes                             |
|------------------------|--------------------|:--------:|:------:|:------:|-----------------------------------|
| `Name`                 | `string`           |    No    |        |        | Primary key and RDN               |
| `Service`              | `string`           |   Yes    |        |        | Directory service type            |
| `DisplayName`          | `string`           |   Yes    |        |        |                                   |
| `Tenant`               | `string`           |   Yes    |        |        | Tenant identifier                 |
| `Status`               | `string`           |   Yes    |        |        |                                   |
| `Config`               | `string`           |   Yes    |        |        | Configuration summary             |
| `SyncRulesConfigured`  | `string` (boolean) |   Yes    |        |        | Whether sync rules are configured |
| `StatusDisplay`        | `string`           |   Yes    |        |        | Display version of status         |
| `Everybody`            | `string` (boolean) |   Yes    |        |        | Visibility flag                   |
| `Description`          | `string`           |   Yes    |        |        |                                   |
| `SyncJobStatus`        | `string`           |   Yes    |        |        |                                   |
| `DisplayNameShort`     | `string`           |   Yes    |        |        |                                   |
| `DirectoryServiceUuid` | `string`           |   Yes    |        |        | Unique identifier                 |
