package org.klojang.templates.x;

import org.klojang.check.Check;
import org.klojang.templates.PathResolutionException;
import org.klojang.templates.PathResolver;
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
  private final PathResolver pathResolver;
  private final Class<?> clazz;
  private final String path;

  public TemplateLocation(TemplateLocation parent) {
    this.type = STRING;
    this.pathResolver = parent.pathResolver;
    this.clazz = parent.clazz;
    this.path = null;
  }

  private TemplateLocation() {
    this.type = STRING;
    this.pathResolver = null;
    this.clazz = null;
    this.path = null;
  }

  public TemplateLocation(String path) {
    this(new File(path));
  }

  public TemplateLocation(File file) {
    this.path = file.getAbsolutePath();
    this.type = FILE;
    this.pathResolver = null;
    this.clazz = null;
  }

  public TemplateLocation(Class<?> clazz) {
    this.type = STRING;
    this.pathResolver = null;
    this.clazz = clazz;
    this.path = null;
  }

  public TemplateLocation(Class<?> clazz, String path) {
    this.clazz = clazz;
    this.path = path;
    this.type = RESOURCE;
    this.pathResolver = null;
  }

  public TemplateLocation(PathResolver pathResolver, String path) {
    this.pathResolver = pathResolver;
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
    try (InputStream in = pathResolver.resolvePath(path)) {
      Check.on(PathResolutionException::new, in).is(notNull(), path);
      return IOMethods.getContents(in);
    } catch (IOException e) {
      throw new PathResolutionException(path);
    }
  }

  public TemplateLocationType type() {
    return type;
  }

  public PathResolver pathResolver() {
    return pathResolver;
  }

  public Class<?> clazz() {
    return clazz;
  }

  public String path() {
    return path;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, path, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TemplateLocation other = (TemplateLocation) obj;
    return type == other.type
        && (type != RESOURCE || clazz.getPackage() == other.clazz.getPackage())
        && Objects.equals(path, other.path);
  }

  public String toString() {
    if (type == STRING) {
      return concat(
          "TemplateId[sourceType=",
          type,
          ";package=",
          clazz.getPackage().getName(),
          ";resolver=",
          pathResolver,
          "]");
    }
    if (type == RESOURCE) {
      return concat(
          "TemplateId[sourceType=",
          type,
          ";path=",
          path,
          ";package=",
          clazz.getPackage().getName(),
          "]");
    } else if (type == FILE) {
      return concat("TemplateId[sourceType=", type, ";path=", path, "]");
    }
    return concat(
        "TemplateId[sourceType=",
        type,
        ";path=",
        path,
        ";resolver=",
        pathResolver,
        "]");
  }

}
