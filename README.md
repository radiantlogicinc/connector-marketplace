# RadiantOne Identity Data Platform connectors

This repository contains official connectors for the [RadiantOne Identity Data Platform](https://www.radiantlogic.com/radiantone-platform/). These connectors help you integrate external identity, security, and event systems with RadiantOne products. Classic connectors are built into Identity Data Platform; this repository distributes their supporting files. Connectors built using the [Radiant Logic Connector SDK](https://github.com/radiantlogicinc/iddm-connector-sdk-documentation) are distributed here as JAR files.

The files and documentation in this repository apply to Identity Data Management 8.2.0 or later and Identity Data Platform 1.0.0 or later. A connector's readme states a higher minimum when one applies.

## Get started

1. Select a connector from the following catalog to open its project folder.
2. Review the connector's readme for prerequisites, configuration details, and deployment instructions.
3. Download the resources linked in the connector's readme.
4. Deploy to a non-production environment.
5. Test the integration before promoting to production.

## Connector catalog

The following table lists the available connectors, their type, and their latest versions:

| Connector | Type | Latest version |
| --- | --- | --- |
| [AWS Bedrock](./agentic-aws-bedrock) | SDK | 1.0.1 |
| [CyberArk Privilege Cloud](./cyberark-privilege-cloud) | SDK | 1.0.0 |
| [Google Cloud Agents](./google-cloud-agents) | SDK | 0.9.2 |
| [Microsoft Active Directory](./microsoft-active-directory) | Classic | Not applicable |
| [Microsoft Azure AI Foundry](./microsoft-azure-ai-foundry) | SDK | 0.9.2 |
| [ServiceNow](./servicenow) | SDK | 1.0.0 |

Classic connectors ship inside Identity Data Platform, so they don't have separate version numbers. To see which product versions include a classic connector, check the readme file for that connector.

Each connector has a project folder in this repository that contains a `readme.md` with connector-specific documentation. It can also contain additional resources, such as prebuilt JAR files and observability configurations.

## Versioning

Connectors built using the Radiant Logic Connector SDK follow [Semantic Versioning](https://semver.org/). In brief:

- The patch version is incremented for backward-compatible bug fixes.
- The minor version is incremented for new backward-compatible functionality or deprecations.
- The major version is incremented for any breaking changes to a connector's configuration or behavior.

Each connector is versioned independently. Connectors with a version below `1.0.0` are considered beta: the configuration and behavior aren't yet stable and may change at any time. Deploy beta connectors to non-production environments only, and expect breaking changes between releases until the connector reaches `1.0.0`.

## License

Unless otherwise noted, the contents of this repository are released under the Apache License 2.0. For more details, see the `LICENSE` file at the root of this repository.

## Third-party product names and trademarks

The connectors in this repository provide integrations with various third-party products and services. All product names, logos, and brands are the property of their respective owners and are used in this repository for identification purposes only. Use of these names doesn't imply any sponsorship, endorsement, certification, or affiliation with those third-party vendors unless explicitly stated in the relevant connector documentation.
