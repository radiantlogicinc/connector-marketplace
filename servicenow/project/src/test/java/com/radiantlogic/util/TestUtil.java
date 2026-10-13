package com.radiantlogic.util;

import static com.radiantlogic.utility.Constants.BATCH_SIZE_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_SECRET_KEY;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.INSTANCE_NAME_KEY;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.RETRY_DELAY_KEY_NAME;
import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;
import okhttp3.Headers;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

/**
 * Class to provide utility methods for test cases.
 */
@NoArgsConstructor
public class TestUtil {

  private static final int WIREMOCK_PORT_NO = 50000;

  public static ReadOnlyProperties getConnectionProperties() {
    return ReadOnlyProperties.builder()
        .put(INSTANCE_NAME_KEY, "test_instance_name")
        .put(CLIENT_ID_KEY, "test_client_id")
        .put(CLIENT_SECRET_KEY, "test_client_secret")
        .put(PAGE_SIZE_KEY, 1)
        .put(CONNECTION_TIMEOUT_KEY_NAME, 1)
        .put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1)
        .put(RETRY_DELAY_KEY_NAME, 1)
        .put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost")
        .put(URL_PORT_KEY, WIREMOCK_PORT_NO)
        .put(BATCH_SIZE_KEY, 1)
        .build();
  }

  public static ReadOnlyProperties getConnectionPropertiesWithBatchSize3AndPageSize3() {
    return ReadOnlyProperties.builder()
        .put(INSTANCE_NAME_KEY, "test_instance_name")
        .put(CLIENT_ID_KEY, "test_client_id")
        .put(CLIENT_SECRET_KEY, "test_client_secret")
        .put(PAGE_SIZE_KEY, 3)
        .put(CONNECTION_TIMEOUT_KEY_NAME, 1)
        .put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1)
        .put(RETRY_DELAY_KEY_NAME, 1)
        .put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost")
        .put(URL_PORT_KEY, WIREMOCK_PORT_NO)
        .put(BATCH_SIZE_KEY, 3)
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
      throw new RuntimeException("Failed to read test resource: " + file, e);
    }
  }

  /**
   * Returns the user schema object with all user fields.
   *
   * @return SchemaObject for user
   */
  public static SchemaObject getSchemaObjectForUser() {
    Field field1 = new Field("Country", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("Source", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("State", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("IdentityType", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 =
        new Field("Zip", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("Active", "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("Phone", "", new HashSet<>(), false,
        Field.Type.STRING);
    Field field8 =
        new Field("Name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field9 =
        new Field("EmployeeNumber", "", new HashSet<>(), false, Field.Type.STRING);
    Field field10 = new Field("Gender", "", new HashSet<>(), false, Field.Type.STRING);
    Field field11 =
        new Field("City", "", new HashSet<>(), false, Field.Type.STRING);
    Field field12 =
        new Field("UserName", "", new HashSet<>(),
            false, Field.Type.STRING);
    Field field13 =
        new Field("Title", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field14 =
        new Field("SysId", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field15 =
        new Field("FederatedId", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field16 =
        new Field("MobilePhone", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field17 =
        new Field("Street", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field18 =
        new Field("FirstName", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field19 =
        new Field("Introduction", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field20 =
        new Field("LockedOut", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field21 =
        new Field("LastName", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field22 =
        new Field("MiddleName", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field23 =
        new Field("InternalIntegrationUser", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field24 =
        new Field("Email", "", new HashSet<>(), false,
            Field.Type.STRING);
    Field field25 =
        new Field("UserRoles", "", new HashSet<>(), false,
            Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12, field13, field14, field15, field16, field17, field18,
            field19, field20, field21, field22, field23, field24, field25));
    return new SchemaObject("user", "vdUser", "", "", "", Collections.singletonList("username"),
        new ArrayList<>(), fields);
  }

  /**
   * Returns the group schema object with all group fields.
   *
   * @return SchemaObject for group
   */
  public static SchemaObject getSchemaObjectForGroup() {
    Field field1 = new Field("Name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("Description", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 = new Field("Active", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("SysId", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("Source", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 = new Field("Type", "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("IncludeMembers", "", new HashSet<>(), false, Field.Type.STRING);
    Field field8 = new Field("SysCreatedBy", "", new HashSet<>(), false, Field.Type.STRING);
    Field field9 = new Field("GroupUsers", "", new HashSet<>(), false, Field.Type.STRING);
    Field field10 = new Field("GroupRoles", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10));
    return new SchemaObject("group", "vdGroup", "", "", "", Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  /**
   * Returns the role schema object with all role fields.
   *
   * @return SchemaObject for role
   */
  public static SchemaObject getSchemaObjectForRole() {
    Field field1 = new Field("Name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("Description", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 = new Field("SysId", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("Grantable", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("ElevatedPrivilege", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 = new Field("Suffix", "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("ScopedAdmin", "", new HashSet<>(), false, Field.Type.STRING);
    Field field8 = new Field("CanDelegate", "", new HashSet<>(), false, Field.Type.STRING);
    Field field9 = new Field("IncludesRoles", "", new HashSet<>(), false, Field.Type.STRING);
    Field field10 = new Field("FederatedId", "", new HashSet<>(), false, Field.Type.STRING);
    Field field11 = new Field("SysCreatedBy", "", new HashSet<>(), false, Field.Type.STRING);
    Field field12 = new Field("SysPolicy", "", new HashSet<>(), false, Field.Type.STRING);
    Field field13 = new Field("ChildRoles", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12, field13));
    return new SchemaObject("role", "vdRole", "", "", "", Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  /**
   * Returns a schema object that has two primary keys.
   *
   * @param schemaObjectName Name of the schema object
   * @param objectClassName  Object class name of the schema object
   * @return SchemaObject with the primary keys sysid and name
   */
  public static SchemaObject getSchemaObjectWithMultiplePrimaryKeys(String schemaObjectName,
                                                                    String objectClassName) {
    Field field1 = new Field("name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("sysid", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2));
    return new SchemaObject(schemaObjectName, objectClassName, "", "", "",
        Arrays.asList("sysid", "name"),
        new ArrayList<>(), fields);
  }

  /**
   * Returns a schema object whose primary key isn't a valid key.
   *
   * @param schemaObjectName Name of the schema object
   * @param objectClassName  Object class name of the schema object
   * @return SchemaObject with the primary key invalidkey
   */
  public static SchemaObject getSchemaObjectWithInvalidPrimaryKey(String schemaObjectName,
                                                                  String objectClassName) {
    Field field1 = new Field("invalidkey", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Collections.singletonList(field1));
    return new SchemaObject(schemaObjectName, objectClassName, "", "", "",
        Collections.singletonList("invalidkey"),
        new ArrayList<>(), fields);
  }

  /**
   * Method to return user when get users endpoint is hit and sysid is passed as a query parameter.
   *
   * @return Response data containing user
   */
  public static ResponseData getUsersResponseWithoutLinkHeader() {
    return new ResponseData(getResponseFromFile("user.json"), "", 200,
        new Headers.Builder().build());
  }

  /**
   * Method to return user as part of api response when say update/create user operation is
   * performed.
   *
   * @return Response data containing updated/created user
   */
  public static ResponseData getUser(String fileName, int responseCode) {
    return new ResponseData(StringUtils.isNotBlank(fileName) ? getResponseFromFile(fileName) : "",
        "", responseCode,
        new Headers.Builder().build());
  }

  /**
   * Returns the users from the user.json response file.
   *
   * @return ServiceNowResponse containing the users
   */
  public static ServiceNowResponse<UserRow> getUsers() {
    ResponseData responseData = getUsersResponseWithoutLinkHeader();
    return SERVICE_NOW_RESPONSE_GSON.fromJson(responseData.getApiResponse(),
        new TypeToken<ServiceNowResponse<UserRow>>() {
        }.getType());
  }

  public static List<Attribute> getBaseUserCreationAttributes() {
    return Lists.newArrayList(
        Attribute.create("UserName", "test_user_name"),
        Attribute.create("FirstName", "Alex"),
        Attribute.create("MiddleName", "test_middle_name"),
        Attribute.create("LastName", "N."),
        Attribute.create("Email", "alex@example.com"),
        Attribute.create("Active", "true"),
        Attribute.create("Phone", "8005550100"),
        Attribute.create("MobilePhone", "8005550101"),
        Attribute.create("Street", "123 Identity Ave"),
        Attribute.create("City", "San Jose"),
        Attribute.create("State", "CA"),
        Attribute.create("Zip", "92600"),
        Attribute.create("Country", "US"),
        Attribute.create("Title", "Software Engineer"),
        Attribute.create("EmployeeNumber", "EMP001"),
        Attribute.create("Gender", "Male"),
        Attribute.create("Introduction", "Mr."),
        Attribute.create("IdentityType", "human"),
        Attribute.create("LockedOut", "false"),
        Attribute.create("InternalIntegrationUser", "false"),
        Attribute.create("Source", "test_source")
    );
  }

  public static List<Attribute> getMinimalUserAttributes() {
    return Lists.newArrayList(
        Attribute.create("FirstName", "Alex"),
        Attribute.create("LastName", "N."),
        Attribute.create("Email", "alex@example.com")
    );
  }

  /**
   * Method to get schema object for user without UserRoles field.
   *
   * @return SchemaObject for user without UserRoles
   */
  public static SchemaObject getSchemaObjectForUserWithoutUserRoles() {
    Field field1 = new Field("Country", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("UserName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 = new Field("Email", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("FirstName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("LastName", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(Arrays.asList(field1, field2, field3, field4, field5));
    return new SchemaObject("user", "vdUser", "", "", "", Collections.singletonList("username"),
        new ArrayList<>(), fields);
  }

  /**
   * Method to get schema object for user with different case UserRoles field.
   *
   * @return SchemaObject for user with different case UserRoles
   */
  public static SchemaObject getSchemaObjectForUserWithDifferentCaseUserRoles() {
    Field field1 = new Field("Country", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("UserName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 = new Field("Email", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("FirstName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("LastName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 = new Field("userroles", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6));
    return new SchemaObject("user", "vdUser", "", "", "", Collections.singletonList("username"),
        new ArrayList<>(), fields);
  }
}
