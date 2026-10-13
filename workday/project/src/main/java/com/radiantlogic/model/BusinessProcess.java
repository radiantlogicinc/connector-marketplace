package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday business process definition.
 *
 * <p>A business process in Workday defines a sequence of tasks, approvals,
 * and actions used to complete a specific business activity.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_FIELD",
    justification = "Fields are populated via Gson JSON deserialization using reflection"
)
public class BusinessProcess {

  /**
   * Unique identifier assigned to the Workday business process.
   */
  @SerializedName("id")
  private String workdayId;

  /**
   * Display name of the Workday business process.
   */
  private String name;

  /**
   * Description of the Workday business process and its purpose.
   */
  private String description;

  /**
   * Functional areas associated with the Workday business process.
   */
  private String functionalAreas;
}
