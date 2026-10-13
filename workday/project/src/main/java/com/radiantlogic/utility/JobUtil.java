package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.radiantlogic.enums.Job;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.JobRow;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for job-related helper methods.
 *
 * <p>This class provides static utility methods for working with job
 * entities, requests, and related transformations.</p>
 */

@Slf4j
public final class JobUtil {

  private JobUtil() {
  }

  /**
   * Extracts job data into a schema-compatible representation.
   *
   * @param jobs         list of job rows to process
   * @param schema       schema definition used for mapping job attributes
   * @param targetSchema target schema configuration for output transformation
   * @return list of extracted job data represented as key-value mappings
   * @throws WorkdayException if schema mapping or data extraction fails
   */
  public static List<Map<String, Object>> extractJobsData(final @NonNull List<JobRow> jobs,
                                                          final @NonNull Schema schema,
                                                          final @NonNull
                                                          ReadOnlyProperties targetSchema)
      throws WorkdayException {
    final List<String> fieldNames =
        Util.getFieldNames(
            Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    final ImmutableList.Builder<Map<String, Object>> searchResultData =
        ImmutableList.builder();
    for (JobRow job : jobs) {
      searchResultData.add(populateJobMap(job, fieldNames));
    }
    return searchResultData.build();
  }

  private static Map<String, Object> populateJobMap(final JobRow job,
                                                    final List<String> fieldNames)
      throws WorkdayException {
    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (String fieldName : fieldNames) {
      final Job jobAttribute = Job.fromName(fieldName);
      final Object value = jobAttribute.extractValue(job);
      if (Util.isValidValue(value)) {
        builder.put(jobAttribute.getAttributeName(), value);
      }
    }
    return builder.build();
  }

}
