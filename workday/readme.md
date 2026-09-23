# Workday connector

This document describes the Radiant Logic custom connector for Workday, including configuration, data source properties, supported operations, schema, known limitations, and release history. The connector uses the Radiant Logic Connector SDK and integrates with Workday through its REST API and through Workday Report-as-a-Service custom report web services. It virtualizes Workday workers, jobs, job postings, and supervisory organizations through the REST API, and integration system users, security groups, security group memberships, security domains, and business processes through custom reports. The connector provides search across every object type and is strictly read-only.

[Download the Workday connector JAR file](builds/workday-connector-1.0.0.jar).

## Connector identity

This section describes how the connector fits into RadiantOne Identity Data Platform: its identity and its support for data management and observability. Refer to the following tables for more details.

<table>
  <tr>
    <th scope="row" align="left">Name</th><td>Workday</td>
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
    <th scope="row" align="left">Supported versions</th><td>8.3.1 or later</td>
  </tr>
  <tr>
    <th scope="row" align="left">Cache refresh types</th><td>Periodic</td>
  </tr>
</table>

### Identity Data Platform support

<table>
  <tr>
    <th scope="row" align="left">Supported versions</th><td>Not available</td>
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

To configure Workday and RadiantOne, complete the steps in the following sections.

### Preconditions

- A Workday tenant, reachable from RadiantOne over HTTPS, administered by an account with Security Administrator rights or an equivalent role that has access to the Integration and System functional areas.
- Workday portal credentials for the same tenant, with permission to create and publish custom reports as web services. The connector uses these credentials directly for HTTP basic authentication. These credentials are separate from the integration system user that the REST API uses.
- Access to **Workday Development > Big Data Analytics** in the Workday portal, where you create and publish the custom reports.
- OpenSSL, or an equivalent utility, to generate an X.509 certificate and a private key outside Workday.

### Configure Workday

Each step names the Workday task that does the work and the result it produces. For the field-by-field procedure, see the Workday documentation for your tenant.

#### Configure REST API access

1. Create an _Integration System User (ISU)_ by running the **Create Integration System User** task. Enter a username and a password. Record the username: you need it when you configure the connector. Workday requires the password to save the account, but the connector doesn't use it and authenticates with a bearer token instead.
2. Create an _Integration System Security Group (ISSG)_ by running the **Create Security Group** task and selecting the **Integration System Security Group (Unconstrained)** type. When creating the group, add the newly created ISU.
3. Outside Workday, generate an X.509 certificate and private key. For example:

   ```shell
   openssl req -newkey rsa:2048 -nodes -keyout private.key -x509 -days 365 -out certificate.crt
   ```

   The private key must be an unencrypted PKCS#8 RSA key in PEM format, as shown in the previous example. Save both the `private.key` and `certificate.crt` files. You'll need them later.

4. In Workday, add an API client by running the **Register API Client** task. When registering the client, do the following:

   - Provide a client name.
   - For the **Client Grant Type**, select **JWT Bearer Grant**.
   - In the **Integration System User** field, enter the ISU username.
   - For **Scope (Functional Areas)**, select **Staffing** and **Recruiting**.
   - Select **Create x509 Public Key**, provide a name, and enter the contents of the `certificate.crt` file that you generated earlier.

5. From the **View API Client** page, save the API client information:

   - **Client ID**
   - **Workday REST API Endpoint**

   Workday also displays the client secret once. The connector doesn't use it, so you don't need to save it. You use the client ID and parts of the Workday REST API endpoint when you configure the connector. The endpoint follows the pattern <code>https://<var>INSTANCE_NAME</var>.workday.com/ccx/api/v1/<var>TENANT</var></code>, so you can read `instance_name` and `tenant` from it.

6. Grant the ISSG domain security permissions by running the **Maintain Permissions for Security Group** task. When granting permissions, do the following:

   - Select the ISSG you created earlier.
   - Assign **Get and View access** (read-only) for each domain security policy that the target resources require: Worker, Job, Job Posting, and Supervisory Organization.

7. Run the **Activate Pending Security Policy Changes** task. This task activates all pending security changes tenant-wide, not only the changes that you made for this integration.

#### Configure Workday Report-as-a-Service access

Complete this process once for each of the five reports in the following table. Each report's web-service URL must be the JSON URL; the connector doesn't read the other formats.

1. Run the **Create Custom Report** task.
2. Configure the **Report Details**:

   - Set **Report Type** to **Advanced**.
   - Select **Enable As Web Service**.
   - If **Temporary Report** is selected, then clear it.

3. Configure the **Data Source** options:

   - Set **Data Source** to the value shown in the following table.
   - If **Optimized for Performance** is selected, then clear it.

4. Configure **Additional Info** by creating **Field** to **Column Heading Override XML Alias** mappings. For each entry listed in the 'Field to alias mapping' column, do the following:

   1. Create a **Business Object** row using the value from the 'Business object' column, such as **Workday Account**.
   2. Select the **Field** value. This is the field name in the mapping, such as **Workday ID**.
   3. Enter the **Column Heading Override XML Alias** value. This is the alias in the mapping, such as `id`. Aliases are case-insensitive.
   4. Repeat until you have configured every 'Field to alias mapping' pair for the business object.

5. From **Related Actions**, select **Web Service > View URLs**. Workday displays the report's JSON URL. Copy and save this URL. The 'Connector data source property' column shows a connector property that you configure later. This URL is the value for that connector property.
6. Repeat until you have created all five custom reports in the following table.

<table>
  <thead>
    <tr>
      <th scope="col">Report name</th>
      <th scope="col">Connector data source property</th>
      <th scope="col">Data source</th>
      <th scope="col">Business object</th>
      <th scope="col">Field to alias mapping</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>ISU_USERS</code></td>
      <td><code>integration_system_users_report_url</code></td>
      <td>All Workday Accounts</td>
      <td>Workday Account</td>
      <td>
        <ul>
          <li><b>Workday ID</b>: <code>id</code></li>
          <li><b>Username</b>: <code>username</code></li>
          <li><b>Creation Details</b>: <code>creationDetails</code></li>
          <li><b>Account Disabled</b>: <code>isDisabled</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><code>Security_Groups</code></td>
      <td><code>security_groups_report_url</code></td>
      <td>Security Groups</td>
      <td>Security Group</td>
      <td>
        <ul>
          <li><b>Inactive</b>: <code>isInactive</code></li>
          <li><b>Context Type</b>: <code>scope</code></li>
          <li><b>Type</b>: <code>type</code></li>
          <li><b>Security Group Name</b>: <code>name</code></li>
          <li><b>Workday ID</b>: <code>id</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><code>Security_Group_Members</code></td>
      <td><code>security_groups_memberships_report_url</code></td>
      <td>Security Groups</td>
      <td>Security Group</td>
      <td>
        <ul>
          <li><b>Security Group Name</b>: <code>name</code></li>
          <li><b>Members</b>: <code>members</code></li>
          <li><b>Workday ID</b>: <code>id</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><code>Domains</code></td>
      <td><code>domains_report_url</code></td>
      <td>All Domains</td>
      <td>Domain</td>
      <td>
        <ul>
          <li><b>Description</b>: <code>description</code></li>
          <li><b>Functional Areas</b>: <code>functionalAreas</code></li>
          <li><b>Domain Name</b>: <code>name</code></li>
          <li><b>Workday ID</b>: <code>id</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><code>Business_Processes</code></td>
      <td><code>business_processes_report_url</code></td>
      <td>All Business Process Types</td>
      <td>Business Process Type</td>
      <td>
        <ul>
          <li><b>Description</b>: <code>description</code></li>
          <li><b>Functional Area(s)</b>: <code>functionalAreas</code></li>
          <li><b>Workday ID</b>: <code>id</code></li>
          <li><b>Business Process Type</b>: <code>name</code></li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>

### Configure RadiantOne

1. [Create a custom template](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-templates) using the Workday connector JAR file. There is no need to fill in template details; the system reads them automatically from the connector configuration file.
2. [Create a custom data source](https://developer.radiantlogic.com/idm/v8.1/configuration/data-sources/data-sources/#creating-data-sources), selecting the newly created **Workday** template. The system automatically generates a new schema.
3. Fill in the data source properties. You created values for many of these properties in the [Configure Workday](#configure-workday) steps earlier in this document. For more information, see the [Data source properties](#data-source-properties) section of this document.
4. Run **Test Connection** to confirm the connector reaches Workday. After it succeeds, the data source is ready. Use it to create a naming context, then browse the directory as you would any data source.

## Data source properties

The connector authenticates to Workday in two ways: it signs a JWT assertion with `private_key` for the REST API, and it sends `report_username` and `report_password` as HTTP basic credentials to the custom report web services. Both sets of properties belong to one data source. Every search needs the REST API connection. The connector checks that it holds a valid OAuth token before it fetches a report, so the report-backed object types need both sets of credentials. The connector requests each report URL exactly as you enter it, so every report property must carry the report's JSON URL.

The connector uses the following data source properties:

| Property | Required | Type | Default | Allowed values | Description |
| --- | --- | --- | --- | --- | --- |
| `instance_name` | Yes | `string` | None | Any valid Workday instance name | The Workday instance identifier: the subdomain of the tenant URL. The connector builds the API host and the token endpoint from it. |
| `tenant` | Yes | `string` | None | Any valid Workday tenant ID | The Workday tenant identifier. The connector puts it in every endpoint path and in the token URL that the JWT assertion names as its audience. |
| `client_id` | Yes | `string` | None | Any | The OAuth client ID that Workday generates when you register the API client. The connector sends it as the issuer claim of the JWT assertion. |
| `username` | Yes | `string` | None | Any | The username of the ISU tied to the API client. The connector sends it as the subject claim of the JWT assertion. This is a different credential from `report_username`. |
| `private_key` | Yes | `password` | None | An unencrypted PKCS#8 RSA private key in PEM format | The private key paired with the X.509 certificate that you uploaded to Workday. The connector signs the JWT assertion with it, using RSA-SHA256. The connector can't parse a PKCS#1 key. |
| `page_size` | No | `number` | `100` | `1-100` (inclusive) | The number of records that the connector requests per page. This property applies only to `job`, `jobPosting`, `supervisoryOrganization`, and `worker` searches. |
| `report_username` | Yes | `string` | None | Any valid Workday portal username for the tenant | The Workday portal username that the connector uses for HTTP basic authentication when it calls the custom report web services. This is a different credential from `username`. |
| `report_password` | Yes | `password` | None | Any | The password for `report_username`, used for HTTP basic authentication on the custom report web services. |
| `integration_system_users_report_url` | Yes | `string` | None | An HTTPS URL | The JSON web-service URL of the custom report that returns integration system user data. The connector populates the `integrationsystemuser` object type from this report. |
| `security_groups_report_url` | Yes | `string` | None | An HTTPS URL | The JSON web-service URL of the custom report that returns security group data. The connector populates the `securitygroup` object type from this report. |
| `security_groups_memberships_report_url` | Yes | `string` | None | An HTTPS URL | The JSON web-service URL of the custom report that returns security group membership data. The connector populates the `securitygroupmembership` object type from this report. |
| `domains_report_url` | Yes | `string` | None | An HTTPS URL | The JSON web-service URL of the custom report that returns security domain data. The connector populates the `domain` object type from this report. |
| `business_processes_report_url` | Yes | `string` | None | An HTTPS URL | The JSON web-service URL of the custom report that returns business process data. The connector populates the `businessprocess` object type from this report. |

## Supported operations

The following table summarizes the operations that the connector supports for its object types. Each cell shows one of two support levels:

- **Yes**: fully supported
- **No**: not supported

| Object type | Search | Create | Modify | Delete |
| --- | --- | --- | --- | --- |
| `businessprocess` | Yes | No | No | No |
| `domain` | Yes | No | No | No |
| `integrationsystemuser` | Yes | No | No | No |
| `job` | Yes | No | No | No |
| `jobPosting` | Yes | No | No | No |
| `securitygroup` | Yes | No | No | No |
| `securitygroupmembership` | Yes | No | No | No |
| `supervisoryOrganization` | Yes | No | No | No |
| `worker` | Yes | No | No | No |

For more information about attribute-level detail, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

### Operation details

| Object type | Operation | Support | Details |
| --- | --- | --- | --- |
| `job`, `jobPosting`, `supervisoryOrganization`, `worker` | Search | Yes | The connector reads these object types through the Workday REST API and pages through the results with `page_size`. A subtree or one-level search returns every entry of the object type, because the connector doesn't evaluate the LDAP filter; the identity data management service applies it. A base search takes the first value of the first RDN of the search base as the record identifier and requests that record from Workday. |
| `businessprocess`, `domain`, `integrationsystemuser`, `securitygroup`, `securitygroupmembership` | Search | Yes | The connector reads these object types from Workday custom reports, one request per search, and `page_size` doesn't apply. A base search matches the identifier against the report rows case-insensitively and returns every row that matches, so an identifier that isn't unique in the report yields several entries. |
| All object types | Create, Modify, Delete | No | The connector is strictly read-only. It implements no write operation and never writes to Workday. |

## Schema overview

The schema defines nine object types:

- `businessprocess`
- `domain`
- `integrationsystemuser`
- `job`
- `jobPosting`
- `securitygroup`
- `securitygroupmembership`
- `supervisoryOrganization`
- `worker`

The primary key and _relative distinguished name (RDN)_ is `name` for `businessprocess` and `domain` entries, `userName` for `integrationsystemuser` entries, `jobId` for `job` entries, `jobPostingId` for `jobPosting` entries, `workdayId` for `securitygroup` and `securitygroupmembership` entries, `supervisoryOrganizationId` for `supervisoryOrganization` entries, and `workerId` for `worker` entries.

The connector supports automatic schema authoring: the system generates the schema when you create the data source. For more information about each attribute's data type and availability, see the [Appendix A: Attribute reference](#appendix-a-attribute-reference) section of this document.

> [!CAUTION]
> Don't modify the generated schema.

## Known limitations

The following table lists the connector's known limitations:

| # | Limitation |
| --- | --- |
| 1 | **Test Connection checks only the REST API credentials.** It completes one OAuth token exchange, so it validates `instance_name`, `tenant`, `client_id`, `username`, and `private_key`. It doesn't check `report_username`, `report_password`, or any report URL. A data source with a wrong report credential or a wrong report URL passes Test Connection and then fails when you search a report-backed object type. |
| 2 | **The report-backed object types don't page.** Workday Report-as-a-Service returns a whole report in one response, so the connector fetches every row in a single request. The `page_size` property has no effect on `businessprocess`, `domain`, `integrationsystemuser`, `securitygroup`, or `securitygroupmembership` entries. |
| 3 | **Supervisory organization members always page in batches of 100.** When the connector reads `supervisoryOrganization` entries, it ignores the `page_size` property and instead fetches 100 members at a time. |
| 4 | **The connector doesn't evaluate LDAP filters.** For a subtree or one-level search it returns every entry of the object type and leaves filter matching to the identity data management service. It reads only the search scope and, for a base search, the first value of the first RDN of the search base. |
| 5 | **A base search on a report-backed object type might return more than one entry.** The connector matches the identifier against the report rows case-insensitively and returns every row that matches, so an identifier that isn't unique in the report yields several entries. |
| 6 | **The connector drops blank values from an entry.** When a Workday record or a report row carries a blank value for an attribute, the connector omits that attribute from the entry rather than returning it empty. An entry can therefore lack an attribute that the schema marks as non-nullable. |
| 7 | **The connector retries after rate limiting and transport failures, but not after server errors.** It retries a request up to five times when Workday returns an HTTP `429 Too Many Requests` status code or when the request fails with an I/O error, backing off exponentially from 1 second to 16 seconds and honoring the `Retry-After` header. It doesn't retry after a `5xx` status code; the connector passes the response to the caller immediately. |
| 8 | **Blank report credentials disable authentication silently.** The connector accepts an empty `report_username` or `report_password` and then sends the report requests with no authorization header, without logging the omission. |
| 9 | **HTTP timeouts aren't configurable.** The connector reads connect, read, and write timeouts from properties that the data source doesn't expose, so they stay at 30, 60, and 60 seconds. |
| 10 | **The connector supports one Workday tenant per data source.** It holds one instance name and one tenant, and builds every request from them. To read a second tenant, create a second data source. |
| 11 | **Domain security policy names vary by tenant.** The domain security policy names that authorize the REST API calls might differ between Workday releases and tenants, because an implementation can rename or split a domain. Confirm the domains for each operation against the Workday REST API Reference before you grant permissions in a target tenant. |
| 12 | **Report URLs must be in JSON format.** Workday exposes each custom report web service in several formats. The connector reads only the JSON URL; the other formats fail. |

## Release history

The following table records the connector's public release history:

| Version | Release date | Description |
| --- | --- | --- |
| 1.0.0 | September 23, 2026 | First official release. |

## Appendix A: Attribute reference

The connector is read-only, so tables in this section omit the 'Create' and 'Modify' columns. The tables describe availability instead. The identity data management service provides search (read) support for every attribute that the connector returns. The 'Nullable' column says whether an entry must carry data for the attribute. A checkmark (✔) means it doesn't; a blank cell means the schema requires data.

Some attributes represent a boolean, a nested object, or a list, but the schema declares them as `string`. To help you identify what each value represents, this document adds a qualifier in the 'Data type' column:

- `string` (boolean)
- `string` (delimited list)
- `string` (JSON object)

An attribute that can carry several values says "Multivalued attribute." in its description. The two are independent: a multivalued attribute still takes the qualifier that its individual values need.

For more information about object-level operation support, see the [Supported operations](#supported-operations) section of this document.

### Object type: `businessprocess`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `name` | `string` | | Primary key and RDN. Name of the business process. |
| `workdayId` | `string` | | Unique Workday identifier of the business process. |
| `description` | `string` | ✔ | Description of the business process. |
| `functionalAreas` | `string` (delimited list) | ✔ | Functional areas associated with the business process. When a business process belongs to several functional areas, Workday returns them as one string with the values separated by a semicolon and a space. The connector doesn't split them. |

### Object type: `domain`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `name` | `string` | | Primary key and RDN. Display name of the security domain. |
| `workdayId` | `string` | | Unique Workday identifier for the security domain. |
| `description` | `string` | ✔ | Details about the purpose or usage of the domain. |
| `functionalAreas` | `string` (delimited list) | ✔ | Functional areas that the security domain belongs to. A domain can belong to more than one, and Workday returns them as one string with the values separated by a semicolon and a space. The connector doesn't split them. |

### Object type: `integrationsystemuser`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `userName` | `string` | | Primary key and RDN. Username that the ISU authenticates with. |
| `workdayId` | `string` | | Unique Workday identifier assigned to the ISU. |
| `description` | `string` | ✔ | User-defined description that identifies the purpose of the ISU. The connector reads it from the report's `creationDetails` column. |
| `isDisabled` | `string` (boolean) | ✔ | Indicates whether the ISU is disabled in Workday. Workday returns `1` for disabled and `0` for enabled, as a string. |

### Object type: `job`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `jobId` | `string` | | Primary key and RDN. Unique identifier of the job. |
| `descriptor` | `string` | ✔ | Human-readable description of the job. |
| `jobWorker` | `string` (JSON object) | ✔ | Worker associated with the job. |
| `supervisoryOrganization` | `string` (JSON object) | ✔ | Supervisory organization associated with the job. |
| `location` | `string` (JSON object) | ✔ | Location of the job. |
| `jobType` | `string` (JSON object) | ✔ | Type classification of the job. |

### Object type: `jobPosting`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `jobPostingId` | `string` | | Primary key and RDN. Unique identifier of the job posting. |
| `timeType` | `string` (JSON object) | ✔ | Employment time classification for the position, such as full time or part time. |
| `spotlightJob` | `string` (boolean) | ✔ | Indicates whether Workday marks the job posting as a spotlight job. |
| `url` | `string` | ✔ | URL of the job posting or the application page. |
| `startDate` | `string` | ✔ | Date on which the job posting becomes active. |
| `jobDescription` | `string` | ✔ | Detailed description of the job role, its responsibilities, and its qualifications. |
| `company` | `string` (JSON object) | ✔ | Company associated with the job posting. |
| `title` | `string` | ✔ | Title of the job position. |
| `primaryLocation` | `string` (JSON object) | ✔ | Primary work location for the job posting. |
| `categories` | `string` (JSON object) | ✔ | Multivalued attribute. Categories associated with the job posting, such as job functions or departments. Each value is a JSON object. |
| `jobSite` | `string` (JSON object) | ✔ | Job site associated with the job posting. |
| `jobType` | `string` (JSON object) | ✔ | Type of employment associated with the job posting. |

### Object type: `securitygroup`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `workdayId` | `string` | | Primary key and RDN. Unique Workday identifier for the security group. |
| `name` | `string` | ✔ | Display name of the security group. |
| `type` | `string` | ✔ | Classification of the security group, such as role-based or user-based. |
| `scope` | `string` | ✔ | Organizational or functional context in which the security group applies. Absent when the group has no scope. |
| `isInactive` | `string` (boolean) | ✔ | Indicates whether the security group is inactive. Workday returns `1` for inactive and `0` for active, as a string. |

### Object type: `securitygroupmembership`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `workdayId` | `string` | | Primary key and RDN. Unique Workday identifier of the security group whose membership this entry lists. |
| `name` | `string` | ✔ | Display name of that security group. |
| `members` | `string` | ✔ | Multivalued attribute. Members of the security group, each value a username and a display name separated by a slash. Workday returns them as one string with the members separated by a semicolon and a space, which the connector splits into separate values. |

### Object type: `supervisoryOrganization`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `supervisoryOrganizationId` | `string` | | Primary key and RDN. Unique identifier of the supervisory organization in Workday. |
| `descriptor` | `string` | ✔ | Human-readable description of the supervisory organization. |
| `organizationReferenceId` | `string` | ✔ | Reference identifier that correlates the organization with related Workday reference data. |
| `inactive` | `string` (boolean) | ✔ | Indicates whether the supervisory organization is inactive. |
| `managers` | `string` (JSON object) | ✔ | Multivalued attribute. Managers assigned to the supervisory organization. Each value is a JSON object carrying the manager's Workday ID and display name. |
| `name` | `string` | ✔ | Name of the supervisory organization. |
| `organizationMember` | `string` (JSON object) | ✔ | Multivalued attribute. Members of the supervisory organization. Each value is a JSON object. The connector populates it from the organization's members endpoint. |

### Object type: `worker`

| Attribute | Data type | Nullable | Description |
| --- | --- | --- | --- |
| `workerId` | `string` | | Primary key and RDN. Unique identifier of the worker. |
| `descriptor` | `string` | ✔ | Human-readable description of the worker. |
| `person` | `string` (JSON object) | ✔ | Person information associated with the worker. |
| `primaryJob` | `string` (JSON object) | ✔ | Primary job information for the worker. |
| `workerType` | `string` (JSON object) | ✔ | Worker type information. |
| `workerSpecificId` | `string` | ✔ | Worker identifier. |
