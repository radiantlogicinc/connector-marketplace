# Microsoft Active Directory connector

This document describes the Radiant Logic classic connector for Microsoft Active Directory, including its resources, configuration, and data source properties.

This connector is built into Identity Data Platform: there is no JAR to download, and it doesn't have a separate version number.

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Active Directory</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>Classic</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>Not applicable</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector SDK version</th><td>Not applicable</td>
  </tr>
</table>

### Identity Data Management support

<table>
  <tr>
    <th scope="row" align="left">Available in</th><td>8.2.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic, Real-time</td>
  </tr>
</table>

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>2.4.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td><a href="user-guide.md">User guide</a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td><a href="resources/microsoft-active-directory-mapping-profile-v1.yaml"><code>microsoft-active-directory-mapping-profile-v1.yaml</code></a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure RadiantOne, complete the steps in the following sections.

### Preconditions

- A Microsoft Active Directory domain with one or more domain controllers reachable from RadiantOne over the network.
- A dedicated service account in the domain to use as the `Bind DN`. Within the data source's `Base DN`, the account needs read access to:
    - The `user` and `group` object classes.
    - The `member` and `memberOf` membership attributes.
- An Identity Data Platform account with permission to create the data source.

### Configure RadiantOne

1. [Create a data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the built-in **Active Directory** template.
2. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
3. Run **Test Connection** to confirm the connector reaches Microsoft Active Directory. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection**, **Advanced**, and **Failover LDAP servers** sections.

### Connection

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `Host` | Yes | `string` | None | A fully qualified domain name | The primary domain controller that the connector connects to. |
| `Port` | Yes | `number` | `0` | Any | The LDAP port that the connector connects to on the domain controller. The default `0` is a placeholder; replace it with the port that the domain controller listens on, typically `389`, or `636` when `SSL/TLS` is on. |
| `Bind DN` | Yes | `string` | None | Any | The distinguished name of the service account that the connector binds as, such as `cn=rl-service,ou=service-accounts,dc=corp,dc=example,dc=com`. |
| `Bind Password` | Yes | `password` | None | Any | The password for the service account. The Identity Data Management service stores the password encrypted. |
| `Base DN` | Yes | `string` | None | Any | The root of every LDAP search that the connector runs, such as `dc=corp,dc=example,dc=com`. |
| `SSL/TLS` | No | `boolean` | `false` | `true`, `false` | When on, the connector encrypts the LDAP connection. When off, the connector sends LDAP traffic unencrypted. |

### Advanced

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `Disable Referral Chasing` | No | `boolean` | `true` | `true`, `false` | When on, the connector ignores the LDAP referrals that the domain controller returns. When off, the connector follows those referrals. |
| `Paged Results Control, page size` | No | `number` | `500` | Any | The number of entries that the connector requests per page when it reads results through the LDAP paged results control. |
| `Verify SSL Certificate Hostname` | No | `boolean` | `false` | `true`, `false` | When on, the connector checks that the domain controller's certificate matches the hostname that the connector connects to. When off, the connector accepts the certificate without that check. The check applies only when the `SSL/TLS` property is on. |

### Failover LDAP servers

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `Failover Servers` | No | `string` | None | Comma-separated fully qualified domain names | The backup domain controllers that the connector connects to when the `Host` domain controller doesn't respond. For more information, see [Active Directory connector failover](https://developer.radiantlogic.com/idm/v8.1/tuning/cache-connector-properties/#active-directory-connector-failover). |
