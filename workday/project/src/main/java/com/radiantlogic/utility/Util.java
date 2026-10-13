package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.ALLOWED_PRIMARY_KEY_ATTRIBUTES;
import static com.radiantlogic.utility.Constants.COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.SIZE_ONE;
import static com.radiantlogic.utility.Constants.UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE;

import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.RDN;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * Utility class providing common helper methods for schema processing,
 * request validation, and data extraction.
 */
@Slf4j
@SuppressWarnings({"PMD.ShortClassName"})
public final class Util {

  /**
   * Private constructor to prevent instantiation.
   */
  private Util() {
  }

  /**
   * Returns the schema object name from the target schema properties.
   *
   * @param targetSchemaObjects schema properties containing schema object definitions
   * @return the single schema object name
   * @throws IllegalStateException if zero or multiple schema object names are found
   */
  public static String getSchemaObjectName(final ReadOnlyProperties targetSchemaObjects) {
    @SuppressWarnings("unchecked")
    // Connector SDK guarantees target schema objects are returned as List<String>
    List<String> schemaObjectNames = (List<String>) targetSchemaObjects.get(SCHEMA_OBJECTS_KEY);

    if (schemaObjectNames == null || schemaObjectNames.size() != 1) {
      log.error(UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE);
      throw new IllegalStateException(UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE);
    }

    return schemaObjectNames.get(0);
  }

  /**
   * Validates that the schema object has exactly one allowed primary key attribute.
   *
   * @param schemaObject schema object to validate
   * @throws IllegalStateException if primary key configuration is invalid
   */
  public static void checkWhetherPrimaryKeyAttributeIsCorrectlySet(
      final SchemaObject schemaObject) {

    if (schemaObject.getPrimaryKeys().size() != SIZE_ONE) {
      log.error(EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE);
      throw new IllegalStateException(EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE);
    }

    String primaryKeyAttributeName = schemaObject.getPrimaryKeys().get(0);

    if (!ALLOWED_PRIMARY_KEY_ATTRIBUTES.contains(
        primaryKeyAttributeName.toLowerCase(Locale.ROOT))) {
      log.error(MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE);
      throw new IllegalStateException(MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE);
    }
  }

  /**
   * Retrieves a schema object by name.
   *
   * @param schema schema containing available schema objects
   * @param schemaObjectName name of the schema object to retrieve
   * @return the matching SchemaObject
   * @throws IllegalStateException if the schema object does not exist
   */
  public static SchemaObject getSchemaObject(final Schema schema, final String schemaObjectName) {
    return schema.getSchemaObject(schemaObjectName)
        .orElseThrow(() -> {
          String errorMessage = COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX + schemaObjectName;
          log.error(errorMessage);
          return new IllegalStateException(errorMessage);
        });
  }

  /**
   * Extracts the unique key from an LDAP search request.
   *
   * @param searchRequest LDAP search request
   * @return extracted unique key value
   */
  public static String getUniqueKey(final LdapSearchRequest searchRequest) {
    if (searchRequest == null || searchRequest.getBaseDN() == null) {
      throw new IllegalStateException("LDAP search request or DN is null");
    }
    RDN rdn = firstOrThrow(
        searchRequest.getBaseDN().getRDNs(),
        "DN has no RDNs: " + searchRequest.getBaseDN());
    Attribute attribute = firstOrThrow(
        rdn.getAttributes(),
        "RDN has no attributes: " + rdn);
    return firstOrThrow(attribute.getValues(),
        "Attribute has no values: " + attribute);
  }

  private static <T> T firstOrThrow(final List<T> list, final String errorMessage) {
    if (list == null || list.isEmpty()) {
      throw new IllegalStateException(errorMessage);
    }
    return list.get(0);
  }

  /**
   * Returns all field names from the schema object.
   *
   * @param schemaObject schema object definition
   * @return list of field names
   */
  public static List<String> getFieldNames(final SchemaObject schemaObject) {
    return schemaObject.getFields()
        .stream()
        .map(Field::getName)
        .collect(Collectors.toList());
  }

  /**
   * Checks whether a value is considered valid (non-null and non-blank for strings).
   *
   * @param value value to validate
   * @return true if the value is valid, false otherwise
   */
  public static boolean isValidValue(final Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof String) {
      return StringUtils.isNotBlank((String) value);
    }

    return true;
  }
}
