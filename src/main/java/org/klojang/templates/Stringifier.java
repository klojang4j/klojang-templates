package org.klojang.templates;

import org.klojang.util.StringMethods;

/**
 * A {@code Stringifier} is responsible for stringifying the values provided by the
 * data access layer. Note that escaping (e.g. HTML) is also considered to be form of
 * stringifying, albeit from string to string.
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
   * {@link RenderException} is thrown if they do
   *
   * @param value the value to be stringified
   * @return a string representation of the value
   * @throws RenderException
   */
  String stringify(Object value) throws RenderException;

}
