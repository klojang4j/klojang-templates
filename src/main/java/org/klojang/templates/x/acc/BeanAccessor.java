package org.klojang.templates.x.acc;

import org.klojang.invoke.BeanReader;
import org.klojang.templates.Accessor;
import org.klojang.templates.NameMapper;
import org.klojang.templates.RenderException;

public final class BeanAccessor<T> implements Accessor<T> {

  private final BeanReader<T> br;
  private final NameMapper nm;

  public BeanAccessor(Class<T> beanClass, NameMapper nm) {
    this.br = new BeanReader<>(beanClass);
    this.nm = nm;
  }

  @Override
  public Object access(T data, String name) throws RenderException {
    String prop = nm == null ? name : nm.map(name);
    if (br.getReadableProperties().contains(prop)) {
      return br.read(data, prop);
    }
    return UNDEFINED;
  }

}
