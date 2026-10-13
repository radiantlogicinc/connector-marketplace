package com.radiantlogic.utility;

import com.radiantlogic.enums.Worker;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.WorkerRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing helper methods for processing worker data.
 *
 * <p>Converts worker objects into structured map representations based on
 * schema definitions.</p>
 */
@Slf4j
public final class WorkerUtil {

  private WorkerUtil() {
  }

  /**
   * Extracts worker data into a list of attribute maps based on schema configuration.
   *
   * @param workers list of worker records to process
   * @param schema schema definition used to determine available fields
   * @param targetSchema schema properties describing the target object configuration
   * @return list of worker data maps where each map represents a worker record
   * @throws WorkdayException if an invalid or unsupported field is encountered
   */
  public static List<Map<String, Object>> extractWorkersData(
      final List<WorkerRow> workers,
      final Schema schema,
      final ReadOnlyProperties targetSchema)
      throws WorkdayException {

    List<Map<String, Object>> searchResultData = new ArrayList<>();

    List<String> fieldNames =
        Util.getFieldNames(
            Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));

    for (WorkerRow worker : workers) {
      Map<String, Object> entry = populateWorkerMap(worker, fieldNames);
      searchResultData.add(entry);
    }

    return searchResultData;
  }

  @SuppressWarnings("PMD.UseConcurrentHashMap")
  private static Map<String, Object> populateWorkerMap(
      final WorkerRow worker,
      final List<String> fieldNames)
      throws WorkdayException {

    Map<String, Object> entry = new HashMap<>();

    for (String fieldName : fieldNames) {
      Worker workerAttribute = Worker.fromName(fieldName);
      Object value = workerAttribute.extractValue(worker);

      if (Util.isValidValue(value)) {
        entry.put(workerAttribute.getAttributeName(), value);
      }
    }

    return entry;
  }
}
