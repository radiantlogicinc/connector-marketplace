package com.radiantlogic.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.schema.SchemaFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that the connector's {@code @Entity} classes produce a valid IDDM schema.
 *
 * <p>IDDM runs this same generation (via {@link SchemaFactory}) when a data source is created; a
 * validation failure there aborts data source creation. These tests therefore pin the schema
 * definition so a future change that breaks it (a duplicate attribute name, a missing naming
 * attribute, an invalid identifier, a renamed object, etc.) fails at build time rather than at
 * deployment. They also assert the exact names, primary keys, and attribute sets the connector's
 * runtime routing and extraction code depend on.
 */
@DisplayName("Schema definition generated from the connector's @Entity classes")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaDefinitionTest {

  /**
   * Data source name passed to the factory. IDDM supplies the real value at runtime; the factory
   * only requires a valid Java identifier here.
   */
  private static final String DATA_SOURCE_NAME = "cyberark";

  private Schema schema;

  @BeforeEach
  void setUp() {
    schema = generateSchema().orElseThrow(() -> new IllegalStateException(
        "Schema generation returned an empty Optional; one or more @Entity classes failed the "
            + "SDK's validation"));
  }

  @Test
  @DisplayName("should generate successfully from all five entity classes")
  void generatesSuccessfully() {
    final Optional<Schema> generated = generateSchema();

    assertThat(generated).isPresent();
  }

  @Test
  @DisplayName("should contain one schema object per object type")
  void allSchemaObjectsGenerated() {
    final List<String> names = schema.getSchemaObjects().stream()
        .map(SchemaObject::getName)
        .collect(Collectors.toList());

    assertThat(names)
        .containsExactlyInAnyOrder("user", "role", "safe", "account", "directoryservice");
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("should derive the object class as the vd-prefixed capitalized name")
  @MethodSource("objectClassExpectations")
  void objectClass(final String schemaObjectName, final String expectedObjectClass) {
    // The object class appears in LDAP objectclass filters; IDDM derives "vd" + capitalized name.
    final SchemaObject schemaObject = schemaObjectNamed(schemaObjectName);

    assertThat(schemaObject.getObjectClass()).isEqualTo(expectedObjectClass);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("should expose exactly one primary key attribute recognized by the connector")
  @MethodSource("primaryKeyExpectations")
  void primaryKey(final String schemaObjectName, final String expectedPrimaryKey) {
    // getNamingAttributes() is what Utils.checkWhetherPrimaryKeyAttributeIsCorrectlySet reads
    // (via the deprecated getPrimaryKeys() alias); it must hold exactly the naming attribute.
    final SchemaObject schemaObject = schemaObjectNamed(schemaObjectName);

    assertThat(schemaObject.getNamingAttributes()).containsExactly(expectedPrimaryKey);
  }

  @ParameterizedTest(name = "{0}")
  @DisplayName("should default the naming key to the object name so RDNs match create routing")
  @MethodSource("objectNames")
  void namingKeyDefaultsToObjectName(final String schemaObjectName) {
    // Utils.getCreateRequestDetails/getModifyRequestDetails/getDeleteRequestDetails look for an
    // RDN attribute named after the object type, so the naming key must equal the object name.
    final SchemaObject schemaObject = schemaObjectNamed(schemaObjectName);

    assertThat(schemaObject.getNamingKey()).isEqualTo(schemaObjectName);
  }

  @ParameterizedTest(name = "{0}")
  @DisplayName("should expose exactly the documented attributes for each object type")
  @MethodSource("attributeExpectations")
  void attributeNames(final String schemaObjectName, final List<String> expectedAttributeNames) {
    final List<String> actualAttributeNames = schemaObjectNamed(schemaObjectName).getFields()
        .stream()
        .map(Field::getName)
        .collect(Collectors.toList());

    assertThat(actualAttributeNames)
        .containsExactlyInAnyOrderElementsOf(expectedAttributeNames);
  }

  @Test
  @DisplayName("should represent every attribute as a string")
  void allAttributesAreStrings() {
    final List<Field.Type> types = schema.getSchemaObjects().stream()
        .flatMap(schemaObject -> schemaObject.getFields().stream())
        .map(Field::getType)
        .distinct()
        .collect(Collectors.toList());

    assertThat(types).containsExactly(Field.Type.STRING);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("should mark only the naming attribute as required")
  @MethodSource("primaryKeyExpectations")
  void onlyNamingAttributeIsRequired(final String schemaObjectName, final String namingAttribute) {
    final List<String> requiredAttributeNames = schemaObjectNamed(schemaObjectName).getFields()
        .stream()
        .filter(field -> !field.isNullable())
        .map(Field::getName)
        .collect(Collectors.toList());

    assertThat(requiredAttributeNames).containsExactly(namingAttribute);
  }

  private Optional<Schema> generateSchema() {
    return SchemaFactory.ldap().from(
        DATA_SOURCE_NAME,
        Arrays.asList(
            UserEntity.class,
            RoleEntity.class,
            SafeEntity.class,
            AccountEntity.class,
            DirectoryServiceEntity.class));
  }

  private SchemaObject schemaObjectNamed(final String name) {
    return schema.getSchemaObject(name).orElseThrow(
        () -> new IllegalStateException("Missing schema object: " + name));
  }

  private Stream<Arguments> objectNames() {
    return Stream.of("user", "role", "safe", "account", "directoryservice").map(Arguments::of);
  }

  private Stream<Arguments> objectClassExpectations() {
    return Stream.of(
        Arguments.of("user", "vdUser"),
        Arguments.of("role", "vdRole"),
        Arguments.of("safe", "vdSafe"),
        Arguments.of("account", "vdAccount"),
        Arguments.of("directoryservice", "vdDirectoryservice"));
  }

  private Stream<Arguments> primaryKeyExpectations() {
    return Stream.of(
        Arguments.of("user", "Name"),
        Arguments.of("role", "Name"),
        Arguments.of("safe", "Name"),
        Arguments.of("account", "ID"),
        Arguments.of("directoryservice", "Name"));
  }

  private Stream<Arguments> attributeExpectations() {
    return Stream.of(
        Arguments.of("user", Arrays.asList(
            "Name", "DisplayName", "ID", "RiskLevelRank", "Status", "MobileNumber", "Mail",
            "Source")),
        Arguments.of("role", Arrays.asList(
            "Name", "ID", "RoleType", "Description", "Members", "RoleRights")),
        Arguments.of("safe", Arrays.asList(
            "Name", "Number", "Description", "Members", "Location", "ManagingCpm",
            "IsExpiredMember", "Created", "Modified", "Creator", "IsOlacEnabled",
            "NumberOfVersionsRetention", "NumberOfDaysRetention", "IsAutoPurgeEnabled")),
        Arguments.of("account", Arrays.asList(
            "ID", "Name", "Address", "UserName", "PlatformId", "SafeName", "SecretType",
            "CreatedTime", "SecretManagement", "PlatformAccountProperties",
            "CategoryModificationTime", "RemoteMachinesAccess")),
        Arguments.of("directoryservice", Arrays.asList(
            "Name", "Service", "DisplayName", "Tenant", "Status", "Config", "SyncRulesConfigured",
            "StatusDisplay", "Everybody", "Description", "SyncJobStatus", "DisplayNameShort",
            "DirectoryServiceUuid")));
  }
}
