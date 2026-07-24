package com.radiantlogic.pagedresponse;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.RDN;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class PagedResponseTest {

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
  void testIsValidTrue() {
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    assertTrue(pagedResponse.isValid(), "PagedResponse should be valid");
  }

  @Test
  void testIsValidFalse() {
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    pagedResponse.invalidate();
    assertFalse(pagedResponse.isValid(), "PagedResponse should be not be valid");
  }

  @Test
  void testHasMoreTrue() {
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    assertTrue(pagedResponse.hasMore(), "");
  }

  @Test
  void testHasMoreFalse() {
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    pagedResponse.next();
    assertFalse(pagedResponse.hasMore(),
        "PagedResponse should have hasMore = false when no data is available");
  }

  @Test
  void testHasMoreWithException() {
    pagedResponse =
        new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest, executor);
    pagedResponse.invalidate();
    assertThrows(IllegalStateException.class, () -> pagedResponse.hasMore());
  }

  @Test
  void testGetDataWithInvalidTargetSchema() {
    when(targetSchemaObjects.get(any())).thenReturn(Collections.singletonList("user1"));
    assertThrows(IllegalStateException.class,
        () -> new PagedResponse(mockSearchRequest, targetSchemaObjects, schema, getRequest,
            executor));
  }

}
