package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.util.TestUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.Request;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateUserRequestTest {

  private static final String USERS_DN = "user=test_user_name,ou=Users,ou=ServiceNow";

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  CreateUserRequest createUserRequest;
  Request request;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;

  @BeforeEach
  void setUp() throws ServiceNowException {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    request = mock(Request.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    SchemaObject schemaObject = TestUtil.getSchemaObjectForUser();

    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(schemaObject));
    when(requestBuilder.getCreateUserRequest(any())).thenReturn(request);
    createUserRequest =
        new CreateUserRequest(requestBuilder, executor, targetSchemaObjects, schema);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithSuccess() {
    ResponseData responseData = TestUtil.getUser("create_user_success.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getBaseUserCreationAttributes())
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
    verify(executor).execute(request);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithBadRequest() {
    ResponseData responseData = TestUtil.getUser("", 400);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getMinimalUserAttributes();
    attributes.add(Attribute.create("UserName", "test_user_name"));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithoutUserName() {
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getMinimalUserAttributes())
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidIdentityTypeAndSuccess() {
    ResponseData responseData = TestUtil.getUser("create_user_invalid_identity_type.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("IdentityType")
            ? Attribute.create("IdentityType", "invalid_identity_type") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidGenderAndSuccess() {
    ResponseData responseData = TestUtil.getUser("create_user_invalid_gender.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Gender") ? Attribute.create("Gender", "invalid_gender") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidIntroductionAndSuccess() {
    ResponseData responseData = TestUtil.getUser("create_user_with_invalid_introduction.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Introduction")
            ? Attribute.create("Introduction", "invalid_introduction") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidCountryAndSuccess() {
    ResponseData responseData = TestUtil.getUser("create_user_with_invalid_country.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Country") ? Attribute.create("Country", "US.") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithDifferentRdnAndUserName() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("UserName") ? Attribute.create("UserName", "test_user_name1") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidActiveStatus() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Active") ? Attribute.create("Active", "invalid_active_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidLockedOutStatus() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("LockedOut")
            ? Attribute.create("LockedOut", "invalid_locked_out_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidInternalIntegrationUserStatus() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("InternalIntegrationUser")
            ? Attribute.create("InternalIntegrationUser",
                "invalid_internal_integration_user_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithIoException() {
    when(executor.execute(request)).thenThrow(new IOException("Network error"));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getBaseUserCreationAttributes())
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithServiceNowException() {
    when(executor.execute(request)).thenThrow(new ServiceNowException("API error"));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getBaseUserCreationAttributes())
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidDnFormat() {
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from("invalidkey=test_user_name,ou=Users,ou=ServiceNow"))
        .attributes(TestUtil.getBaseUserCreationAttributes())
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithMultipleValuedAttribute() {
    List<Attribute> attributes = Lists.newArrayList(
        Attribute.create("UserName", "test_user_name"),
        Attribute.create("FirstName", Lists.newArrayList("Alex", "Kim")),
        Attribute.create("LastName", "N."),
        Attribute.create("Email", "alex@example.com")
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithAttributeNotInSchema() {
    ResponseData responseData = TestUtil.getUser("create_user_success.json", 201);

    when(executor.execute(request)).thenReturn(responseData);

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.add(Attribute.create("UnknownAttribute", "some_value"));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = createUserRequest.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }
}
