package org.klojang.templates;

/**
 * Defines a very general contract for name-based extraction of values from objects.
 * The extraction takes places based on the name of template variable. Just as
 * {@link Template} and {@link SoloSession} are the two central classes of Klojang
 * Templates, so are {@link Accessor} and {@link Stringifier} the two central
 * interfaces of Klojang Templates. See {@link AccessorRegistry} for more
 * information.
 *
 * @param <T> the type of the source data object
 * @author Ayco Holleman
 */
@FunctionalInterface
public interface Accessor<T> {

  /**
   * The value that <b>should</b> be returned by the
   * {@link #access(Object, String) access()} method if a template variable cannot be
   * mapped to an identifier in the source data. For example, if the template is
   * populated from a {@code HashMap}, then the template variable must correspond to
   * a map key (possibly indirectly, via a {@link NameMapper}). If the
   * {@code HashMap} does not contain that key, the {@code access()} must return
   * {@code UNDEFINED}. {@code Accessor} implementations <b>should not</b> throw an
   * exception and they <b>should not</b> return {@code null}. {@code null} is a
   * legitimate, "insertable" value (usually converted to an empty string). If an
   * {@code Accessor} returns {@code UNDEFINED} for a particular template variable,
   * the {@link SoloSession} will skip setting that variable. This allows you to
   * {@link SoloSession#insert(Object, String...) insert} another source data
   * object into the template that <i>does</i> have a value for the variable. (Note
   * that <i>once a template variable has been set, it cannot be set again</i> within
   * the same {@code RenderSession}.)
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
   * @throws RenderException if an error occurs while extracting the value
   */
  Object access(T data, String property) throws RenderException;

}
