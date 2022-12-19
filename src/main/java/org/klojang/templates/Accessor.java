package org.klojang.templates;

/**
 * Interface for objects implementing a mechanism to access source data for templates. See {@link
 * AccessorRegistry} for more information.
 *
 * @author Ayco Holleman
 */
@FunctionalInterface
public interface Accessor<T> {

  /**
   * The value that <i>must</i> be returned if a name used in a template does not identify a value
   * in a source data object. The {@code Accessor} should not (accidentally) throw an exception and
   * it should not return {@code null} in this case. {@code null} is considered to be a legitimate,
   * "insertable" value. If a {@link RenderSession} requests the {@code Accessor} to provide a value
   * for some template variable and it receives a {@code null} value, it will simply insert it (or
   * rather its {@link StringifierRegistry stringification}) into the template. The variable can no
   * longer be set after that, since cannot overwrite variable values within one and the same {@code
   * RenderSession}. If, on the other hand, the {@code RenderSession} receives {@code UNDEFINED}, it
   * will skip setting that variable, leaving you the option to {@link RenderSession#insert(Object,
   * String...) insert} another source data object into the template that <i>does</i> have a value
   * for the variable.
   */
  public static final Object UNDEFINED = new Object();

  /**
   * Returns the value of the specified property within the specified model object. The term
   * "property" is somewhat misleading here, because the {@code data} argument can be anything a
   * specific {@code Accessor} implementation decides to take care of. It could, for example, also
   * be a {@code Map} and {@code property} would then (most likely) specify a map key.
   *
   * @param data The data to be accessed
   * @param property The name by which to retrieve the desired value from the data
   * @return The value
   * @throws RenderException
   */
  Object access(T data, String property) throws RenderException;
}
