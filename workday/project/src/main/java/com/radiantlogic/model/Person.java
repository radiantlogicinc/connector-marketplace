package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents personal information associated with a worker.
 * Contains identifying and contact details for the person.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Person {

  /**
   * Unique identifier of the person.
   */
  @SerializedName("id")
  private String personId;

  /**
   * Email address of the person.
   */
  private String email;

}
