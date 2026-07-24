package com.radiantlogic.pagedresponse;

import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.RDN;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import java.io.IOException;
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
@SuppressWarnings(PMD_KEY)
class UserPagedResponseTest {

  PagedResponse pagedResponse;
  RequestExecutor executor;
  LdapSearchRequest mockSearchRequest;
  Attribute attribute;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  GetRequest getRequest;
  DN baseDN;
  RDN rdn;
  SearchFilter searchFilter;

  Type listType = new TypeToken<List<Map<String, ?>>>() {
  }.getType();

  @BeforeEach
  void setupMocks() {
    mockSearchRequest = mock(LdapSearchRequest.class);
    executor = mock(RequestExecutor.class);
    baseDN = mock(DN.class);
    rdn = mock(RDN.class);
    attribute = mock(Attribute.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = mock(GetRequest.class);
    searchFilter = mock(SearchFilter.class);
    when(executor.canConnect()).thenReturn(true);
    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.SUB);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDN);
    when(baseDN.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(targetSchemaObjects.get(any())).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForUser()));
    when(mockSearchRequest.getFilter()).thenReturn(searchFilter);
    when(searchFilter.getAttribute()).thenReturn("objectClass");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn("*");
    when(searchFilter.getComponents()).thenReturn(new ArrayList<>());
  }

  @Test
  @SneakyThrows
  void testGetUsersWithPaging() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> emptyUsers =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("empty_users.json"),
            CyberArkResponse.class).getResultData().getRecords();
    when(getRequest.getUsers(anyInt(), any(), any(), any(), any())).thenReturn(users, emptyUsers);
    when(executor.canConnect()).thenReturn(true);
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void testGetUsersWithPagingWithException() {
    when(getRequest.getUsers(anyInt(), any(), any(), any(), any())).thenThrow(
        new CyberArkPrivilegeCloudException(""));
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);

    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testGetUserByUsername() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getUsers(anyInt(), any(), any(), any(), any())).thenReturn(users);
    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.BASE);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDN);
    when(baseDN.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(attribute.getValues()).thenReturn(
        Collections.singletonList("abcc1s232@cyberark.cloud.39470"));

    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  private void stubCommonMocks(String attributeName, String attributeValue)
      throws CyberArkPrivilegeCloudException, IOException {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> emptyUsers =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("empty_users.json"),
            CyberArkResponse.class).getResultData().getRecords();
    when(getRequest.getUsers(anyInt(), any(), any(), any(), any())).thenReturn(users, emptyUsers);
    when(executor.canConnect()).thenReturn(true);
    when(searchFilter.getAttribute()).thenReturn(attributeName);
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn(attributeValue);
  }

  @Test
  @SneakyThrows
  void getUserByUsernameUsingFilters() {
    stubCommonMocks("name", "abcc1s232@cyberark.cloud.39470");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserByIdUsingFilters() {
    stubCommonMocks("id", "8d64055b-1234-4550-9dbb-5c8f2bce35db");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserByDisplayNameUsingFilters() {
    stubCommonMocks("displayname", "abcc1s232");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserByMailUsingFilters() {
    stubCommonMocks("mail", "abc@gmail.com");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserByMobileNumberUsingFilters() {
    stubCommonMocks("mobilenumber", "+91123456");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserBySourceUsingFilters() {
    stubCommonMocks("source", "CyberArk Cloud Directory");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void getUserByRiskLevelRankUsingFilters() {
    stubCommonMocks("risklevelrank", "5");
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }
}
