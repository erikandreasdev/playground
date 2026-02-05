package com.example.demo.core.internal.domain.result;

/**
 * Metadata about a file used in validation.
 *
 * @param filename Name of the file
 * @param size Size in bytes
 * @param source Source location (e.g., path or URL)
 */
public record FileMetadata(String filename, long size, String source) {}
