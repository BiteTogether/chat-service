package com.bitetogether.chat_service.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for cursor-based pagination operations.
 */
public final class PaginationUtils {

  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = 100;

  private PaginationUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Normalize the page size to be within valid bounds.
   *
   * @param limit the requested limit
   * @return normalized page size (default 20, max 100)
   */
  public static int normalizePageSize(Integer limit) {
    return normalizePageSize(limit, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  }

  /**
   * Normalize the page size with custom bounds.
   *
   * @param limit      the requested limit
   * @param defaultSize the default page size
   * @param maxSize    the maximum page size
   * @return normalized page size
   */
  public static int normalizePageSize(Integer limit, int defaultSize, int maxSize) {
    if (limit == null || limit <= 0) {
      return defaultSize;
    }
    return Math.min(limit, maxSize);
  }

  /**
   * Create a PageRequest for fetching one extra item to determine if there are more items.
   *
   * @param limit the requested limit
   * @return PageRequest with size = normalizedLimit + 1
   */
  public static PageRequest createPageRequest(Integer limit) {
    int pageSize = normalizePageSize(limit);
    return PageRequest.of(0, pageSize + 1);
  }

  /**
   * Create a PageRequest with custom bounds for fetching one extra item.
   *
   * @param limit      the requested limit
   * @param defaultSize the default page size
   * @param maxSize    the maximum page size
   * @return PageRequest with size = normalizedLimit + 1
   */
  public static PageRequest createPageRequest(Integer limit, int defaultSize, int maxSize) {
    int pageSize = normalizePageSize(limit, defaultSize, maxSize);
    return PageRequest.of(0, pageSize + 1);
  }

  /**
   * Build a cursor-based page result from a list of items.
   *
   * @param items          the fetched items (may include one extra for hasMore detection)
   * @param requestedLimit the originally requested limit
   * @param cursorExtractor function to extract cursor value from an item
   * @param <T>            the item type
   * @param <C>            the cursor type
   * @return CursorPageResult containing the items and pagination info
   */
  public static <T, C> CursorPageResult<T, C> buildCursorPageResult(
      List<T> items,
      Integer requestedLimit,
      Function<T, C> cursorExtractor) {
    int pageSize = normalizePageSize(requestedLimit);
    boolean hasMore = items.size() > pageSize;

    // Remove the extra item if we fetched more than requested
    List<T> resultItems = hasMore
        ? items.subList(0, pageSize)
        : items;

    C nextCursor = resultItems.isEmpty()
        ? null
        : cursorExtractor.apply(resultItems.getLast());

    return CursorPageResult.<T, C>builder()
        .items(resultItems)
        .nextCursor(nextCursor)
        .hasMore(hasMore)
        .size(resultItems.size())
        .build();
  }

  /**
   * Generic cursor-based page result.
   *
   * @param <T> the item type
   * @param <C> the cursor type
   */
  @Data
  @Builder
  public static class CursorPageResult<T, C> {
    private List<T> items;
    private C nextCursor;
    private boolean hasMore;
    private int size;
  }
}



