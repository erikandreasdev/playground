package com.example.demo.adapters.outbound.resource;

import com.example.demo.core.exceptions.MappingException;
import com.example.demo.core.internal.domain.config.ValidationConfig;
import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.ports.outbound.ConfigLoaderPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adapter responsible for loading and parsing YAML validation configurations.
 *
 * <p>Handles YAML parsing with proper error handling and exception mapping.
 */
public class YamlConfigLoaderAdapter implements ConfigLoaderPort {

  private final ObjectMapper yamlMapper;

  /** Constructs the adapter with a YAML mapper. */
  public YamlConfigLoaderAdapter() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  /**
   * Loads validation configuration from a resource.
   *
   * @param configResource loaded configuration resource
   * @return parsed validation configuration
   * @throws MappingException if YAML is invalid
   * @throws RuntimeException if resource cannot be read
   */
  @Override
  public ValidationConfig loadConfig(LoadedResource configResource) {
    try (InputStream is = configResource.resource().getInputStream()) {
      return yamlMapper.readValue(is, ValidationConfig.class);
    } catch (JsonProcessingException e) {
      throw new MappingException("Invalid validation configuration: " + e.getOriginalMessage(), e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read validation config", e);
    }
  }
}
