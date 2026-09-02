# RadiantOne Identity Data Platform connectors

This repository contains official connectors for the [RadiantOne Identity Data Platform](https://www.radiantlogic.com/radiantone-platform/). These connectors help you integrate external identity, security, and event systems with RadiantOne [Identity Data Management (IDDM)](https://developer.radiantlogic.com/idm/v8.1/) and [Identity Observability (IDO)](https://developer.radiantlogic.com/ido/v2/). All connectors are implemented using the [IDDM Connector SDK](https://github.com/radiantlogicinc/iddm-connector-sdk-documentation).

## Get started

1. Select a connector from the following catalog to open its project folder.
2. Review the connector's README for prerequisites, configuration details, and deployment instructions.
3. Download the latest JAR file linked in the connector's README.
4. Deploy to a non-production environment.
5. Test the integration before promoting to production.

## Connector catalog

The following table lists the available connectors and their latest versions:

| Connector                                              | Latest version |
|--------------------------------------------------------|----------------|
| [AWS Bedrock](./agentic-aws-bedrock)                   | 1.0.0          |
| [CyberArk Privilege Cloud](./cyberark-privilege-cloud) | 1.0.0          |
|[Google Cloud Agents](./google-cloud-agents) | 0.9.2 (beta) |
|[Microsoft Azure AI Foundry](./microsoft-azure-ai-foundry) | 0.9.2 (beta) |
|[ServiceNow](./servicenow) | 1.0.0 |

Each connector has a project folder in this repository that contains:

- `README.md`: Connector-specific documentation
- `builds/`: Pre-built connector JAR files organized by version

Some connectors also publish their source:

- `project/`: Connector source code and tests

## Versioning

Connectors in this repository follow [Semantic Versioning](https://semver.org/). In brief:

- The patch version is incremented for backward-compatible bug fixes.
- The minor version is incremented for new backward-compatible functionality or deprecations.
- The major version is incremented for any breaking changes to a connector's configuration or behavior.

Each connector is versioned independently. Connectors with a version below `1.0.0` are considered beta: the configuration and behavior are not yet stable and may change at any time. Deploy beta connectors to non-production environments only, and expect breaking changes between releases until the connector reaches `1.0.0`.

## License

Unless otherwise noted, the contents of this repository are released under the Apache License 2.0. For more details, see the `LICENSE` file at the root of this repository.

## Third-party product names and trademarks

The connectors in this repository provide integrations with various third-party products and services. All product names, logos, and brands are the property of 
their respective owners and are used in this repository for identification purposes only. Use of these names does not imply any sponsorship, endorsement, certification, or affiliation with those third-party vendors unless explicitly stated in the relevant connector documentation.