package org.klojang.templates;

import org.klojang.util.StringMethods;

/**
 * A stringifier is responsible for stringifying the values that are inserted into a
 * template. There is no limitation on the types of values that can be inserted into
 * a template, but they must ultimately be interwoven as strings with the boilerplate
 * text of the template. By default, all values except {@code null} are stringified
 * by calling {@code toString()} on them &#8212; {@code null} is stringified to an
 * empty string. However, this can be customized in multiple ways using a
 * {@link StringifierRegistry}. Note that escaping (e.g. HTML) and formatting (e.g.
 * dates) are also considered to be forms of stringification.
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
