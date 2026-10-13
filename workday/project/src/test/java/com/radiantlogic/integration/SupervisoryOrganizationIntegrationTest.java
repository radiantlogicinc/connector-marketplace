package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.utility.TestUtil;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Supervisory Organization related functionality.
 *
 * <p>Uses a per-class test instance lifecycle to allow shared state
 * and non-static setup across test methods.</p>
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts"})
public class SupervisoryOrganizationIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Path used to identify the supervisory organization resource in API requests.
   */
  private static final String SUPERVISORY_ORGANIZATION_ID_PATH = "/11";
  /**
   * JSON file containing the supervisory organizations response data.
   */
  private static final String SUPERVISORY_ORGANIZATIONS_RESPONSE = "supervisoryOrganizations.json";
  /**
   * JSON file containing the supervisory organizations response data.
   */
  private static final String SUPERVISORY_ORGANIZATION_RESPONSE = "supervisoryOrganization.json";

  /**
   * JSON file containing the members associated with supervisory organization with id 11 response
   * data.
   */
  private static final String SUP_ORG_ID_11_MEMBERS_RESPONSE = "11_members.json";
  /**
   * The query parameters used for limiting and offsetting API response data.
   */
  private static final String LIMIT_OFFSET = "?limit=100&offset=0";

  @BeforeEach
  void setUp() {
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(
        Collections.singletonList("supervisoryOrganization"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForSupervisoryOrganization()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID))).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where supervisory organizations exist in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * supervisory organizations are available.
   */
  @Nested
  @DisplayName("Given supervisory organizations exist")
  class GivenSupervisoryOrganizationsExist {

    @BeforeEach
    void setup() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(
              PATH + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATIONS_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUP_ORG_ID_11_MEMBERS_RESPONSE))));

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "12")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("12_members.json"))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "13")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("13_members.json"))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "14")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("14_members.json"))));
    }

    @Test
    @DisplayName("Should return all supervisory organizations ")
    void shouldReturnAllSupervisoryOrganizations() throws Exception {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(baseRequest());
      assertThat(count(dataProvider)).isEqualTo(4);
    }
  }

  /**
   * Tests for scenarios where supervisory organization exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * supervisory organization is available.
   */
  @Nested
  @DisplayName("Given supervisory organization exists in the system")
  class GivenSupervisoryOrganizationExists {

    @BeforeEach
    void setupSupervisoryOrganization() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUP_ORG_ID_11_MEMBERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the supervisory organization")
    void shouldReturnSupervisoryOrganization() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the supervisory organization API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the supervisory organization API response is invalid.
   */
  @Nested
  @DisplayName("Given supervisory organization API returns invalid response")
  class GivenInvalidSupervisoryOrganizationResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSupervisoryOrganizationRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where the API rejects a request as a bad request.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API returns a bad request response.
   */
  @Nested
  @DisplayName("Given API rejects request as bad request")
  class GivenBadRequest {

    @BeforeEach
    void setupBadRequest() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSupervisoryOrganizationRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where authentication fails.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * authentication is unsuccessful.
   */
  @Nested
  @DisplayName("Given authentication fails")
  class GivenAuthenticationFails {

    @BeforeEach
    void setupAuthFailure() {
      wireMockServer.stubFor(
          post(urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .willReturn(aResponse().withStatus(401)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSupervisoryOrganizationRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where a supervisory organization supports pagination.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * paginated supervisory organization data retrieval is enabled.
   */
  @Nested
  @DisplayName("Given supervisory organization supports pagination")
  class GivenPaginationIsEnabled {

    @BeforeEach
    void setupPagination() {

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + LIMIT_OFFSET))
              .willReturn(aResponse()
                  .withBody(
                      TestUtil.getResponseFromFile("paginated_supervisoryOrganizations.json"))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + "?limit=100&offset=100"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATIONS_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUP_ORG_ID_11_MEMBERS_RESPONSE))));

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "12")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("12_members.json"))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "13")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("13_members.json"))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "14")))
              .willReturn(
                  aResponse().withBody(TestUtil.getResponseFromFile("14_members.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return all supervisory Organizations across pages")
    void shouldReturnAllSupervisoryOrganizationsAcrossPages() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(paginatedRequest());

      assertThat(count(dataProvider)).isEqualTo(8);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API returns a rate limit response without a header.
   */
  @Nested
  @DisplayName("Given API rate limits requests")
  class GivenRateLimitWithoutHeader {

    @BeforeEach
    void setupRetryScenario() {

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .inScenario("supervisoryOrganization-retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .inScenario("supervisoryOrganization-retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUP_ORG_ID_11_MEMBERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests with a Retry-After header.
   *
   * <p>This nested test class groups test cases that verify retry behavior when
   * the API provides a Retry-After header in the rate limit response.
   */
  @Nested
  @DisplayName("Given API rate limits requests with Retry-After header")
  class GivenRateLimitWithRetryAfter {

    /**
     * Test scenario name for retry handling with Retry-After header.
     */
    private static final String RETRY_SCENARIO = "supervisoryOrganization-retry-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {

      wireMockServer.resetScenarios();

      String url = PATH
          + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
          + SUPERVISORY_ORGANIZATION_ID_PATH;

      wireMockServer.stubFor(
          get(urlPathEqualTo(url))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(url))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withBody(
                      TestUtil.getResponseFromFile(SUP_ORG_ID_11_MEMBERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the supervisory organizations API returns null data in the response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the supervisory organizations API response contains null data.
   */
  @Nested
  @DisplayName("Given supervisory organizations API returns null data in the response")
  class GivenNullDataInResponse {

    @BeforeEach
    void setupNullDataInResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse().withStatus(200)
                  .withBody(JSON_DATA_BODY_WITH_NULL_RESPONSE)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponseForNullDataInResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSupervisoryOrganizationRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where a supervisory organization exists in the system but an error occurs
   * while fetching members.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * supervisory organization member retrieval fails.
   */
  @Nested
  @DisplayName("Given supervisory organization exists in the system but having error while "
      + "fetching members")
  class GivenSupervisoryOrganizationExistsHavingErrorWhileFetchingMembers {

    @BeforeEach
    void setupSupervisoryOrganizationWithMembersResponseFailure() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, TENANT_ID,
              "11")))
              .willReturn(
                  aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the error when failed to fetch members")
    void shouldReturnError() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(dataProvider.next().getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where supervisory organization members fit within a single page.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * all member data is returned in a single page response.
   */
  @Nested
  @DisplayName("Given supervisory organization members fit in a single page")
  class GivenSinglePageMembers {

    @BeforeEach
    void setupMembers() {
      wireMockServer.resetRequests();

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(
              SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH,
              TENANT_ID,
              "11")
              + LIMIT_OFFSET))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(
                      SUP_ORG_ID_11_MEMBERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fetch members from a single page")
    void shouldFetchMembersFromSinglePage() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);

      wireMockServer.verify(1,
          getRequestedFor(urlMatching(".*offset=0.*")));

      wireMockServer.verify(0,
          getRequestedFor(urlMatching(".*offset=100.*")));
    }
  }

  /**
   * Tests for scenarios where supervisory organization members exactly fill one page.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the member data count matches the page size limit exactly.
   */
  @Nested
  @DisplayName("Given supervisory organization members exactly equal one page")
  class GivenMembersExactlyOnePage {

    @BeforeEach
    void setupMembers() {
      wireMockServer.resetRequests();

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(
              SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH,
              TENANT_ID,
              "11")
              + LIMIT_OFFSET))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(
                      "11_members_exactly_100.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should stop fetching when total equals page size")
    void shouldStopFetchingWhenTotalEqualsPageSize() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);

      wireMockServer.verify(1,
          getRequestedFor(urlMatching(".*offset=0.*")));

      wireMockServer.verify(0,
          getRequestedFor(urlMatching(".*offset=100.*")));
    }
  }

  /**
   * Tests for scenarios where supervisory organization members span multiple pages.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * member data is returned across multiple pages.
   */
  @Nested
  @DisplayName("Given supervisory organization members span multiple pages")
  class GivenMultiPageMembers {

    @BeforeEach
    void setupMembers() {

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, TENANT_ID)
              + SUPERVISORY_ORGANIZATION_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SUPERVISORY_ORGANIZATION_RESPONSE))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(
              SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH,
              TENANT_ID,
              "11")
              + LIMIT_OFFSET))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(
                      "11_members_page1.json"))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(
              SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH,
              TENANT_ID,
              "11")
              + "?limit=100&offset=100"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(
                      "11_members_page2.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fetch members across multiple pages")
    void shouldFetchMembersAcrossMultiplePages() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSupervisoryOrganizationRequest());

      assertThat(count(dataProvider)).isEqualTo(1);

      wireMockServer.verify(1,
          getRequestedFor(urlMatching(".*offset=0.*")));

      wireMockServer.verify(1,
          getRequestedFor(urlMatching(".*offset=100.*")));
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest singleSupervisoryOrganizationRequest() {
    return LdapSearchRequest.create(
        DN.from("supervisoryOrganization=11,ou=supervisoryOrganization,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest paginatedRequest() {
    return LdapSearchRequest.create(
        DN.from("ou=supervisoryOrganization"),
        SearchScope.SUB,
        SearchFilter.from("(objectclass=*)"));
  }
}
