package org.klojang.templates;

//import org.klojang.templates.x.parse.IncludedTemplatePart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * The {@code PathResolver} interface enables you to define a custom mechanism for
 * loading templates. Klojang provides two built-in mechanisms: loading templates
 * from the file system and loading templates from the classpath.
 *
 * @author Ayco Holleman
 * @see Template#fromResolver(PathResolver, String)
 * @see Template#fromFile(String)
 * @see Template#fromResource(Class, String)
 */
@FunctionalInterface
public interface PathResolver {

  /**
   * Returns an {@code Optional} containing a {@code Boolean} that specifies whether
   * the path specified in an included template
   * ({@code ~%%include:/path/to/resource%} represents a valid resource. If it is
   * expensive to determine this (e.g. requiring a database lookup or and FTP
   * connection) you may return an empty {@code Optional}. If the {@code Optional}
   * <i>does</i> contain a {@code Boolean} this will result in fail-fast behaviour
   * of the template parser.
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
