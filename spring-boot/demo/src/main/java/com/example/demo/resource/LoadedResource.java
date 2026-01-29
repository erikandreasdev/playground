package com.example.demo.resource;

import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * Represents a resource that has been resolved and loaded with associated metadata.
 *
 * @param filename the name of the file or resource
 * @param size the size of the resource in bytes
 * @param sourceType the origin of the resource
 * @param contentType the MIME type of the resource
 * @param resource the actual Spring resource handle
 */
public record LoadedResource(
    String filename, long size, SourceType sourceType, MediaType contentType, Resource resource) {

  /**
   * Compact constructor to ensure non-null values.
   *
   * @param filename the name of the file or resource
   * @param size the size of the resource in bytes
   * @param sourceType the origin of the resource
   * @param contentType the MIME type of the resource
   * @param resource the actual Spring resource handle
   */
  public LoadedResource {
    Objects.requireNonNull(filename, "Filename must not be null");
    Objects.requireNonNull(sourceType, "SourceType must not be null");
    Objects.requireNonNull(contentType, "ContentType must not be null");
    Objects.requireNonNull(resource, "Resource must not be null");
    if (size < 0) {
      throw new IllegalArgumentException("Size must be non-negative");
    }
  }

  /**
   * Returns the size in a human-readable format (e.g., "1.2 MB").
   *
   * @return formatted size string
   */
  public String readableSize() {
    if (size < 1024) {
      return size + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
    return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
  }
}
