package org.klojang.templates;

import org.klojang.check.extra.Result;
import org.klojang.util.Path;
import org.klojang.path.PathWalker;
import org.klojang.path.DeadEndException;

import static java.util.Collections.singletonList;
//import static org.klojang.templates.RenderErrorCode.ACCESS_EXCEPTION;

final class PathAccessor implements Accessor<Object> {

  private final NameMapper nm;

  PathAccessor(NameMapper nm) {
    this.nm = nm;
  }

  @Override
  public Object access(Object data, String name) {
    String path = nm == null ? name : nm.map(name);
    PathWalker pw = new PathWalker(singletonList(Path.from(path)));
    try {
      Result<Object> result = pw.read(data);
      return result.orElse(UNDEFINED);
    } catch (DeadEndException e) {
      return UNDEFINED;
// Maybe we should re-introduce DeadEndException::getErrorCode())
//      return switch (e.getErrorCode()) {
//        case NO_SUCH_KEY, NO_SUCH_PROPERTY -> UNDEFINED;
//        default -> throw ACCESS_EXCEPTION.getException(name, e.getMessage());
//      };
    }
  }

}
