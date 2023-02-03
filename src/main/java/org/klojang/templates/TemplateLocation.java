package org.klojang.templates;

import org.klojang.util.IOMethods;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class TemplateLocation {

  public static final TemplateLocation STRING = new TemplateLocation();

  private final PathResolver resolver;
  private final String path;

  private TemplateLocation() {
    this.path = null;
    this.resolver = null;
  }

  public TemplateLocation(PathResolver resolver) {
    this(null, resolver);
  }

  public TemplateLocation(String path, PathResolver resolver) {
    this.path = path;
    this.resolver = resolver;
  }

  public boolean isInvalid() {
    return resolver.isValidPath(path).equals(PathResolver.INVALID_PATH);
  }

  public String read() throws PathResolutionException {
    try (InputStream in = resolver.resolvePath(path)) {
      return IOMethods.getContents(in);
    } catch (IOException e) {
      throw new PathResolutionException(path);
    }
  }

  public String getPath() {
    return path;
  }

  public PathResolver getResolver() {
    return resolver;
  }

  /*
   * Returns whether the template was created from a string (hence doesn't really
   * have a "physical" location).
   */
  boolean isString() {
    return path == null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, resolver);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof TemplateLocation other) {
      return Objects.equals(path, other.path) && resolver.equals(other.resolver);
    }
    return false;
  }

  public String toString() {
    return path;
  }

}
