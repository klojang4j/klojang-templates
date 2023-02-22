package org.klojang.templates;

import org.klojang.util.StringMethods;

/**
 * Stringifies the values provided by the data access layer.
 *
 * @author Ayco Holleman
 * @see StringifierRegistry
 */
@FunctionalInterface
public interface Stringifier {

  /**
   * Stringifies {@code null} to an empty string and any other value by calling
   * {@code toString()} on it. It is the {@code Stringifier} that is used by default
   * for all template variables.
   *
   * @see StringifierRegistry.Builder
   */
  Stringifier DEFAULT = x -> x == null ? StringMethods.EMPTY_STRING : x.toString();

  /**
   * Stringifies the specified value. Stringifier implementations <i>must</i> be able
   * to handle null values and they <i>must never</i> return null. A
   * {@link BadStringifierException} is thrown if
   *
   * @param value The value to be stringified
   * @return A string representation of the value
   * @throws RenderException
   */
  String toString(Object value) throws RenderException;

}
