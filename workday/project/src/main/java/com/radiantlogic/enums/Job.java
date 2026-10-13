package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.JobRow;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported job attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link JobRow}, enabling attribute-driven data extraction without
 * switch statements.
 */
@Getter
public enum Job {

  JOBID("JobID", JobRow::getJobId),
  DESCRIPTOR("Descriptor", JobRow::getDescriptor),
  JOBWORKER("JobWorker", JobRow::getJobWorker),
  SUPERVISORYORGANIZATION("SupervisoryOrganization", JobRow::getSupervisoryOrganization),
  LOCATION("Location", JobRow::getLocation),
  JOBTYPE("JobType", JobRow::getJobType);

  /**
   * External attribute name used for lookup and mapping to enum constants.
   */
  private final String attributeName;

  /**
   * Function used to extract the attribute value from a {@link JobRow}.
   */
  private final Function<JobRow, Object> getter;

  /**
   * Lookup map that resolves attribute names (case-insensitive) to enum constants.
   */
  private static final Map<String, Job> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              job -> job.attributeName.toLowerCase(Locale.ROOT),
              job -> job));

  Job(final String attributeName, final Function<JobRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the attribute value from the given job row.
   *
   * @param job job row instance
   * @return extracted attribute value
   */
  public Object extractValue(final JobRow job) {
    return getter.apply(job);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching job enum constant
   * @throws WorkdayException If the attribute name is invalid
   */
  public static Job fromName(final String fieldName) throws WorkdayException {
    final Job job = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (job == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return job;
  }
}
