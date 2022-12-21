package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.klojang.check.CommonChecks.empty;
import static org.klojang.check.CommonChecks.emptyString;
import static org.klojang.util.StringMethods.trim;

/**
 * Converts snake case identifiers to word case identifiers. For example
 * {@code my_bloody_valentine} becomes {@code myBloodyValentine}. Note that it
 * doesn't matter whether the snake case identifier consists of lowercase letters,
 * uppercase letters or both. {@code MY_BLOODY_VALENTINE} and
 * {@code My_Bloody_Valentine} would also become {@code myBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class SnakeCaseToCamelCase implements NameMapper {

  /**
   * Returns an instance of {@code SnakeCaseToCamelCase}.
   *
   * @return An instance of {@code SnakeCaseToCamelCase}
   */
  public static SnakeCaseToCamelCase snakeCaseToCamelCase() {
    return new SnakeCaseToCamelCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    String in = trim(name, " _\t\r\n");
    Check.that(in).isNot(empty(), "Cannot map \"%s\"", name);
    char[] out = new char[in.length()];
    out[0] = toLowerCase(in.charAt(0));
    boolean nextWord = false;
    int j = 1;
    for (int i = 1; i < in.length(); ++i) {
      char c = in.charAt(i);
      if (c == '_') {
        nextWord = true;
      } else {
        out[j++] = nextWord ? toUpperCase(c) : toLowerCase(c);
        nextWord = false;
      }
    }
    return new String(out, 0, j);
  }

}
