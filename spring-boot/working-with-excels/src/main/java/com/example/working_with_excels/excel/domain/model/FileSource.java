package com.example.working_with_excels.excel.domain.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;

/**
 * Represents potential sources for Excel and configuration files.
 *
 * <p>
 * Supports multiple file origins: classpath resources, filesystem paths,
 * input streams (for uploads), and URLs (for cloud downloads).
 */
public sealed interface FileSource {

    /** Default connection timeout for URL downloads (5 seconds). */
    int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    /** Default read timeout for URL downloads (30 seconds). */
    int DEFAULT_READ_TIMEOUT_MS = 30000;

    /** Default User-Agent for URL downloads. */
    String DEFAULT_USER_AGENT = "ExcelImporter/1.0";

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
     * <p>
     * <strong>Warning:</strong> Stream can only be read once.
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
     * File downloaded from a URL with configurable timeouts.
     *
     * @param url              the URL to download from
     * @param connectTimeoutMs connection timeout in milliseconds
     * @param readTimeoutMs    read timeout in milliseconds
     * @param allowedDomains   set of allowed domains (empty = allow all)
     * @param userAgent        the User-Agent header to use
     */
    record Url(URI url, int connectTimeoutMs, int readTimeoutMs, Set<String> allowedDomains, String userAgent)
            implements FileSource {

        /**
         * Creates a URL source with default timeouts and user agent.
         *
         * @param url the URL to download from
         */
        public Url(URI url) {
            this(url, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, Set.of(), DEFAULT_USER_AGENT);
        }

        @Override
        public InputStream openStream() throws IOException {
            validateDomain();

            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", userAgent);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + " from " + url);
            }

            return connection.getInputStream();
        }

        @Override
        public String description() {
            return "url:" + url;
        }

        private void validateDomain() throws IOException {
            if (allowedDomains.isEmpty()) {
                return; // No restriction
            }

            String host = url.getHost();
            if (host == null) {
                throw new IOException("Invalid URL: no host in " + url);
            }

            boolean allowed = allowedDomains.stream()
                    .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));

            if (!allowed) {
                throw new IOException("Domain not allowed: " + host
                        + ". Allowed domains: " + allowedDomains);
            }
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
     * Creates a URL source with default timeouts.
     *
     * @param url the URL string
     * @return a FileSource for URL
     */
    static FileSource url(String url) {
        return new Url(URI.create(url));
    }

    /**
     * Creates a URL source with default timeouts.
     *
     * @param uri the URI
     * @return a FileSource for URL
     */
    static FileSource url(URI uri) {
        return new Url(uri);
    }

    /**
     * Creates a URL source with custom settings.
     *
     * @param uri              the URI
     * @param connectTimeoutMs connection timeout in milliseconds
     * @param readTimeoutMs    read timeout in milliseconds
     * @param allowedDomains   set of allowed domains (empty = allow all)
     * @return a FileSource for URL
     */
    static FileSource url(URI uri, int connectTimeoutMs, int readTimeoutMs, Set<String> allowedDomains) {
        return new Url(uri, connectTimeoutMs, readTimeoutMs, allowedDomains, DEFAULT_USER_AGENT);
    }

    /**
     * Creates a URL source with domain restrictions.
     *
     * @param url            the URL string
     * @param allowedDomains set of allowed domains
     * @return a FileSource for URL
     */
    static FileSource urlWithDomainRestriction(String url, Set<String> allowedDomains) {
        return new Url(URI.create(url), DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, allowedDomains,
                DEFAULT_USER_AGENT);
    }

    /**
     * Creates a URL source with a custom User-Agent.
     *
     * @param url       the URL string
     * @param userAgent the User-Agent string
     * @return a FileSource for URL
     */
    static FileSource urlWithUserAgent(String url, String userAgent) {
        return new Url(URI.create(url), DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, Set.of(), userAgent);
    }
}
