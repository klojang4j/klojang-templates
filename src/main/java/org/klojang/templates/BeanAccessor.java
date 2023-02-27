package org.klojang.templates;

import org.klojang.invoke.BeanReader;

final class BeanAccessor<T> implements Accessor<T> {

  private final BeanReader<T> br;
  private final NameMapper nm;

  BeanAccessor(BeanReader<T> beanReader, NameMapper nm) {
    this.br = beanReader;
    this.nm = nm;
  }

  @Override
  public Object access(T data, String name) throws RenderException {
    String prop = nm == null ? name : nm.map(name);
    if (br.canRead(prop)) {
      return br.read(data, prop);
    }
    return UNDEFINED;
  }

}
