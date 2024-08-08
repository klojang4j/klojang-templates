package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.klojang.check.CommonChecks.emptyString;
import static org.klojang.util.StringMethods.trim;

/**
 * Converts snake case identifiers to word case identifiers. For example
 * {@code my_bloody_valentine} becomes {@code MyBloodyValentine}. Note that it doesn't
 * matter whether the snake case identifier consists of lowercase letters, uppercase
 * letters or both. {@code MY_BLOODY_VALENTINE} and {@code My_Bloody_Valentine} would also
 * become {@code MyBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class SnakeCaseToWordCase implements NameMapper {

  /**
   * Maps a snake case name to a word case name. Any leading and trailing underscores in
   * the name are ignored.
   *
   * @param name a word case name
   * @return a camel case name
   */
  public static String mapName(String name) {
    String in = Check.that(trim(name, "_\t\r\n"))
          .isNot(emptyString(), "cannot map \"%s\"", name).ok();
    char[] out = new char[in.length()];
    out[0] = toUpperCase(in.charAt(0));
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

  /**
   * Returns an instance of {@code SnakeCaseToWordCase}.
   *
   * @return an instance of {@code SnakeCaseToWordCase}
   */
  public static SnakeCaseToWordCase snakeCaseToWordCase() {
    return new SnakeCaseToWordCase();
  }

  /**
   * Maps a snake case name to a word case name. Any leading and trailing underscores in
   * the name are ignored.
   *
   * @param name a word case name
   * @return a camel case name
   */
  @Override
  public String map(String name) {
    return mapName(name);
  }

}
