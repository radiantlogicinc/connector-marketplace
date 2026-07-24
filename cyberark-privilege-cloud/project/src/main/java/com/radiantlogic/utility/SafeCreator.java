package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse safe's creator details.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class SafeCreator {

  /**
   * This field will parse safe creator's id.
   */
  private String id;
  /**
   * This field will parse safe creator's name.
   */
  private String name;
}