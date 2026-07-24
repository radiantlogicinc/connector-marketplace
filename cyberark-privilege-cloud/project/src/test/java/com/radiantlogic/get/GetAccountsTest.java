package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.Accounts;
import com.radiantlogic.utility.ResponseData;
import java.util.HashMap;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class GetAccountsTest {

  RequestBuilder requestBuilder;
  RequestExecutor requestExecutor;
  GetRequest getRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  String accountId = "16_4";

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    requestExecutor = mock(RequestExecutor.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = new GetRequest(requestBuilder, requestExecutor);
    when(requestExecutor.canConnect()).thenReturn(true);
    when(requestExecutor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
  }

  @Test
  @SneakyThrows
  void testGetAccountsWithNextLinkReturned() {
    HttpUrl accountsUrl = TestUtils.getAccountsUrl(null, null, 1);

    ResponseData accountsWithNextLink = TestUtils.getAccountsWithNextLink();

    when(requestBuilder.getAccountsUrl(any(), any(), any())).thenReturn(accountsUrl);
    when(requestExecutor.execute(any())).thenReturn(accountsWithNextLink);

    Accounts accountsResult = getRequest.getAccounts("", null, new HashMap<>());
    assertEquals("API/Accounts?offset=1&limit=1&sort=name", accountsResult.getNextLink());
  }

  @Test
  @SneakyThrows
  void testGetAccountsWithoutNextLinkReturned() {
    HttpUrl accountsUrl = TestUtils.getAccountsUrl(null, null, 1);

    ResponseData accountsWithoutNextLink = TestUtils.getAccountsWithoutNextLink();

    when(requestBuilder.getAccountsUrl(any(), any(), any())).thenReturn(accountsUrl);
    when(requestExecutor.execute(any())).thenReturn(accountsWithoutNextLink);

    Accounts accountsResult = getRequest.getAccounts("", null, new HashMap<>());
    assertNull(accountsResult.getNextLink());
  }

  @Test
  @SneakyThrows
  void testGetAccountsWithoutInvalidCredentials() {
    when(requestExecutor.canConnect()).thenReturn(false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getAccounts("", null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void testGetAccountsWithBadRequest() {
    HttpUrl accountsUrl = TestUtils.getAccountsUrl(null, null, 1);

    ResponseData accountsWithoutNextLink = TestUtils.getAccountsWithBadRequest();

    when(requestBuilder.getAccountsUrl(any(), any(), any())).thenReturn(accountsUrl);
    when(requestExecutor.execute(any())).thenReturn(accountsWithoutNextLink);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getAccounts("", null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void testGetAccount() {
    HttpUrl accountUrl = TestUtils.getAccountsUrl(accountId, null, 1);

    ResponseData account = TestUtils.getAccount();

    when(requestBuilder.getAccountsUrl(any(), any(), any())).thenReturn(accountUrl);
    when(requestExecutor.execute(any())).thenReturn(account);

    Accounts accountsResult = getRequest.getAccounts(accountId, null, new HashMap<>());
    assertEquals("s2", accountsResult.getAccountsList().get(0).getName());
  }

  @Test
  @SneakyThrows
  void testGetDeletedAccount() {
    HttpUrl accountUrl = TestUtils.getAccountsUrl(accountId, null, 1);

    ResponseData account = TestUtils.getDeletedAccount();

    when(requestBuilder.getAccountsUrl(any(), any(), any())).thenReturn(accountUrl);
    when(requestExecutor.execute(any())).thenReturn(account);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getAccounts(accountId, null, new HashMap<>()));
  }


}
