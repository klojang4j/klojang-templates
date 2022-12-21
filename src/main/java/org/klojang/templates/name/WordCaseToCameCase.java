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
public class WordCaseToCameCase implements NameMapper {

  /**
   * Returns an instance of {@code WordCaseToCameCase}.
   *
   * @return An instance of {@code WordCaseToCameCase}
   */
  public static WordCaseToCameCase wordCaseToCameCase() {
    return new WordCaseToCameCase();
  }

  @Override
  public String map(String name) {
    Check.that(name, Tag.NAME).isNot(emptyString());
    return toLowerCase(name.charAt(0)) + name.substring(1);
  }

}
