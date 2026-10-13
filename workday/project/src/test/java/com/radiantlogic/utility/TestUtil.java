package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.BUSINESS_PROCESSES_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.DOMAINS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.INSTANCE_NAME_KEY;
import static com.radiantlogic.utility.Constants.INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.PRIVATE_KEY;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.REPORT_PASSWORD_KEY;
import static com.radiantlogic.utility.Constants.REPORT_USERNAME_KEY;
import static com.radiantlogic.utility.Constants.RETRY_DELAY_KEY_NAME;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.TENANT_NAME_KEY;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.USERNAME_KEY;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.SchemaObject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Utility class providing reusable helper methods and test data setup for Workday connector unit
 * and integration tests.
 *
 * <p>This class contains common functionality such as loading test resources,
 * creating mock connection properties, and preparing reusable test fixtures used across multiple
 * test scenarios.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.ExcessiveImports",
    "PMD.JUnit5TestShouldBePackagePrivate", "PMD.TooManyMethods"})
public class TestUtil {

  /**
   * The port number used by WireMock server for testing.
   */
  @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
  private static final int WIREMOCK_PORT_NO = 50000;
  /**
   * The schema field name in uppercase format.
   */
  private static final String NAME = "Name";

  /**
   * The schema field name in lowercase format.
   */
  private static final String NAME_SMALL_CASE = "name";
  /**
   * The schema field for WorkdayId.
   */
  private static final String WORKDAY_ID = "WorkdayId";

  /**
   * The URL path used for report service testing.
   */
  private static final String REPORT_URL_PATH =
      "http://localhost:50000/ccx/service/customreport2/test_tenant/test_username";

  /**
   * Valid RSA PKCS8 private key for testing. Generated only for testing purposes.
   */
  private static final String VALID_PRIVATE_KEY =
      "-----BEGIN PRIVATE KEY-----\n"
          + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDOLa2qzyzTXTkd\n"
          + "7PIfLqkPF8Zg5VxYUKXOhWXEkJiMI8L9FvmJgeZnLgNZtil0BNUbQCE3b5y5sbdf\n"
          + "nJR0Q9Eo6MDhQD4K/wsV90Krh1HDsAPlEE/xNCw0sbFUvMHiELRTOq5hZiT50eXj\n"
          + "rdAr9BkAEqyBUVVPJYPSjfH7cEj+QihbinE6cth5n3qLAzMkRI5Ql+QtDkSd3NLu\n"
          + "p0ob098tFtPvVp0CNSseNsvDMozZ049ClrxAgK4vfoH/1pWRAGOSi53m8TYbE8bq\n"
          + "yIXgWyPSHEPPk1sOhDyUUlfparX1OHlQksRWa+EmlD88XI1fPDe/uEdG5S9WTw1D\n"
          + "CMCZJ9GrAgMBAAECggEARqyvyEpmIjnld1WelYPIHc1XiiV1JyzV+VoOzXeHYxpr\n"
          + "H5h9h15I3XuPWHQx93wklMfB2x/eSGUb56Fj3grVKsHGRDBiVCCi6y+r4eJr67lt\n"
          + "TFrwHE13DcUPdW2SxGxXc+Q8nFSIPj5uIEKXhkmRPE4qZdW8eXnxmv5fQYJ28via\n"
          + "q4cPQpe6DQOIg1sQi6RhqR3neffsDsV8uCudBRbkNZ8uzukAvw2FM++Zpw/4CbXr\n"
          + "9g/prxsDt7gzNt9lMpFaeS2WWWcN+EF9DhBcF1/pbl/RRzWHS1StTvYQVxSgXNJC\n"
          + "0N/vsxKZ8Ke24BaXXbsvL+mn+epEW+vQ812z+WvcIQKBgQD3qXg2HOpZcehjwEC2\n"
          + "I1xY8bTEDJW3mvqolrafaqWcYCa0qchb3WtBM3x5nGiLJMteb88wsA9Eg/QJp0lP\n"
          + "B/FjrOuMXbQFhmBNExrKhUgxlUHKmKPGOF9qKe+Dpc8P/5SVhyjS13gvyE7XdKdB\n"
          + "TUg6ru1863Ao/Zp0sXWQ3sc9UQKBgQDVHqxnMneFXnZm8wNxnIN1FrC0fIICQG+O\n"
          + "I0oBXqTifcTZOPRT8hZLfczpYpXqq0oPevSsnGlnXB17+AZlxViOnp6DQH82CShs\n"
          + "qbosifdquwKcx5e0deunoSvgaPlnyr2LkpfH6eJHxksQGvhGVaLXksJ5FuM6QySm\n"
          + "QKjPrIWwOwKBgG0YTOjhYeYWxDy0oozZFMPlbTBzLrxbW5FKGpYsv1kcDdDFfYni\n"
          + "Bm4JRQq4+GBay+8Ep6sV0xPmYGikNSnIIPA8auJBIJzSbbdl9AZEIeIdkoaxKGuX\n"
          + "x4gXgvdl4cl1Vzlq+XOvELVTsk88hrTAW1HmXp0JhTW5SQGgbl5rlC6RAoGAbxnn\n"
          + "fIDlqARfqzmioPab3U1v3xdNW0kmHq4s5VvmjyBLn4Y+cQiyMLarPTuktcqH+Fv7\n"
          + "84QyuqxdvoPFKgbgLLFqJzhXWN8bUNKGeLOP5PcK3Jp8bppOuHuf+QQgiMg7WeiY\n"
          + "8aHu0cYA2auFpHz+QG2ErkWNPZFFKNpqZqEkrIMCgYEA4BajnTlaonRBuPAvUwpX\n"
          + "Szu25EGMJ4scEgudUHeAie3sx7Plqo833kMZpkvM4KpFxyVTTR6ZBDPwFWEdcpnf\n"
          + "Lzp1cvfsKHIVu+3uvQrAlCuaU5dP4yH30+j+IsSDSTCCiSq/u9CNn/ozJXn+kK0h\n"
          + "tl2axFNN5xwkFEXd/6aJnc4=\n"
          + "-----END PRIVATE KEY-----";

  /**
   * Returns connection properties configured with valid authentication credentials for use in
   * connector integration and connectivity tests.
   *
   * @return a {@link ReadOnlyProperties} instance containing valid test connection settings and a
   *          valid private key
   */
  public static ReadOnlyProperties getConnectionProperties() {
    return ReadOnlyProperties.builder()
        .put(INSTANCE_NAME_KEY, "test_instance_name")
        .put(CLIENT_ID_KEY, "test_client_id")
        .put(PRIVATE_KEY, VALID_PRIVATE_KEY)
        .put(USERNAME_KEY, "test_user")
        .put(TENANT_NAME_KEY, "test_tenant")
        .put(PAGE_SIZE_KEY, 100)
        .put(CONNECTION_TIMEOUT_KEY_NAME, 1)
        .put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1)
        .put(RETRY_DELAY_KEY_NAME, 1)
        .put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost")
        .put(URL_PORT_KEY, WIREMOCK_PORT_NO)
        .put(REPORT_USERNAME_KEY, "test_username")
        .put(REPORT_PASSWORD_KEY, "test_password")
        .put(INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/All_Integration_System_User_Accounts_Test?format=json")
        .put(SECURITY_GROUPS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Security_Groups_Test?format=json")
        .put(SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Security_Group_Members_Test?format=json")
        .put(DOMAINS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Domains_Test?format=json")
        .put(BUSINESS_PROCESSES_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/BusinessProcesses_Test?format=json")
        .build();
  }

  /**
   * Returns connection properties configured with an intentionally invalid private key for use in
   * negative-path connectivity tests.
   *
   * @return a {@link ReadOnlyProperties} instance containing test connection settings and an
   *          invalid private key
   */
  public static ReadOnlyProperties getInvalidConnectionProperties() {
    return ReadOnlyProperties.builder()
        .put(INSTANCE_NAME_KEY, "test_instance_name")
        .put(CLIENT_ID_KEY, "test_client_id")
        .put(PRIVATE_KEY, "INVALID_PRIVATE_KEY")
        .put(USERNAME_KEY, "test_user")
        .put(TENANT_NAME_KEY, "test_tenant")
        .put(PAGE_SIZE_KEY, 100)
        .put(CONNECTION_TIMEOUT_KEY_NAME, 1)
        .put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1)
        .put(RETRY_DELAY_KEY_NAME, 1)
        .put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost")
        .put(URL_PORT_KEY, WIREMOCK_PORT_NO)
        .put(REPORT_USERNAME_KEY, "test_username")
        .put(REPORT_PASSWORD_KEY, "test_password")
        .put(INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/All_Integration_System_User_Accounts_Test?format=json")
        .put(SECURITY_GROUPS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Security_Groups_Test?format=json")
        .put(SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Security_Group_Members_Test?format=json")
        .put(DOMAINS_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/Domains_Test?format=json")
        .put(BUSINESS_PROCESSES_REPORT_URL_KEY,
            REPORT_URL_PATH
                + "/BusinessProcesses_Test?format=json")
        .build();
  }

  /**
   * Method to return file contents.
   *
   * @param file File name of whose contents to be fetched
   * @return Fetched contents
   */
  public static String getResponseFromFile(final String file) {
    try {
      return IOUtils.toString(Objects.requireNonNull(
          Thread.currentThread().getContextClassLoader().getResourceAsStream(file)));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read test resource: " + file, e);
    }
  }

  @Test
  @DisplayName("Should load RSA private key successfully when a valid PEM key is provided")
  @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
  void shouldLoadPrivateKeySuccessfully()
      throws NoSuchAlgorithmException, InvalidKeySpecException, WorkdayException {
    PrivateKey privateKey = JwtUtil.loadPrivateKey(VALID_PRIVATE_KEY);
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when private key is null")
  void shouldThrowExceptionWhenPemIsNull() {
    final Throwable thrown =
        catchThrowable(() -> JwtUtil.loadPrivateKey(null));

    assertThat(thrown)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Private Key cannot be null");
  }

  /**
   * Returns the schema object definition for worker entity.
   *
   * @return schema object representing worker configuration
   */
  public static SchemaObject getSchemaObjectForWorker() {
    Field field1 = new Field("WorkerID", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("Descriptor", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Person", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("PrimaryJob", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 =
        new Field("WorkerType", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("WorkerSpecificId", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6));
    return new SchemaObject("worker", "vdWorker", "", "", "", Collections.singletonList("workerId"),
        new ArrayList<>(), fields);
  }

  /**
   * Returns the schema object definition for job entity.
   *
   * @return schema object representing job configuration
   */
  public static SchemaObject getSchemaObjectForJob() {

    final Field field1 = new Field("JobID", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field2 = new Field("Descriptor", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field3 =
        new Field("JobWorker", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field4 = new Field("SupervisoryOrganization", "", new HashSet<>(),
        false, Field.Type.STRING);
    final Field field5 =
        new Field("Location", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field6 =
        new Field("JobType", "", new HashSet<>(), false, Field.Type.STRING);

    final List<Field> fields = ImmutableList.of(field1, field2, field3, field4, field5, field6);

    return new SchemaObject(
        "job",
        "vdJob",
        "",
        "",
        "",
        ImmutableList.of("jobId"),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for job posting entity.
   *
   * @return schema object representing job posting configuration
   */
  public static SchemaObject getSchemaObjectForJobPosting() {
    final Field field1 = new Field("JobPostingId", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field2 = new Field("TimeType", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field3 =
        new Field("SpotlightJob", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field4 = new Field("Url", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field5 =
        new Field("StartDate", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field6 =
        new Field("JobDescription", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field7 =
        new Field("Company", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field8 =
        new Field("Title", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field9 =
        new Field("PrimaryLocation", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field10 =
        new Field("Categories", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field11 =
        new Field("JobSite", "", new HashSet<>(), false, Field.Type.STRING);
    final Field field12 =
        new Field("JobType", "", new HashSet<>(), false, Field.Type.STRING);

    final List<Field> fields =
        ImmutableList.of(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12);

    return new SchemaObject(
        "jobPosting",
        "vdJobPosting",
        "",
        "",
        "",
        ImmutableList.of("jobPostingId"),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for supervisory organization entity.
   *
   * @return schema object representing supervisory organization configuration
   */
  public static SchemaObject getSchemaObjectForSupervisoryOrganization() {
    Field field1 =
        new Field("SupervisoryOrganizationId", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("Descriptor", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("OrganizationReferenceId", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("Inactive", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("Managers", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 = new Field(NAME, "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("OrganizationMember", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3, field4, field5, field6, field7);

    return new SchemaObject(
        "supervisoryOrganization",
        "vdSupervisoryOrganization",
        "",
        "",
        "",
        ImmutableList.of("supervisoryOrganizationId"),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for Integration System User.
   *
   * @return schema object representing Integration System User configuration
   */
  public static SchemaObject getSchemaObjectForIntegrationSystemUser() {
    Field field1 =
        new Field("Description", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("IsDisabled", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("UserName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field(WORKDAY_ID, "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3, field4);

    return new SchemaObject(
        "integrationSystemUser",
        "vdIntegrationSystemUser",
        "",
        "",
        "",
        ImmutableList.of("userName"),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for Security Group.
   *
   * @return schema object representing Security Group configuration
   */
  public static SchemaObject getSchemaObjectForSecurityGroup() {
    Field field1 =
        new Field(WORKDAY_ID, "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field(NAME, "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Type", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("Scope", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("IsInactive", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3, field4, field5);

    return new SchemaObject(
        "securityGroup",
        "vdSecurityGroup",
        "",
        "",
        "",
        ImmutableList.of("workdayId"),
        ImmutableList.of(),
        fields);

  }

  /**
   * Returns the schema object definition for Security Group Membership.
   *
   * @return schema object representing Security Group Membership configuration
   */
  public static SchemaObject getSchemaObjectForSecurityGroupMembership() {
    Field field1 =
        new Field(NAME, "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field(WORKDAY_ID, "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Members", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3);

    return new SchemaObject(
        "securityGroupMembership",
        "vdSecurityGroupMembership",
        "",
        "",
        "",
        ImmutableList.of("workdayId"),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for domain.
   *
   * @return schema object representing domain configuration
   */
  public static SchemaObject getSchemaObjectForDomain() {
    Field field1 =
        new Field(WORKDAY_ID, "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field(NAME, "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Description", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("FunctionalAreas", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3, field4);

    return new SchemaObject(
        "domain",
        "vdDomain",
        "",
        "",
        "",
        ImmutableList.of(NAME_SMALL_CASE),
        ImmutableList.of(),
        fields);
  }

  /**
   * Returns the schema object definition for business Process.
   *
   * @return schema object representing business process configuration
   */
  public static SchemaObject getSchemaObjectForBusinessProcess() {
    Field field1 =
        new Field(WORKDAY_ID, "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field(NAME, "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Description", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("FunctionalAreas", "", new HashSet<>(), false, Field.Type.STRING);

    List<Field> fields = ImmutableList.of(field1, field2, field3, field4);

    return new SchemaObject(
        "buinessProcess",
        "vdBusinessProcess",
        "",
        "",
        "",
        ImmutableList.of(NAME_SMALL_CASE),
        ImmutableList.of(),
        fields);
  }

}
