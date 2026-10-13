package com.radiantlogic.factory;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.JobPostingDataProvider;
import com.radiantlogic.get.JobsDataProvider;
import com.radiantlogic.get.SupervisoryOrganizationDataProvider;
import com.radiantlogic.get.WorkersDataProvider;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.ReportDataExtractor;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.report.GetReport;
import com.radiantlogic.utility.SearchResponseConverter;
import java.lang.reflect.Type;

/**
 * Factory for creating {@link SchemaObjectDataProvider} instances.
 *
 * <p>Provides a centralized mechanism for selecting and instantiating
 * the appropriate data provider implementation based on the requested
 * schema object type.</p>
 */
public final class SchemaObjectDataProviderFactory {

  private SchemaObjectDataProviderFactory() {
  }

  /**
   * Creates a {@link SchemaObjectDataProvider} for retrieving worker data.
   *
   * @param getRequest the request containing the attributes and filters for the retrieval operation
   * @param uniqueKey the unique identifier of the worker to retrieve;
   *                 may be {@code null} for non-specific queries
   * @param schema the schema definition associated with the requested object
   * @param targetSchemaObjects the schema configuration describing
   *                            the target objects and attributes
   * @param converter the converter used to transform search results
   *                  into schema object representations
   * @return a {@link SchemaObjectDataProvider} configured for worker data retrieval
   */
  public static SchemaObjectDataProvider createWorkersProvider(
      final GetRequest getRequest,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter) {

    return WorkersDataProvider.builder()
        .getRequest(getRequest)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .build();
  }

  /**
   * Creates a {@link SchemaObjectDataProvider} for job entities.
   *
   * @param getRequest request containing parameters used to fetch job data
   * @param uniqueKey identifier of the job record to retrieve; may be {@code null}
   *                  or blank to fetch all jobs using paginated results
   * @param schema schema definition used for mapping job attributes
   * @param targetSchemaObjects configuration describing target schema objects
   * @param converter converter used to transform raw response data into schema objects
   * @return a {@link SchemaObjectDataProvider} for job data retrieval
   */
  public static SchemaObjectDataProvider createJobsProvider(
      final GetRequest getRequest,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter) {

    return JobsDataProvider.builder()
        .getRequest(getRequest)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .build();
  }

  /**
   * Creates a {@link SchemaObjectDataProvider} for retrieving and processing
   * job posting data from the Workday integration.
   *
   * <p>The returned provider uses the supplied request context, schema
   * definition, and response converter to fetch and transform job posting
   * data into the requested schema representation.</p>
   *
   * @param getRequest request context containing information required to
   *                   retrieve job posting data
   * @param uniqueKey unique identifier of the job posting record to retrieve
   * @param schema schema definition associated with the requested object
   * @param targetSchemaObjects schema object configuration specifying the
   *                            fields and related objects to be populated
   * @param converter converter used to transform Workday responses into
   *                  schema-compliant objects
   * @return a {@link SchemaObjectDataProvider} configured for job posting data
   *         retrieval and conversion
   */
  public static SchemaObjectDataProvider createJobPostingProvider(
      final GetRequest getRequest,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter) {

    return JobPostingDataProvider.builder()
        .getRequest(getRequest)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .build();
  }

  /**
   * Creates a {@link SchemaObjectDataProvider} for retrieving and converting
   * Workday Supervisory Organization data.
   *
   * <p>The returned provider uses the supplied request configuration and schema
   * metadata to fetch supervisory organization records and transform them into
   * schema object representations.
   *
   * @param getRequest the Workday request configuration used to retrieve
   *     supervisory organization data
   * @param uniqueKey the unique key that identifies the supervisory organization
   *     within the target schema
   * @param schema the schema definition that describes the supervisory
   *     organization object
   * @param targetSchemaObjects the target schema object properties used during
   *     data mapping
   * @param converter the converter responsible for transforming Workday search
   *     responses into schema object data
   * @return a schema object data provider for supervisory organization entities
   */
  public static SchemaObjectDataProvider createSupervisoryOrganizationProvider(
      final GetRequest getRequest,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter) {

    return SupervisoryOrganizationDataProvider.builder()
        .getRequest(getRequest)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .build();
  }

  /**
   * Creates a generic {@link SchemaObjectDataProvider} for retrieving report data.
   *
   * @param <T>                 the type of individual records in the report
   * @param <R>                 the type of the report response object
   * @param getRequest          the request containing the attributes and filters for the retrieval
   *                            operation
   * @param uniqueKey           the unique identifier to filter a specific record from the report;
   *                            may be {@code null} to retrieve all records
   * @param reportUrl           the URL from which to fetch the report
   * @param schema              the schema definition associated with the requested object
   * @param targetSchemaObjects the schema configuration describing the target objects and
   *                            attributes
   * @param converter           the converter used to transform search results into schema object
   *                            representations
   * @param responseType        the Type of the report response object (supports generics)
   * @param dataExtractor       the data extractor for transforming report data
   * @return a {@link SchemaObjectDataProvider} configured for report data retrieval
   */
  public static <T, R> SchemaObjectDataProvider createReportProvider(
      final GetRequest getRequest,
      final String uniqueKey,
      final String reportUrl,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter,
      final Type responseType,
      final ReportDataExtractor<T, R> dataExtractor) {

    return GetReport.<T, R>builder()
        .getRequest(getRequest)
        .uniqueKey(uniqueKey)
        .reportUrl(reportUrl)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .responseType(responseType)
        .dataExtractor(dataExtractor)
        .build();
  }

}
