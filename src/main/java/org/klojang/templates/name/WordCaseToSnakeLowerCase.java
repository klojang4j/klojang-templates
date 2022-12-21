package org.klojang.templates.name;

import org.klojang.templates.NameMapper;

/**
 * Converts camel case identifiers to snake case identifiers. For example {@code MyBloodyValentine}
 * becomes {@code my_bloody_valentine}.
 *
 * @author Ayco Holleman
 */
public class WordCaseToSnakeLowerCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToSnakeLowerCase}.
   *
   * @return An instance of {@code WordCaseToSnakeLowerCase}
   */
  public static WordCaseToSnakeLowerCase wordCaseToSnakeLowerCase() {
    return new WordCaseToSnakeLowerCase();
  }

  @Override
  public String map(String name) {
    return new CamelCaseToSnakeLowerCase().map(name);
  }
}
