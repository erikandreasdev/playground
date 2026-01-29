package com.example.demo.resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/** Service to load resources from filesystem, classpath or web. */
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
   * Loads a resource from the given path or filename. Supports searching in: 1. Web (if URL
   * provided) 2. Classpath 3. Filesystem (absolute or relative)
   *
   * @param source the file path, URL, or filename
   * @return the loaded resource information
   * @throws IllegalArgumentException if resource cannot be loaded or found
   */
  public LoadedResource loadResource(String source) {
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("Source must not be null or empty");
    }

    // 1. Try as Web Resource (URL)
    if (source.startsWith("http://") || source.startsWith("https://")) {
      return loadFromWeb(source);
    }

    // 2. Try as Classpath Resource
    // Try with "classpath:" prefix explicitly first, or if it looks like a
    // classpath path
    Resource cpResource = resourceLoader.getResource("classpath:" + source);
    if (cpResource.exists()) {
      return loadFromSpringResource(cpResource, source, SourceType.CLASSPATH);
    }
    // Also try without prefix if the resourceLoader defaults to classpath (standard
    // behavior)
    Resource rawResource = resourceLoader.getResource(source);
    if (rawResource.exists() && isClasspathResource(rawResource)) {
      return loadFromSpringResource(rawResource, source, SourceType.CLASSPATH);
    }

    // 3. Try as Filesystem Resource
    // Absolute
    Path fsPath = Paths.get(source);
    if (fsPath.isAbsolute() && Files.exists(fsPath)) {
      return loadFromFileSystem(fsPath);
    }
    // Relative
    Path relativeFsPath = Paths.get(".").resolve(source).normalize();
    if (Files.exists(relativeFsPath)) {
      return loadFromFileSystem(relativeFsPath);
    }

    throw new com.example.demo.exception.ResourceNotFoundException("Resource not found: " + source);
  }

  private boolean isClasspathResource(Resource resource) {
    try {
      return resource.getURL().getProtocol().equals("jar")
          || resource.getURL().getProtocol().equals("file")
              && resource.getDescription().contains("class path resource");
    } catch (IOException e) {
      return false;
    }
  }

  private LoadedResource loadFromWeb(String url) {
    try {
      Resource resource = new UrlResource(url);
      if (resource.exists() && resource.isReadable()) {
        long size = resource.contentLength(); // Network call!
        String filename = resource.getFilename();
        if (filename == null) {
          filename = "unknown";
        }
        return new LoadedResource(
            filename, size, SourceType.WEB, MediaType.APPLICATION_OCTET_STREAM, resource);
      } else {
        throw new com.example.demo.exception.ResourceNotFoundException(
            "Web resource not found or unreadable: " + url);
      }
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL: " + url, e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load web resource: " + url, e);
    }
  }

  private LoadedResource loadFromFileSystem(Path path) {
    try {
      long size = Files.size(path);
      String filename = path.getFileName().toString();
      Resource resource = new FileSystemResource(path);
      return new LoadedResource(
          filename, size, SourceType.FILESYSTEM, MediaType.APPLICATION_OCTET_STREAM, resource);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file attributes: " + path, e);
    }
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
