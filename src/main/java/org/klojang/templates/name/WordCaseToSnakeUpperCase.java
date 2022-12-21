package org.klojang.templates.name;

import org.klojang.templates.NameMapper;

/**
 * Converts camel case identifiers to snake case identifiers. For example {@code MyBloodyValentine}
 * becomes {@code MY_BLOODY_VALENTINE}.
 *
 * @author Ayco Holleman
 */
public class WordCaseToSnakeUpperCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToSnakeUpperCase}.
   *
   * @return An instance of {@code WordCaseToSnakeUpperCase}
   */
  public static WordCaseToSnakeUpperCase wordCaseToSnakeUpperCase() {
    return new WordCaseToSnakeUpperCase();
  }

  @Override
  public String map(String name) {
    return new CamelCaseToSnakeUpperCase().map(name);
  }
}
