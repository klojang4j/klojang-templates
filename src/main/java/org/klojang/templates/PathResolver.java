package org.klojang.templates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A {@code PathResolver} is used by the template parsing process to load the source
 * code for a template. Implementations are given the path string extracted from
 * {@code ~%%include:/path/to/resource.html%} and must return an {@link InputStream}
 * to the source code. This enables you to implement and use a custom mechanism for
 * loading templates. Klojang Templates provides two built-in mechanisms: loading
 * templates from the file system and loading templates from the classpath.
 *
 * @author Ayco Holleman
 * @see Template#fromResolver(PathResolver, String)
 * @see Template#fromFile(String)
 * @see Template#fromResource(Class, String)
 */
@FunctionalInterface
public interface PathResolver {

  /**
   * Returns whether the path specified in an included template
   * ({@code ~%%include:/path/to/resource.html%}) represents a valid resource. If it
   * is expensive to determine this (e.g. it requires a database lookup or an FTP
   * connection), you may return an empty {@code Optional}. If the {@code Optional}
   * contains a {@code Boolean.FALSE} this will result in fail-fast behaviour (namely
   * already when the template source is being parsed).
   *
   * @param path the path to verify
   * @return whether it is a valid path
   */
  default Optional<Boolean> isValidPath(String path) {
    return Optional.empty();
  }

  /**
   * Returns an {@code InputStream} to the resource denoted by the specified path.
   *
   * @param path the path
   * @return an {@code InputStream} to the resource denoted by the specified path
   * @throws IOException If an error occurred while setting up the
   *     {@code InputStream}.
   */
  InputStream resolvePath(String path) throws IOException;

}
