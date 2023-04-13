package org.klojang.templates.x;

import org.klojang.templates.PathResolver;

import java.io.InputStream;
import java.util.Optional;

public final class ClassPathResolver implements PathResolver {

  private final Class<?> clazz;

  public ClassPathResolver(Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean isValidPath(String path) {
    return clazz.getResource(path) != null;
  }

  @Override
  public InputStream resolve(String path) {
    return clazz.getResourceAsStream(path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof ClassPathResolver cpr) {
      return clazz.getPackage() == cpr.clazz.getPackage();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return clazz.hashCode();
  }

}
