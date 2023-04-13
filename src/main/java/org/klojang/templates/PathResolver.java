package org.klojang.templates;

import java.io.IOException;
import java.io.InputStream;

/**
 * Path resolvers are used by the template parser to load the source code for a
 * template. Implementations are given the path string extracted from
 * {@code ~%%include:/path/to/resource.html%%} and must return an {@link InputStream}
 * to the source code. This enables you to implement and use a custom mechanism for
 * loading templates. <i>Klojang Templates</i> provides two implementations itself:
 * one that loads templates from the file system (used under the hood by
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
   * Returns whether the path specified in an included template (like
   * {@code ~%%include:/path/to/foo.html%%}) points to an existing resource. If it is
   * expensive to determine this (e.g. it requires a database lookup or an FTP
   * connection), you may simply return {@code true}. Parsing will still fail
   * graciously when the path turns out to be invalid, but it will be some time after
   * the included template was first encountered, which may make it harder to
   * understand what was wrong with the template code. If this method returns
   * {@code false}, parsing will be aborted immediately. The default implementation
   * returns {@code true}.
   *
   * @param path the path to verify
   * @return whether it is a valid path
   */
  default boolean isValidPath(String path) {
    return true;
  }

  /**
   * Returns an {@code InputStream} to the resource denoted by the specified path.
   *
   * @param path the path
   * @return an {@code InputStream} to the resource denoted by the specified path
   * @throws IOException If an error occurred while setting up the
   *     {@code InputStream}.
   */
  InputStream resolve(String path) throws IOException;

}
