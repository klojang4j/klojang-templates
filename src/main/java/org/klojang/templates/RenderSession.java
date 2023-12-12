package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.invoke.BeanReader;

import java.io.OutputStream;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A {@code RenderSession} lets you populate a template and then render it. You obtain a
 * {@code RenderSession} for a template by calling {@link Template#newRenderSession()}.
 * Render sessions are meant to be throw-away objects. They should not typically survive
 * the method in which they are created. A {@code RenderSession} is cheap to instantiate,
 * but can gain a lot of state as the template gets populated. Therefore, make sure they
 * don't stick around hogging the heap.
 *
 * <h2>Thead Safety</h2>
 *
 * <p>The {@code RenderSession} class is not thread-safe. However, if different
 * threads populate different parts of the template, they cannot get in each other's way.
 * Of course, when using multiple threads to populate a template, the moment at which to
 * {@linkplain #render(OutputStream) render} it needs to be carefully synchronized.
 *
 * @author Ayco Holleman
 * @see Template#newRenderSession()
 */
public sealed interface RenderSession permits SoloSession, MultiSession {

  /**
   * Sets the specified variable to the specified value.
   *
   * @param varName the name of the variable to set
   * @param value the value
   * @return this {@code RenderSession}
   */
  default RenderSession set(String varName, Object value) {
    return set(varName, value, null);
  }

  /**
   * Sets the specified variable to the specified value. The {@link Stringifier}
   * associated with the specified {@link VarGroup variable group} will be used to
   * stringify and/or escape the value. If the variable has an inline group name prefix
   * (as in {@code ~%html:firstName%}), the variable group specified through the prefix
   * will prevail.
   *
   * @param varName the name of the variable to set
   * @param value the value
   * @param varGroup the variable group to assign the variable to if the variable has no
   * group name prefix. May be {@code null}.
   * @return this {@code RenderSession}
   * @see StringifierRegistry.Builder#forVarGroup(String, Stringifier)
   */
  RenderSession set(String varName, Object value, VarGroup varGroup);

  /**
   * Sets the specified variable to the value produced by the specified {@code Supplier}.
   * The supplier's {@code get()} method will be called each time the template is actually
   * {@linkplain #render(OutputStream) rendered}. This may be useful if you plan to render
   * the template multiple times using the same {@code RenderSession}.
   *
   * @param varName the name of the variable to set
   * @param valueGenerator the supplier of the value
   * @return this {@code RenderSession}
   */
  default RenderSession setDelayed(String varName, Supplier<Object> valueGenerator) {
    return setDelayed(varName, null, valueGenerator);
  }

  /**
   * Sets the specified variable to the value produced by the specified {@code Supplier}.
   * The supplier's {@code get()} method will be called eah time the template is actually
   * {@linkplain #render(OutputStream) rendered}. This may be useful if you plan to render
   * the template multiple times using the same {@code RenderSession}.
   *
   * @param varName the name of the variable to set
   * @param varGroup the variable group to assign the variable to if the variable has no
   * group name prefix.
   * @param valueGenerator the supplier of the value
   * @return this {@code RenderSession}
   */
  RenderSession setDelayed(
        String varName,
        VarGroup varGroup,
        Supplier<Object> valueGenerator);

  /**
   * Sets the value of the specified variable. The variable may be (deeply) nested and is
   * specified using its
   * {@linkplain TemplateUtils#getFQN(Template, String) fully-qualified  name}. If the
   * variable is a top-level variable (the path consists of just one path segment), this
   * method behaves just like {@link #set(String, Object)}.
   *
   * @param path a path to a potentially deeply-nested variable
   * @param valueGenerator a function which is given the array index of the template
   * instance for which to produce a value
   * @return this {@code RenderSession}
   * @see #setPath(String, VarGroup, boolean, IntFunction)
   */
  default RenderSession setPath(String path, IntFunction<Object> valueGenerator) {
    return setPath(path, null, true, valueGenerator);
  }

  /**
   * <p>Sets the value of the specified variable. The variable may be (deeply)
   * nested. For example:
   *
   * <blockquote><pre>{@code
   * setPath("companies.departments.employees.firstName", idx -> "John");
   * }</pre></blockquote>
   *
   * <p>This sets the {@code ~%firstName%} variable within the {@code employees} template
   * within the {@code departments} template within the {@code companies} template within
   * the main template to "John". Since the {@code employees} template may already have
   * been instantiated as a repeating template, the value for each instance of the
   * template is set via an {@link IntFunction}. The function is given the array index of
   * the instance and must return the value for that particular instance. For example, if
   * the above statement would populate a cell in an HTML table, which had already been
   * instantiated through other calls on the {@code RenderSession}, then this call:
   *
   * <blockquote><pre>{@code
   * setPath("companies.departments.employees.firstName", idx -> "John" + idx);
   * }</pre></blockquote>
   *
   * would put "John0" in the first row of the table, "John1" in the second row of the
   * table, etc.
   *
   * <p>If {@code force} equals {@code false}, then the variable will only be set if the
   * template containing it has already been made visible, for example via
   * {@link #repeat(String, int) repeat()} or
   * {@link #populate(String, Object) populate()}. If {@code force} equals {@code true},
   * {@code setPath()} will itself cause the template to become visible (boilerplate text
   * and all) and the variable will always be set.
   *
   * <p>If the variable is a top-level variable (the path consists of just one path
   * segment), this method behaves like {@link #set(String, Object, VarGroup)}. However,
   * you might still be able to put the {@code IntFunction} to good use:
   *
   * <blockquote><pre>{@code
   * String src = """
   *    ~%%begin:departments%
   *    <tr><td>Department number:</td><td>~%deptNo%</td></tr>
   *    ~%%end:departments%
   *    """;
   * Template template = Template.fromString(src);
   * RenderSession session = template.newRenderSession();
   * session.repeat("departments", 3).setPath("deptNo", i -> 100 + i).render();
   *
   * // Output:
   * // <tr><td>Department number:</td><td>100</td></tr>
   * // <tr><td>Department number:</td><td>101</td></tr>
   * // <tr><td>Department number:</td><td>102</td></tr>
   * }</pre></blockquote>
   *
   * @param path a path to a potentially deeply-nested variable
   * @param varGroup the variable group to assign the variable to if the variable has no
   * group name prefix.
   * @param force whether to set the variable even if the containing template has not been
   * made visible yet via other means
   * @param valueGenerator a function which is given the array index of the template
   * instance for which to produce a value
   * @return this {@code RenderSession}
   * @see TemplateUtils#getFQN(Template, String)
   * @see org.klojang.path.Path
   */
  RenderSession setPath(
        String path,
        VarGroup varGroup,
        boolean force,
        IntFunction<Object> valueGenerator);

  /**
   * Sets the specified variable to the value produced by the specified
   * {@code IntFunction} <i>if</i> the variable has not already been set.
   *
   * @param path a path to a potentially deeply-nested variable
   * @param valueGenerator the supplier of the value
   * @return this {@code RenderSession}
   * @see #ifNotSet(String, VarGroup, IntFunction)
   */
  default RenderSession ifNotSet(String path, IntFunction<Object> valueGenerator) {
    return ifNotSet(path, null, valueGenerator);
  }

  /**
   * Sets the specified variable to the value produced by the specified
   * {@code IntFunction} <i>if</i> the variable has not already been set. The variable may
   * be (deeply) nested and is therefore specified as a path (like
   * {@code companies.departments.employees.firstName}). This method is especially meant
   * to be called after a call to {@link #insert(Object) insert()} or
   * {@link #populate(String, Object) populate()}, to set any variables for which the
   * source data object passed to these methods did not provide a value.
   *
   * @param path a path to a potentially deeply-nested variable
   * @param varGroup the variable group to assign the variable to if the variable has no
   * group name prefix.
   * @param valueGenerator the supplier of the value
   * @return this {@code RenderSession}
   * @see #getAllUnsetVariables(boolean)
   * @see Accessor#UNDEFINED
   * @see AccessorRegistry.Builder#nullEqualsUndefined(boolean)
   */
  RenderSession ifNotSet(
        String path,
        VarGroup varGroup,
        IntFunction<Object> valueGenerator);

  /**
   * Sets the specified variable to the concatenation of all readable properties of the
   * provided JavaBean. This could be used to print out simple beans and records without
   * making use of nested templates.
   *
   * @param varName the name of the variable to set
   * @param bean the bean whose values to read
   * @param delimiter the delimiter to insert between the values. May be {@code null} or
   * empty.
   * @param prefix a string to insert before the concatenated values. May be {@code null}
   * or empty.
   * @param suffix a string to insert after the concatenated values. May be {@code null}
   * or empty.
   * @param properties the properties to read. An empty array causes all properties to be
   * read and printed. A non-empty array causes the properties to be printed in the
   * specified order.
   * @param <T>
   * @return this {@code RenderSession}
   */
  @SuppressWarnings("unchecked")
  default <T> RenderSession scatter(String varName,
        T bean,
        String delimiter,
        String prefix,
        String suffix,
        String... properties) {
    Class<T> c = (Class<T>) Check.notNull(bean, "bean").ok(Object::getClass);
    BeanReader<T> reader = new BeanReader<>(c, properties);
    return scatter(varName, bean, reader, delimiter, prefix, suffix);
  }

  /**
   * Sets the specified variable to the concatenation of all readable properties of the
   * provided JavaBean. This could be used to print out simple beans and records without
   * making use of nested templates.
   *
   * @param varName the name of the variable to set
   * @param bean the bean whose values to read
   * @param beanReader the {@code BeanReader} to be used to read the bean
   * @param delimiter the delimiter to insert between the values. May be {@code null} or
   * empty.
   * @param prefix a string to insert before the concatenated values. May be {@code null}
   * or empty.
   * @param suffix a string to insert after the concatenated values. May be {@code null}
   * or empty.
   * @param <T> the type of the bean
   * @return this {@code RenderSession}
   * @see BeanReader
   */
  <T> RenderSession scatter(String varName,
        T bean,
        BeanReader<T> beanReader,
        String delimiter,
        String prefix,
        String suffix);

  /**
   * <p>Populates the template with values extracted from the specified object. If the
   * structure of the object reflects the structure of the template, the template almost
   * literally becomes a "mold" into which to "sink" the object. On the other hand: the
   * object is not
   * <i>required</i> to exactly match the structure of the template. The
   * {@link Accessor accessors} will grab from it what they can from the object and leave
   * the rest alone.
   *
   * <p>Only template variables and nested templates whose name is in the provided
   * {@code names} array will be populated. The {@code names} array is allowed to be
   * {@code null} or empty, in which case an attempt is made to populate the entire
   * template from the source data object.
   *
   * @param data an object that provides data for all or some of the template variables
   * and nested templates
   * @return this {@code RenderSession}
   */
  default RenderSession insert(Object data) {
    return insert(data, null, null);
  }

  /**
   * Populates the template with values from the specified source data object. Only
   * template variables and nested templates whose name is in the provided {@code names}
   * array will be populated. The {@code names} array is allowed to be {@code null} or
   * empty, in which case an attempt is made to populate the entire template from the
   * source data object.
   *
   * @param data an object that provides data for all or some of the template variables
   * and nested templates
   * @param varGroup the variable group to assign the template variables to if they have
   * no inline group name prefix. May be {@code null}.
   * @param names the names of the variables and nested templates that must be populated.
   * May be {@code null} or empty, in which case all variables and nested templates will
   * be checked to see if they can be populated from the specified source data object
   * @return this {@code RenderSession}
   */
  RenderSession insert(Object data, VarGroup varGroup, List<String> names);

  /**
   * Populates a template nested inside the template managed by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data.
   *
   * @param nestedTemplateName the name of the nested template
   * @param data an object that provides data for all or some of the nested template's
   * variables and nested templates
   * @return this {@code RenderSession}
   */
  default RenderSession populate(String nestedTemplateName, Object data) {
    return populate(nestedTemplateName, data, null, null, null);
  }

  /**
   * Populates a template nested inside the template managed by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data.
   *
   * @param nestedTemplateName the name of the nested template
   * @param data an object that provides data for all or some of the nested template's
   * variables and nested templates
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything) if
   * {@code data} is not a {@code Collection} or an array, or if it is any array or
   * {@code Collection} containing less than two elements
   * @return this {@code RenderSession}
   */
  default RenderSession populate(
        String nestedTemplateName,
        Object data,
        String separator) {
    return populate(nestedTemplateName, data, separator, null, null);
  }

  /**
   * Populates a template nested inside the template being managed by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data. Only variables and (doubly) nested templates whose names are
   * present in the {@code names} argument will be populated. Values will be stringified
   * using the {@link Stringifier} associated with the specified {@link VarGroup}.
   *
   * <h4>Repeating Templates</h4>
   *
   * <p>If the specified object is an array or a {@code Collection}, the template
   * will be repeated for each element in the array or {@code Collection}. This can be
   * used, for example, to generate an HTML table from a nested template that contains
   * just a single {@code <tr>} element.
   *
   * <h4>Conditional Rendering</h4>
   *
   * <p>By default, nested templates are not rendered. It takes an explicit call to
   * {@link #repeat(String, int) repeat()} or {@code populate()} to force the template to
   * become visible. However, you can make it more explicit that you
   * <i>do not</i> want the template to become visible. If you pass an empty array
   * or collection to the {@code populate()} method, the template is going to be repeated
   * zero times. In other words it will remain invisible. This can be useful in
   * combination with a subsequent call to {@link #enable(int, String...) enable()} or
   * {@link #enableRecursive(String...) enableRecursive()}.
   *
   * <h4>Optionals</h4>
   *
   * <p>It is valid and legitimate to populate a nested template (or the main
   * template for that matter) with an {@code Optional}. {@code Optional} objects are
   * typically returned from the ubiquitous find-by-id method of a data access object
   * (DAO). If the {@code Optional} is empty, the nested template is explicitly disabled,
   * as though by a call to {@link #repeat(String, int) repeat(nestedTemplateName, 0)}.
   * Otherwise the template is populated with the contents of the {@code Optional}.
   *
   * @param nestedTemplateName the name of the nested template
   * @param data an object that provides data for all or some of the nested template's
   * variables and nested templates. If the object is an array or {@code Collection}, the
   * template will be rendered multiple times, once for each element in the array or
   * {@code Collection}.
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything) if
   * {@code data} is not a {@code Collection} or an array, or if it is any array or
   * {@code Collection} containing less than two elements
   * @param varGroup the variable group to assign the variables to if they have no group
   * name prefix. May be {@code null}.
   * @param names the names of the variables and doubly-nested templates that you want to
   * be populated using the specified data object. May be {@code null} or empty, in which
   * case all variables and nested templates will be checked to see if they can be
   * populated from the specified source data object
   * @return this {@code RenderSession}
   */
  RenderSession populate(String nestedTemplateName,
        Object data,
        String separator,
        VarGroup varGroup,
        List<String> names);

  /**
   * Causes the specified nested template to become visible and to be repeated the
   * specified number of times.
   *
   * @param nestedTemplateName the name of the nested template
   * @param times the number of times the template will repeat itself
   * @return a {@code RenderSession} that works on all instances (repetitions) of the
   * nested template
   * @see #repeat(String, String, int)
   * @see #in(String)
   * @see #getChildSessions(String)
   */
  default RenderSession repeat(String nestedTemplateName, int times) {
    return repeat(nestedTemplateName, null, times);
  }

  /**
   * Causes the specified nested template to become visible and to be repeated the
   * specified number of times. If {@code times} equals zero, the template is explicitly
   * disabled (in other words: <i>not</i> rendered). Usually it is not necessary to
   * disable a nested template, because nested templates are anyhow only rendered once you
   * populate them. However, it can be useful in combination with a subsequent call to
   * {@link #enable(String...) enable()} and
   * {@link #enableRecursive(String...) enableRecursive()}.
   *
   * <p><b>Contrary to most of the other methods, this method does not return
   * <i>this</i> {@code RenderSession}. It is not part of the fluent interface.</b>
   * Instead, it returns a <i>new</i> {@code RenderSession} &#8212; a special
   * implementation of {@code RenderSession} that works on <i>all</i> instances
   * (repetitions) of the <i>nested</i> template. For example, if the nested template
   * contains a variable {@code foo} and you set its value to "bar", then "bar" will
   * appear in all instances of the template.
   *
   * <p>Beware of the effect of chaining calls to {@code repeat()}:
   *
   * <blockquote><pre>{@code
   * String src = """
   * ~%%begin:companies%
   *     ~%%begin:departments%
   *         ~%%begin:employees%
   *             ~%firstName% ~%lastName%
   *         ~%%end:employees%
   *     ~%%end:departments%
   * ~%%end:companies%
   * """;
   * Template tmpl = Template.fromString(src);
   * RenderSession rs = tmpl.newRenderSession();
   * rs.repeat("companies", 2)
   *    .repeat("departments", 3)
   *    .repeat("employees", 4)
   *    .set("firstName", "John");
   * assertEquals(2, rs.getChildSessions("companies").size());
   * assertEquals(6, rs.in("companies").getChildSessions("departments").size());
   * assertEquals(24, rs.in("companies").in("departments").getChildSessions("employees").size());
   * // "John" will appear 24 times
   * }</pre></blockquote>
   *
   * @param nestedTemplateName the name of the nested template
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything)
   * {@code times} is less than 2.
   * @param times the number of times the template will repeat itself
   * @return a {@code RenderSession} that works on all instances (repetitions) of the
   * nested template
   * @see #in(String)
   * @see #getChildSessions(String)
   */
  RenderSession repeat(String nestedTemplateName, String separator, int times);

  /**
   * <p>
   * Returns a {@code RenderSession} for the specified nested template. If the template
   * has not been instantiated yet, this method behaves as though calling
   * {@code repeat(nestedTemplateName, 1)}. Otherwise it returns a {@code RenderSession}
   * that works on all instances (repetitions) of the nested template. So, like the
   * {@link #repeat(String, int) repeat()} method, this method does not return <i>this</i>
   * {@code RenderSession}; it is not part of the fluent interface.
   * </p>
   * <blockquote><pre>{@code
   * Template template = Template.fromString("""
   * ~%%begin:employees%
   *     ~%firstName% ~%lastName%
   * ~%%end:employees%
   * """);
   * RenderSession session = template.newRenderSession();
   * session.in("employees").set("firstName", "john").set("lastName", "Smith");
   * }</pre></blockquote>
   * <p>
   * The argument is allowed to be a fully-qualified name to a deeply nested template:
   * </p>
   * <blockquote><pre>{@code
   * String src = """
   * ~%%begin:companies%
   *     ~%%begin:departments%
   *         ~%%begin:employees%
   *             ~%firstName% ~%lastName%
   *         ~%%end:employees%
   *     ~%%end:departments%
   * ~%%end:companies%
   * """;
   * Template tmpl = Template.fromString(src);
   * RenderSession rs = tmpl.newRenderSession();
   * rs.in("companies.departments.employees").set("firstName", "John");
   * }</pre></blockquote>
   * <p>
   * The last statement in the above example is equivalent to:
   * </p>
   * <blockquote><pre>{@code
   * rs.in("companies").in("departments").in("employees").set("firstName", "John");
   * }</pre></blockquote>
   *
   * @param nestedTemplateName the name of the nested template
   * @return a {@code RenderSession} that works on all instances (repetitions) of the
   * nested template
   * @see TemplateUtils#getFQN(Template)
   */
  RenderSession in(String nestedTemplateName);

  /**
   * Convenience method for rendering one or more nested text-only templates. Equivalent
   * to {@link #enable(int, String...) enable(1, nestedTemplateNames)}.
   *
   * @param nestedTemplateNames the names of the nested templates to be rendered.
   * @return this {@code RenderSession}
   */
  default RenderSession enable(String... nestedTemplateNames) {
    return enable(1, nestedTemplateNames);
  }

  /**
   * Convenience method for rendering one or more nested text-only templates. A text-only
   * template is a template that does not contain any variables or doubly-nested
   * templates.
   *
   * <p>To explicitly disable the rendering of a text-only template, you can call
   * this method and specify 0 (zero) for the {@code repeats} argument. Note, however,
   * that by default template variables and nested templates are not rendered in the first
   * place.
   *
   * <p>Specify an empty {@code String} array to enable <i>all</i> text-only
   * templates that have not been explicitly enabled or disabled yet. In that case, of
   * course, it <i>does</i> make sense to first explicitly disable the text-only templates
   * that should <i>not</i> be rendered.
   *
   * @param repeats the number of times the nested template(s) must be repeated
   * @param nestedTemplateNames the names of the nested text-only templates to be
   * rendered
   * @return this {@code RenderSession}
   * @see Template#isTextOnly()
   */
  default RenderSession enable(int repeats, String... nestedTemplateNames) {
    return enable(null, repeats, nestedTemplateNames);
  }

  /**
   * Convenience method for rendering one or more nested text-only templates. A text-only
   * template is a template that does not contain any variables or doubly-nested
   * templates.
   *
   * <p>To explicitly disable the rendering of a text-only template, you can call
   * this method and specify 0 (zero) for the {@code repeats} argument. Note, however,
   * that by default template variables and nested templates are not rendered in the first
   * place.
   *
   * <p>Specify an empty {@code String} array to enable <i>all</i> text-only
   * templates that have not been explicitly enabled or disabled yet. In that case, of
   * course, it <i>does</i> make sense to first explicitly disable the text-only templates
   * that should <i>not</i> be rendered.
   *
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything) if
   * {@code repeats} is less than 2.
   * @param repeats the number of times the nested template(s) must be repeated
   * @param nestedTemplateNames the names of the nested text-only templates to be
   * rendered
   * @return this {@code RenderSession}
   * @see Template#isTextOnly()
   */
  RenderSession enable(String separator, int repeats, String... nestedTemplateNames);

  /**
   * Enables all nested text-only templates that have not been explicitly disabled yet.
   * The nested templates may themselves contain nested templates, but they must not
   * contain variables at any nesting level. A {@code RenderException} if they do.
   *
   * @param nestedTemplateNames the names of the nested text-only templates to be
   * rendered
   * @return this {@code RenderSession}
   */
  RenderSession enableRecursive(String... nestedTemplateNames);

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable. The variable may still occur multiple times within the template. The
   * template is going to be repeated for each value in the provided list. In other words,
   * if the list contains one value, the template will be rendered once, with its one and
   * only variable being set to that value.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i> contain
   * exactly one variable
   * @param values the value to populate the nested template with
   * @return this {@code RenderSession}
   */
  default RenderSession populateSolo(String nestedTemplateName, List<?> values) {
    return populateSolo(nestedTemplateName, null, null, values);
  }

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable. The variable may still occur multiple times within the template. The
   * template is going to be repeated for each value in the provided list.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i> contain
   * exactly one variable
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything) if
   * {@code values} contains less than two elements
   * @param varGroup the variable group to assign the variable to if the variable has no
   * group name prefix.
   * @param values the values to populate the instances of the specified template with
   * @return this {@code RenderSession}
   */
  RenderSession populateSolo(
        String nestedTemplateName,
        String separator,
        VarGroup varGroup,
        List<?> values);

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables. The provided varargs array must contain an even number of elements,
   * alternating between a value for the first template variable and a value for the
   * second one. The template is going to be repeated for each <i>pair</i> of values in
   * the varargs array.
   *
   * @param nestedTemplateName the name of the nested template.
   * @param values an array of values, alternating between a value for the first template
   * variable and a value for the second one
   * @return this {@code RenderSession}
   */
  default RenderSession populateDuo(String nestedTemplateName, List<?> values) {
    return populateDuo(nestedTemplateName, null, null, values);
  }

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables. The provided list of values must contain an even number of elements,
   * alternating between a value for the first template variable and a value for the
   * second one. The template is going to be repeated for each <i>pair</i> of values in
   * the varargs array.
   *
   * @param nestedTemplateName the name of the nested template
   * @param separator the separator to place between instances of the template. May be
   * {@code null} (no separator). The argument is ignored (and may be anything) if
   * {@code values} contains zero or two elements.
   * @param varGroup the variable group to assign the variables to if they have no group
   * name prefix
   * @param values an array of values, alternating between a value for the first template
   * variable and a value for the second one
   * @return this {@code RenderSession}
   */
  RenderSession populateDuo(
        String nestedTemplateName,
        String separator,
        VarGroup varGroup,
        List<?> values);

  /**
   * Returns the child sessions that have been created for the specified nested template.
   * When you populate a nested template, the {@code RenderSession} tacitly spawns a new
   * {@code RenderSession} for that template. For example, the
   * {@link #populate(String, Object) populate()} method basically comes down to calling
   * {@link #insert(Object) insert()} on the child session. Since the nested template may
   * be repeating, the parent session actually spawns an array of child sessions. In most
   * cases you don't need to be aware of all this, but if you want full control over what
   * happens in the nested template, you can have it via this method. A
   * {@code RenderException} is thrown if no child sessions have been created yet for the
   * specified nested template.
   *
   * @param nestedTemplateName the nested template
   * @return A {@code List} of child sessions
   */
  List<RenderSession> getChildSessions(String nestedTemplateName);

  /**
   * Returns all variables that have not been set yet in the template managed by this
   * {@code RenderSession}. Variables in nested templates are not considered.
   *
   * @return all variables that have not been set yet in the template managed by this
   * {@code RenderSession}
   * @see Accessor#UNDEFINED
   * @see AccessorRegistry.Builder#nullEqualsUndefined(boolean)
   * @see #allSet()
   */
  List<String> getUnsetVariables();

  /**
   * Returns the {@linkplain TemplateUtils#getFQN(Template, String) fully-qualified names}
   * of all variables that have not been set yet in the template managed by this
   * {@code RenderSession} and all templates descending from it. Equivalent to calling
   * {@code getAllUnsetVariables(false)}.
   *
   * @return all variables that have not been set yet in the template managed by this
   * {@code RenderSession} and all templates nested inside it
   * @see #getAllUnsetVariables(boolean)
   */
  List<String> getAllUnsetVariables();

  /**
   * <p>Returns the
   * {@linkplain TemplateUtils#getFQN(Template, String) fully-qualified names} of all
   * variables that have not been set yet in the template managed by this
   * {@code RenderSession} and all templates descending from it. If {@code relativePaths}
   * equals {@code true}, the names will be fully-qualified relative to the template being
   * populated by <i>this</i> {@code RenderSession}. If you find yourself in a child
   * session, as you would after a call to {@link #in(String) in()} or
   * {@link #repeat(String, int) repeat()}, that is
   * <i>not</i> the template that is ultimately being populated. If {@code relativePaths}
   * equals {@code false}, all paths will be absolute, i.e. relative to the template
   * ultimately being populated.
   *
   * <p>Note that there is a certain indeterminacy in the result. One of the
   * descendent templates may be a repeating template, and it may have received
   * {@link Accessor#UNDEFINED} for a variable in one of the instances, but a regular
   * value for the same variable in another instance. In other words, the variable is not
   * set in one instance of the template, but set in another instance of the same
   * template. You need to establish whether this can actually happen in your case and, if
   * so, whether it has any practical effect (again, see {@link Accessor#UNDEFINED}). This
   * method only looks at the first instance of a repeating template. If you need more
   * precision, you will have to drill down into the child template using methods like
   * {@link #in(String) in()} or {@link #getChildSessions(String) getChildSessions()}.
   *
   * @param relativePaths whether to return paths relative to the template being populated
   * by this {@code RenderSession}
   * @return all variables that have not been set yet in the template managed by this
   * {@code RenderSession} and all templates nested inside it
   * @see AccessorRegistry.Builder#nullEqualsUndefined(boolean)
   * @see #allSet()
   */
  List<String> getAllUnsetVariables(boolean relativePaths);

  /**
   * Returns {@code true} if all variables (at whatever nesting level) have been set. Note
   * that you may not <i>want</i> the template to be fully populated.
   *
   * @return {@code true} if the template is fully populated
   */
  boolean allSet();

  /**
   * Unsets the specified variables. You could also simply set their value to the empty
   * string and get the exact same result when rendering. However, this method more
   * thoroughly unregisters the variables, which will likely affect the outcome of, for
   * example, {@link #allSet()}. You can unset deeply nested variables by specifying a
   * path string. For example: {@code unset(companies.departments.name)}.
   *
   * @param paths the variables to unset
   * @return this {@code RenderSession}
   */
  RenderSession unset(String... paths);

  /**
   * Depopulates and hides the specified nested templates. Note that you cannot clear the
   * entire session. If that is what you want, you should simply create a new one using
   * {@link Template#newRenderSession()}.
   *
   * @param nestedTemplateNames the nested templates to depopulate
   * @return this {@code RenderSession}
   */
  RenderSession clear(String... nestedTemplateNames);

  /**
   * Renders the template.
   *
   * @param out the output stream to which to write the populated template
   */
  void render(OutputStream out);

  /**
   * Renders the template.
   *
   * @param sb the {@code StringBuilder} to which to write the populated template
   */
  void render(StringBuilder sb);

  /**
   * Renders the template.
   *
   * @return the populated template (UTF8-encoded)
   */
  String render();

  /**
   * Returns the template being populated by this {@code RenderSession}.
   *
   * @return the template being populated by this {@code RenderSession}
   */
  Template getTemplate();

}
