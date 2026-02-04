package com.example.demo.resource;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Service to load Excel files and mapping configurations from fixed classpath directories.
 *
 * <p>Excel files are loaded from: {@code classpath:excel/}
 *
 * <p>Mapping YAML files are loaded from: {@code classpath:excel/mappings/}
 */
@Service
public class ResourceLoaderService {

  private final ResourceLoader resourceLoader;

  /**
   * Constructs the service.
   *
   * @param resourceLoader Spring's resource loader
   */
  public ResourceLoaderService(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Loads an Excel file from the fixed directory {@code classpath:excel/}.
   *
   * @param filename name of the Excel file (e.g., "customers.xlsx")
   * @return the loaded resource information
   * @throws IllegalArgumentException if filename is null or empty
   * @throws com.example.demo.exception.ResourceNotFoundException if file not found
   */
  public LoadedResource loadExcelFile(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Filename must not be null or empty");
    }

    String path = "classpath:excel/" + filename;
    Resource resource = resourceLoader.getResource(path);

    if (!resource.exists()) {
      throw new com.example.demo.exception.ResourceNotFoundException(
          "Excel file not found: " + filename + " (expected in classpath:excel/)");
    }

    return loadFromSpringResource(resource, filename, SourceType.CLASSPATH);
  }

  /**
   * Loads a mapping YAML file from the fixed directory {@code classpath:excel/mappings/}.
   *
   * @param filename name of the mapping file (e.g., "customer-validation.yml")
   * @return the loaded resource information
   * @throws IllegalArgumentException if filename is null or empty
   * @throws com.example.demo.exception.ResourceNotFoundException if file not found
   */
  public LoadedResource loadMappingFile(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Filename must not be null or empty");
    }

    String path = "classpath:excel/mappings/" + filename;
    Resource resource = resourceLoader.getResource(path);

    if (!resource.exists()) {
      throw new com.example.demo.exception.ResourceNotFoundException(
          "Mapping file not found: " + filename + " (expected in classpath:excel/mappings/)");
    }

    return loadFromSpringResource(resource, filename, SourceType.CLASSPATH);
  }

  private LoadedResource loadFromSpringResource(
      Resource resource, String originalPath, SourceType type) {
    try {
      long size = resource.contentLength();
      String filename = resource.getFilename();
      if (filename == null) {
        filename = originalPath;
      }
      return new LoadedResource(filename, size, type, MediaType.APPLICATION_OCTET_STREAM, resource);
    } catch (IOException e) {
      return new LoadedResource(
          originalPath, 0, type, MediaType.APPLICATION_OCTET_STREAM, resource);
    }
  }
}
