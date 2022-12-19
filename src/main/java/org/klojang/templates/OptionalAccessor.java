package org.klojang.templates;

import java.util.Optional;

/**
 * An {@link Accessor} implementation that can be used to access {@link Optional} instances. If the
 * {@code Optional} is empty its {@code access} method returns {@link Accessor#UNDEFINED}. Otherwise
 * the {@code OptionalAccessor} will consult the {@link AccessorRegistry} that created it and
 * delegate to the {@code Accessor} for the object inside the {@code Optional}.
 *
 * @author Ayco Holleman
 * @param <T> The type of the object inside the {@code Optional}
 */
class OptionalAccessor<T> implements Accessor<Optional<T>> {

  private final AccessorRegistry af;
  private final Template tmpl;

  OptionalAccessor(AccessorRegistry af, Template tmpl) {
    this.af = af;
    this.tmpl = tmpl;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object access(Optional<T> data, String property) throws RenderException {
    if (data.isEmpty()) {
      return UNDEFINED;
    }
    T obj = data.get();
    return ((Accessor<T>) af.getAccessor(obj, tmpl)).access(obj, property);
  }
}
