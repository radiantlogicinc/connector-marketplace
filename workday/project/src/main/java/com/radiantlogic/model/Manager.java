package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Manager associated with a Workday Supervisory Organization.
 *
 * <p>A Manager is a worker who has supervisory responsibility within a
 * Supervisory Organization. This object captures the basic identifying information required to
 * reference the manager in Workday responses.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public final class Manager {

  /**
   * Unique identifier of the manager in Workday.
   */
  @SerializedName("id")
  private String managerId;

  /**
   * Human-readable descriptor of the manager.
   */
  @SuppressFBWarnings(
      value = "UWF_UNWRITTEN_FIELD",
      justification = "Field is populated by Gson during deserialization")
  private String descriptor;
}