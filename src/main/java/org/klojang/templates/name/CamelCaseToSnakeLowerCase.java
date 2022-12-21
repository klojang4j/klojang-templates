package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.*;
import static org.klojang.check.CommonChecks.emptyString;

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
   * @return An instance of {@code CamelCaseToSnakeLowerCase}
   */
  public static CamelCaseToSnakeLowerCase camelCaseToSnakeLowerCase() {
    return new CamelCaseToSnakeLowerCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    int maxLen = (int) Math.ceil(name.length() * 1.5F);
    char[] out = new char[maxLen];
    out[0] = isUpperCase(name.charAt(0)) ? toLowerCase(name.charAt(0)) : name.charAt(
        0);
    int j = 1;
    for (int i = 1; i < name.length(); ++i) {
      if (isUpperCase(name.charAt(i))) {
        if ((i != (name.length() - 1)) && isLowerCase(name.charAt(i + 1))) {
          out[j++] = '_';
          out[j++] = toLowerCase(name.charAt(i));
        } else if (isLowerCase(name.charAt(i - 1))) {
          out[j++] = '_';
          out[j++] = toLowerCase(name.charAt(i));
        } else {
          out[j++] = toLowerCase(name.charAt(i));
        }
      } else {
        out[j++] = name.charAt(i);
      }
    }
    return new String(out, 0, j);
  }

}
