package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.JobPostingRow;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;


/**
 * Enum representing supported job posting attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link JobPostingRow}, enabling attribute-driven data extraction
 * without switch statements.
 */
@Getter
public enum JobPosting {

  JOBPOSTINGID("JobPostingId", JobPostingRow::getJobPostingId),
  TIMETYPE("TimeType", JobPostingRow::getTimeType),
  SPOTLIGHTJOB("SpotlightJob", JobPostingRow::isSpotlightJob),
  URL("Url", JobPostingRow::getUrl),
  STARTDATE("StartDate", JobPostingRow::getStartDate),
  JOBDESCRIPTION("JobDescription", JobPostingRow::getJobDescription),
  COMPANY("Company", JobPostingRow::getCompany),
  TITLE("Title", JobPostingRow::getTitle),
  PRIMARYLOCATION("PrimaryLocation", JobPostingRow::getPrimaryLocation),
  CATEGORIES("Categories", JobPostingRow::getCategories),
  JOBSITE("JobSite", JobPostingRow::getJobSite),
  JOBTYPE("JobType", JobPostingRow::getJobType);

  /**
   * External attribute name used for lookup and mapping to enum constants.
   */
  private final String attributeName;

  /**
   * Function used to extract the attribute value from a {@link JobPostingRow}.
   */
  private final Function<JobPostingRow, Object> getter;

  /**
   * Lookup map that resolves attribute names (case-insensitive) to enum constants.
   */
  private static final Map<String, JobPosting> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              jobPosting -> jobPosting.attributeName.toLowerCase(Locale.ROOT),
              jobPosting -> jobPosting));

  JobPosting(final String attributeName, final Function<JobPostingRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the attribute value from the given JobPostingRow.
   *
   * @param jobPostingRow Job Posting row instance
   * @return Extracted value
   */
  public Object extractValue(final JobPostingRow jobPostingRow) {
    return getter.apply(jobPostingRow);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching job enum constant
   * @throws WorkdayException If the attribute name is invalid
   */
  public static JobPosting fromName(final String fieldName) throws WorkdayException {
    JobPosting jobPosting = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (jobPosting == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return jobPosting;
  }
}
