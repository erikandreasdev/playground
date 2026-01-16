package com.example.working_with_excels.excel.domain.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.ClassPathResource;

/**
 * Represents potential sources for Excel and configuration files.
 *
 * <p>
 * Supports multiple file origins: classpath resources, filesystem paths,
 * input streams (for uploads), and URLs (for cloud downloads).
 */
public sealed interface FileSource {

    /**
     * Opens an input stream to read the file content.
     *
     * @return an input stream to the file
     * @throws IOException if the file cannot be read
     */
    InputStream openStream() throws IOException;

    /**
     * Gets a descriptive name for this source (for logging/reporting).
     *
     * @return the source description
     */
    String description();

    // -------------------------------------------------------------------------
    // Implementations
    // -------------------------------------------------------------------------

    /**
     * File from classpath resources (e.g., src/main/resources).
     *
     * @param resourcePath the classpath resource path
     */
    record Classpath(String resourcePath) implements FileSource {
        @Override
        public InputStream openStream() throws IOException {
            return new ClassPathResource(resourcePath).getInputStream();
        }

        @Override
        public String description() {
            return "classpath:" + resourcePath;
        }
    }

    /**
     * File from the filesystem.
     *
     * @param path the filesystem path
     */
    record Filesystem(Path path) implements FileSource {
        @Override
        public InputStream openStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public String description() {
            return "file:" + path.toAbsolutePath();
        }
    }

    /**
     * File from an input stream (e.g., multipart upload).
     *
     * @param inputStream the input stream
     * @param filename    the original filename
     */
    record Stream(InputStream inputStream, String filename) implements FileSource {
        @Override
        public InputStream openStream() {
            return inputStream;
        }

        @Override
        public String description() {
            return "stream:" + filename;
        }
    }

    /**
     * File downloaded from a URL.
     *
     * @param url the URL to download from
     */
    record Url(URI url) implements FileSource {
        @Override
        public InputStream openStream() throws IOException {
            return url.toURL().openStream();
        }

        @Override
        public String description() {
            return "url:" + url;
        }
    }

    // -------------------------------------------------------------------------
    // Factory Methods
    // -------------------------------------------------------------------------

    /**
     * Creates a classpath source.
     *
     * @param resourcePath the classpath resource path
     * @return a FileSource for classpath
     */
    static FileSource classpath(String resourcePath) {
        return new Classpath(resourcePath);
    }

    /**
     * Creates a filesystem source.
     *
     * @param path the filesystem path as string
     * @return a FileSource for filesystem
     */
    static FileSource filesystem(String path) {
        return new Filesystem(Path.of(path));
    }

    /**
     * Creates a filesystem source.
     *
     * @param path the filesystem path
     * @return a FileSource for filesystem
     */
    static FileSource filesystem(Path path) {
        return new Filesystem(path);
    }

    /**
     * Creates a stream source (for multipart uploads).
     *
     * @param inputStream the input stream
     * @param filename    the original filename
     * @return a FileSource for stream
     */
    static FileSource stream(InputStream inputStream, String filename) {
        return new Stream(inputStream, filename);
    }

    /**
     * Creates a URL source (for cloud downloads).
     *
     * @param url the URL string
     * @return a FileSource for URL
     */
    static FileSource url(String url) {
        return new Url(URI.create(url));
    }

    /**
     * Creates a URL source.
     *
     * @param uri the URI
     * @return a FileSource for URL
     */
    static FileSource url(URI uri) {
        return new Url(uri);
    }
}
