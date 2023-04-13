package org.klojang.templates;

import org.klojang.util.IOMethods;

import java.io.IOException;
import java.io.InputStream;

import static org.klojang.templates.ParseErrorCode.INVALID_INCLUDE_PATH;

record TemplateLocation(String path, PathResolver resolver) {

  static final TemplateLocation STRING = new TemplateLocation();

  private TemplateLocation() {
    this(null, null);
  }

  TemplateLocation(PathResolver resolver) {
    this(null, resolver);
  }

  boolean isValid() {
    return resolver.isValidPath(path);
  }

  String read() throws ParseException {
    try (InputStream in = resolver.resolve(path)) {
      return IOMethods.getContents(in);
    } catch (IOException e) {
      throw INVALID_INCLUDE_PATH.getTracelessException(path, e);
    }
  }

  /*
   * Returns whether the template was created from a string (hence doesn't really
   * have a "physical" location).
   */
  boolean isString() {
    return path == null;
  }

  @Override
  public String toString() {
    return path;
  }

}
