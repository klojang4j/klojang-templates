package org.klojang.templates.name;

import org.klojang.templates.NameMapper;

import static org.klojang.templates.name.CamelCaseToSnakeLowerCase.camelCaseToSnakeLowerCase;

/**
 * Converts word case identifiers to snake case identifiers. For example
 * {@code MyBloodyValentine} becomes {@code my_bloody_valentine}.
 *
 * @author Ayco Holleman
 */
public class WordCaseToSnakeLowerCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToSnakeLowerCase}.
   *
   * @return an instance of {@code WordCaseToSnakeLowerCase}
   */
  public static WordCaseToSnakeLowerCase wordCaseToSnakeLowerCase() {
    return new WordCaseToSnakeLowerCase();
  }

  /**
   * Maps a word case name to an all-lowercase snake case name. Any leading and
   * trailing underscores in the name are ignored.
   *
   * @param name a word case name
   * @return an all-lowercase snake case name
   */
  @Override
  public String map(String name) {
    return camelCaseToSnakeLowerCase().map(name);
  }

}
