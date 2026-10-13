package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Util}.
 *
 * <p>Verifies schema object retrieval, schema object name resolution,
 * and primary key validation behavior, including both successful and
 * exceptional execution paths.</p>
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class UtilTest {

  /**
   * The worker object name used for testing.
   */
  private static final String WORKER = "Worker";


  /**
   * Tests for scenarios where the schema object name is requested.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * retrieving the schema object name.
   */
  @Nested
  @DisplayName("When schema object name is requested")
  class WhenSchemaObjectNameIsRequested {

    @Test
    @DisplayName("Then an exception should be thrown when schema objects are null")
    void shouldThrowExceptionWhenSchemaObjectsIsNull() {
      ReadOnlyProperties targetSchemaObjects = mock(ReadOnlyProperties.class);

      when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY))
          .thenReturn(null);

      assertThatThrownBy(() ->
          Util.getSchemaObjectName(targetSchemaObjects))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE);
    }
  }

  /**
   * Tests for scenarios where primary key configuration is validated.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * validating primary key configuration settings.
   */
  @Nested
  @DisplayName("When primary key configuration is validated")
  class WhenPrimaryKeyConfigurationIsValidated {

    @Test
    @DisplayName("Then an exception should be thrown when a non-allowed primary key is configured")
    void shouldThrowExceptionWhenNonAllowedPrimaryKeyIsConfigured() {
      SchemaObject schemaObject = mock(SchemaObject.class);

      when(schemaObject.getPrimaryKeys())
          .thenReturn(Collections.singletonList("id"));

      assertThatThrownBy(() ->
          Util.checkWhetherPrimaryKeyAttributeIsCorrectlySet(schemaObject))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE);
    }

    @Test
    @DisplayName("Then an exception should be thrown when no primary key is configured")
    void shouldThrowExceptionWhenNoPrimaryKeyIsConfigured() {
      SchemaObject schemaObject = mock(SchemaObject.class);

      when(schemaObject.getPrimaryKeys())
          .thenReturn(Collections.emptyList());

      assertThatThrownBy(() ->
          Util.checkWhetherPrimaryKeyAttributeIsCorrectlySet(schemaObject))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE);
    }
  }

  /**
   * Tests for scenarios where a schema object is retrieved.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * retrieving schema object details.
   */
  @Nested
  @DisplayName("When schema object is retrieved")
  class WhenSchemaObjectIsRetrieved {

    @Test
    @DisplayName("Then the schema object should be returned when present")
    void shouldReturnSchemaObjectWhenPresent() {
      Schema schema = mock(Schema.class);
      SchemaObject schemaObject = mock(SchemaObject.class);

      when(schema.getSchemaObject(WORKER))
          .thenReturn(Optional.of(schemaObject));

      SchemaObject result = Util.getSchemaObject(schema, WORKER);

      assertThat(result).isSameAs(schemaObject);
    }

    @Test
    @DisplayName("Then an exception should be thrown when the schema object does not exist")
    void shouldThrowExceptionWhenSchemaObjectDoesNotExist() {
      Schema schema = mock(Schema.class);

      when(schema.getSchemaObject(WORKER))
          .thenReturn(Optional.empty());

      String expectedMessage =
          COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX + WORKER;

      assertThatThrownBy(() ->
          Util.getSchemaObject(schema, WORKER))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(expectedMessage);
    }
  }
}

