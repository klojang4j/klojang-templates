package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.klojang.check.CommonChecks.emptyString;

/**
 * Converts snake case identifiers to word case identifiers. For example
 * {@code my_bloody_valentine} becomes {@code MyBloodyValentine}. Note that it
 * doesn't matter whether the snake case identifier consists of lowercase letters,
 * uppercase letters or both. {@code MY_BLOODY_VALENTINE} and
 * {@code My_Bloody_Valentine} would also become {@code MyBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class SnakeCaseToWordCase implements NameMapper {

  /**
   * Returns an instance of {@code SnakeCaseToWordCase}.
   *
   * @return An instance of {@code SnakeCaseToWordCase}
   */
  public static SnakeCaseToWordCase snakeCaseToWordCase() {
    return new SnakeCaseToWordCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    char[] out = new char[name.length()];
    out[0] = toUpperCase(name.charAt(0));
    boolean nextWord = false;
    int j = 1;
    for (int i = 1; i < name.length(); ++i) {
      char c = name.charAt(i);
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
