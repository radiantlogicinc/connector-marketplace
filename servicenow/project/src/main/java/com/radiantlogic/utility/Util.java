package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.ALLOWED_ENTITIES;
import static com.radiantlogic.utility.Constants.ALLOWED_PRIMARY_KEY_ATTRIBUTES;
import static com.radiantlogic.utility.Constants.BOOLEAN_ALLOWED_VALUES;
import static com.radiantlogic.utility.Constants.COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.GROUP_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.LINK_HEADER_KEY;
import static com.radiantlogic.utility.Constants.MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.ROLE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.SIZE_ONE;
import static com.radiantlogic.utility.Constants.UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.USER_ENTITY_KEY;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.iddm.ldap.RDN;
import com.radiantlogic.model.RequestDetails;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import org.apache.commons.lang3.StringUtils;

/**
 * Class containing common utility methods.
 */
@Slf4j
@SuppressWarnings({"PMD.ShortClassName", "PMD.ExcessiveImports", "PMD.GodClass",
    "PMD.TooManyMethods"})
public final class Util {

  /**
   * Field to define next link pattern.
   */
  private static final Pattern NEXT_LINK_PATTERN =
      Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"next\"");

  private Util() {
  }

  /**
   * Method to extract next link from response headers.
   *
   * @param headers Response headers.
   * @return Optional url to fetch next set of paginated data
   * @throws ServiceNowException In case link header is malformed
   */
  public static Optional<String> getNextLink(final Headers headers) throws ServiceNowException {
    if (headers == null || headers.names().isEmpty()) {
      log.debug("Response contains no headers.");
      return Optional.empty();
    }
    String linkHeader = headers.get(LINK_HEADER_KEY);
    if (linkHeader == null) {
      // in certain cases it has been seen that Link header is mising in normal cases also where
      // no further data is available
      log.info("Response missing Link header.");
      return Optional.empty();
    }
    Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    }
    if (linkHeader.toLowerCase(Locale.ROOT).contains("rel=")) {
      return Optional.empty(); // valid Link header, no next page
    }
    throw new ServiceNowException(String.format("Malformed Link header: %s", linkHeader));
  }

  /**
   * Method to return schema object name.
   *
   * @param targetSchemaObjects ReadOnlyProperties containing schema objects
   * @return Schema object name
   */
  public static String getSchemaObjectName(final ReadOnlyProperties targetSchemaObjects) {
    @SuppressWarnings("unchecked")
    // Connector SDK guarantees target schema objects are returned in List<String>
    List<String> schemaObjectNames = (List<String>) targetSchemaObjects.get(SCHEMA_OBJECTS_KEY);
    if (schemaObjectNames == null || schemaObjectNames.size() != 1) {
      log.error(UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE);
      throw new IllegalStateException(UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE);
    }
    return schemaObjectNames.get(0);
  }

  /**
   * Method to check whether primary key attribute is correctly set.
   *
   * @param schemaObject Schema object for which primary key attribute is to be checked
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
   * Method to return schema object.
   *
   * @param schema           Schema
   * @param schemaObjectName Schema object name
   * @return SchemaObject
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
   * Method to extract unique key from ldap search request.
   *
   * @param searchRequest LdapSearchRequest containing search request details
   * @return Unique key for which data to be fetched
   */
  public static String getUniqueKey(final LdapSearchRequest searchRequest) {
    return searchRequest.getBaseDN().getRDNs().get(0).getAttributes().get(0).getValues().get(0);
  }

  /**
   * Method to get fields of a schema object.
   *
   * @param schemaObject Schema object for which fields to be fetched
   * @return List of fields
   */
  public static List<String> getFieldNames(final SchemaObject schemaObject) {
    return schemaObject.getFields().stream().map(Field::getName).collect(Collectors.toList());
  }

  /**
   * Method to check whether value is valid or not.
   *
   * @param value Value to be checked
   * @return boolean Whether value is valid or not
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

  /**
   * Method to gather request details for delete request.
   *
   * @param ldapDeleteRequest LdapDeleteRequest containing request details
   * @return RequestDetails containing delete request details
   * @throws ServiceNowException In case required details are not found
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public static RequestDetails getDeleteRequestDetails(final LdapDeleteRequest ldapDeleteRequest)
      throws ServiceNowException {
    List<String> dnAttributeNames = new ArrayList<>();
    for (RDN component : ldapDeleteRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        dnAttributeNames.add(attribute.getName());
        if (ALLOWED_ENTITIES.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
          List<String> values = attribute.getValues();
          if (values == null || values.isEmpty() || StringUtils.isBlank(values.get(0))) {
            continue;
          }
          RequestDetails requestDetails = new RequestDetails();
          requestDetails.setUserRequest(
              USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setUniqueKey(values.get(0));
          return requestDetails;
        }
      }
    }
    throw new ServiceNowException(String.format(
        "allowed entities are: %s, but found: %s or the unique key is blank", ALLOWED_ENTITIES,
        dnAttributeNames));
  }

  /**
   * Method to capture request details for update request.
   *
   * @param ldapModifyRequest LDAP modify request containing the DN and attributes
   * @return RequestDetails Contains the extracted entity information and the unique key
   * @throws ServiceNowException In case the request entities contain invalid entities or the unique
   *                             key is blank
   */
  public static RequestDetails getModifyRequestDetails(final LdapModifyRequest ldapModifyRequest)
      throws ServiceNowException {
    List<String> dnAttributeNames = new ArrayList<>();
    for (RDN component : ldapModifyRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        dnAttributeNames.add(attribute.getName());
        if (ALLOWED_ENTITIES.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
          List<String> values = attribute.getValues();
          if (values.isEmpty() || StringUtils.isBlank(values.get(0))) {
            continue;
          }
          @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
          RequestDetails requestDetails = new RequestDetails();
          requestDetails.setUserRequest(USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setGroupRequest(GROUP_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setRoleRequest(ROLE_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setUniqueKey(values.get(0));
          return requestDetails;
        }
      }
    }
    throw new ServiceNowException(String.format(
        "allowed entities are: %s, but found: %s or the unique key is blank", ALLOWED_ENTITIES,
        dnAttributeNames));
  }

  /**
   * Method to return backend attribute name corresponding to frontend mapped/attribute name.
   *
   * @param schemaObject  Schema object containing details
   * @param attributeName Mapped/Attribute name for which backend attribute name to be fetched
   * @return Matching Attribute name
   */
  public static String getAttributeName(final SchemaObject schemaObject,
                                        final String attributeName) {
    if (schemaObject == null || StringUtils.isBlank(attributeName)) {
      return null;
    }
    checkWhetherPrimaryKeyAttributeIsCorrectlySet(schemaObject);
    return schemaObject.getFields().stream()
        .filter(Objects::nonNull)
        .filter(field -> StringUtils.equalsIgnoreCase(field.getMappedAttrName(), attributeName)
            || StringUtils.equalsIgnoreCase(field.getName(), attributeName))
        .map(Field::getName)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElse(null);
  }

  /**
   * Validates that the provided value represents a valid boolean string.
   *
   * @param value boolean value as a string to be validated
   * @throws ServiceNowException If the lowercase value is not a valid boolean string
   */
  public static void checkWhetherValidBooleanValue(final String value) throws ServiceNowException {
    if (StringUtils.isBlank(value) || !BOOLEAN_ALLOWED_VALUES.contains(
        value.toLowerCase(Locale.ROOT))) {
      String errorMessage = String.format("invalid boolean value received: %s", value);
      log.error(errorMessage);
      throw new ServiceNowException(errorMessage);
    }
  }

  /**
   * Validates that the given attribute contains exactly one value.
   *
   * @param attribute Attribute to validate
   * @throws ServiceNowException If the attribute does not contain exactly one value
   */
  public static void validateSingleValue(final Attribute attribute)
      throws ServiceNowException {
    if (attribute.getValues().size() != SIZE_ONE) {
      throw new ServiceNowException(
          String.format("Only single value for attribute: %s is supported",
              attribute.getName()));
    }
  }

  /**
   * Validates that the given modification type is not DELETE.
   *
   * <p>This method is used to prevent deletion of attributes that are not
   * allowed to be removed.
   *
   * @param modificationType Modification type to validate
   * @param message          Error message to include in the exception if validation fails
   * @throws ServiceNowException If the modification type is DELETE
   */
  public static void validateNotDelete(
      final Modification.ModificationType modificationType,
      final String message) throws ServiceNowException {
    if (modificationType == Modification.ModificationType.DELETE) {
      throw new ServiceNowException(message);
    }
  }

  /**
   * Validates whether the value in lowercase is a string boolean or not.
   *
   * @param value Value to be validated
   * @return boolean string in lowercase
   * @throws ServiceNowException In case the value is not a valid boolean string
   */
  public static String validateAndNormalizeBoolean(final String value)
      throws ServiceNowException {
    String normalized = value.toLowerCase(Locale.ROOT);
    checkWhetherValidBooleanValue(normalized);
    return normalized;
  }

  /**
   * Validates that the provided string value is not blank.
   *
   * @param value        String value to validate
   * @param errorMessage Error message to include in the exception if validation fails
   * @throws ServiceNowException If the value is blank
   */
  public static void validateNotBlank(final String value, final String errorMessage)
      throws ServiceNowException {
    if (StringUtils.isBlank(value)) {
      throw new ServiceNowException(errorMessage);
    }
  }

  /**
   * Validates that the given modification type is ADD.
   *
   * <p>This method ensures that only ADD operations are permitted. If the
   * provided modification type is anything other than ADD, a {@link ServiceNowException} is
   * thrown.
   *
   * @param modificationType the modification type to validate
   * @param message          the error message to include in the exception if validation fails
   * @throws ServiceNowException if the modification type is not ADD
   */
  public static void validateOnlyAdd(
      final Modification.ModificationType modificationType,
      final String message) throws ServiceNowException {
    if (modificationType != Modification.ModificationType.ADD) {
      throw new ServiceNowException(message);
    }
  }

  /**
   * Extracts values for ADD & REPLACE operations, handling both single values and JSON arrays.
   * Values are normalized to lowercase.
   *
   * <p><b>Null Filtering:</b> This method filters out null values from the result set via
   * {@code .filter(Objects::nonNull)}. This is a deliberate design decision to handle cases where
   * JSON arrays contain null elements (e.g., ["admin", null]). The backend does not properly handle
   * nulls, so the connector enforces this filtering.
   *
   * @param attribute Attribute containing values to extract
   * @return Set of extracted and normalized values
   * @throws ServiceNowException if any value is blank/if the value is null
   */
  @NonNull
  public static Set<String> getValuesForModificationType(final Attribute attribute)
      throws ServiceNowException {

    // if the value contains a comma, then paste it individually & not in either json array or comma
    // separated like value1, is fine, but [value1,] or value1,,value2 will give an error
    // the problem lies in the fact that backend is receiving values without inverted comma like
    // [fd_read_operations, view_changer] as against ideal ["fd_read_operations", "view_changer"]

    Type setType = new TypeToken<Set<String>>() {
    }.getType();
    // pasted as an input or json, pasted in an array as input ["value1","value"]
    Set<String> allValues = new HashSet<>();
    for (String value : attribute.getValues()) {
      if (StringUtils.isBlank(value)) {
        throw new ServiceNowException("value cannot be blank");
      }
      try {
        Set<String> values = GSON.fromJson(value, setType);
        if (values == null) {
          throw new ServiceNowException("value can't be null");
        }
        allValues.addAll(values);
      } catch (JsonSyntaxException e) {
        allValues.add(value);
      }
    }
    return allValues.stream().filter(Objects::nonNull).map(String::toLowerCase)
        .collect(Collectors.toSet());
  }
}
