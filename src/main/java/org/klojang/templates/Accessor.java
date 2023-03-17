package org.klojang.templates;

/**
 * Accessors are used to extract values from objects. The {@link RenderSession} uses
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
   * The value that <b>should</b> be returned by accessors if a template variable
   * cannot be mapped to a value in the source data object. {@code Accessor}
   * implementations should not throw an exception and they should not return
   * {@code null} in this case.
   */
  Object UNDEFINED = new Object();

  /**
   * Returns the value identified by the specified name from the specified source
   * data object. If the source data object is a {@code Map}, {@code name} would
   * likely be a map key; if it is a JavaBean, {@code name} would likely be a bean
   * property. However, it is up to individual {@code Accessor} implementation
   * determine the type of objects they provide access to, and how names are to be
   * interpreted.
   *
   * @param data the data to be accessed
   * @param name the name by which to retrieve the desired value from the data
   * @return the value
   */
  Object access(T data, String name);

}
