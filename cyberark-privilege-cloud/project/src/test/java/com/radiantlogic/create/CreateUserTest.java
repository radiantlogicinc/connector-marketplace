package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.CREATE_USER_ENDPOINT;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class CreateUserTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  CreateRequest createRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;

  static final String CREATE_USER_MOCK_RESPONSE_FILE_NAME = "create_user.json";
  static final String DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED = "Bearer ey..";
  static final String CREATE_USER_DN = "user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk";
  static final String USER_NAME_ATTRIBUTE_KEY = "user-name";
  static final String USER_DISPLAY_NAME_ATTRIBUTE_KEY = "display-name";
  static final String USER_MAIL_ATTRIBUTE_KEY = "user-mail";
  static final String USER_MOBILE_ATTRIBUTE_KEY = "user-mobile";
  static final String USER_NAME_VALUE = "abcc1s232@cyberark.cloud.39470";
  static final String USER_DISPLAY_NAME_VALUE = "test display name";
  static final String USER_EMAIL_VALUE = "abc@gmail.com";
  static final String USER_MOBILE_VALUE = "+91123456789";

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    getRequest = mock(GetRequest.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);

    createRequest =
        new CreateRequest(requestBuilder, getRequest, executor, targetSchemaObjects, schema);
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForUser()));
    when(executor.canConnect()).thenReturn(true);
  }

  @Test
  @SneakyThrows
  void createUser() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_USER_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile(CREATE_USER_MOCK_RESPONSE_FILE_NAME));

    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);
    when(requestBuilder.getCreateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateUserPayload(any())).thenReturn(getRequestBodyForCreateUser());

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void createUserWithNotAllowedAttribute() {

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE),
            Attribute.create("user-id", "test user id"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createUserWithCredentialsBecameInvalidDuringRunTime() {
    when(executor.canConnect()).thenReturn(true, false);

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createUserWithNoMailButWithMobile() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_USER_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile(CREATE_USER_MOCK_RESPONSE_FILE_NAME));

    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);
    when(requestBuilder.getCreateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateUserPayload(any())).thenReturn(getRequestBodyForCreateUser());

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void createUserWithNoMailAndMobile() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_USER_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile(CREATE_USER_MOCK_RESPONSE_FILE_NAME));

    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);
    when(requestBuilder.getCreateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateUserPayload(any())).thenReturn(getRequestBodyForCreateUser());

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void createUserWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_USER_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);
    when(requestBuilder.getCreateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateUserPayload(any())).thenReturn(getRequestBodyForCreateUser());

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createUserWithUniqueKeyAndRDNNotSame() {

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39471,ou=Users,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
                Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
                Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
                Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createUserWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39471,ou=Users,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
                Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
                Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE),
                Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  RequestBody getRequestBodyForCreateUser() {
    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setDisplayName(USER_DISPLAY_NAME_VALUE);
    userRecordRow.setMobile(USER_MOBILE_VALUE);
    userRecordRow.setMail(USER_EMAIL_VALUE);
    userRecordRow.setName(USER_NAME_VALUE);
    String jsonString = GOOGLE_JSON.toJson(userRecordRow);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }
}
