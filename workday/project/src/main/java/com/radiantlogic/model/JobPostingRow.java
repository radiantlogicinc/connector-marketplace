package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a job posting record returned by the Workday API.
 *
 * <p>This model contains job posting details including identification,
 * employment information, company details, location information,
 * categorization, and application-related metadata. The class is used
 * for deserializing job posting responses received from Workday.</p>
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobPostingRow {

  /**
   * Unique identifier of the job posting.
   */
  @SerializedName("id")
  private final String jobPostingId;

  /**
   * Employment time classification for the position,
   * such as full-time or part-time.
   */
  private final TimeType timeType;

  /**
   * Indicates whether the job posting is marked as a spotlight job.
   */
  private final boolean spotlightJob;

  /**
   * URL of the job posting or application page.
   */
  private final String url;

  /**
   * Date on which the job posting becomes active or available.
   */
  private final String startDate;

  /**
   * Detailed description of the job role, responsibilities,
   * qualifications, and other related information.
   */
  private final String jobDescription;

  /**
   * Company associated with the job posting.
   */
  private final Company company;

  /**
   * Title or name of the job position.
   */
  private final String title;

  /**
   * Primary work location for the job posting.
   */
  private final PrimaryLocation primaryLocation;

  /**
   * Collection of categories associated with the job posting.
   * Categories may represent job functions, departments,
   * or other classification types.
   */
  @Getter(AccessLevel.NONE)
  private final List<Category> categories;

  /**
   * Job site information associated with the posting.
   */
  private final JobSite jobSite;

  /**
   * Type of employment associated with the job posting,
   * such as permanent, temporary, contract, or internship.
   */
  private final JobPostingJobType jobType;

  /**
   * Defensive copy constructor used to prevent external mutation of categories list.
   */
  @SuppressWarnings("PMD.ExcessiveParameterList")
  public JobPostingRow(
      final String jobPostingId,
      final TimeType timeType,
      final boolean spotlightJob,
      final String url,
      final String startDate,
      final String jobDescription,
      final Company company,
      final String title,
      final PrimaryLocation primaryLocation,
      final List<Category> categories,
      final JobSite jobSite,
      final JobPostingJobType jobType) {

    this.jobPostingId = jobPostingId;
    this.timeType = timeType;
    this.spotlightJob = spotlightJob;
    this.url = url;
    this.startDate = startDate;
    this.jobDescription = jobDescription;
    this.company = company;
    this.title = title;
    this.primaryLocation = primaryLocation;
    this.categories = categories == null
        ? Collections.emptyList()
        : new ArrayList<>(categories);
    this.jobSite = jobSite;
    this.jobType = jobType;
  }

  /**
   * Returns an unmodifiable view of categories to prevent external mutation
   * of internal state.
   */
  public List<Category> getCategories() {
    return categories == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(categories);
  }
}