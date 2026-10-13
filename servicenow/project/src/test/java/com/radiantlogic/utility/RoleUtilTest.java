package com.radiantlogic.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RoleUtil.
 */
class RoleUtilTest {

  @Test
  void testCheckWhetherFetchChildRolesReturnsTrueWhenChildRolesFieldPresent() {
    Schema schema = mock(Schema.class);
    SchemaObject roleObject = createRoleSchemaObject(true);

    when(schema.getSchemaObject("role")).thenReturn(Optional.of(roleObject));

    boolean result = RoleUtil.checkWhetherFetchChildRoles(schema, "role");

    assertTrue(result, "Should return true when ChildRoles field is present");
  }

  @Test
  void testCheckWhetherFetchChildRolesReturnsFalseWhenChildRolesFieldAbsent() {
    Schema schema = mock(Schema.class);
    SchemaObject roleObject = createRoleSchemaObject(false);

    when(schema.getSchemaObject("role")).thenReturn(Optional.of(roleObject));

    boolean result = RoleUtil.checkWhetherFetchChildRoles(schema, "role");

    assertFalse(result, "Should return false when ChildRoles field is absent");
  }

  @Test
  void testCheckWhetherFetchChildRolesIsCaseInsensitive() {
    Schema schema = mock(Schema.class);
    SchemaObject roleObject = createRoleSchemaObjectWithField("childroles");

    when(schema.getSchemaObject("role")).thenReturn(Optional.of(roleObject));

    boolean result = RoleUtil.checkWhetherFetchChildRoles(schema, "role");

    assertTrue(result, "Should return true for case-insensitive match");
  }

  private SchemaObject createRoleSchemaObject(final boolean includeChildRoles) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Name", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field("Description", "", new HashSet<>(), false, Field.Type.STRING));

    if (includeChildRoles) {
      fields.add(new Field("ChildRoles", "", new HashSet<>(), false, Field.Type.STRING));
    }

    return new SchemaObject("role", "vdRole", "", "", "",
        Collections.singletonList("name"), new ArrayList<>(), fields);
  }

  private SchemaObject createRoleSchemaObjectWithField(final String fieldName) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Name", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field(fieldName, "", new HashSet<>(), false, Field.Type.STRING));

    return new SchemaObject("role", "vdRole", "", "", "",
        Collections.singletonList("name"), new ArrayList<>(), fields);
  }
}
