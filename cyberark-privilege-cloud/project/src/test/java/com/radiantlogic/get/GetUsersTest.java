package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.GET_USERS_BY_IDS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USER_BY_ID_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USER_BY_USERNAME_PAYLOAD;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.RED_ROCK_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class GetUsersTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = new GetRequest(requestBuilder, executor);
    when(executor.canConnect()).thenReturn(true);
    when(executor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
  }

  @Test
  @SneakyThrows
  void fetchUsersForParticularPage() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = TestUtils.getUsersResponse();

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody = getUsersPayload(1, null, null, new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUsersPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> users = getRequest.getUsers(1, null, null, new HashSet<>(), new HashMap<>());
    assertEquals(2, users.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchUsersForParticularPageWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody = getUsersPayload(1, null, null, new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUsersPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getUsers(1, null, null, new HashSet<>(), new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void fetchUserByUsername() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("another_user.json"));

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody =
        getUsersPayload(0, "abcc1s233@cyberark.cloud.39470", null, new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUsersPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> users =
        getRequest.getUsers(0, "abcc1s233@cyberark.cloud.39470", null, new HashSet<>(),
            new HashMap<>());
    assertEquals(1, users.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchUserById() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("user.json"));

    RequestBody requestBody =
        getUsersPayload(0, null, "8d64055b-1234-4550-9dbb-5c8f2bce35db", new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUsersPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> users =
        getRequest.getUsers(0, null, "8d64055b-1234-4550-9dbb-5c8f2bce35db", new HashSet<>(),
            new HashMap<>());
    assertEquals(1, users.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchUsersByIds() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = TestUtils.getUsersResponse();

    Set<String> userIds = new HashSet<>(Arrays.asList("8d64055b-1234-1234-1234-5c8f2bce35db",
        "8d64055b-2345-2345-2345-5c8f2bce35db"));
    RequestBody requestBody = getUsersPayload(0, null, null, userIds);

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUsersPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> users = getRequest.getUsers(0, null, null, userIds, new HashMap<>());
    assertEquals(2, users.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchUsersWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getUsers(0, "abcc1s233@cyberark.cloud.39470", null, new HashSet<>(),
            new HashMap<>()));
  }

  RequestBody getUsersPayload(final int pageNumber, final String username, final String userId,
                              final Set<String> userIds) {
    String jsonString;
    if (org.apache.commons.lang3.StringUtils.isNotBlank(username)) {
      jsonString = String.format(GET_USER_BY_USERNAME_PAYLOAD, username);
    } else if (org.apache.commons.lang3.StringUtils.isNotBlank(userId)) {
      jsonString = String.format(GET_USER_BY_ID_PAYLOAD, userId);
    } else if (userIds != null && !userIds.isEmpty()) {
      jsonString = String.format(GET_USERS_BY_IDS_PAYLOAD,
          userIds.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
    } else {
      jsonString = String.format(GET_USERS_PAYLOAD, pageNumber, requestBuilder.getPageSize(),
          requestBuilder.getPageSize());
    }
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }
}
