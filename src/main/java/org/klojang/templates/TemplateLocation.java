package org.klojang.templates;

import org.klojang.util.IOMethods;

import java.io.IOException;
import java.io.InputStream;

import static org.klojang.templates.PathResolver.INVALID_PATH;

public record TemplateLocation(String path, PathResolver resolver) {

  public static final TemplateLocation STRING = new TemplateLocation();

  private TemplateLocation() {
    this(null, null);
  }

  public TemplateLocation(PathResolver resolver) {
    this(null, resolver);
  }

  public boolean isInvalid() {
    return resolver.isValidPath(path).equals(INVALID_PATH);
  }

  public String read() throws PathResolutionException {
    try (InputStream in = resolver.resolvePath(path)) {
      return IOMethods.getContents(in);
    } catch (IOException e) {
      throw new PathResolutionException(path);
    }
  }

  /*
   * Returns whether the template was created from a string (hence doesn't really
   * have a "physical" location).
   */
  boolean isString() {
    return path == null;
  }

  public String toString() {
    return path;
  }

}
