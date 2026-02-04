package com.example.demo.resource;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/** Unit tests for {@link ResourceLoaderService}. */
class ResourceLoaderServiceTest {

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();
  private final ResourceLoaderService service = new ResourceLoaderService(resourceLoader);

  /** Verifies that loadExcelFile throws ResourceNotFoundException when file doesn't exist. */
  @Test
  void loadExcelFile_shouldThrowExceptionWhenNotFound() {
    String filename = "nonexistent.xlsx";
    org.junit.jupiter.api.Assertions.assertThrows(
        com.example.demo.exception.ResourceNotFoundException.class,
        () -> service.loadExcelFile(filename));
  }

  /** Verifies that loadMappingFile throws ResourceNotFoundException when file doesn't exist. */
  @Test
  void loadMappingFile_shouldThrowExceptionWhenNotFound() {
    String filename = "nonexistent.yml";
    org.junit.jupiter.api.Assertions.assertThrows(
        com.example.demo.exception.ResourceNotFoundException.class,
        () -> service.loadMappingFile(filename));
  }
}
