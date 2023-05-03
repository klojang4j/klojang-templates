package org.klojang.templates;

import org.klojang.path.Path;
import org.klojang.path.PathWalker;
import org.klojang.path.PathWalkerException;

import static java.util.Arrays.asList;
import static org.klojang.templates.RenderErrorCode.ACCESS_EXCEPTION;

final class PathAccessor implements Accessor<Object> {

  private final NameMapper nm;

  PathAccessor(NameMapper nm) {
    this.nm = nm;
  }

  @Override
  public Object access(Object data, String name) {
    String path = nm == null ? name : nm.map(name);
    PathWalker pw = new PathWalker(asList(Path.from(path)), false);
    try {
      return pw.read(data);
    } catch (PathWalkerException e) {
      return switch (e.getErrorCode()) {
        case NO_SUCH_KEY, NO_SUCH_PROPERTY -> UNDEFINED;
        default -> throw ACCESS_EXCEPTION.getException(name, e.getMessage());
      };
    }
  }

}
