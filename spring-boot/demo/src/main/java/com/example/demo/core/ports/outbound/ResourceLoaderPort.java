package com.example.demo.core.ports.outbound;

import com.example.demo.core.internal.valueobjects.LoadedResource;

/**
 * Outbound port for loading resources from various sources.
 *
 * <p>This port abstracts resource loading, supporting classpath, filesystem, and web sources.
 */
public interface ResourceLoaderPort {

  /**
   * Loads a resource from the given path or filename.
   *
   * <p>Supports searching in:
   * <ol>
   *   <li>Web (if URL provided)
   *   <li>Classpath
   *   <li>Filesystem (absolute or relative)
   * </ol>
   *
   * @param source the file path, URL, or filename
   * @return the loaded resource information
   * @throws IllegalArgumentException if resource cannot be loaded or found
   */
  LoadedResource loadResource(String source);
}
