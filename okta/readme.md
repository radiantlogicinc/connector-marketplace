# Okta connector

This document describes the Radiant Logic classic connector for Okta, including its resources, configuration, and data source properties.

This connector is built into Identity Data Platform: there is no JAR to download, and it doesn't have a separate version number.

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Okta</td>
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
    <th scope="row" align="left">Mapping profile</th><td><a href="resources/okta-mapping-profile-v1.yaml"><code>okta-mapping-profile-v1.yaml</code></a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure RadiantOne, complete the steps in the following sections.

### Preconditions

- An Okta tenant, such as <code>https://<var>YOUR_ORG</var>.okta.com</code>, and an administrator account for it
- The ability to generate an Okta API token for a suitably privileged administrator account. The connector authenticates with a static API token, not OAuth.
- The host and port of the outbound HTTP or HTTPS proxy, if your environment requires one to reach Okta.
- An Identity Data Platform account with permission to create the data source.

### Configure RadiantOne

1. [Create a data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the built-in **Okta** template.
2. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
3. Run **Test Connection** to confirm the connector reaches Okta. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection Info**, **Request Metering**, and **Proxy** sections.

### Connection Info

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `URL` | Yes | `string` | `https://[your_org].okta.com` | Any | The default string `https://[your_org].okta.com` is a placeholder. Replace it with the base URL of your Okta tenant, such as `https://radiantlogic.okta.com`. |
| `APITOKEN` | Yes | `string` | `[your_api_token]` | Any | The default string `[your_api_token]` is a placeholder. Replace it with the API token that you created in the Okta Admin Console. |
| `MAXRETRIES` | No | `number` | None | Any positive integer | The maximum number of times that the connector retries a failed request. |
| `TIMEOUT` | No | `number` | None | Any positive integer | The request timeout, in seconds. |

### Request Metering

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `RATELIMIT` | Yes | `number` | `50` | Any positive integer | The maximum number of requests per minute that the connector sends to Okta, to help avoid throttling. |

### Proxy

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `PROXY` | No | `string` | None | Any | The address and port of the HTTP proxy, if your organization requires one. |
| `PROXYSSL` | No | `string` | None | Any | The host and port of the HTTPS proxy that the connector uses for SSL or TLS traffic to Okta, if your environment requires one. Use the form <code><var>HOST</var>:<var>PORT</var></code>. |
