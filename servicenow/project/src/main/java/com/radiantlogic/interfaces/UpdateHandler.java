package com.radiantlogic.interfaces;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.model.RequestDetails;
import java.io.IOException;

/**
 * Strategy interface for handling different types of update requests.
 *
 * <p>Implementations determine whether they can process a given request and execute
 * the appropriate update logic for the supported object type.
 */
public interface UpdateHandler {

  /**
   * Determines whether this handler supports the provided request.
   *
   * @param requestDetails details extracted from the modify request
   * @return {@code true} if this handler can process the request; otherwise {@code false}
   */
  boolean supports(RequestDetails requestDetails);

  /**
   * Processes the update request for the supported object type.
   *
   * @param uniqueKey         unique identifier of the object being updated
   * @param ldapModifyRequest LDAP modify request containing update details
   * @param schemaObject      schema definition for the target object
   * @throws ServiceNowException if the update operation fails due to API or validation issues
   * @throws IOException         if a communication error occurs while invoking the API
   */
  void process(String uniqueKey,
               LdapModifyRequest ldapModifyRequest,
               SchemaObject schemaObject)
      throws ServiceNowException, IOException;
}
