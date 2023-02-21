package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toLowerCase;
import static org.klojang.check.CommonChecks.emptyString;

/**
 * Converts word case identifiers to came cal identifiers. For example
 * {@code MyBloodyValentine} becomes {@code myBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class WordCaseToCamelCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToCameCase}.
   *
   * @return an instance of {@code WordCaseToCameCase}
   */
  public static WordCaseToCamelCase wordCaseToCameCase() {
    return new WordCaseToCamelCase();
  }

  /**
   * Maps a word case name to a camel case name.
   *
   * @param name a word case name
   * @return a camel case name
   */
  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    return toLowerCase(name.charAt(0)) + name.substring(1);
  }

}
