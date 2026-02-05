package com.example.demo.core.exceptions;

/** Exception thrown when there are mapping errors (structural or config). */
public class MappingException extends RuntimeException {

  public MappingException(String message) {
    super(message);
  }

  public MappingException(String message, Throwable cause) {
    super(message, cause);
  }
}
