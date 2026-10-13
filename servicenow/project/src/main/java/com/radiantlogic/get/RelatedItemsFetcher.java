package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;

import com.google.common.collect.Iterables;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * Generic fetcher for related items (child roles, user roles, group users, etc.). Consolidates the
 * common pattern of batching parent items and fetching related items via OR-joined queries with
 * pagination support.
 *
 * @param <P> Type of parent entity (e.g., RoleRow, UserRow, GroupRow)
 * @param <L> Type of link/relationship entity (e.g., ChildRoleRow, UserRoleRow, GroupUserRow)
 */
@Slf4j
@Builder
public class RelatedItemsFetcher<P, L> {

  /**
   * Request builder for creating API requests.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Executor for making API calls.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Number of parent items to process per batch.
   */
  private final int batchSize;
  /**
   * Function to extract name from parent entity.
   */
  private final Function<P, String> parentNameGetter;
  /**
   * Function to extract parent name from link entity.
   */
  private final Function<L, String> linkParentNameGetter;
  /**
   * Function to extract child name from link entity.
   */
  private final Function<L, String> linkChildNameGetter;
  /**
   * Function to set related items on parent entity.
   */
  private final BiConsumer<P, List<String>> parentSetter;
  /**
   * Function to build API request for fetching related items.
   */
  private final RequestBuilderFunction requestBuilderFunction;
  /**
   * Type token for deserializing link response.
   */
  private final Type linkResponseType;
  /**
   * Name of entity type for logging.
   */
  private final String entityTypeName;
  /**
   * Whether to normalize parent names for case-insensitive matching.
   */
  private final boolean normalizeParentNames;
  /**
   * Whether to initialize empty lists for parents with no related items.
   */
  private final boolean initializeEmptyLists;

  /**
   * Fetches and populates related items for all parent entities. This method will also capture
   * related items with blank name used by delete all related item operation. For simple get all
   * related items, same can be filtered out.
   *
   * @param parents List of parent entities
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case API response is not ok
   */
  public void populateRelatedItems(final List<P> parents)
      throws IOException, ServiceNowException {
    Iterable<List<P>> batches = Iterables.partition(parents, batchSize);
    for (List<P> batch : batches) {
      List<String> parentNames = batch.stream()
          .map(parentNameGetter)
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList());
      if (parentNames.isEmpty()) {
        continue;
      }
      Map<String, List<String>> relatedItemsMap = getRelatedItemsForBatch(parentNames);
      assignRelatedItemsToBatch(batch, relatedItemsMap);
    }
  }

  private void assignRelatedItemsToBatch(final List<P> batch,
                                         final Map<String, List<String>> relatedItemsMap) {
    for (P parent : batch) {
      String parentName = parentNameGetter.apply(parent);
      if (StringUtils.isBlank(parentName)) {
        continue;
      }
      String key = normalizeParentNames ? normalizeName(parentName) : parentName;
      parentSetter.accept(parent, relatedItemsMap.getOrDefault(key, Collections.emptyList()));
    }
  }

  /**
   * Fetches related items for a batch of parent entities.
   *
   * @param parentNames List of parent entity names
   * @return Map of parent name to list of related item names
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case API response is not ok
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private Map<String, List<String>> getRelatedItemsForBatch(final List<String> parentNames)
      throws IOException, ServiceNowException {
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    Map<String, List<String>> relatedItemsMap = new HashMap<>();
    Set<String> expectedParents = normalizeParentNames
        ? parentNames.stream().map(this::normalizeName).collect(Collectors.toSet())
        : null;

    String nextLink = "";
    do {
      Request request = requestBuilderFunction.buildRequest(requestBuilder, nextLink, parentNames);
      ResponseData responseData = requestExecutor.execute(request);
      validateResponse(responseData, parentNames);
      ServiceNowResponse<L> response =
          SERVICE_NOW_RESPONSE_GSON.fromJson(responseData.getApiResponse(), linkResponseType);
      processRelatedItems(response.getResult(), relatedItemsMap, expectedParents);
      nextLink = Util.getNextLink(responseData.getHeaders()).orElse("");
    } while (!nextLink.isEmpty());

    if (initializeEmptyLists) {
      for (String parentName : parentNames) {
        String key = normalizeParentNames ? normalizeName(parentName) : parentName;
        relatedItemsMap.computeIfAbsent(key, k -> new ArrayList<>());
      }
    }
    return relatedItemsMap;
  }

  private void validateResponse(final ResponseData responseData, final List<String> parentNames)
      throws ServiceNowException {
    if (responseData.getResponseCode() != HttpStatus.OK.asInt()) {
      throw new ServiceNowException(
          String.format(
              "Get %s API failed. status=%s, parents=%s, response=%s",
              entityTypeName,
              responseData.getResponseCode(),
              parentNames,
              responseData.getApiResponse()));
    }
  }

  /**
   * Processes related item rows from the API response and populates the related items map.
   *
   * <p>This method extracts parent-child relationships from the API response rows and groups
   * them by parent name. Both parent names and child names with blank values are skipped to ensure
   * data quality.
   *
   * <p><b>Blank Value Filtering:</b>
   * Rows with blank parent names or blank child names are skipped entirely. This prevents invalid
   * or incomplete relationship data from being processed.
   *
   * @param rows            List of link/relationship rows from the API response
   * @param relatedItemsMap Map to populate with parent name to list of child names
   * @param expectedParents Set of expected parent names for validation (null to skip validation)
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void processRelatedItems(final List<L> rows,
                                   final Map<String, List<String>> relatedItemsMap,
                                   final Set<String> expectedParents) {
    log.debug("{} fetched: {}", entityTypeName, rows.size());
    for (L row : rows) {
      String parentName = linkParentNameGetter.apply(row);
      String childName = linkChildNameGetter.apply(row);
      if (StringUtils.isBlank(parentName) || StringUtils.isBlank(childName)) {
        continue;
      }
      String normalizedParentName = normalizeParentNames ? normalizeName(parentName) : parentName;
      if (expectedParents != null && !expectedParents.contains(normalizedParentName)) {
        log.warn("Unexpected parent name in {} response: {}", entityTypeName, parentName);
        continue;
      }
      relatedItemsMap.computeIfAbsent(normalizedParentName, k -> new ArrayList<>())
          .add(childName);
    }
  }

  private String normalizeName(final String name) {
    return StringUtils.lowerCase(StringUtils.trim(name), Locale.ROOT);
  }

  /**
   * Functional interface for building API requests.
   */
  @FunctionalInterface
  public interface RequestBuilderFunction {

    /**
     * Builds an API request for fetching related items.
     *
     * @param requestBuilder Request builder instance
     * @param nextLink       Next link for pagination (empty string for first request)
     * @param parentNames    List of parent entity names
     * @return Built request
     * @throws IOException         In case of connectivity errors
     * @throws ServiceNowException In case of request building errors
     */
    Request buildRequest(RequestBuilder requestBuilder, String nextLink, List<String> parentNames)
        throws IOException, ServiceNowException;
  }
}
