package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.*;
import static org.klojang.check.CommonChecks.emptyString;

/**
 * Converts camel case identifiers to snake case identifiers. For example
 * {@code myBloodyValentine} becomes {@code MY_BLOODY_VALENTINE}.
 *
 * @author Ayco Holleman
 */
public class CamelCaseToSnakeUpperCase implements NameMapper {

  /**
   * Returns an instance of {@code CamelCaseToSnakeUpperCase}.
   *
   * @return An instance of {@code CamelCaseToSnakeUpperCase}
   */
  public static CamelCaseToSnakeUpperCase camelCaseToSnakeUpperCase() {
    return new CamelCaseToSnakeUpperCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    int maxLen = (int) Math.ceil(name.length() * 1.5F);
    char[] out = new char[maxLen];
    out[0] = toUpperCase(name.charAt(0));
    int j = 1;
    for (int i = 1; i < name.length(); ++i) {
      if (isUpperCase(name.charAt(i))) {
        if ((i != (name.length() - 1)) && isLowerCase(name.charAt(i + 1))) {
          out[j++] = '_';
          out[j++] = name.charAt(i);
        } else if (isLowerCase(name.charAt(i - 1))) {
          out[j++] = '_';
          out[j++] = toUpperCase(name.charAt(i));
        } else {
          out[j++] = toUpperCase(name.charAt(i));
        }
      } else {
        out[j++] = toUpperCase(name.charAt(i));
      }
    }
    return new String(out, 0, j);
  }

}
