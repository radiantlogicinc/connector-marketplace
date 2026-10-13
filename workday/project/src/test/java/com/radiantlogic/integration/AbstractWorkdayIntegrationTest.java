package com.radiantlogic.integration;

import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.radiantlogic.WorkdayConnector;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.utility.TestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

/**
 * Base integration test class for Workday connector tests.
 *
 * <p>This class is executed using {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS},
 * meaning a single test instance is created and shared across all test methods within a class.
 * This lifecycle is used to support shared infrastructure such as WireMock server setup,
 * connector initialization, and reusable test configuration without requiring static state.
 * It enables consistent and efficient integration testing across multiple derived test classes.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TestClassWithoutTestCases"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbstractWorkdayIntegrationTest {

  /** WireMock server used to mock HTTP interactions during testing. */
  protected WireMockServer wireMockServer;

  /** Read-only connection properties used by the connector. */
  protected ReadOnlyProperties connectionProperties;

  /** Read-only target schema object properties used for testing. */
  protected ReadOnlyProperties targetSchemaObjects;

  /** Schema used by the connector under test. */
  protected Schema schema;

  /** HTTP client used to send requests. */
  protected HttpClient httpClient;

  /** Executes HTTP requests. */
  protected RequestExecutor requestExecutor;

  /** Builds HTTP requests. */
  protected RequestBuilder requestBuilder;

  /** Connector instance under test. */
  protected WorkdayConnector workdayConnector;

  /** Root path used in test requests. */
  public static final String PATH = "/";

  /** Test tenant identifier. */
  public static final String TENANT_ID = "test_tenant";

  /** Format parameter name. */
  public static final String FORMAT = "format";

  /** JSON format type value. */
  public static final String FORMAT_TYPE = "json";

  /** Expected error response message. */
  public static final String ERROR_RESPONSE = "Should return error response";

  /** Scenario representing second iteration. */
  public static final String SECOND = "SECOND";
  /**
   * JSON response body containing a null report entry.
   */
  public static final String JSON_BODY_WITH_NULL_RESPONSE = "{\n"
      + "  \"Report_Entry\": null\n"
      + "}";
  /**
   * JSON response body containing a null report entry.
   */
  public static final String JSON_DATA_BODY_WITH_NULL_RESPONSE = "{\n"
      + "  \"data\": null\n"
      + "}";

  @BeforeAll
  void globalSetup() {
    connectionProperties = TestUtil.getConnectionProperties();
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);

    wireMockServer = new WireMockServer(
        WireMockConfiguration.options()
            .notifier(new ConsoleNotifier(true))
            .port((Integer) connectionProperties.get(URL_PORT_KEY))
    );
    wireMockServer.start();
  }

  @AfterAll
  void globalTeardown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void baseSetup() {
    httpClient = new HttpClient(connectionProperties);
    requestExecutor = new RequestExecutor(httpClient);
    requestBuilder = new RequestBuilder(connectionProperties, requestExecutor);

    workdayConnector =
        new WorkdayConnector(targetSchemaObjects, schema, connectionProperties, httpClient);
  }

  /**
   * Counts the number of entries from the provided LDAP response data provider.
   *
   * @param dataProvider the provider containing LDAP response data
   * @return the count of entries available from the data provider
   * @throws JsonProcessingException if the response data cannot be processed
   */
  public int count(final SequentialDataProvider<LdapResponse<?>> dataProvider)
      throws JsonProcessingException {
    int count = 0;
    ObjectMapper objectMapper = new ObjectMapper();

    while (dataProvider.hasMore()) {
      count += objectMapper
          .readTree((String) dataProvider.next().getData())
          .size();
    }

    return count;
  }

  /**
   * Creates and returns the base LDAP search request.
   *
   * @return the base LDAP search request
   */
  public LdapSearchRequest baseRequest() {
    return LdapSearchRequest.create(
        DN.from("ou="),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

}
