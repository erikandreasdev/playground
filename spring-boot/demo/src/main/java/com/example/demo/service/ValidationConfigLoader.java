package com.example.demo.service;

import com.example.demo.domain.ValidationConfig;
import com.example.demo.resource.LoadedResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * Service responsible for loading and parsing YAML validation configurations.
 *
 * <p>Handles YAML parsing with proper error handling and exception mapping.
 */
@Service
public class ValidationConfigLoader {

  private final ObjectMapper yamlMapper;

  /** Constructs the loader with a YAML mapper. */
  public ValidationConfigLoader() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  /**
   * Loads validation configuration from a resource.
   *
   * @param configResource loaded configuration resource
   * @return parsed validation configuration
   * @throws com.example.demo.exception.MappingException if YAML is invalid
   * @throws RuntimeException if resource cannot be read
   */
  public ValidationConfig loadConfig(LoadedResource configResource) {
    try (InputStream is = configResource.resource().getInputStream()) {
      return yamlMapper.readValue(is, ValidationConfig.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new com.example.demo.exception.MappingException(
          "Invalid validation configuration: " + e.getOriginalMessage(), e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read validation config", e);
    }
  }
}
