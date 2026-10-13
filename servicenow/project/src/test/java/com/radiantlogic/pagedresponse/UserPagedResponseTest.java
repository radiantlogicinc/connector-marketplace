package com.radiantlogic.pagedresponse;

import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.RDN;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.util.TestUtil;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserPagedResponseTest {

  PagedResponse pagedResponse;
  LdapSearchRequest mockSearchRequest;
  Attribute attribute;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  GetRequest getRequest;
  DN baseDn;
  RDN rdn;
  RequestBuilder requestBuilder;
  Type listType = new TypeToken<List<Map<String, ?>>>() {
  }.getType();

  @BeforeEach
  void setupMocks() {
    mockSearchRequest = mock(LdapSearchRequest.class);
    baseDn = mock(DN.class);
    rdn = mock(RDN.class);
    attribute = mock(Attribute.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = mock(GetRequest.class);
    requestBuilder = mock(RequestBuilder.class);
    when(requestBuilder.canConnectWithValidToken()).thenReturn(true);
    when(targetSchemaObjects.get(any())).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtil.getSchemaObjectForUser()));
  }

  @Test
  @SneakyThrows
  void getUsersWithPaging() {
    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.SUB);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDn);
    when(baseDn.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));

    ServiceNowResponse<UserRow> page1 =
        SERVICE_NOW_RESPONSE_GSON.fromJson(TestUtil.getResponseFromFile("user.json"),
            new com.google.gson.reflect.TypeToken<ServiceNowResponse<UserRow>>() {
            }.getType());
    page1.setNextLink(
        "<http://localhost:50000/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=0"
            + ">;rel=\"first\",<http://localhost:50000/api/now/table/sys_user?sysparm_limit"
            + "=1&sysparm_offset=1>;rel=\"next\"");

    ServiceNowResponse<UserRow> page2 =
        SERVICE_NOW_RESPONSE_GSON.fromJson(TestUtil.getResponseFromFile("another_user.json"),
            new com.google.gson.reflect.TypeToken<ServiceNowResponse<UserRow>>() {
            }.getType());

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(page1, page2);
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GSON.fromJson(response.getData(), listType));
    }
    assertEquals(2, actualData.size());
  }

  @Test
  @SneakyThrows
  void getUserByUserName() {
    ServiceNowResponse<UserRow> page1 =
        SERVICE_NOW_RESPONSE_GSON.fromJson(TestUtil.getResponseFromFile("user.json"),
            new com.google.gson.reflect.TypeToken<ServiceNowResponse<UserRow>>() {
            }.getType());

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(page1);
    stubCommonMocksForBaseRequest();

    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size());
  }

  @Test
  @SneakyThrows
  void getUserByUserNameWithBadRequest() {

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenThrow(new ServiceNowException(""));
    stubCommonMocksForBaseRequest();

    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder);
    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void getUserByUserNameWithInvalidCredentials() {

    when(requestBuilder.canConnectWithValidToken()).thenReturn(false);
    stubCommonMocksForBaseRequest();

    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder);
    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void getUserWithNullTargetSchemaObjects() {

    when(requestBuilder.canConnectWithValidToken()).thenReturn(false);
    stubCommonMocksForBaseRequest();
    when(targetSchemaObjects.get(any())).thenReturn(null);

    assertThrows(IllegalStateException.class,
        () -> new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder));
  }

  @Test
  @SneakyThrows
  void getUserByUserNameWithNullSchemaObject() {
    when(schema.getSchemaObject(any())).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class,
        () -> new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder));
  }

  @Test
  @SneakyThrows
  void getUserByUserNameWithObjectHavingMultiplePrimaryKeys() {
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectWithMultiplePrimaryKeys("user", "vdUser")));

    assertThrows(IllegalStateException.class,
        () -> new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder));
  }

  @Test
  @SneakyThrows
  void getUserByUserNameWithObjectHavingInvalidPrimaryKey() {
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectWithInvalidPrimaryKey("user", "vdUser")));

    assertThrows(IllegalStateException.class,
        () -> new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder));
  }

  private void stubCommonMocksForBaseRequest() {
    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.BASE);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDn);
    when(baseDn.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(attribute.getValues()).thenReturn(
        Collections.singletonList("test_user_name"));
  }

  @Test
  @SneakyThrows
  void testHasMoreWithInvalidCredentials() {
    when(requestBuilder.canConnectWithValidToken()).thenReturn(false);
    stubCommonMocksForBaseRequest();

    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            requestBuilder);

    assertThrows(IllegalStateException.class,
        () -> pagedResponse.hasMore());
  }
}
