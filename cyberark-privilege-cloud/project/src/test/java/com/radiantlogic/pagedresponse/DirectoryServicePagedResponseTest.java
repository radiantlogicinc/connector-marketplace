package com.radiantlogic.pagedresponse;

import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_KEY;
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
import com.radiantlogic.utility.DirectoryServiceRecord;
import com.radiantlogic.utility.DirectoryServiceResponse;
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
class DirectoryServicePagedResponseTest {

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
  String directoryServiceName = "CDS";

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
    when(targetSchemaObjects.get(any())).thenReturn(
        Collections.singletonList(DIRECTORY_SERVICES_KEY));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtils.getSchemaObjectForDirectoryService()));
    when(mockSearchRequest.getFilter()).thenReturn(searchFilter);
    when(searchFilter.getAttribute()).thenReturn("objectClass");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn("*");
    when(searchFilter.getComponents()).thenReturn(new ArrayList<>());
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServices() {
    //directory services is a not paginated endpoint

    List<DirectoryServiceRecord> directoryServiceRecords =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("directory_services.json"),
            DirectoryServiceResponse.class).getResult().getResults();

    when(getRequest.getDirectoryServices(any())).thenReturn(directoryServiceRecords);

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);

    List<Map<String, ?>> actualData = new ArrayList<>();
    while (pagedResponse.hasMore()) {
      LdapResponse<String> response = pagedResponse.next();
      actualData.addAll(GOOGLE_JSON.fromJson(response.getData(), listType));
    }
    assertEquals(3, actualData.size(), "");
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServiceWithName() {

// to get a directory service, same get all directory services endpoint will be used and
// particular directory service will be fetched from it by name

    List<DirectoryServiceRecord> directoryServiceRecords =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("directory_services.json"),
            DirectoryServiceResponse.class).getResult().getResults();

    when(mockSearchRequest.getSearchScope()).thenReturn(SearchScope.BASE);
    when(mockSearchRequest.getBaseDN()).thenReturn(baseDN);
    when(baseDN.getRDNs()).thenReturn(Collections.singletonList(rdn));
    when(rdn.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(attribute.getValues()).thenReturn(Collections.singletonList(directoryServiceName));

    when(getRequest.getDirectoryServices(any())).thenReturn(
        Collections.singletonList(directoryServiceRecords.get(0)));

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
  void testGetDirectoryServicesWithException() {

    when(getRequest.getDirectoryServices(any())).thenThrow(new CyberArkPrivilegeCloudException(""));

    pagedResponse = new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);

    LdapResponse<String> response = pagedResponse.next();
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testGetDirectoryServiceWithNameUsingFilters() {
    when(searchFilter.getAttribute()).thenReturn("name");
    when(searchFilter.getType()).thenReturn(SearchFilter.Type.EQUAL);
    when(searchFilter.getValue()).thenReturn(directoryServiceName);

    List<DirectoryServiceRecord> directoryServiceRecords =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("directory_services.json"),
            DirectoryServiceResponse.class).getResult().getResults();

    when(getRequest.getDirectoryServices(any())).thenReturn(
        Collections.singletonList(directoryServiceRecords.get(0)));

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
