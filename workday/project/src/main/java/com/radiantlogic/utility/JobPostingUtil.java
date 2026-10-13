package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.radiantlogic.enums.JobPosting;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.JobPostingRow;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Utility class for jobPosting-related helper methods.
 *
 * <p>This class provides static utility methods for working with job
 * posting entities, requests, and related transformations.</p>
 */

public final class JobPostingUtil {

  private JobPostingUtil() {
  }

  /**
   * Extracts job posting data and converts it into a schema-compatible representation.
   *
   * <p>This method transforms a list of {@link JobPostingRow} objects into a list of
   * maps, where each map represents a job posting aligned with the provided schema definition and
   * target schema configuration.</p>
   *
   * @param jobPostings  list of job posting rows returned from Workday API
   * @param schema       schema definition used to determine output structure
   * @param targetSchema configuration describing target schema fields and mappings
   * @return a list of maps, where each map contains extracted job posting data aligned to the
   *          schema
   * @throws WorkdayException if an error occurs during data extraction or mapping
   */
  public static List<Map<String, Object>> extractJobPostingsData(
      final @NonNull List<JobPostingRow> jobPostings,
      final @NonNull Schema schema,
      final @NonNull ReadOnlyProperties targetSchema)
      throws WorkdayException {

    final List<String> fieldNames =
        Util.getFieldNames(
            Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    final ImmutableList.Builder<Map<String, Object>> searchResultData =
        ImmutableList.builder();
    for (JobPostingRow jobPosting : jobPostings) {
      Map<String, Object> entry = populateJobPostingMap(jobPosting, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData.build();
  }

  private static Map<String, Object> populateJobPostingMap(final JobPostingRow jobPosting,
                                                           final List<String> fieldNames)
      throws WorkdayException {

    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (String fieldName : fieldNames) {
      JobPosting jobAttribute = JobPosting.fromName(fieldName);
      Object value = jobAttribute.extractValue(jobPosting);
      if (Util.isValidValue(value)) {
        builder.put(jobAttribute.getAttributeName(), value);
      }
    }
    return builder.build();
  }

}
