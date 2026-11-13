# Okta connector

This document describes the Radiant Logic custom connector for Okta, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector uses the Radiant Logic Connector SDK and integrates with Okta through the Okta REST API. It virtualizes Okta applications, groups, and users as three object types. It supports search for every object type, create and delete for `groups` and `users`, and limited modify for every object type. For `apps`, delete is limited and create isn't supported.

[Download the Okta connector JAR file](builds/okta-connector-1.0.0.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Okta</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector type</th><td>SDK</td>
  </tr>
  <tr>
    <th scope="row" align="left">Latest version</th><td>1.0.0</td>
  </tr>
  <tr>
    <th scope="row" align="left">Connector SDK version</th><td>2.0.0</td>
  </tr>
</table>

### Identity Data Management support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>9.0.0 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic, Real-time</td>
  </tr>
</table>

Real-time cache refresh is supported through a custom Identity Data Management implementation, not through the Connector SDK.

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>Not supported</td>
  </tr>
  <tr>
    <th scope="row" align="left">Documentation</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Mapping profile</th><td>Not available</td>
  </tr>
  <tr>
    <th scope="row" align="left">Agentic source</th><td>No</td>
  </tr>
</table>

## Configuration walkthrough

To configure Okta and RadiantOne, complete the steps in the following sections.

### Preconditions

- An Okta tenant that RadiantOne can reach over HTTPS, such as <code>https://<var>ORG_SUBDOMAIN</var>.okta.com</code>.
- An administrator account that can generate a static Okta API token, which the connector uses instead of OAuth 2.0.
- Optional: The host and port of an outbound HTTP proxy, when the environment requires one to reach Okta.

### Configure Okta

1. Sign in to the Okta Admin Console as an administrator.
2. Go to **Security > API**, click the **Tokens** tab, and then create an API token.
3. Copy the token value immediately, because Okta shows it only at creation time. This value becomes the `apitoken` data source property.
4. Record the tenant's base URL, such as <code>https://<var>ORG_SUBDOMAIN</var>.okta.com</code>. This value becomes the `baseUrl` data source property.

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v9/configuration/data-sources/data-sources/#creating-templates) using the Okta connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file. If a template named **Okta** already exists, don't upload the new JAR file to it. Follow [Upgrade an SDK connector](../readme.md#upgrade-an-sdk-connector) instead.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v9/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **Okta** template.
3. Fill in the data source properties. For more information, see the [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches Okta.
5. In **File Manager**, upload the [`oktaclient-default-schema.orx` file](resources/oktaclient-default-schema.orx), and then click **Link Schema to Data Source**. After you link the schema, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector uses the following data source properties. The properties are grouped into the **Connection Info**, **Request Metering**, and **Proxy** sections.

### Connection Info

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `baseUrl` | Yes | `string` | `https://[your_org].okta.com` | Any | The base URL of your Okta tenant, which must be an HTTPS URL. The connector sends every API request to this host. The default is a placeholder, so replace it with your tenant's URL. |
| `apitoken` | Yes | `password` | None | Any | The Okta API token that the connector authenticates with. Generate it in the Okta Admin Console. |
| `timeout` | No | `number` | None | Any | The maximum time, in seconds, that the connector spends retrying a rate-limited request. The connector checks this limit before each retry and doesn't interrupt a request in progress. When you leave the value empty, the connector uses 120 seconds. |

### Request Metering

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `ratelimit` | Yes | `number` | `50` | Any | The assumed maximum number of requests per minute. The connector doesn't throttle requests with this value. The connector uses the value only to cap the `groups` page size at 30% of `ratelimit`, rounded down, because each group needs an extra membership request. A `ratelimit` below 4 removes the cap. |
| `maxretries` | Yes | `number` | `2` | Any | The maximum number of times that the connector retries a request that Okta rate-limits. Each retry waits about 60 seconds and counts against `timeout`, so the default `maxretries` value and the 120-second `timeout` fallback allow one retry. When no retries remain, the request fails. A negative value makes every request fail. |

### Proxy

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `proxy` | No | `string` | None | Any | The <code><var>HOST</var>:<var>PORT</var></code> address of an outbound proxy. When RadiantOne reaches Okta directly, leave this value empty. A malformed value causes the connection to fail. |
| `proxyssl` | No | `string` | None | Any | Whether to connect to the proxy over HTTPS. The value `true` selects HTTPS. The connector reads this value but doesn't apply it in this release, so the connector always connects to the proxy over HTTP. |

## Supported operations

The following table summarizes the operations that the connector supports for its object types. Each cell shows one of three support levels:

- **Yes**: fully supported
- **Limited**: supported with caveats
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `apps` | Yes | No | Limited | Limited |
| `groups` | Yes | Yes | Limited | Yes |
| `users` | Yes | Yes | Limited | Yes |

The [Operation details](#operation-details) section explains cells marked _No_ or _Limited_. For more information about attribute-level detail, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `apps`, `groups`, `users` | Search | Yes | The connector pages through results with Okta cursors at 200 entries per page, and no property sets the page size directly. For `groups`, the connector caps the page size at 30% of `ratelimit`, rounded down, unless `ratelimit` is below 4. For `apps`, a base search passes the RDN value to Okta as the application ID without resolving the `label` value first. For every object type, the connector translates a filter on `lastupdated` into a greater-than-or-equal comparison. |
| `apps` | Create | No | The connector sends a create request, but it doesn't map the application's `name` or the RDN's `label` value into it, so the operation doesn't produce an application entry. |
| `apps` | Modify | Limited | The connector passes the RDN value to Okta as the application ID without resolving the `label` value first. As a result, a modification against an <code>app=<var>LABEL</var></code> RDN fails to find the entry. A change to `status` starts an Okta lifecycle transition rather than a field update: `ACTIVE` activates the application, `INACTIVE` deactivates it, and `DELETED` deletes it. The connector accepts the change only as a replace operation. |
| `apps` | Delete | Limited | The connector passes the RDN value to Okta as the application ID without resolving the `label` value first. As a result, a deletion against an <code>app=<var>LABEL</var></code> RDN fails to find the entry. |
| `groups` | Modify | Limited | The connector rejects every modification to a group whose `type` is `BUILT_IN` or `APP_GROUP`. It applies `member` changes as separate assign and unassign requests, one for each value, so a partial failure leaves some values applied. A replacement of `profile-name` returns success, but the connector discards it and never sends it to Okta. |
| `users` | Modify | Limited | A replacement of `profile-login` returns success, but the connector discards it and never sends it to Okta. A change to `status` starts an Okta lifecycle transition, such as activate, deactivate, suspend, or unlock, rather than a field update. The connector accepts the change only as a replace operation. |

## Schema overview

The schema defines three object types:

- `apps`
- `groups`
- `users`

The primary key is `id` for `apps` entries, `profile-name` for `groups` entries, and `profile-login` for `users` entries. The _relative distinguished name (RDN)_ attribute is `app` for `apps`, `group` for `groups`, and `user` for `users`. The RDN value is the `label` value for `apps` entries, such as <code>app=<var>LABEL</var></code>, and the primary key for `groups` and `users` entries, such as <code>user=<var>LOGIN</var></code>.

On create, give the RDN and the primary key attribute the same value. The connector maps the RDN value into the primary key attribute. If you supply an RDN of <code>user=<var>LOGIN</var></code> with a different `profile-login` value, either value can reach Okta, so the outcome is unpredictable. The same applies to a `groups` entry and its `profile-name` attribute.

To create an object, supply its required attributes:

| Object type | Required for create |
| --- | --- |
| `apps` | Not applicable (create not supported) |
| `groups` | `profile-name` |
| `users` | `profile-login`, `profile-email`, `profile-firstname`, `profile-lastname` |

Okta requires these attributes and rejects a request that lacks one. The connector doesn't validate them before it sends the request.

The connector doesn't generate its schema. It ships the schema as the `oktaclient-default-schema.orx` file, which you upload and link to the data source. For more information, see the [Configure RadiantOne](#configure-radiantone) section of this document. For more information about each attribute's data type and write support, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the provided schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **The connector doesn't verify bind credentials.** It accepts every LDAP bind request and returns success without checking the password against Okta. Don't use this data source as an authentication source. |
| 2 | **Authentication to Okta uses a static API token only.** The connector doesn't support OAuth 2.0 or any other credential type. |
| 3 | **A rate-limited request always waits about 60 seconds before the connector retries it.** The connector ignores the rate-limit reset headers that Okta returns. The `maxretries` and `timeout` properties bound how many retries the connector attempts, not how long each wait lasts. |
| 4 | **Some rate-limited requests can report failure after they succeed.** This limitation affects deletions, group membership changes, and every application status transition. It also affects the user deactivate, suspend, unsuspend, and unlock transitions. When the connector retries one of these requests and the retry succeeds, it still returns the original rate-limit error. |
| 5 | **The connector strips `objectClass` clauses from every search filter.** Okta has no equivalent concept, so a filter on `objectClass` doesn't narrow the results. When `objectClass` is the only clause, the connector sends no filter for `apps` and `groups`, and only the status clause for `users`. |
| 6 | **The connector ignores a search filter that it can't parse.** It logs the error and runs the search without a filter, so the search returns every entry of the object type. |
| 7 | **By default, searches on `users` return accounts in every Okta status.** Unless the filter references the `status` attribute, the connector adds a clause that matches all eight statuses. As a result, deprovisioned, suspended, and locked-out accounts appear in the results. |
| 8 | **The connector doesn't implement the LDAP compare and invoke operations.** |
| 9 | **An attribute-mapping failure during a create or a modify returns success.** The connector logs the error and returns success without writing anything to Okta. |
| 10 | **Backend error details don't reach the LDAP caller.** A failed operation returns `NO_SUCH_OBJECT (32)` in two cases: Okta returns no entry for a base search, or an ID lookup gets an HTTP `404 Not Found` status code. In most other cases, including a modify or delete whose `profile-login` or `profile-name` matches no entry, the operation returns a generic `OPERATIONS_ERROR (1)`. Some failures reach the caller as an error that the connector doesn't map to an LDAP result code. Examples include an error on a later page of search results and a modify with an unrecognized `type` or `signonmode` value. |
| 11 | **The connector logs Okta error reasons under one logger only.** When the connector handles an Okta error, it logs the reason only under the `com.rli.scripts.customobjects.oktaclient` logger at `ERROR` level. **Test Connection** reports its own failure reason. |
| 12 | **Okta applies changes with eventual consistency.** A change, including one that the connector makes, can take several minutes to appear in search results. This delay mostly affects cache refresh. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 1.0.0 | November 13, 2026 | First official release. |

## Appendix A: Attribute reference

Identity Data Platform provides search (read) support for every attribute that the connector returns, so the following tables focus on write support. In the 'Create' and 'Modify' columns, a checkmark (✔) marks an attribute that you can write during that operation, and a blank cell marks one that you can't:

- **Create**: the attribute can be set when the object is created.
- **Modify**: the attribute can be changed on an existing object.

An attribute with a blank cell in both columns is read-only.

The 'Nullable' column indicates whether the attribute must have a value. An attribute marked with a checkmark (✔) is allowed to have a null value; one with a blank cell must have a value on every entry.

Some attributes represent a boolean, but the schema declares them as `string`. To help you identify what each value represents, this document adds a qualifier in the 'Data type' column:

- `string` (boolean)

An attribute that can carry several values says "Multivalued attribute." in its description. The two are independent: a multivalued attribute still takes the qualifier that its individual values need.

For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

### Object type: `apps`

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `id` | `string` | | | ✔ | Primary key. System-generated identifier. Base searches, modifications, and deletions resolve the entry by this value. |
| `label` | `string` | | ✔ | | RDN value, in the form <code>app=<var>LABEL</var></code>. The schema also declares the alias `displayName` for this attribute. |
| `accessibility-errorredirecturl` | `string` | | ✔ | ✔ | |
| `accessibility-loginredirecturl` | `string` | | ✔ | ✔ | |
| `accessibility-selfservice` | `string` (boolean) | | ✔ | ✔ | |
| `created` | `string` | | | ✔ | Okta sets this value. |
| `features` | `string` | | ✔ | ✔ | Multivalued attribute. On modify, the connector writes only the first value of each change, and a replacement adds that value to the application's feature list rather than replacing the list. |
| `lastupdated` | `string` | | | ✔ | Okta sets this value. |
| `name` | `string` | | | | The internal name that Okta uses for the application type. The schema declares it non-nullable, but the connector doesn't return or write this attribute. |
| `signonmode` | `string` | | ✔ | | The application's sign-on mode, such as `SAML_2_0`, `AUTO_LOGIN`, or `BOOKMARK`. |
| `status` | `string` | | ✔ | ✔ | A change starts an Okta lifecycle transition rather than a field update: `ACTIVE` activates the application, `INACTIVE` deactivates it, and `DELETED` deletes it. The connector accepts the change only as a replace operation. |
| `visibility-autosubmittoolbar` | `string` (boolean) | | ✔ | ✔ | |
| `visibility-hide-ios` | `string` (boolean) | | ✔ | ✔ | |
| `visibility-hide-web` | `string` (boolean) | | ✔ | ✔ | |

### Object type: `groups`

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `profile-name` | `string` | ✔ | | | Primary key and RDN. The connector discards a replacement of this attribute on modify and returns success. To rename a group, use the Okta Admin Console. |
| `created` | `string` | | | ✔ | Okta sets this value. |
| `id` | `string` | | | ✔ | System-generated identifier. |
| `lastmembershipupdated` | `string` | | | ✔ | Okta sets this value. |
| `lastupdated` | `string` | | | ✔ | Okta sets this value. |
| `member` | `string` | | ✔ | ✔ | Multivalued attribute. On read, each value is a member's `profile-login`. On modify, each value can be a `profile-login` or an Okta user ID, and it must resolve to exactly one Okta user. A replacement assigns the listed users without unassigning members that the group already has. |
| `profile-description` | `string` | ✔ | ✔ | ✔ | |
| `type` | `string` | ✔ | ✔ | ✔ | The Okta group type, such as `OKTA_GROUP`, `APP_GROUP`, or `BUILT_IN`. The connector rejects every modification to a group of type `BUILT_IN` or `APP_GROUP`. |

### Object type: `users`

| Attribute name | Data type | Create | Modify | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| `profile-login` | `string` | ✔ | | | Primary key and RDN. Okta requires a valid email address. The connector discards a replacement of this attribute on modify and returns success. To rename a user, use the Okta Admin Console. |
| `activated` | `string` | | | ✔ | Okta sets this value. |
| `created` | `string` | | | ✔ | Okta sets this value. |
| `id` | `string` | | | ✔ | System-generated identifier. |
| `lastlogin` | `string` | | | ✔ | Okta sets this value. |
| `lastupdated` | `string` | | | ✔ | Okta sets this value. |
| `password` | `string` | ✔ | ✔ | ✔ | Write-only. Okta never returns the value on read. |
| `passwordchanged` | `string` | | | ✔ | Okta sets this value. |
| `profile-city` | `string` | ✔ | ✔ | ✔ | |
| `profile-costcenter` | `string` | ✔ | ✔ | ✔ | |
| `profile-countrycode` | `string` | ✔ | ✔ | ✔ | |
| `profile-department` | `string` | ✔ | ✔ | ✔ | |
| `profile-displayname` | `string` | ✔ | ✔ | ✔ | |
| `profile-division` | `string` | ✔ | ✔ | ✔ | |
| `profile-email` | `string` | ✔ | ✔ | | |
| `profile-employeenumber` | `string` | ✔ | ✔ | ✔ | |
| `profile-firstname` | `string` | ✔ | ✔ | | |
| `profile-honorificprefix` | `string` | ✔ | ✔ | ✔ | |
| `profile-honorificsuffix` | `string` | ✔ | ✔ | ✔ | |
| `profile-lastname` | `string` | ✔ | ✔ | | |
| `profile-locale` | `string` | ✔ | ✔ | ✔ | |
| `profile-manager` | `string` | ✔ | ✔ | ✔ | |
| `profile-managerid` | `string` | ✔ | ✔ | ✔ | |
| `profile-middlename` | `string` | ✔ | ✔ | ✔ | |
| `profile-mobilephone` | `string` | ✔ | ✔ | ✔ | |
| `profile-nickname` | `string` | ✔ | ✔ | ✔ | |
| `profile-organization` | `string` | ✔ | ✔ | ✔ | |
| `profile-postaladdress` | `string` | ✔ | ✔ | ✔ | |
| `profile-preferredlanguage` | `string` | | | ✔ | When the connector converts an entry for Okta, it lowercases each attribute name and then compares it, case-sensitively, with `preferredLanguage`. The comparison never matches, so a value of this attribute never reaches Okta on create or on modify. |
| `profile-primaryphone` | `string` | ✔ | ✔ | ✔ | |
| `profile-profileurl` | `string` | ✔ | ✔ | ✔ | |
| `profile-state` | `string` | ✔ | ✔ | ✔ | |
| `profile-streetaddress` | `string` | ✔ | ✔ | ✔ | |
| `profile-timezone` | `string` | ✔ | ✔ | ✔ | |
| `profile-title` | `string` | ✔ | ✔ | ✔ | |
| `profile-usertype` | `string` | ✔ | ✔ | ✔ | |
| `profile-zipcode` | `string` | ✔ | ✔ | ✔ | |
| `status` | `string` | | ✔ | ✔ | The connector requests activation of each user that it creates. A change starts an Okta lifecycle transition, such as activate, deactivate, suspend, or unlock, rather than a field update. The connector accepts the change only as a replace operation. |
| `statuschanged` | `string` | | | ✔ | Okta sets this value. |
| `transitioningtostatus` | `string` | | | ✔ | Okta sets this value. It reflects an asynchronous lifecycle transition in progress. |
