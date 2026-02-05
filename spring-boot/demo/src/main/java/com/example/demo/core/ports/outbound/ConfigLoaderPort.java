package com.example.demo.core.ports.outbound;

import com.example.demo.core.internal.domain.config.ValidationConfig;
import com.example.demo.core.internal.valueobjects.LoadedResource;

/**
 * Outbound port for loading validation configuration.
 *
 * <p>This port abstracts the parsing of validation configuration files (YAML, JSON, etc.).
 */
public interface ConfigLoaderPort {

  /**
   * Loads validation configuration from a resource.
   *
   * @param configResource loaded configuration resource
   * @return parsed validation configuration
   * @throws com.example.demo.core.exceptions.MappingException if configuration is invalid
   * @throws RuntimeException if resource cannot be read
   */
  ValidationConfig loadConfig(LoadedResource configResource);
}
