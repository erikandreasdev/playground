package com.example.demo.core.internal.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

class LoadedResourceTest {

  @Test
  void shouldCreateLoadedResource() {
    Resource mockResource = mock(Resource.class);
    LoadedResource loadedResource =
        new LoadedResource(
            "test.txt", 1024, SourceType.CLASSPATH, MediaType.TEXT_PLAIN, mockResource);

    assertThat(loadedResource.filename()).isEqualTo("test.txt");
    assertThat(loadedResource.size()).isEqualTo(1024);
    assertThat(loadedResource.sourceType()).isEqualTo(SourceType.CLASSPATH);
    assertThat(loadedResource.contentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(loadedResource.resource()).isEqualTo(mockResource);
  }

  @Test
  void shouldThrowExceptionForNullValues() {
    Resource mockResource = mock(Resource.class);

    assertThatThrownBy(
            () ->
                new LoadedResource(
                    null, 1024, SourceType.CLASSPATH, MediaType.TEXT_PLAIN, mockResource))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Filename must not be null");

    assertThatThrownBy(
            () -> new LoadedResource("test.txt", 1024, null, MediaType.TEXT_PLAIN, mockResource))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("SourceType must not be null");
  }

  @Test
  void shouldThrowExceptionForNegativeSize() {
    Resource mockResource = mock(Resource.class);

    assertThatThrownBy(
            () ->
                new LoadedResource(
                    "test.txt", -1, SourceType.CLASSPATH, MediaType.TEXT_PLAIN, mockResource))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Size must be non-negative");
  }

  @Test
  void shouldFormatReadableSize() {
    Resource mock = mock(Resource.class);

    assertThat(new LoadedResource("f", 500, SourceType.WEB, MediaType.ALL, mock).readableSize())
        .isEqualTo("500 B");

    assertThat(new LoadedResource("f", 1024, SourceType.WEB, MediaType.ALL, mock).readableSize())
        .isEqualTo("1.0 KB");

    assertThat(new LoadedResource("f", 1536, SourceType.WEB, MediaType.ALL, mock).readableSize())
        .isEqualTo("1.5 KB"); // 1.5 * 1024

    assertThat(new LoadedResource("f", 1048576, SourceType.WEB, MediaType.ALL, mock).readableSize())
        .isEqualTo("1.0 MB");
  }
}
