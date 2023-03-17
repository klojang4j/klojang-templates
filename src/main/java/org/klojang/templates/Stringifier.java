package org.klojang.templates;

import org.klojang.util.StringMethods;

/**
 * A {@code Stringifier} is responsible for stringifying the values coming back from
 * the data access layer. Since these can, in principle, have any type, the
 * {@link RenderSession} needs to know how to stringy them, so they can be injected
 * into the template. This can be configured using a {@link StringifierRegistry}.
 * Note that escaping (e.g. HTML) and formatting (e.g. dates) are also considered to
 * be forms of stringification.
 *
 * @author Ayco Holleman
 * @see StringifierRegistry
 * @see VarGroup
 */
@FunctionalInterface
public interface Stringifier {

  /**
   * A stringifier that stringifies {@code null} to an empty string and any other
   * value by calling {@code toString()} on it. This is the default stringifier for
   * values coming back from the data access layer, unless you
   * {@linkplain StringifierRegistry.Builder#setDefaultStringifier(Stringifier)
   * specify an alternative default stringifier}.
   *
   * @see StringifierRegistry.Builder
   */
  Stringifier DEFAULT = x -> x == null ? StringMethods.EMPTY_STRING : x.toString();

  /**
   * Stringifies the specified value. Stringifier implementations <i>must</i> be able
   * to handle null values and they <i>must never</i> return null. A
   * {@link RenderException} is thrown if either requirement is not met.
   *
   * @param value the value to be stringified
   * @return a string representation of the value
   */
  String stringify(Object value);

}
