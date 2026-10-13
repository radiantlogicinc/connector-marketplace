package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import com.radiantlogic.model.Manager;
import com.radiantlogic.model.OrganizationMemberRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a Workday Supervisory Organization.
 *
 * <p>A Supervisory Organization is a Workday organizational structure that groups
 * workers under a management hierarchy. It serves as the primary framework for
 * worker assignment, manager relationships, and organizational reporting.
 *
 * <p>This entity captures key supervisory organization attributes, including the
 * organization identifier, name, status, managers, and organization members.
 */
@Entity(name = "supervisoryOrganization", namingKey = "supervisoryOrganization")
@Getter
@NoArgsConstructor
public final class SupervisoryOrganizationEntity {

  /**
   * Unique identifier of the supervisory organization in Workday.
   */
  @Attribute(isNamingAttribute = true)
  private String supervisoryOrganizationId;

  /**
   * Human-readable description of the supervisory organization.
   */
  @Attribute(isNullable = true)
  private String descriptor;

  /**
   * Reference identifier associated with the supervisory organization.
   *
   * <p>This value can be used to correlate the organization with related
   * Workday reference data.
   */
  @Attribute(isNullable = true)
  private String organizationReferenceId;

  /**
   * Indicates whether the supervisory organization is inactive.
   */
  @Attribute(isNullable = true)
  private boolean inactive;

  /**
   * Collection of managers assigned to the supervisory organization.
   */
  @Attribute(isNullable = true)
  @Getter(AccessLevel.NONE)
  private List<Manager> managers;

  /**
   * Name of the supervisory organization.
   */
  @Attribute(isNullable = true)
  private String name;

  /**
   * Collection of members belonging to the supervisory organization.
   */
  @Attribute(isNullable = true)
  @Getter(AccessLevel.NONE)
  private List<OrganizationMemberRow> organizationMember;

  /**
   * Returns an unmodifiable view of managers to prevent external mutation
   * of internal state.
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
