package com.radiantlogic.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UserUtil.
 */
class UserUtilTest {

  @Test
  void testIsGetUserRolesReturnsTrueWhenUserRolesFieldPresent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject userObject = createUserSchemaObject(true);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject("user")).thenReturn(Optional.of(userObject));

    boolean result = UserUtil.isGetUserRoles(schema, targetSchema);

    assertTrue(result, "Should return true when UserRoles field is present");
  }

  @Test
  void testIsGetUserRolesReturnsFalseWhenUserRolesFieldAbsent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject userObject = createUserSchemaObject(false);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject("user")).thenReturn(Optional.of(userObject));

    boolean result = UserUtil.isGetUserRoles(schema, targetSchema);

    assertFalse(result, "Should return false when UserRoles field is absent");
  }

  @Test
  void testIsGetUserRolesIsCaseInsensitive() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject userObject = createUserSchemaObjectWithField("userroles");

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject("user")).thenReturn(Optional.of(userObject));

    boolean result = UserUtil.isGetUserRoles(schema, targetSchema);

    assertTrue(result, "Should return true for case-insensitive match");
  }

  private SchemaObject createUserSchemaObject(final boolean includeUserRoles) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("UserName", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field("Email", "", new HashSet<>(), false, Field.Type.STRING));

    if (includeUserRoles) {
      fields.add(new Field("UserRoles", "", new HashSet<>(), false, Field.Type.STRING));
    }

    return new SchemaObject("user", "vdUser", "", "", "",
        Collections.singletonList("username"), new ArrayList<>(), fields);
  }

  private SchemaObject createUserSchemaObjectWithField(final String fieldName) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("UserName", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field(fieldName, "", new HashSet<>(), false, Field.Type.STRING));

    return new SchemaObject("user", "vdUser", "", "", "",
        Collections.singletonList("username"), new ArrayList<>(), fields);
  }
}
