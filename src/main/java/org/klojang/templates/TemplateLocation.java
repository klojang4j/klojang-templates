package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.templates.x.TemplateLocationType;
import org.klojang.util.IOMethods;

import java.io.*;
import java.util.Objects;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.templates.x.TemplateLocationType.*;
import static org.klojang.util.StringMethods.concat;

public final class TemplateLocation {

  public static final TemplateLocation NONE = new TemplateLocation();

  private static final String ERR_NO_PATH = "Cannot load source for %s";

  private final TemplateLocationType type;
  private final PathResolver resolver;
  private final Class<?> clazz;
  private final String path;

  public TemplateLocation(TemplateLocation parent) {
    this.type = STRING;
    this.resolver = parent.resolver;
    this.clazz = parent.clazz;
    this.path = null;
  }

  private TemplateLocation() {
    this.type = STRING;
    this.resolver = null;
    this.clazz = null;
    this.path = null;
  }

  public TemplateLocation(String path) {
    this(new File(path));
  }

  public TemplateLocation(File file) {
    this.path = file.getAbsolutePath();
    this.type = FILE;
    this.resolver = null;
    this.clazz = null;
  }

  public TemplateLocation(Class<?> clazz) {
    this.type = STRING;
    this.resolver = null;
    this.clazz = clazz;
    this.path = null;
  }

  public TemplateLocation(Class<?> clazz, String path) {
    this.clazz = clazz;
    this.path = path;
    this.type = RESOURCE;
    this.resolver = null;
  }

  public TemplateLocation(PathResolver resolver, String path) {
    this.resolver = resolver;
    this.path = path;
    this.type = RESOLVER;
    this.clazz = null;
  }

  public String getSource() throws PathResolutionException {
    if (path == null) {
      return Check.fail(STATE, ERR_NO_PATH, this);
    } else if (type == FILE) {
      try (InputStream in = new FileInputStream(path)) {
        return IOMethods.getContents(in);
      } catch (FileNotFoundException e) {
        throw new PathResolutionException(path);
      } catch (IOException e) {
        throw new PathResolutionException(path);
      }
    } else if (type == RESOURCE) {
      try (InputStream in = clazz.getResourceAsStream(path)) {
        Check.on(PathResolutionException::new, in).is(notNull(), path);
        return IOMethods.getContents(in);
      } catch (IOException e) {
        throw new PathResolutionException(path);
      }
    }
    try (InputStream in = resolver.resolvePath(path)) {
      Check.on(PathResolutionException::new, in).is(notNull(), path);
      return IOMethods.getContents(in);
    } catch (IOException e) {
      throw new PathResolutionException(path);
    }
  }

  public TemplateLocationType type() {
    return type;
  }

  public PathResolver resolver() {
    return resolver;
  }

  public Class<?> clazz() {
    return clazz;
  }

  public String path() {
    return path;
  }

  boolean isString() {
    return type == STRING;
  }

  @Override
  public int hashCode() {
    if (type == RESOURCE) {
      return Objects.hash(type, path, clazz.getPackage());
    }
    return Objects.hash(type, path, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof TemplateLocation other) {
      return type == other.type && path.equals(other.path) &&
          switch (type) {
            case RESOURCE -> clazz.getPackage() == other.clazz.getPackage();
            case RESOLVER -> resolver.equals(other.path);
            case FILE -> true;
            case STRING -> false;
          };
    }
    return false;
  }

  public String toString() {
    if (type == STRING) {
      return concat(
          getClass().getSimpleName(),
          "[sourceType=",
          type,
          ";package=",
          clazz.getPackage().getName(),
          ";resolver=",
          resolver,
          "]");
    }
    if (type == RESOURCE) {
      return concat(
          getClass().getSimpleName(),
          "[sourceType=",
          type,
          ";path=",
          path,
          ";package=",
          clazz.getPackage().getName(),
          "]");
    } else if (type == FILE) {
      return concat(getClass().getSimpleName(),
          "[sourceType=",
          type,
          ";path=",
          path,
          "]");
    }
    return concat(
        getClass().getSimpleName(),
        "[sourceType=",
        type,
        ";path=",
        path,
        ";resolver=",
        resolver,
        "]");
  }

}
