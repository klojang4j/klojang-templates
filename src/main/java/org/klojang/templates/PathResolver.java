package org.klojang.templates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A {@code PathResolver} is used by the template parsing process to load the source
 * code for a template. Implementations are given the path string extracted from
 * {@code ~%%include:/path/to/resource.html%} and must return an {@link InputStream}
 * to the source code. This enables you to implement and use a custom mechanism for
 * loading templates. Klojang Templates provides two implementations itself: one that
 * loads templates from the file system (used under the hood by
 * {@link Template#fromFile(String) Template.fromFile()}), and another one that loads
 * templates from the classpath (used by
 * {@link Template#fromResource(Class, String) Template.fromResource()}). You can use
 * different path resolvers for different templates, but nested templates always
 * inherit the {@code PathResolver} from the root template (the template that was
 * explicitly instantiated using one of the {@code fromXXX()} methods on the
 * {@code Template} class.
 *
 * @author Ayco Holleman
 * @see Template#fromResolver(PathResolver, String)
 * @see Template#fromFile(String)
 * @see Template#fromResource(Class, String)
 */
@FunctionalInterface
public interface PathResolver {

  /**
   * An {@code Optional} containing {@code Boolean.TRUE}. Can be used as return value
   * for {@link #isValidPath(String) isValidPath()}.
   */
  Optional<Boolean> VALID_PATH = Optional.of(Boolean.TRUE);
  /**
   * An {@code Optional} containing {@code Boolean.FALSE}. Can be used as return
   * value for {@link #isValidPath(String) isValidPath()}.
   */
  Optional<Boolean> INVALID_PATH = Optional.of(Boolean.FALSE);

  /**
   * Returns whether the path specified in an included template (like
   * {@code ~%%include:/path/to/foo.html%}) points to an existing resource. If it is
   * expensive to determine this (e.g. it requires a database lookup or an FTP
   * connection), you may return an empty {@code Optional}. If the {@code Optional}
   * does contain a value and it is {@code Boolean.FALSE}, this will result in
   * fail-fast behaviour; a {@link ParseException} is thrown immediately upon
   * receiving {@code Boolean.FALSE}. When returning an empty {@code Optional},
   * parsing will fail at a later point in the parsing process, which makes it
   * somewhat harder to understand what was wrong with the template code. The default
   * implementation returns an empty {@code Optional}.
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
