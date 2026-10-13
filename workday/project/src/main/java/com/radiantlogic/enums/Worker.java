package com.radiantlogic.enums;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.WorkerRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Enum representing supported worker attributes.
 *
 * <p>Each constant defines:
 * <ul>
 *   <li>The external attribute name</li>
 *   <li>A getter for extracting the value from {@code WorkerRow}</li>
 *   <li>Custom update logic for applying modifications</li>
 * </ul>
 *
 * <p>This enum encapsulates attribute-specific validation and update behavior,
 * eliminating the need for switch-based processing.
 */
@Getter
public enum Worker {

  WORKER_ID("WorkerID", WorkerRow::getWorkerId),
  DESCRIPTOR("Descriptor", WorkerRow::getDescriptor),
  PERSON("Person", WorkerRow::getPerson),
  PRIMARYJOB("PrimaryJob", WorkerRow::getPrimaryJob),
  WORKERTYPE("WorkerType", WorkerRow::getWorkerType),
  WORKERSPECIFICID("WorkerSpecificId",
      WorkerRow::getWorkerSpecificId);

  /**
   * Name of the external attribute that this mapping represents.
   * Used to identify and resolve values from external data sources.
   */
  private final String attributeName;

  /**
   * Function that extracts the corresponding attribute value
   * from a {@link WorkerRow} instance.
   */
  private final Function<WorkerRow, Object> getter;
  /**
   * Immutable lookup map that associates normalized attribute names
   * with their corresponding {@code Worker} enum constants.
   *
   * <p>The keys are stored in lowercase using {@link Locale#ROOT}
   * to support case-insensitive attribute resolution.</p>
   */
  private static final Map<String, Worker> LOOKUP = Collections.unmodifiableMap(
      Arrays.stream(values()).collect(
          Collectors.toMap(worker -> worker.attributeName.toLowerCase(Locale.ROOT),
              worker -> worker)));

  Worker(final String attributeName, final Function<WorkerRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified worker row.
   *
   * @param worker the worker row from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  public Object extractValue(final WorkerRow worker) {
    return getter.apply(worker);
  }

  /**
   * Returns the {@code Worker} enum constant associated with the specified
   * attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code Worker} enum constant
   * @throws WorkdayException if no {@code Worker} constant exists for the
   *     specified attribute name
   */
  public static Worker fromName(final String fieldName) throws WorkdayException {
    Worker worker = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (worker == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return worker;
  }

}
