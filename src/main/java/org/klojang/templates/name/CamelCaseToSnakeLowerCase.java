package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.templates.NameMapper;

import static java.lang.Character.*;
import static org.klojang.check.CommonChecks.emptyString;
import static org.klojang.util.StringMethods.trim;

/**
 * Converts camel case identifiers to snake case identifiers. For example
 * {@code myBloodyValentine} becomes {@code my_bloody_valentine}.
 *
 * @author Ayco Holleman
 */
public class CamelCaseToSnakeLowerCase implements NameMapper {

  /**
   * Returns an instance of {@code CamelCaseToSnakeLowerCase}.
   *
   * @return an instance of {@code CamelCaseToSnakeLowerCase}
   */
  public static CamelCaseToSnakeLowerCase camelCaseToSnakeLowerCase() {
    return new CamelCaseToSnakeLowerCase();
  }

  /**
   * Maps a camel case name to an all-lowercase snake case name. Any leading and
   * trailing underscores in the name are ignored.
   *
   * @param name a camel case name
   * @return an all-lowercase snake case name
   */
  @Override
  public String map(String name) {
    String in = Check.that(trim(name, "_\t\r\n"))
        .isNot(emptyString(), "cannot map \"%s\"", name).ok();
    int maxLen = (int) Math.ceil(in.length() * 1.5F);
    char[] out = new char[maxLen];
    out[0] = toLowerCase(in.charAt(0));
    int j = 1;
    for (int i = 1; i < in.length(); ++i) {
      char c = in.charAt(i);
      if (isUpperCase(c)) {
        if ((i != (in.length() - 1)) && isLowerCase(in.charAt(i + 1))) {
          out[j++] = '_';
          out[j++] = toLowerCase(c);
        } else if (isLowerCase(in.charAt(i - 1))) {
          out[j++] = '_';
          out[j++] = toLowerCase(c);
        } else {
          out[j++] = toLowerCase(c);
        }
      } else {
        out[j++] = c;
      }
    }
    return new String(out, 0, j);
  }

}
