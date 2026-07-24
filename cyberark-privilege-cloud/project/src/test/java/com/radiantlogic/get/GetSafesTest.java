package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.SafesResult;
import java.util.HashMap;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class GetSafesTest {

  RequestBuilder requestBuilder;
  RequestExecutor requestExecutor;
  GetRequest getRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  String safeName = "test_safe";

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
  void getSafeWithMembers() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 100);
    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 100);

    ResponseData safe = TestUtils.getSafe();

    ResponseData safeMembers = new ResponseData();
    safeMembers.setResponseCode(200);
    safeMembers.setApiResponse(TestUtils.getResponseFromFile("safe_members.json"));

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safe, safeMembers);

    SafesResult safesResult = getRequest.getSafes(safeName, null, new HashMap<>());
    assertEquals(1, safesResult.getSafes().size());
  }

  @Test
  @SneakyThrows
  void getSafesWithMembersWithoutNextLink() {
    HttpUrl safesUrlWithoutNextLink = TestUtils.getSafesUrl(null, null, 2);

    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 2);

    ResponseData safesWithoutNextLink = TestUtils.getSafesWithoutNextLink();
    ResponseData safeMembersWithoutNextLink = TestUtils.getSafesMembersWithoutNextLink();

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safesUrlWithoutNextLink);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safesWithoutNextLink,
        safeMembersWithoutNextLink);

    SafesResult safesResult = getRequest.getSafes(null, null, new HashMap<>());
    assertEquals(1, safesResult.getSafes().size());
  }

  @Test
  @SneakyThrows
  void getSafesWithMembersWithNextLinkPassed() {
    HttpUrl safesUrlWithNextLink =
        TestUtils.getSafesUrl(null, "API/Safes/?offset=2&limit=2&useCache=False", 2);

    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 2);

    ResponseData safesWithoutNextLink = TestUtils.getSafesWithoutNextLink();
    ResponseData safeMembersWithoutNextLink = TestUtils.getSafesMembersWithoutNextLink();

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safesUrlWithNextLink);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safesWithoutNextLink,
        safeMembersWithoutNextLink);

    SafesResult safesResult = getRequest.getSafes(null, null, new HashMap<>());
    assertEquals(1, safesResult.getSafes().size());
  }

  @Test
  @SneakyThrows
  void getSafesWithNextLinkAndNoMembers() {
    HttpUrl safesUrlWithNextLink = TestUtils.getSafesUrl(null, null, 1);

    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 1);

    ResponseData safesWithNextLink = new ResponseData();
    safesWithNextLink.setResponseCode(200);
    safesWithNextLink.setApiResponse(TestUtils.getResponseFromFile("safes_with_next_link.json"));

    ResponseData emptySafeMembers = new ResponseData();
    emptySafeMembers.setResponseCode(200);
    emptySafeMembers.setApiResponse(TestUtils.getResponseFromFile("empty_safe_members.json"));

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safesUrlWithNextLink);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safesWithNextLink, emptySafeMembers);

    SafesResult safesResult = getRequest.getSafes(null, null, new HashMap<>());
    assertEquals("API/Safes/?offset=1&limit=1&useCache=False", safesResult.getNextLink());
  }

  @Test
  @SneakyThrows
  void getSafeWithPaginatedMembers() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 1);
    HttpUrl safeMembersUrlWithoutNextLink = TestUtils.getSafeMembersUrl(safeName, null, 1);
    HttpUrl safeMembersUrlWithNextLink = TestUtils.getSafeMembersUrl(safeName,
        "API/Safes/test_safe/Members?offset=1&limit=1&useCache=False", 1);

    ResponseData safe = TestUtils.getSafe();

    ResponseData safeMembersWithNextLink = TestUtils.getSafeMembersWithNextLink();

    ResponseData safeMemberWithoutNextLink = TestUtils.getSafeMemberWithoutNextLink();

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrlWithoutNextLink,
        safeMembersUrlWithNextLink);
    when(requestExecutor.execute(any())).thenReturn(safe, safeMembersWithNextLink,
        safeMemberWithoutNextLink);

    SafesResult safesResult = getRequest.getSafes(safeName, null, new HashMap<>());
    assertEquals(2, safesResult.getSafes().get(0).getMembers().size());
  }

  @Test
  @SneakyThrows
  void getSafeWithPaginatedMembersWithTokenExpiredInBetween() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 1);
    HttpUrl safeMembersUrlWithoutNextLink = TestUtils.getSafeMembersUrl(safeName, null, 1);
    HttpUrl safeMembersUrlWithNextLink = TestUtils.getSafeMembersUrl(safeName,
        "API/Safes/test_safe/Members?offset=1&limit=1&useCache=False", 1);

    ResponseData safe = TestUtils.getSafe();

    ResponseData safeMembersWithNextLink = TestUtils.getSafeMembersWithNextLink();

    ResponseData tokenExpiredResponse = new ResponseData();
    tokenExpiredResponse.setResponseCode(401);

    ResponseData safeMemberWithoutNextLink = TestUtils.getSafeMemberWithoutNextLink();

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrlWithoutNextLink,
        safeMembersUrlWithNextLink);
    when(requestExecutor.execute(any())).thenReturn(safe, safeMembersWithNextLink,
        tokenExpiredResponse, safeMemberWithoutNextLink);

    SafesResult safesResult = getRequest.getSafes(safeName, null, new HashMap<>());
    assertEquals(2, safesResult.getSafes().get(0).getMembers().size());
  }

  @Test
  @SneakyThrows
  void getSafeWithInvalidCredentials() {
    when(requestExecutor.canConnect()).thenReturn(false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getSafes(safeName, null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void getSafeWithException() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 100);

    ResponseData safes = new ResponseData();
    safes.setResponseCode(400);

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestExecutor.execute(any())).thenReturn(safes);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getSafes(safeName, null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void getSafeMembersWithInvalidCredentialsInBetween() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 100);

    ResponseData safe = TestUtils.getSafe();

    when(requestExecutor.canConnect()).thenReturn(true, false);
    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestExecutor.execute(any())).thenReturn(safe);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getSafes(safeName, null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void getSafeMembersWithBadRequest() {

    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 100);
    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 100);

    ResponseData safe = TestUtils.getSafe();
    ResponseData safeMembers = new ResponseData();
    safeMembers.setResponseCode(400);

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safe, safeMembers);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getSafes(safeName, null, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void getSafeWithNoMembers() {
    HttpUrl safeUrl = TestUtils.getSafesUrl(safeName, null, 100);
    HttpUrl safeMembersUrl = TestUtils.getSafeMembersUrl(safeName, null, 100);

    ResponseData safe = TestUtils.getSafe();
    ResponseData safeMembers = new ResponseData();
    safeMembers.setResponseCode(200);
    safeMembers.setApiResponse(TestUtils.getResponseFromFile("empty_safe_members.json"));

    when(requestBuilder.getSafesUrl(any(), any(), any())).thenReturn(safeUrl);
    when(requestBuilder.getSafeMembersUrl(any(), any())).thenReturn(safeMembersUrl);
    when(requestExecutor.execute(any())).thenReturn(safe, safeMembers);

    SafesResult safesResult = getRequest.getSafes(safeName, null, new HashMap<>());
    assertEquals(1, safesResult.getSafes().size());
  }


}
