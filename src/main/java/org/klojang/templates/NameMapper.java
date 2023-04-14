package org.klojang.templates;

/**
 * Generic name mapping interface. Name mappers can optionally be used to map the
 * names used in a Klojang template to the names used in the data access layer, thus
 * allowing for an extra level of indirection between the view layer and the data
 * layer. The {@link org.klojang.templates.name} package contains various prefab name
 * mappers, for example to map snake case names to camel case names.
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
   * Maps the specified name to another name.
   *
   * @param name the name used in the source system
   * @return the name used in the target system
   */
  String map(String name);

}
