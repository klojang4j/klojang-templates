package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toUpperCase;
import static org.klojang.check.CommonChecks.emptyString;

/**
 * Converts camel case identifiers to word case (a&#46;k&#46;a&#46; pascal case)
 * identifiers. For example {@code myBloodyValentine} becomes {@code MyBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class CamelCaseToWordCase implements NameMapper {

  /**
   * Maps a camel case name to a word case name.
   *
   * @param name a word case name
   * @return a camel case name
   */
  public static String mapName(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    return toUpperCase(name.charAt(0)) + name.substring(1);
  }

  /**
   * Returns an instance of {@code CamelCaseToWordCase}.
   *
   * @return an instance of {@code CamelCaseToWordCase}
   */
  public static CamelCaseToWordCase camelCaseToWordCase() {
    return new CamelCaseToWordCase();
  }

  /**
   * Maps a camel case name to a word case name.
   *
   * @param name a word case name
   * @return a camel case name
   */
  @Override
  public String map(String name) {
    return mapName(name);
  }

}
