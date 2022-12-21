package org.klojang.templates;

/**
 * Generic name mapping interface. Name mappers are used to map template variable
 * names to the names used in the data model, thus allowing for an extra level of
 * indirection between the view layer and the data layer. Note that in <i>Klojang
 * Templates</i> model objects might just as well be {@code Map<String, Object>}
 * pseudo-objects, so a {@code NameMapper} is agnostic as to whether the names map to
 * bean properties or map keys.
 *
 * @author Ayco Holleman
 * @see AccessorRegistry.Builder#setDefaultNameMapper(NameMapper)
 */
@FunctionalInterface
public interface NameMapper {

  /**
   * The no-op name mapper. Returns the input string as-is.
   */
  NameMapper AS_IS = x -> x;

  /**
   * Maps the specified name to a name that can be used to access its value.
   *
   * @param name The name of the variable or nested template
   * @return A (new) name that can be used to access the value
   */
  String map(String name);

}
