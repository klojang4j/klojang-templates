package org.klojang.templates.name;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.NameMapper;

import static java.lang.Character.toUpperCase;
import static org.klojang.check.CommonChecks.emptyString;

/**
 * Converts camel case identifiers to word case identifiers. For example
 * {@code myBloodyValentine} becomes {@code MyBloodyValentine}.
 *
 * @author Ayco Holleman
 */
public class CamelCaseToWordCase implements NameMapper {

  /**
   * Returns an instance of {@code CamelCaseToWordCase}.
   *
   * @return An instance of {@code CamelCaseToWordCase}
   */
  public static CamelCaseToWordCase camelCaseToWordCase() {
    return new CamelCaseToWordCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    return toUpperCase(name.charAt(0)) + name.substring(1);
  }

}
