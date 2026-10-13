package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a category associated with a job posting.
 *
 * <p>This model contains category details returned by the Workday API,
 * including the category identifier and display name. Categories may
 * represent job functions, departments, or other classification types.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Category {

  /**
   * Unique identifier of the category.
   */
  @SerializedName("id")
  private String categoryId;

  /**
   * Human-readable name or description of the category.
   */
  private String descriptor;
}