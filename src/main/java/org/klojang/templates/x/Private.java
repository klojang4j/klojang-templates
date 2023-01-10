package org.klojang.templates.x;

/*
 * This class exists solely to mimic module-private accessibility. Since this class
 * lives in a non-accessible package we can use it to smuggle stuff across package
 * boundaries without the user being able to call publicly accessible methods that
 * take or return a Private.
 */
public final class Private<T> {

  public static <U> Private<U> of(U thing) {
    return new Private<>(thing);
  }

  private final T thing;

  private Private(T thing) {
    this.thing = thing;
  }

  public T get() {
    return thing;
  }

}
