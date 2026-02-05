package com.example.demo.adapters.outbound.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.core.internal.valueobjects.LoadedResource;
import com.example.demo.core.internal.valueobjects.SourceType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/** Unit tests for {@link SpringResourceLoaderAdapter}. */
class SpringResourceLoaderAdapterTest {

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();
  private final SpringResourceLoaderAdapter adapter =
      new SpringResourceLoaderAdapter(resourceLoader);

  /** Verifies that a resource can be loaded from the filesystem. */
  @Test
  void loadResource_shouldLoadFromFilesystem(@TempDir File tempDir) throws IOException {
    File tempFile = new File(tempDir, "test-file.txt");
    Files.writeString(tempFile.toPath(), "content");

    String path = tempFile.getAbsolutePath();
    LoadedResource loaded = adapter.loadResource(path);

    assertThat(loaded).isNotNull();
    assertThat(loaded.filename()).isEqualTo("test-file.txt");
    assertThat(loaded.sourceType()).isEqualTo(SourceType.FILESYSTEM);
    assertThat(loaded.resource().exists()).isTrue();
  }

  /** Verifies that a resource can be loaded from the classpath. */
  @Test
  void loadResource_shouldLoadFromClasspath() {
    // Rely on a standard spring boot resource
    String path = "application.yml";
    LoadedResource loaded = adapter.loadResource(path);

    assertThat(loaded).isNotNull();
    assertThat(loaded.sourceType()).isEqualTo(SourceType.CLASSPATH);
  }
}
