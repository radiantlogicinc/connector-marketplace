package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.DirectoryServiceRecord;
import com.radiantlogic.utility.ResponseData;
import java.util.List;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class GetDirectoryServicesTest {

  RequestBuilder requestBuilder;
  RequestExecutor requestExecutor;
  GetRequest getRequest;
  String directoryServiceName = "RL_CArk";

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    requestExecutor = mock(RequestExecutor.class);
    getRequest = new GetRequest(requestBuilder, requestExecutor);
    when(requestExecutor.canConnect()).thenReturn(true);
    when(requestExecutor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServices() {
    HttpUrl directoryServicesUrl = TestUtils.getDirectoryServicesUrl();

    ResponseData directoryServicesResponse = TestUtils.getDirectoryServicesResponse();
    RequestBody payload = TestUtils.getDirectoryServicesRequestBody();

    when(requestBuilder.getDirectoryServicesUrl()).thenReturn(directoryServicesUrl);
    when(requestBuilder.getDirectoryServicesRequestBody()).thenReturn(payload);
    when(requestExecutor.execute(any())).thenReturn(directoryServicesResponse);

    List<DirectoryServiceRecord> directoryServiceRecords = getRequest.getDirectoryServices("");
    assertEquals(3, directoryServiceRecords.size());
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServiceByName() {
    HttpUrl directoryServicesUrl = TestUtils.getDirectoryServicesUrl();

    ResponseData directoryServicesResponse = TestUtils.getDirectoryServicesResponse();
    RequestBody payload = TestUtils.getDirectoryServicesRequestBody();

    when(requestBuilder.getDirectoryServicesUrl()).thenReturn(directoryServicesUrl);
    when(requestBuilder.getDirectoryServicesRequestBody()).thenReturn(payload);
    when(requestExecutor.execute(any())).thenReturn(directoryServicesResponse);

    List<DirectoryServiceRecord> directoryServiceRecords =
        getRequest.getDirectoryServices(directoryServiceName);
    assertEquals(1, directoryServiceRecords.size());
    assertEquals(directoryServiceName, directoryServiceRecords.get(0).getRow().getName());
  }

  @Test
  void testGetDirectoryServicesWithInvalidCredentials() {
    when(requestExecutor.canConnect()).thenReturn(false);
    assertThrows(CyberArkPrivilegeCloudException.class, () -> getRequest.getDirectoryServices(""));
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServicesWithBadRequest() {
    HttpUrl directoryServicesUrl = TestUtils.getDirectoryServicesUrl();

    ResponseData directoryServicesResponse = TestUtils.getDirectoryServicesResponseWithBadRequest();
    RequestBody payload = TestUtils.getDirectoryServicesRequestBody();

    when(requestBuilder.getDirectoryServicesUrl()).thenReturn(directoryServicesUrl);
    when(requestBuilder.getDirectoryServicesRequestBody()).thenReturn(payload);
    when(requestExecutor.execute(any())).thenReturn(directoryServicesResponse);

    assertThrows(CyberArkPrivilegeCloudException.class, () -> getRequest.getDirectoryServices(""));
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServiceByInvalidName() {
    HttpUrl directoryServicesUrl = TestUtils.getDirectoryServicesUrl();

    ResponseData directoryServicesResponse = TestUtils.getDirectoryServicesResponse();
    RequestBody payload = TestUtils.getDirectoryServicesRequestBody();

    when(requestBuilder.getDirectoryServicesUrl()).thenReturn(directoryServicesUrl);
    when(requestBuilder.getDirectoryServicesRequestBody()).thenReturn(payload);
    when(requestExecutor.execute(any())).thenReturn(directoryServicesResponse);

    List<DirectoryServiceRecord> directoryServiceRecords =
        getRequest.getDirectoryServices("invalid_directory_service_name");
    assertEquals(0, directoryServiceRecords.size());
  }

}
