package com.radiantlogic.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.util.TestUtil;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  Request request;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    request = mock(Request.class);
    when(requestBuilder.getBatchSize()).thenReturn(1);
    getRequest = new GetRequest(requestBuilder, executor, requestBuilder.getBatchSize());
  }

  @Test
  @SneakyThrows
  void getUsersWithNextLink() {
    ResponseData responseData = TestUtil.getUsersResponseWithoutLinkHeader();

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getUsersRequest(any(), any())).thenReturn(request);

    ServiceNowResponse<UserRow> response = getRequest.getUsers(
        "http://localhost:50000/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=1", null,
        false);
    assertEquals(1, response.getResult().size());
  }

  @Test
  @SneakyThrows
  void getUsersWithoutNextLink() {
    ResponseData responseData = TestUtil.getUsersResponseWithoutLinkHeader();

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getUsersRequest(any(), any())).thenReturn(request);

    ServiceNowResponse<UserRow> response = getRequest.getUsers(
        null, null, false);
    assertEquals(1, response.getResult().size());
  }

  @Test
  @SneakyThrows
  void getUserByUserName() {
    ResponseData responseData = TestUtil.getUsersResponseWithoutLinkHeader();

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getUsersRequest(any(), any())).thenReturn(request);

    ServiceNowResponse<UserRow> response = getRequest.getUsers(
        null, "test_user_name", false);
    assertEquals(1, response.getResult().size());
  }

  @Test
  @SneakyThrows
  void getUserWithBadRequest() {
    ResponseData responseData = new ResponseData("{}", "", 400, new Headers.Builder().build());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getUsersRequest(any(), any())).thenReturn(request);

    assertThrows(ServiceNowException.class,
        () -> getRequest.getUsers(null, "test_user_name", false));
  }
}
