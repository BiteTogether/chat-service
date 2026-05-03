package com.bitetogether.chat_service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class PaginationUtilsTest {

  @Test
  void normalizePageSize_WithNull_ReturnsDefault() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(null);

    // Assert
    assertEquals(20, result);
  }

  @Test
  void normalizePageSize_WithZero_ReturnsDefault() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(0);

    // Assert
    assertEquals(20, result);
  }

  @Test
  void normalizePageSize_WithNegative_ReturnsDefault() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(-5);

    // Assert
    assertEquals(20, result);
  }

  @Test
  void normalizePageSize_WithinBounds_ReturnsValue() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(50);

    // Assert
    assertEquals(50, result);
  }

  @Test
  void normalizePageSize_ExceedingMax_ReturnsMax() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(200);

    // Assert
    assertEquals(100, result);
  }

  @Test
  void normalizePageSize_WithCustomBounds_WorksCorrectly() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(15, 10, 50);

    // Assert
    assertEquals(15, result);
  }

  @Test
  void normalizePageSize_WithCustomBoundsExceeding_ReturnsCustomMax() {
    // Arrange & Act
    int result = PaginationUtils.normalizePageSize(60, 10, 50);

    // Assert
    assertEquals(50, result);
  }

  @Test
  void createPageRequest_IncludesExtraItem() {
    // Arrange & Act
    PageRequest pageRequest = PaginationUtils.createPageRequest(20);

    // Assert
    assertEquals(21, pageRequest.getPageSize());
    assertEquals(0, pageRequest.getPageNumber());
  }

  @Test
  void createPageRequest_WithNull_UsesDefault() {
    // Arrange & Act
    PageRequest pageRequest = PaginationUtils.createPageRequest(null);

    // Assert
    assertEquals(21, pageRequest.getPageSize());
  }

  @Test
  void buildCursorPageResult_WhenHasMore_TruncatesAndSetsNextCursor() {
    // Arrange
    List<String> items = IntStream.rangeClosed(1, 21).mapToObj(i -> "item_" + i).toList();

    // Act
    PaginationUtils.CursorPageResult<String, Integer> result =
        PaginationUtils.buildCursorPageResult(items, 20, String::length);

    // Assert
    assertTrue(result.isHasMore());
    assertEquals(20, result.getSize());
    assertEquals(20, result.getItems().size());
    assertNotNull(result.getNextCursor());
  }

  @Test
  void buildCursorPageResult_WhenNoMore_ReturnsFull() {
    // Arrange
    List<String> items = List.of("a", "b", "c");

    // Act
    PaginationUtils.CursorPageResult<String, Integer> result =
        PaginationUtils.buildCursorPageResult(items, 20, String::length);

    // Assert
    assertFalse(result.isHasMore());
    assertEquals(3, result.getSize());
    assertEquals(3, result.getItems().size());
  }

  @Test
  void buildCursorPageResult_WhenEmpty_ReturnsEmptyResult() {
    // Arrange
    List<String> items = List.of();

    // Act
    PaginationUtils.CursorPageResult<String, Integer> result =
        PaginationUtils.buildCursorPageResult(items, 20, String::length);

    // Assert
    assertFalse(result.isHasMore());
    assertEquals(0, result.getSize());
    assertNull(result.getNextCursor());
  }
}
