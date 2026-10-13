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
 * Unit tests for GroupUtil.
 */
class GroupUtilTest {

  @Test
  void testIsGetGroupUsersReturnsTrueWhenGroupUsersFieldPresent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObject(true);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupUsers(schema, targetSchema);

    assertTrue(result, "Should return true when GroupUsers field is present");
  }

  @Test
  void testIsGetGroupUsersReturnsFalseWhenGroupUsersFieldAbsent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObject(false);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupUsers(schema, targetSchema);

    assertFalse(result, "Should return false when GroupUsers field is absent");
  }

  @Test
  void testIsGetGroupUsersIsCaseInsensitive() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObjectWithField("groupusers");

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupUsers(schema, targetSchema);

    assertTrue(result, "Should return true for case-insensitive match");
  }

  @Test
  void testIsGetGroupRolesReturnsTrueWhenGroupRolesFieldPresent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObjectWithGroupRoles(true);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupRoles(schema, targetSchema);

    assertTrue(result, "Should return true when GroupRoles field is present");
  }

  @Test
  void testIsGetGroupRolesReturnsFalseWhenGroupRolesFieldAbsent() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObjectWithGroupRoles(false);

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupRoles(schema, targetSchema);

    assertFalse(result, "Should return false when GroupRoles field is absent");
  }

  @Test
  void testIsGetGroupRolesIsCaseInsensitive() {
    Schema schema = mock(Schema.class);
    ReadOnlyProperties targetSchema = mock(ReadOnlyProperties.class);
    SchemaObject groupObject = createGroupSchemaObjectWithField("grouproles");

    when(targetSchema.get("schemaObjects")).thenReturn(Collections.singletonList("group"));
    when(schema.getSchemaObject("group")).thenReturn(Optional.of(groupObject));

    boolean result = GroupUtil.isGetGroupRoles(schema, targetSchema);

    assertTrue(result, "Should return true for case-insensitive match");
  }

  private SchemaObject createGroupSchemaObject(final boolean includeGroupUsers) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Name", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field("Description", "", new HashSet<>(), false, Field.Type.STRING));

    if (includeGroupUsers) {
      fields.add(new Field("GroupUsers", "", new HashSet<>(), false, Field.Type.STRING));
    }

    return new SchemaObject("group", "vdGroup", "", "", "",
        Collections.singletonList("name"), new ArrayList<>(), fields);
  }

  private SchemaObject createGroupSchemaObjectWithGroupRoles(final boolean includeGroupRoles) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Name", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field("Description", "", new HashSet<>(), false, Field.Type.STRING));

    if (includeGroupRoles) {
      fields.add(new Field("GroupRoles", "", new HashSet<>(), false, Field.Type.STRING));
    }

    return new SchemaObject("group", "vdGroup", "", "", "",
        Collections.singletonList("name"), new ArrayList<>(), fields);
  }

  private SchemaObject createGroupSchemaObjectWithField(final String fieldName) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Name", "", new HashSet<>(), false, Field.Type.STRING));
    fields.add(new Field(fieldName, "", new HashSet<>(), false, Field.Type.STRING));

    return new SchemaObject("group", "vdGroup", "", "", "",
        Collections.singletonList("name"), new ArrayList<>(), fields);
  }
}
