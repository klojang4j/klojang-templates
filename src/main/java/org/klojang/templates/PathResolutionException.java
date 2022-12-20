package org.klojang.templates;

import static org.klojang.util.StringMethods.concat;

/**
 * Thrown if the path specified in an included template could not be resolved to a
 * loadable resource.
 *
 * @author Ayco Holleman
 */
public class PathResolutionException extends ParseException {

  public PathResolutionException(String path) {
    super(concat("invalid path: \"", path, '"'));
  }

}
