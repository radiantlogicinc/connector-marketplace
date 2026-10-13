package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday Supervisory Organization search result row.
 *
 * <p>This class is used to deserialize Supervisory Organization data returned
 * from Workday APIs. It contains basic organizational attributes along with related manager and
 * member references.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public final class SupervisoryOrganizationRow {

  /**
   * Unique identifier of the Supervisory Organization in Workday.
   */
  @SerializedName("id")
  private String supervisoryOrganizationId;

  /**
   * Human-readable description of the Supervisory Organization.
   */
  @SuppressFBWarnings(
      value = "UWF_UNWRITTEN_FIELD",
      justification = "Field is populated by Gson during deserialization")
  private String descriptor;

  /**
   * Reference identifier for the Supervisory Organization.
   */
  @SerializedName("Organization_Reference_ID")
  private String organizationReferenceId;

  /**
   * Indicates whether the Supervisory Organization is inactive.
   */
  private boolean inactive;

  /**
   * List of managers associated with the Supervisory Organization.
   */
  @SuppressFBWarnings(
      value = "UWF_UNWRITTEN_FIELD",
      justification = "Field is populated by Gson during deserialization")
  @Getter(AccessLevel.NONE)
  private List<Manager> managers;

  /**
   * Name of the Supervisory Organization.
   */
  @SuppressFBWarnings(
      value = "UWF_UNWRITTEN_FIELD",
      justification = "Field is populated by Gson during deserialization")
  private String name;

  /**
   * List of members belonging to the Supervisory Organization.
   */
  @Getter(AccessLevel.NONE)
  private List<OrganizationMemberRow> organizationMember;

  /**
   * Returns an unmodifiable view of managers to prevent external mutation of internal state.
   */
  public List<Manager> getManagers() {
    return managers == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(managers);
  }

  /**
   * Sets the organization members.
   *
   * <p>A defensive copy of the supplied list is created to prevent external
   * modifications from affecting the internal state of this object.</p>
   *
   * @param organizationMember list of organization members
   */
  public void setOrganizationMember(final List<OrganizationMemberRow> organizationMember) {
    this.organizationMember = organizationMember == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(new ArrayList<>(organizationMember));
  }

  /**
   * Returns the organization members.
   *
   * <p>An unmodifiable view is returned to protect the internal state from
   * external modification.</p>
   *
   * @return unmodifiable list of organization members
   */
  public List<OrganizationMemberRow> getOrganizationMember() {
    return organizationMember == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(organizationMember);
  }
}