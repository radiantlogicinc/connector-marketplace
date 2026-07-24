package com.radiantlogic.pagedresponse;

import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
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
import com.radiantlogic.utility.SafeMembers;
import com.radiantlogic.utility.SafeRow;
import com.radiantlogic.utility.SafeUtils;
import com.radiantlogic.utility.SafesResult;
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
class SafePagedResponseTest {

  PagedResponse pagedResponse;
  RequestExecutor requestExecutor;
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
    requestExecutor = mock(RequestExecutor.class);
    baseDN = mock(DN.class);
    rdn = mock(RDN.class);
    attribute = mock(Attribute.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = mock(GetRequest.class);
    searchFilter = mock(SearchFilter.class);
    when(requestExecutor.canConnect()).thenReturn(true);
    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.SUB);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDN);
    when(baseDN.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(targetSchemaObjects.get(any())).thenReturn(Collections.singletonList("safe"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForSafe()));
    when(mockSearchRequest.getFilter()).thenReturn(searchFilter);
    when(searchFilter.getAttribute()).thenReturn("objectClass");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn("*");
    when(searchFilter.getComponents()).thenReturn(new ArrayList<>());
  }

  @Test
  @SneakyThrows
  void testGetSafesWithPagingWithNoMembers() {
    CyberArkResponse safesWithNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("safes_with_next_link.json"),
            CyberArkResponse.class);
    CyberArkResponse safesWithoutNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_safes_without_next_link.json"),
            CyberArkResponse.class);
    List<SafeRow> safesFromNextLinkResponse = safesWithNextLink.getSafes();
    List<SafeRow> safesFromWithoutNextLinkResponse = safesWithoutNextLink.getSafes();
    SafeUtils.formatSafesDates(safesFromNextLinkResponse);
    SafeUtils.formatSafesDates(safesFromWithoutNextLinkResponse);

    SafesResult safesResultWithNextLink = new SafesResult();
    safesResultWithNextLink.setSafes(safesFromNextLinkResponse);
    safesResultWithNextLink.setNextLink(safesWithNextLink.getNextLink());

    SafesResult safesResultWithoutNextLink = new SafesResult();
    safesResultWithoutNextLink.setSafes(safesFromWithoutNextLinkResponse);

    when(getRequest.getSafes(any(), any(), any())).thenReturn(safesResultWithNextLink,
        safesResultWithoutNextLink);
    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(2, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void testGetSafesWithPagingWithMembers() {
    CyberArkResponse safesWithNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("safes_with_next_link.json"),
            CyberArkResponse.class);
    CyberArkResponse safesWithoutNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_safes_without_next_link.json"),
            CyberArkResponse.class);
    SafeMembers safeMembers =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("safe_members.json"), SafeMembers.class);

    List<SafeRow> safesFromNextLinkResponse = safesWithNextLink.getSafes();
    List<SafeRow> safesFromWithoutNextLinkResponse = safesWithoutNextLink.getSafes();
    SafeUtils.formatSafesDates(safesFromNextLinkResponse);
    SafeUtils.formatSafesDates(safesFromWithoutNextLinkResponse);
    safesFromNextLinkResponse.get(0).setMembers(safeMembers.getMembers());

    SafesResult safesResultWithNextLink = new SafesResult();
    safesResultWithNextLink.setSafes(safesFromNextLinkResponse);
    safesResultWithNextLink.setNextLink(safesWithNextLink.getNextLink());

    SafesResult safesResultWithoutNextLink = new SafesResult();
    safesResultWithoutNextLink.setSafes(safesFromWithoutNextLinkResponse);

    when(getRequest.getSafes(any(), any(), any())).thenReturn(safesResultWithNextLink,
        safesResultWithoutNextLink);
    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(2, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void testGetSafesWithException() {

    when(getRequest.getSafes(any(), any(), any())).thenThrow(
        new CyberArkPrivilegeCloudException(""));

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);

    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testGetSafeByName() {
    String safeName = "test_safe";
    SafeRow safeRow =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("safe.json"), SafeRow.class);

    List<SafeRow> safesList = Collections.singletonList(safeRow);
    SafeUtils.formatSafesDates(safesList);

    SafesResult safesResult = new SafesResult();
    safesResult.setSafes(safesList);

    when(getRequest.getSafes(any(), any(), any())).thenReturn(safesResult);

    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.BASE);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDN);
    when(baseDN.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(attribute.getValues()).thenReturn(Collections.singletonList(safeName));

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  private void stubCommonMocks(String attributeName, String attributeValue)
      throws CyberArkPrivilegeCloudException, IOException {
    when(searchFilter.getAttribute()).thenReturn(attributeName);
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn(attributeValue);
    CyberArkResponse safesWithoutNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_safes_without_next_link.json"),
            CyberArkResponse.class);
    List<SafeRow> safesFromWithoutNextLinkResponse = safesWithoutNextLink.getSafes();
    SafeUtils.formatSafesDates(safesFromWithoutNextLinkResponse);
    SafesResult safesResultWithoutNextLink = new SafesResult();
    safesResultWithoutNextLink.setSafes(safesFromWithoutNextLinkResponse);

    when(getRequest.getSafes(any(), any(), any())).thenReturn(safesResultWithoutNextLink);
  }

  @Test
  @SneakyThrows
  void testGetSafesByPartialSafeNameUsingFilters() {
    stubCommonMocks("name", "engine");
    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void testGetSafesByExactSafeNameUsingFilters() {
    stubCommonMocks("name", "Notification Engine");
    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(1, actualData.size(), "");
  }
}