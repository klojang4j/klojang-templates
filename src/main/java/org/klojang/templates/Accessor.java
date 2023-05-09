package org.klojang.templates;

import java.util.function.IntFunction;

/**
 * Accessors are used to extract values from objects. The {@link RenderSession} uses
 * them to extract values from the objects passed to
 * {@link RenderSession#insert(Object, String...) insert()} and
 * {@link RenderSession#populate(String, Object, String...) populate()}. Object
 * access is name-based and requires a mapping between template variables and bean
 * properties or map keys. By default, <i>Klojang Templates</i> assumes an as-is
 * mapping between the two, but you can use {@linkplain NameMapper name mappers} for
 * more sophisticated mappings.
 *
 * @param <T> the type of the source data object
 * @author Ayco Holleman
 * @see AccessorRegistry
 */
@FunctionalInterface
public interface Accessor<T> {

    /**
     * <p>
     * The value that <b>should</b> be returned by accessors if a template variable
     * cannot be mapped to a value in the source data object. {@code Accessor}
     * implementations should not throw an exception and they should not return
     * {@code null} in this case.
     * </p>
     * <h4>Null vs. UNDEFINED</h4>
     * <p>
     * There is a subtle difference in how a {@link RenderSession} treats {@code null}
     * values versus how it treats {@code UNDEFINED}. {@code null} is a valid and
     * legitimate value for a template variable. If the bean property or map key
     * corresponding to the variable is {@code null}, the variable will be set to
     * whatever {@code null} is stringified to &#8212; the
     * {@linkplain Stringifier#DEFAULT default stringifier} stringifies {@code null} to an
     * empty string. If, on the other hand, the {@code RenderSession} receives
     * {@code UNDEFINED} as the value for a template variable, it will just skip
     * setting that variable. By itself this will make no difference when the template
     * is rendered. An unset variable will result in replacing the variable with
     * nothing &#8212; i.e. an empty string. However, it <i>does</i> make a difference
     * if you want to set all unset variables to some default value after you have
     * populated your template with model objects, hash maps, and/or anything else for
     * which you have defined an accessor:
     * </p>
     * <blockquote><pre>{@code
     * CompanyDao dao = new CompanyDao();
     * Template template = Template.fromResource(getClass(), "/views/companies.html");
     * RenderSession session = template.newRenderSession();
     * session.populate("companies", dao.list());
     * session.getAllUnsetVariables().forEach(var -> session.setPath(var, i -> "(unknown)");
     * }</pre></blockquote>
     * <p>
     * You can make the {@code RenderSession} treat {@code null} just like
     * {@code UNDEFINED}:
     * </p>
     * <blockquote><pre>{@code
     * AccessorRegistry accessors = AccessorRegistry.build()
     *    .nullEqualsUndefined(true);
     *    .freeze();
     * Template template = Template.fromResource(getClass(), "/views/companies.html");
     * RenderSession session = template.newRenderSession(accessors);
     * }</pre></blockquote>
     *
     * @see AccessorRegistry.Builder#nullEqualsUndefined(boolean)
     * @see RenderSession#setPath(String, IntFunction)
     */
    Object UNDEFINED = new Object();

    /**
     * Returns the value identified by the specified name from the specified source
     * data object. If the source data object is a {@code Map}, {@code name} would
     * likely be a map key; if it is a JavaBean, {@code name} would likely be a bean
     * property. However, it is up to individual {@code Accessor} implementation to
     * determine the type of objects they provide access to, and how names are to be
     * interpreted.
     *
     * @param data the data to be accessed
     * @param name the name by which to retrieve the desired value from the data
     * @return the value
     */
    Object access(T data, String name);

}
