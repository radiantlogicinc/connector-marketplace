package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.utility.TestUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for security group membership functionality in the Workday integration.
 *
 * <p>This test class verifies the behavior of security group membership
 * operations by exercising the integration with the Workday system in an end-to-end test
 * environment.
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts"})
public class SecurityGroupMembershipIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Security Group Memberships report endpoint used in tests.
   */
  private static final String SECURITY_GROUP_MEMBERSHIP_REPORT_URL =
      "/ccx/service/customreport2/test_tenant/test_username"
          + "/Security_Group_Members_Test";
  /**
   * JSON file containing the security groups memberships response data.
   */
  private static final String SECURITY_GROUP_MEMBERSHIP_RESPONSE = "securityGroupMemberships.json";

  @BeforeEach
  void setUp() {
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY))
        .thenReturn(Collections.singletonList("securitygroupmembership"));
    when(schema.getSchemaObject(any()))
        .thenReturn(Optional.of(TestUtil.getSchemaObjectForSecurityGroupMembership()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
        .willReturn(aResponse()
            .withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where security group memberships exist in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * security group memberships are available.
   */
  @Nested
  @DisplayName("Given security group memberships exist")
  class GivenSecurityGroupMembershipsExist {

    @BeforeEach
    void setup() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SECURITY_GROUP_MEMBERSHIP_RESPONSE))));
    }

    @Test
    @DisplayName("Should return all security group memberships")
    void shouldReturnAllSecurityGroupMemberships() throws Exception {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(baseRequest());
      assertThat(count(dataProvider)).isEqualTo(14);
    }
  }

  /**
   * Tests for scenarios where a security group membership exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * a security group membership is available.
   */
  @Nested
  @DisplayName("Given a security group membership exists in the system")
  class GivenSecurityGroupMembershipExists {

    @BeforeEach
    void setupSecurityGroupMembership() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SECURITY_GROUP_MEMBERSHIP_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the security group membership")
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void shouldReturnSecurityGroupMembership() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSecurityGroupMembershipRequest());
      ObjectMapper objectMapper = new ObjectMapper();

      int securityGroupMembershipFetched = 0;

      while (dataProvider.hasMore()) {
        String response = (String) dataProvider.next().getData();
        List<Map<String, Object>> list =
            objectMapper.readValue(response,
                new TypeReference<List<Map<String, Object>>>() {
                });
        Map<String, Object> entry = list.get(0);
        securityGroupMembershipFetched += list.size();
        assertThat(entry.get("WorkdayId")).isEqualTo("SG000006");
        assertThat(entry.get("Name")).isEqualTo("IT Operations");
        assertThat(entry.get("Members"))
            .isEqualTo(Arrays.asList(
                "kim.lead / Kim Lead",
                "kiran.dev / Kiran Developer",
                "lee.ops / Lee Operations",
                "lucian.hr / Lucian HR",
                "luka.admin / Luka Admin"));
      }
      assertThat(securityGroupMembershipFetched).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the security group membership API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the security group membership API response is invalid.
   */
  @Nested
  @DisplayName("Given security group membership API returns invalid response")
  class GivenInvalidSecurityGroupMembershipResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSecurityGroupMembershipRequest())
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
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSecurityGroupMembershipRequest())
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
          (LdapResponse<String>) workdayConnector.search(singleSecurityGroupMembershipRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
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
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("securityGroupMembership-retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("securityGroupMembership-retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SECURITY_GROUP_MEMBERSHIP_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSecurityGroupMembershipRequest());

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
    private static final String RETRY_SCENARIO = "securityGroupMembership-retry-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {

      wireMockServer.resetScenarios();

      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(SECURITY_GROUP_MEMBERSHIP_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleSecurityGroupMembershipRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the security group membership API returns null data in the response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the security group membership API response contains null data.
   */
  @Nested
  @DisplayName("Given security group membership API returns null data in the response")
  class GivenNullDataInResponse {

    @BeforeEach
    void setupNullDataInResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(SECURITY_GROUP_MEMBERSHIP_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(200)
                  .withBody(JSON_BODY_WITH_NULL_RESPONSE)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponseForNullDataInResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleSecurityGroupMembershipRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest singleSecurityGroupMembershipRequest() {
    return LdapSearchRequest.create(
        DN.from("workdayId=SG000006,ou=securityGroupMembership,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

}

