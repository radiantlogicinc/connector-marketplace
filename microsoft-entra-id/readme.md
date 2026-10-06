# Microsoft Entra ID connector

This document describes the Radiant Logic classic connector for Microsoft Entra ID, including its resources, configuration, and data source properties.

The connector is built into Identity Data Platform: there is no JAR to download, and it doesn't have a separate version number.

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Microsoft Entra ID</td>
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
    <th scope="row" align="left">Mapping profile</th><td><a href="resources/microsoft-entra-id-mapping-profile-v1.yaml"><code>microsoft-entra-id-mapping-profile-v1.yaml</code></a></td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure RadiantOne, complete the steps in the following sections.

### Preconditions

- A Microsoft Entra ID tenant, and its tenant ID or verified domain, which you need to build the `OauthURL` value.
- Outbound network access from RadiantOne to `https://login.microsoftonline.com` and `https://graph.microsoft.com`.
- An app registration in that tenant that RadiantOne uses to authenticate. Note its application (client) ID.
- A credential for the app registration that matches the `Auth_type` value that you plan to use: a client secret, or a certificate and its private key.
- The Microsoft Graph application permissions that this app registration uses. A tenant administrator must grant and consent to these permissions before they take effect.
- An Identity Data Platform account with permission to create the data source.

### Configure RadiantOne

1. [Create a data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the built-in **Microsoft Entra ID** template.
2. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
3. Run **Test Connection** to confirm the connector reaches Microsoft Entra ID. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection Info**, **Proxy**, **Multifactor Authentication**, and **Graph Object Links** sections.

### Connection Info

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `URL` | Yes | `string` | `https://graph.microsoft.com/v1.0` | Any | The Microsoft Graph API URL that the connector calls, for example, `https://graph.microsoft.com/beta`. |
| `Scope` | Yes | `string` | `https://graph.microsoft.com/.default` | Any | The Microsoft Graph API scope. The typical value, `https://graph.microsoft.com/.default`, uses the app's assigned permissions. |
| `OauthURL` | Yes | `string` | <code>https://login.microsoftonline.com/<var>[domain]</var>/oauth2/v2.0/token</code> | Any | The OAuth 2.0 token URL for your tenant: <code>https://login.microsoftonline.com/<var>DOMAIN</var>/oauth2/v2.0/token</code>. Replace <code><var>DOMAIN</var></code> with your Microsoft Entra ID tenant ID or verified domain. |
| `Auth_type` | No | `string` | None | `access_token_with_certificate` or empty | The OAuth authentication method. To authenticate with a certificate, enter `access_token_with_certificate`. To authenticate with the client secret in `Password`, leave this field empty. |
| `OAuth_Cert_Public_Path` | No | `string` | None | Any | Provide the file path to the public certificate if you authenticate with certificates. |
| `OAuth_Cert_Private_Path` | No | `string` | None | Any | Provide the file path to the private key that pairs with the public certificate for certificate-based client authentication. |
| `ClientId` | No | `string` | None | Any | The Application Client ID of the Entra ID app registration RadiantOne should use to obtain tokens. |
| `Username` | Yes | `string` | `[username]` | Any | 	Same as Application Client ID. |
| `Password` | Yes | `password` | None | Any | The client secret of the app registration. |
| `JWT_Assertion_Generator_ClassName` | No | `string` | None | Any | Provide the generator class name if you use a custom Java class to generate JWT assertions for authentication. |
| `Max_Retries_On_Error` | No | `number` | None | Any | The maximum number of retries per request. |
| `Timeout` | No | `number` | None | Any | The timeout duration (in milliseconds). |
| `ConnectionTimeout` | No | `number` | None | Any | The initial HTTP connection timeout duration (in milliseconds). |

### Proxy

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `Proxy` | No | `string` | None | Any | If your organization requires an HTTP web proxy, enter the proxy server host and port (for example, `proxy.example.com:9090`). |
| `ProxySSL` | No | `string` | None | `true`, `false` | If your organization requires an HTTPS web proxy that uses SSL, set the value to `true`. If your organization uses an HTTP proxy, set the value to `false`. |

### Multifactor Authentication

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `MFAEnabled` | No | `boolean` | `false` | `true`, `false` | If your security policy enforces multifactor authentication on the account and you want to fetch MFA data, set this property to `true`. |

### Graph Object Links

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `LINKBYDN` | No | `string` | None | Comma-separated attribute names | The connector returns these link attribute values as _distinguished names (DN)_ only, such as `memberOf`. |
| `LINKBYURL` | No | `string` | None | Comma-separated attribute names | The connector returns these link attributes as relative URLs, such as `memberOf`. |
| `LINKBYDISPLAYNAME` | No | `string` | None | Comma-separated attribute names. | The connector returns these link attributes as display names. For example, to return display names for the `memberOf` attribute, set `LINKBYDISPLAYNAME` to `memberOf`. |

If you leave the `LINKBYDN`, `LINKBYURL`, and `LINKBYDISPLAYNAME` properties empty, then the connector returns each link attribute in all three formats, as separate attributes. For example, the `memberOf` attribute returns DNs, the `memberOfDisplayName` attribute returns display names, and the `memberOfLink` attribute returns relative URLs. When you list an attribute in one of these properties, the connector returns only the chosen format in that attribute and omits the others.
