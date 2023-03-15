package org.klojang.templates;

/**
 * Accessors are used to extract values from objects. A {@link RenderSession} uses
 * them to extract values from the objects passed to its
 * {@link RenderSession#insert(Object, String...) insert()} and
 * {@link RenderSession#populate(String, Object, String...) populate()} methods.
 * Object access is name-based and requires some sort of mapping between template
 * variables and named values (e.g. JavaBean properties or map keys). By default,
 * Klojang Templates assumes an as-is mapping between the two, but you can inject
 * {@linkplain NameMapper name mappers} for more sophisticated mappings.
 *
 * @param <T> the type of the source data object
 * @author Ayco Holleman
 * @see AccessorRegistry
 */
@FunctionalInterface
public interface Accessor<T> {

  /**
   * The value that <b>should</b> be returned by the
   * {@link #access(Object, String) access()} method if a template variable cannot be
   * mapped to a value in the source data object. {@code Accessor} implementations
   * should not throw an exception and they should not return {@code null} in this
   * case.
   */
  Object UNDEFINED = new Object();

  /**
   * Returns the value of the specified property within the specified model object.
   * The term "property" is somewhat misleading here, because the {@code data}
   * argument can be anything a specific {@code Accessor} implementation decides to
   * take care of. It could, for example, also be a {@code Map} and {@code property}
   * would then (most likely) specify a map key.
   *
   * @param data the data to be accessed
   * @param property the name by which to retrieve the desired value from the
   *     data
   * @return the value
   */
  Object access(T data, String property);

}
