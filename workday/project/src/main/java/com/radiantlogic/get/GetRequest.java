package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.WORKDAY_RESPONSE_GSON;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.factory.CaseInsensitiveTypeAdapterFactory;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.JobPostingRow;
import com.radiantlogic.model.JobRow;
import com.radiantlogic.model.OrganizationMemberRow;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.SupervisoryOrganizationRow;
import com.radiantlogic.model.WorkdayResponse;
import com.radiantlogic.model.WorkerRow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Executes authenticated HTTP requests against the Workday staffing API and returns deserialized
 * responses for worker and job entities.
 */
@Slf4j
public final class GetRequest {

  /**
   * Builds requests for various operations and performs credential validation.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Executes prepared requests.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Custom Gson instance with case-insensitive type adapter for report parsing.
   */
  private static final Gson REPORT_GSON = new GsonBuilder()
      .registerTypeAdapterFactory(new CaseInsensitiveTypeAdapterFactory())
      .create();

  /**
   * Creates a new GetRequest instance with the provided builder and executor.
   *
   * @param requestBuilder  the builder used to construct requests and perform credential
   *                        validation
   * @param requestExecutor the executor used to execute constructed requests
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RequestBuilder is a shared connector component "
          + "and is intentionally injected.")
  public GetRequest(final RequestBuilder requestBuilder, final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * Retrieves worker records for the specified worker ID with pagination support.
   *
   * @param workerId the unique identifier of the worker to retrieve
   * @param offset   the starting position for pagination of results
   * @return a response containing a list of worker rows
   * @throws IOException      if an I/O error occurs while processing the request
   * @throws WorkdayException if the request fails or returns an invalid response
   */
  public WorkdayResponse<WorkerRow> getWorkers(final String workerId, final int offset)
      throws IOException, WorkdayException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getWorkersRequest(workerId, offset));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return WORKDAY_RESPONSE_GSON.fromJson(apiResponse,
          new TypeToken<WorkdayResponse<WorkerRow>>() {
          }.getType());
    }
    throw new WorkdayException(
        String.format(
            "Get workers API call failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));
  }

  /**
   * Retrieves job data from the Workday API.
   *
   * <p>If any exception occurs while fetching jobs, the operation fails
   * and the flow is aborted.</p>
   *
   * @param jobId  identifier of the job to retrieve
   * @param offset pagination offset for the request
   * @return response containing job rows and pagination information
   * @throws IOException      if a network or connectivity error occurs
   * @throws WorkdayException if the API response is unsuccessful
   */
  public WorkdayResponse<JobRow> getJobs(final String jobId, final int offset)
      throws IOException, WorkdayException {
    final ResponseData responseData =
        requestExecutor.execute(requestBuilder.getJobsRequest(jobId, offset));
    final String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return WORKDAY_RESPONSE_GSON.fromJson(apiResponse,
          new TypeToken<WorkdayResponse<JobRow>>() {
          }.getType());
    }
    throw new WorkdayException(
        String.format(
            "Get jobs API call failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));

  }

  /**
   * Retrieves job posting data from Workday for the specified job posting identifier.
   *
   * <p>This method requests job posting records from Workday starting at the
   * specified offset and returns the response containing matching job posting data.</p>
   *
   * @param jobPostingId unique identifier of the job posting to retrieve
   * @param offset       zero-based offset used for paginated retrieval of results
   * @return a {@link WorkdayResponse} containing the retrieved {@link JobPostingRow} records
   * @throws IOException      if an error occurs while communicating with the Workday service
   * @throws WorkdayException if the Workday API returns an error or the response cannot be
   *                          processed
   */
  public WorkdayResponse<JobPostingRow> getJobPostings(final String jobPostingId, final int offset)
      throws IOException, WorkdayException {
    final ResponseData responseData =
        requestExecutor.execute(requestBuilder.getJobPostingsRequest(jobPostingId, offset));
    final String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return WORKDAY_RESPONSE_GSON.fromJson(apiResponse,
          new TypeToken<WorkdayResponse<JobPostingRow>>() {
          }.getType());
    }
    throw new WorkdayException(
        String.format(
            "Get job postings API call failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));

  }

  /**
   * Retrieves Supervisory Organizations from Workday.
   *
   * <p>If a Supervisory Organization identifier is provided, the request
   * retrieves the matching Supervisory Organization. Otherwise, the request retrieves Supervisory
   * Organizations starting at the specified offset.
   *
   * @param supervisoryOrganizationId the unique identifier of the Supervisory Organization to
   *                                  retrieve; may be {@code null} to retrieve multiple
   *                                  organizations
   * @param offset                    the starting offset used for paginated retrieval
   * @return a Workday response containing Supervisory Organization records
   * @throws IOException      if an error occurs while communicating with Workday
   * @throws WorkdayException if Workday returns an error while processing the request
   */
  public WorkdayResponse<SupervisoryOrganizationRow> getSupervisoryOrganizations(
      final String supervisoryOrganizationId, final int offset)
      throws IOException, WorkdayException {
    ResponseData responseData =
        requestExecutor.execute(
            requestBuilder.getSupervisoryOrganizationsRequest(supervisoryOrganizationId, offset));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return WORKDAY_RESPONSE_GSON.fromJson(apiResponse,
          new TypeToken<WorkdayResponse<SupervisoryOrganizationRow>>() {
          }.getType());
    }
    throw new WorkdayException(
        String.format(
            "Get Supervisory Organizations API call failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));
  }

  /**
   * Retrieves members of a Supervisory Organization from Workday.
   *
   * <p>The request returns workers or organization members associated with the
   * specified Supervisory Organization. Results are retrieved using the provided offset to support
   * paginated access.
   *
   * @param supervisoryOrganizationsId the unique identifier of the Supervisory Organization whose
   *                                   members are to be retrieved
   * @param offset                     the starting offset used for paginated retrieval
   * @param limit             no of records to be fetched in each execution
   * @return a Workday response containing Supervisory Organization member records
   * @throws IOException      if an error occurs while communicating with Workday
   * @throws WorkdayException if Workday returns an error while processing the request
   */
  public WorkdayResponse<OrganizationMemberRow> getSupervisoryOrganizationMembers(
      final String supervisoryOrganizationsId, final int offset, final int limit)
      throws IOException, WorkdayException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getSupervisoryOrganizationMembersRequest(
            supervisoryOrganizationsId, offset, limit));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return WORKDAY_RESPONSE_GSON.fromJson(apiResponse,
          new TypeToken<WorkdayResponse<OrganizationMemberRow>>() {
          }.getType());
    }
    throw new WorkdayException(
        String.format(
            "Get Supervisory Organizations Members API call failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));
  }

  /**
   * Retrieves report data from the specified URL and parses it using the provided Type.
   *
   * @param <T>       the type of the report response object
   * @param reportUrl the URL from which to fetch the report
   * @param type      the Type to parse the response into (supports generics)
   * @return the parsed report response object
   * @throws IOException      if an I/O error occurs while processing the request
   * @throws WorkdayException if the request fails or returns an invalid response
   */
  public <T> T getReport(final String reportUrl, final Type type)
      throws IOException, WorkdayException {
    if (StringUtils.isBlank(reportUrl)) {
      throw new WorkdayException(
          "A report URL must be configured before attempting to retrieve a report.");
    }
    ResponseData responseData = requestExecutor.execute(requestBuilder.getReportRequest(reportUrl));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      return REPORT_GSON.fromJson(apiResponse, type);
    }
    throw new WorkdayException(
        String.format(
            "Get report API failed with HTTP %d: %s",
            responseData.getResponseCode(),
            apiResponse
        ));
  }

}
