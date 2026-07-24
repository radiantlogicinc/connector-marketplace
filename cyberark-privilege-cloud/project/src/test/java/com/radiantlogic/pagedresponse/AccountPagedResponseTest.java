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
import com.radiantlogic.utility.AccountRow;
import com.radiantlogic.utility.AccountUtils;
import com.radiantlogic.utility.Accounts;
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
class AccountPagedResponseTest {

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
    when(targetSchemaObjects.get(any())).thenReturn(Collections.singletonList("account"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtils.getSchemaObjectForAccount()));
    when(mockSearchRequest.getFilter()).thenReturn(searchFilter);
    when(searchFilter.getAttribute()).thenReturn("objectClass");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn("*");
    when(searchFilter.getComponents()).thenReturn(new ArrayList<>());
  }

  @Test
  @SneakyThrows
  void testGetAccountsWithPaging() {
    Accounts accountsWithNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("accounts_with_next_link.json"),
            Accounts.class);
    Accounts accountsWithoutNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("accounts_without_next_link.json"),
            Accounts.class);

    AccountUtils.formatAccountsDates(accountsWithNextLink.getAccountsList());
    AccountUtils.formatAccountsDates(accountsWithoutNextLink.getAccountsList());

    Accounts accountsResultWithNextLink = new Accounts();
    accountsResultWithNextLink.setAccountsList(accountsWithNextLink.getAccountsList());
    accountsResultWithNextLink.setNextLink(accountsWithNextLink.getNextLink());

    Accounts accountsResultWithoutNextLink = new Accounts();
    accountsResultWithoutNextLink.setAccountsList(accountsWithoutNextLink.getAccountsList());

    when(getRequest.getAccounts(any(), any(), any())).thenReturn(accountsResultWithNextLink,
        accountsResultWithoutNextLink);

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
  void testGetAccountById() {
    List<AccountRow> accounts =
        Collections.singletonList(
            GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("account.json"),
                AccountRow.class));

    AccountUtils.formatAccountsDates(accounts);

    Accounts accountsResult = new Accounts();
    accountsResult.setAccountsList(accounts);

    when(getRequest.getAccounts(any(), any(), any())).thenReturn(accountsResult);

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
  void testGetAccountWithException() {
    when(getRequest.getAccounts(any(), any(), any())).thenThrow(
        new CyberArkPrivilegeCloudException(""));

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void getAccountsBasedOnSafeNameUsingFilters() {

    when(searchFilter.getAttribute()).thenReturn("safename");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn("another_test_safe");

    Accounts accountsWithNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("accounts_with_next_link.json"),
            Accounts.class);
    Accounts accountsWithoutNextLink =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("accounts_without_next_link.json"),
            Accounts.class);

    AccountUtils.formatAccountsDates(accountsWithNextLink.getAccountsList());
    AccountUtils.formatAccountsDates(accountsWithoutNextLink.getAccountsList());

    Accounts accountsResultWithNextLink = new Accounts();
    accountsResultWithNextLink.setAccountsList(accountsWithNextLink.getAccountsList());
    accountsResultWithNextLink.setNextLink(accountsWithNextLink.getNextLink());

    Accounts accountsResultWithoutNextLink = new Accounts();
    accountsResultWithoutNextLink.setAccountsList(accountsWithoutNextLink.getAccountsList());

    when(getRequest.getAccounts(any(), any(), any())).thenReturn(accountsResultWithNextLink,
        accountsResultWithoutNextLink);

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);

    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(2, actualData.size(), "");
  }
}
