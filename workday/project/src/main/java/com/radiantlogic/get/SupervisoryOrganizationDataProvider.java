package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.OrganizationMemberRow;
import com.radiantlogic.model.SupervisoryOrganizationRow;
import com.radiantlogic.model.WorkdayResponse;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.SupervisoryOrganizationUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides Workday Supervisory Organization data as schema objects.
 *
 * <p>This provider retrieves Supervisory Organization records from Workday,
 * converts the responses into schema object representations, and supports paginated data
 * retrieval.
 */
@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SupervisoryOrganizationDataProvider implements SchemaObjectDataProvider {

  /**
   * Workday request configuration used to retrieve Supervisory Organization data.
   */
  private final GetRequest getRequest;

  /**
   * Converts Workday search responses into schema object data.
   */
  private final SearchResponseConverter converter;

  /**
   * Schema definition of the target Supervisory Organization object.
   */
  private final Schema schema;

  /**
   * Target schema object properties used during data mapping.
   */
  private final ReadOnlyProperties targetSchemaObjects;

  /**
   * Unique identifier used when retrieving a specific Supervisory Organization.
   */
  private final String uniqueKey;

  /**
   * Indicates whether additional records are available for retrieval.
   */
  @Builder.Default
  private boolean hasMoreRecordsFlag = true;

  /**
   * Current offset used for paginated data retrieval.
   */
  private int offset;
  /**
   * Setting page limit at its highest for fetching supervisory organization members.
   */
  public static final int SUPERVISORY_ORGANIZATION_MEMBERS_PAGE_LIMIT = 100;

  /**
   * Returns whether additional records are available for retrieval.
   *
   * @return {@code true} if more records are available; {@code false} otherwise
   */
  @Override
  public boolean hasMoreRecords() {
    return hasMoreRecordsFlag;
  }

  /**
   * Retrieves the next page of Supervisory Organization records from Workday.
   *
   * <p>The number of records returned is determined by the specified page size.
   * Pagination state is maintained internally using the current offset and available record count.
   *
   * @param pageSize the maximum number of records to retrieve in a single page
   * @return a list of Supervisory Organization search result entries; an empty list if no
   *         additional records are available
   * @throws IOException      if an error occurs while communicating with Workday
   * @throws WorkdayException if Workday returns an error while processing the request
   */
  @Override
  public List<SearchResultEntry> getNextPage(final int pageSize)
      throws IOException, WorkdayException {
    log.debug("Fetching Supervisory Organizations page: uniqueKey={}, offset={}", uniqueKey,
        offset);
    final WorkdayResponse<SupervisoryOrganizationRow> workdayResponse =
        getRequest.getSupervisoryOrganizations(uniqueKey, offset);
    offset += pageSize;
    this.hasMoreRecordsFlag = (offset < workdayResponse.getTotal());
    if (workdayResponse.getData() == null) {
      throw new WorkdayException("Workday response data is null for Supervisory Organizations");
    }
    final List<SupervisoryOrganizationRow> supervisoryOrganizations =
        getSupervisoryOrganizationsMembers(workdayResponse.getData());
    final ImmutableList.Builder<SearchResultEntry> pageEntries = ImmutableList.builder();
    log.debug("Supervisory Organizations fetched {}", supervisoryOrganizations.size());
    if (!supervisoryOrganizations.isEmpty()) {
      pageEntries.addAll(
          converter.convert(
              SupervisoryOrganizationUtil.extractSupervisoryOrganizationsData(
                  supervisoryOrganizations, schema, targetSchemaObjects)));
    }
    return pageEntries.build();
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private List<SupervisoryOrganizationRow> getSupervisoryOrganizationsMembers(
      final @NonNull List<SupervisoryOrganizationRow> supervisoryOrganizations)
      throws WorkdayException {
    for (SupervisoryOrganizationRow supervisoryOrganization
        : supervisoryOrganizations) {
      try {
        final int limit = SUPERVISORY_ORGANIZATION_MEMBERS_PAGE_LIMIT;
        int memberOffset = 0;
        List<OrganizationMemberRow> collectedMembersList = new ArrayList<>();
        WorkdayResponse<OrganizationMemberRow> supervisoryOrganizationWorkers;
        do {
          supervisoryOrganizationWorkers =
              getRequest.getSupervisoryOrganizationMembers(
                  supervisoryOrganization.getSupervisoryOrganizationId(), memberOffset, limit);

          memberOffset += limit;
          final List<OrganizationMemberRow> members = supervisoryOrganizationWorkers.getData();
          if (members != null) {
            collectedMembersList.addAll(members);
          }
        } while (memberOffset < supervisoryOrganizationWorkers.getTotal());
        log.debug("Members are  {}", supervisoryOrganizationWorkers.getTotal());
        supervisoryOrganization.setOrganizationMember(collectedMembersList);
      } catch (IOException e) {
        log.error(EXCEPTION_PREFIX, e.getMessage(), e);
        throw new WorkdayException("Failed to retrieve supervisory organization members", e);
      }
    }

    return supervisoryOrganizations;
  }

}
