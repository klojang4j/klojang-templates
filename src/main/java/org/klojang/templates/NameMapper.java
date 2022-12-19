package org.klojang.templates;

/**
 * Generic name mapping interface. Name mappers are used to map template variable
 * names to model object properties. See
 * {@link AccessorRegistry.Builder#setDefaultNameMapper(NameMapper)}. They are also
 * used to map column names to model object properties. See
 * {@link SQLQuery#withMapper(NameMapper)}. Note that the term "property" is in fact
 * rather misleading because, for Klojang, model objects might just as well be
 * {@code Map<String,Object>} objects, in which case template variables would map to
 * map keys.
 *
 * @author Ayco Holleman
 */
@FunctionalInterface
public interface NameMapper {

  /**
   * The no-op mapper. Returns the name as-is.
   */
  public static NameMapper AS_IS = x -> x;

  /**
   * Maps the specified name to a name that can be used to access its value.
   *
   * @param template The template containing the variable or nested template
   * @param name The name of the variable or nested template
   * @return A (new) name that can be used to access the value
   */
  String map(String name);

}
