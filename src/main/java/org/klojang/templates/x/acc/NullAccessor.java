package org.klojang.templates.x.acc;

import org.klojang.templates.Accessor;
import org.klojang.templates.RenderException;

/**
 * An {@link Accessor} implementation that always returns
 * {@link Accessor#UNDEFINED}.
 *
 * @author Ayco Holleman
 */
public final class NullAccessor implements Accessor<Object> {

  public NullAccessor() {}

  @Override
  public Object access(Object data, String property) throws RenderException {
    return UNDEFINED;
  }

}
