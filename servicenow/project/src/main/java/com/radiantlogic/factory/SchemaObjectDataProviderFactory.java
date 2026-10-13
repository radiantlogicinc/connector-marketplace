package com.radiantlogic.factory;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.GroupsDataProvider;
import com.radiantlogic.get.RolesDataProvider;
import com.radiantlogic.get.UsersDataProvider;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.utility.SearchResponseConverter;

/**
 * Factory class for creating schema object data providers.
 */
public final class SchemaObjectDataProviderFactory {

  private SchemaObjectDataProviderFactory() {
  }

  /**
   * Method to get user schema data provider object.
   *
   * @param getRequest          Field to get data for various objects.
   * @param nextLink            Field containing next link to get paginated data.
   * @param uniqueKey           Field containing id of a particular user record to be fetched
   * @param schema              Field containing requested schema object
   * @param targetSchemaObjects Field containing requested schema details
   * @param converter           Field used to convert data
   * @param isGetUserRoles      Whether user roles to be fetched
   * @return User object SchemaObjectDataProvider
   */
  public static SchemaObjectDataProvider createUsersProvider(
      final GetRequest getRequest,
      final String nextLink,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter,
      final boolean isGetUserRoles) {

    return UsersDataProvider.builder()
        .getRequest(getRequest)
        .nextLink(nextLink)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .isGetUserRoles(isGetUserRoles)
        .build();
  }

  /**
   * Returns group schema data provider object.
   *
   * @param getRequest          Used to get data for various objects.
   * @param nextLink            Contains next link to get paginated data.
   * @param uniqueKey           Contains name of the particular group record to be fetched
   * @param schema              Contains requested schema object
   * @param targetSchemaObjects Contains requested schema details
   * @param converter           Used to convert data
   * @param isGetGroupUsers     Whether group users to be fetched
   * @param isGetGroupRoles     Whether group roles to be fetched
   * @return Group object SchemaObjectDataProvider
   */
  public static SchemaObjectDataProvider createGroupsProvider(
      final GetRequest getRequest,
      final String nextLink,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter,
      final boolean isGetGroupUsers,
      final boolean isGetGroupRoles) {

    return GroupsDataProvider.builder()
        .getRequest(getRequest)
        .nextLink(nextLink)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .isGetGroupUsers(isGetGroupUsers)
        .isGetGroupRoles(isGetGroupRoles)
        .build();
  }

  /**
   * Returns role schema data provider object.
   *
   * @param getRequest          Used to get data for various objects.
   * @param nextLink            Contains next link to get paginated data.
   * @param uniqueKey           Contains name of the particular role record to be fetched
   * @param schema              Contains requested schema object
   * @param targetSchemaObjects Contains requested schema details
   * @param converter           Used to convert data
   * @param isGetChildRoles     Whether child roles to be fetched
   * @return Role object SchemaObjectDataProvider
   */
  public static SchemaObjectDataProvider createRolesProvider(
      final GetRequest getRequest,
      final String nextLink,
      final String uniqueKey,
      final Schema schema,
      final ReadOnlyProperties targetSchemaObjects,
      final SearchResponseConverter converter,
      final boolean isGetChildRoles) {

    return RolesDataProvider.builder()
        .getRequest(getRequest)
        .nextLink(nextLink)
        .uniqueKey(uniqueKey)
        .schema(schema)
        .targetSchemaObjects(targetSchemaObjects)
        .converter(converter)
        .isGetChildRoles(isGetChildRoles)
        .build();
  }
}
