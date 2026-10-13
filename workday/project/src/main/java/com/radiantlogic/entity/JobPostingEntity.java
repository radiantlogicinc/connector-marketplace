package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import com.radiantlogic.model.Category;
import com.radiantlogic.model.Company;
import com.radiantlogic.model.JobPostingJobType;
import com.radiantlogic.model.JobSite;
import com.radiantlogic.model.PrimaryLocation;
import com.radiantlogic.model.TimeType;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a job posting retrieved from the Workday integration.
 *
 * <p>This entity contains job posting details including identification,
 * employment information, location, company details, categorization,
 * and application-related metadata.</p>
 */
@Entity(name = "jobPosting", namingKey = "jobPosting")
@Getter
@NoArgsConstructor
public final class JobPostingEntity {
  /**
   * Unique identifier of the job posting.
   * This field acts as the naming attribute for the entity.
   */
  @Attribute(isNamingAttribute = true)
  private String jobPostingId;

  /**
   * Employment time classification for the position,
   * such as full-time or part-time.
   */
  @Attribute(isNullable = true)
  private TimeType timeType;

  /**
   * Indicates whether the job posting is marked as a spotlight job.
   */
  @Attribute(isNullable = true)
  private boolean spotlightJob;

  /**
   * URL that directs users to the job posting or application page.
   */
  @Attribute(isNullable = true)
  private String url;

  /**
   * Date on which the job posting becomes active or available.
   */
  @Attribute(isNullable = true)
  private String startDate;

  /**
   * Detailed description of the job role, responsibilities,
   * qualifications, and other related information.
   */
  @Attribute(isNullable = true)
  private String jobDescription;

  /**
   * Company associated with the job posting.
   */
  @Attribute(isNullable = true)
  private Company company;

  /**
   * Title or name of the job position.
   */
  @Attribute(isNullable = true)
  private String title;

  /**
   * Primary work location for the job posting.
   */
  @Attribute(isNullable = true)
  private PrimaryLocation primaryLocation;

  /**
   * Collection of categories associated with the job posting.
   * Categories may represent job functions, departments,
   * or other classification types.
   */
  @Attribute(isNullable = true)
  private List<Category> categories;

  /**
   * Job site information associated with the posting.
   */
  @Attribute(isNullable = true)
  private JobSite jobSite;

  /**
   * Type of employment associated with the job posting,
   * such as permanent, temporary, contract, or internship.
   */
  @Attribute(isNullable = true)
  private JobPostingJobType jobType;

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