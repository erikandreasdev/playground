package com.example.demo.core.internal.valueobjects;

/** Defines the origin of a loaded resource. */
public enum SourceType {
  /** Resource loaded from the application classpath. */
  CLASSPATH,

  /** Resource loaded from the local filesystem. */
  FILESYSTEM,

  /** Resource loaded from a web URL. */
  WEB
}
