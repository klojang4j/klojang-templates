package org.klojang.templates.name;

import org.klojang.templates.NameMapper;

import static org.klojang.templates.name.CamelCaseToSnakeUpperCase.camelCaseToSnakeUpperCase;

/**
 * Converts camel case identifiers to snake case identifiers. For example
 * {@code MyBloodyValentine} becomes {@code MY_BLOODY_VALENTINE}.
 *
 * @author Ayco Holleman
 */
public class WordCaseToSnakeUpperCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToSnakeUpperCase}.
   *
   * @return an instance of {@code WordCaseToSnakeUpperCase}
   */
  public static WordCaseToSnakeUpperCase wordCaseToSnakeUpperCase() {
    return new WordCaseToSnakeUpperCase();
  }

  /**
   * Maps a word case name to an all-uppercase snake case name. Any leading and
   * trailing underscores in the name are ignored.
   *
   * @param name a word case name
   * @return an all-uppercase snake case name
   */
  @Override
  public String map(String name) {
    return camelCaseToSnakeUpperCase().map(name);
  }

}
