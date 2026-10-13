package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday security domain.
 *
 * <p>A security domain in Workday groups related business process tasks,
 * reports, or data that can be secured through security policies. This model
 * contains identifying information and metadata for a security domain returned
 * by Workday.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_FIELD",
    justification = "Fields are populated via Gson JSON deserialization using reflection"
)
public class Domain {

  /**
   * Unique Workday identifier for the security domain.
   */
  @SerializedName("id")
  private String workdayId;
  /**
   * Display name of the Workday security domain.
   */
  private String name;

  /**
   * Description of the Workday security domain.
   *
   * <p>Provides additional details about the purpose or usage of the domain.
   */
  private String description;

  /**
   * Functional areas associated with the Workday security domain.
   *
   * <p>A domain can be associated with one or more functional areas within
   * Workday.
   */
  private String functionalAreas;
}
