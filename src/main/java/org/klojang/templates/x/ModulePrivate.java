package org.klojang.templates.x;

/*
 * This class exists solely because Java still has no module-private visibility.
 * Since this class lives in a non-accessible package we can use it to smuggle stuff
 * across package boundaries without the user being able to use the publicly
 * accessible methods that take or return a Cloak.
 */
public final class ModulePrivate<T> {

  public static <U> ModulePrivate<U> hide(U thing) {
    return new ModulePrivate<>(thing);
  }

  private final T thing;

  private ModulePrivate(T thing) {
    this.thing = thing;
  }

  public T get() {
    return thing;
  }

}
