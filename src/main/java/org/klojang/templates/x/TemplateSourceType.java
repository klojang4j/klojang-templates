package org.klojang.templates.x;

/**
 * Symbolic constants for the origin of a Template instance
 */
public enum TemplateSourceType {
  /**
   * The template source code was provided as a string.
   */
  STRING,
  /**
   * The template source code was provided via a path string, to be resolved using
   * {@link Class#getResourceAsStream(String)}.
   */
  RESOURCE,
  /**
   * The template source code was provided via a {@code File} object.
   */
  FILE_SYSTEM,
  /**
   * The template source code was provided via a {@code PathResolver} object.
   */
  RESOLVER;
}
