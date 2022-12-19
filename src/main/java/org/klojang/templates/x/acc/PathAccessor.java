package org.klojang.templates.x.acc;

import org.klojang.path.Path;
import org.klojang.path.PathWalker;
import org.klojang.path.PathWalkerException;
import org.klojang.templates.Accessor;
import org.klojang.templates.NameMapper;
import org.klojang.templates.RenderException;

import static java.util.Arrays.asList;

public final class PathAccessor implements Accessor<Object> {

  private final NameMapper nm;

  public PathAccessor(NameMapper nm) {
    this.nm = nm;
  }

  @Override
  public Object access(Object data, String property) throws RenderException {
    String path = nm == null ? property : nm.map(property);
    PathWalker pw = new PathWalker(asList(Path.from(path)), false);
    try {
      return pw.read(data);
    } catch (PathWalkerException e) {
      return switch (e.getErrorCode()) {
        case NO_SUCH_KEY, NO_SUCH_PROPERTY -> UNDEFINED;
        default -> new RenderException(e.getMessage());
      };
    }
  }

}
