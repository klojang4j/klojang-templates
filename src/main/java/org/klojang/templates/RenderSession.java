package org.klojang.templates;

import java.io.OutputStream;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A {@code RenderSession} lets you populate a template and then render it. You
 * obtain a {@code RenderSession} for a template by calling
 * {@link Template#newRenderSession()}. By default template variables and nested
 * templates are not rendered. If you do not provide values for them, they will
 * simply disappear from the template upon rendering.
 *
 * <p>Render sessions are meant to be throw-away objects. They should not typically
 * survive the method in which they are created. A {@code RenderSession} is cheap to
 * instantiate, but can gain a lot of state as the template gets populated.
 * Therefore, make sure they don't stick around hogging the heap.
 *
 * <h2>Thead Safety</h2>
 *
 * <p>The {@code RenderSession} class is not thread-safe. However, if different
 * threads populate different parts of the template, they cannot get in each other's
 * way. Of course, when using multiple threads to populate a template, the moment at
 * which to {@linkplain #render(OutputStream) render} it needs to be carefully
 * synchronized.
 *
 * @author Ayco Holleman
 * @see Template#newRenderSession()
 */
public sealed interface RenderSession permits SoloSession, MultiSession {

  /**
   * Sets the specified variable to the specified value. Equivalent to
   * {@link #set(String, Object, VarGroup) set(varName, value, null)}. If the
   * variable came with an inline {@linkplain VarGroup variable group} (as in
   * {@code ~%html:firstName%}), the value will be stringified and/or escaped
   * according to the variable group's {@link Stringifier}. Otherwise a suitable
   * {@code Stringifier} will be looked up in the {@link StringifierRegistry} passed
   * on to this {@code RenderSession}. (See
   * {@link Template#newRenderSession(StringifierRegistry)}.)
   *
   * @param varName the name of the variable to set
   * @param value the value of the variable
   * @return this {@code RenderSession}
   */
  RenderSession set(String varName, Object value);

  /**
   * Sets the specified variable to the specified value. The {@link Stringifier}
   * associated with the specified {@link VarGroup variable group} will be used to
   * stringify and/or escape the value. If the variable has an inline group name
   * prefix (as in {@code ~%html:firstName%}), the group specified through the prefix
   * will prevail.
   *
   * @param varName the name of the variable to set
   * @param value the value of the variable
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix. May be {@code null}.
   * @return this {@code RenderSession}
   * @see StringifierRegistry.Builder#registerByGroup(Stringifier, String...)
   */
  RenderSession set(String varName, Object value, VarGroup varGroup);

  /**
   * Sets the specified variable to the value produced by the specified
   * {@code Supplier}. The supplier's {@code get()} method will be called when the
   * template is actually {@linkplain #render(OutputStream) rendered}. This may be
   * useful if you plain to render the template multiple times using the same
   * {@code RenderSession}.
   *
   * @param varName the name of the variable to set
   * @param valueGenerator the supplier of the value
   * @return this {@code RenderSession}
   */
  RenderSession setDelayed(String varName, Supplier<Object> valueGenerator);

  /**
   * Sets the specified variable to the value produced by the specified
   * {@code Supplier}. The supplier's {@code get()} method will be called when the
   * template is actually {@linkplain #render(OutputStream) rendered}. This may be
   * useful if you plain to render the template multiple times using the same
   * {@code RenderSession}.
   *
   * @param varName the name of the variable to set
   * @param valueGenerator the supplier of the value
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix.
   * @return this {@code RenderSession}
   */
  RenderSession setDelayed(String varName,
      Supplier<Object> valueGenerator,
      VarGroup varGroup);

  /**
   * Equivalent to {@code setNested(path, valueGenerator, VarGroup.TEXT, true)}.
   *
   * @param path a path to a potentially deeply-nested variable
   * @param valueGenerator a function which is given the array index of the
   *     template instance for which to produce a value
   * @return this {@code RenderSession}
   */
  RenderSession setNested(String path, IntFunction<Object> valueGenerator);

  /**
   * Sets the value of the specified variable. The variable must reside in a nested
   * template, and it may be a deeply nested template. For example
   * {@code setNested("companies.departments.employees.firstName", idx -> "John")}
   * sets the {@code ~%firstName%} variable within the {@code employees} template
   * within the {@code departments} template within the {@code companies} template
   * within the template managed by this {@code RenderSession} to "John". Because the
   * {@code employees} template may be repeating, the value for each instance is set
   * via an {@link IntFunction}. The function is given the array index of the
   * instance and produces the value for that particular instance. If {@code force}
   * equals {@code false}, then the variable will only be set if the template
   * containing it had already been made visible via other means, e.g. via
   * {@link #repeat(String, int) repeat()} or
   * {@link #populate(String, Object, String...) populate()}. Otherwise
   * {@code setNested} will itself cause the template to become visible.
   *
   * @param path a path to a potentially deeply-nested variable
   * @param valueGenerator a function which is given the array index of the
   *     template instance for which to produce a value
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix.
   * @param force if {@code}
   * @return this {@code RenderSession}
   */
  RenderSession setNested(String path,
      IntFunction<Object> valueGenerator,
      VarGroup varGroup,
      boolean force);

  /**
   * <p>Populates the template with values extracted from the specified object. For
   * example, a "Personal Details" template may contain a {@code ~%firstName%},
   * {@code ~%lastName%} and {@code ~%birthDate%} variable. If you "insert" a
   * {@code Person} object into the template with those same properties, their values
   * will be copied to the variables using the
   * {@linkplain AccessorRegistry accessors} with which the {@code RenderSession} was
   * created (see
   * {@link Template#newRenderSession(AccessorRegistry)
   * Template.newRenderSession()}). If the template also contains a nested template
   * named "address", and the {@code Person} class contains an {@code address}
   * property referencing an {@code Address} object, then the values in the
   * {@code Address} object will be used to populate the "address" template. If the
   * {@code address} property were a {@code List} or array of {@code Address}
   * objects, then the "address" template will be repeated for each element in the
   * {@code List} or array. In short: if the object reflects the structure of the
   * template, the template almost literally becomes a "mold" into which to "sink"
   * the object. (On the other hand: the object is not <i>required</i> to exactly
   * match the structure of the template. The accessors will grab from it what they
   * can and leave the rest alone.)
   *
   * <p>Only template variables and nested templates whose name is in the provided
   * {@code names} array will be populated. The {@code names} array is allowed to be
   * {@code null} or empty, in which case an attempt is made to populate the entire
   * template from the source data object.
   *
   * @param data an object that provides data for all or some of the template
   *     variables and nested templates
   * @param names the names of the variables and nested templates that must be
   *     populated. May be {@code null} or empty, in which case all variables and
   *     nested templates will be checked to see if they can be populated from the
   *     specified source data object
   * @return this {@code RenderSession}
   */
  RenderSession insert(Object data, String... names);

  /**
   * Populates the template with values from the specified source data object. Only
   * template variables and nested templates whose name is in the provided
   * {@code names} array will be populated. The {@code names} array is allowed to be
   * {@code null} or empty, in which case an attempt is made to populate the entire
   * template from the source data object.
   *
   * @param data an object that provides data for all or some of the template
   *     variables and nested templates
   * @param varGroup the variable group to assign the template variables to if
   *     they have no inline group name prefix. May be {@code null}.
   * @param names the names of the variables and nested templates that must be
   *     populated. May be {@code null} or empty, in which case all variables and
   *     nested templates will be checked to see if they can be populated from the
   *     specified source data object
   * @return this {@code RenderSession}
   */
  RenderSession insert(Object data, VarGroup varGroup, String... names);

  /**
   * Populates a template nested inside the template being rendered by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data. Only variables and doubly-nested templates whose names
   * are present in the {@code names} argument will be populated. No escaping will be
   * applied to the values retrieved from the data object.
   *
   * @param nestedTemplateName the name of the nested template
   * @param data an object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param names the names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   * @return this {@code RenderSession}
   */
  RenderSession populate(String nestedTemplateName, Object data, String... names);

  /**
   * Populates a template nested inside the template being rendered by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data. Only variables and (doubly) nested templates whose names
   * are present in the {@code names} argument will be populated. Values will be
   * stringified using the {@link Stringifier} associated with the specified
   * {@link VarGroup}.
   *
   * <h4>Repeating Templates</h4>
   *
   * <p>If the specified object is an array or a {@code Collection}, the template
   * will be repeated for each object in the array or {@code Collection}. This can be
   * used, for example, to generate an HTML table from a nested template that
   * contains just a single row.
   *
   * <h4>Conditional Rendering</h4>
   *
   * <p>If the specified object is an empty array or an empty {@code Collection},
   * the template will not be rendered at all. This allows for conditional rendering:
   * "populate" the nested template with an empty array or {@code Collection} and the
   * template will not be rendered. Note, however, that by default neither variables
   * nor nested templates are rendered, so you could also just not call
   * {@code populate} for the nested template.
   *
   * @param nestedTemplateName the name of the nested template
   * @param data an object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param varGroup the variable group to assign the variables to if they have
   *     no group name prefix. May be {@code null}.
   * @param names the names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   * @return this {@code RenderSession}
   */
  RenderSession populate(String nestedTemplateName,
      Object data,
      VarGroup varGroup,
      String... names);

  /**
   * Causes the specified nested template to become visible and to be repeated the
   * specified number of times. If {@code times} equals zero, the template is
   * explicitly disabled (in other words: <i>not</i> rendered). Usually it is not
   * necessary to disable a nested template, because nested templates are anyhow only
   * rendered once you populate them. However, it can be useful in combination with a
   * subsequent call to {@link #show(String...) show()} and
   * {@link #showRecursive(String...) showRecursive()}.
   *
   * <p>Contrary to most of the other methods, this method does not return
   * <i>this</i> {@code RenderSession}. Instead, it returns a {@code RenderSession}
   * that works on all instances (repetitions) of the nested template. For example,
   * if the nested template contains a variable {@code foo} and you set its value to
   * "bar", then "bar" will appear in all instances of the template.
   *
   * <p>You can still call {@link #populate(String, Object, String...) populate()}
   * after a call to {@code repeat()}, but the number of source data objects you
   * provide must be equal to {@code times}. Thus, if {@code times} equals three, you
   * must provide a list or array containing three source data objects.
   *
   * @param nestedTemplateName the name of the nested template
   * @param times the number of times the template will repeat itself
   * @return a {@code RenderSession} that works on all instances (repetitions) of the
   *     nested template
   */
  RenderSession repeat(String nestedTemplateName, int times);

  /**
   * Returns a {@code RenderSession} for the specified nested template. If this is
   * the first time the nested template is processed, this method behaves as though
   * calling {@code repeat(nestedTemplateName, 1)}. Otherwise it returns a
   * {@code RenderSession} that works on all instances (repetitions) of the nested
   * template.
   *
   * <blockquote><pre>{@code
   * Template template = Template.fromResource(getClass(), "/templates/company.html");
   * RenderSession session = template.newRenderSession();
   * session.in("employees")
   *  .set("firstName", "john")
   *  .set("lastName", "Smith");
   * }</pre></blockquote>
   *
   * @param nestedTemplateName the name of the nested template
   * @return a {@code RenderSession} that works on all instances (repetitions) of the
   *     nested template
   */
  RenderSession in(String nestedTemplateName);

  /**
   * Enables or disables the rendering of nested text-only templates. The specified
   * templates must all be text-only templates, otherwise a {@link RenderException}
   * is thrown. Equivalent to
   * {@link #show(int, String...) show(1, nestedTemplateNames)}.
   *
   * @param nestedTemplateNames the names of the nested templates to be
   *     rendered.
   * @return this {@code RenderSession}
   */
  RenderSession show(String... nestedTemplateNames);

  /**
   * Enables or disables the rendering of nested text-only templates. A text-only
   * template is a template that does not contain any variables or nested templates.
   *
   * <p>To <i>disable</i> rendering of a text-only template, specify 0 (zero) for
   * the {@code repeats} argument. Note, however, that by default template variables
   * and nested templates are not rendered in the first place, so you could also just
   * not call this method for the template in question.
   *
   * <p>Specify an empty {@code String} array ({@code new String[0]}) to enable
   * <i>all</i> text-only templates that have not been explicitly enabled or
   * disabled yet. In that case, of course, it <i>does</i> make sense to first
   * explicitly disable the text-only templates that should not be rendered.
   *
   * <p>You could achieve the same by calling
   * {@code populate(nestedTemplateName, null)} (show the template) or
   * {@code populate(nestedTemplateName, new Object())} (show the template) or
   * {@code populate(nestedTemplateName, new Object[1])} (show the template) or
   * {@code populate(nestedTemplateName, new Object[6])} (repeat six times) or
   * {@code populate(nestedTemplateName, new Object[0])} (disable the template).
   * However, the {@code show} method bypasses some code that is irrelevant to
   * text-only templates.
   *
   * @param repeats the number of times the nested template(s) must be repeated
   * @param nestedTemplateNames the names of the nested text-only templates to be
   *     rendered
   * @return this {@code RenderSession}
   */
  RenderSession show(int repeats, String... nestedTemplateNames);

  /**
   * Enables all nested text-only templates that have not been explicitly disabled
   * yet. The nested templates may themselves contain nested templates, but they must
   * not contain variables at any nesting level. A {@code RenderException} if they
   * do.
   *
   * @param nestedTemplateNames the names of the nested text-only templates to be
   *     rendered
   * @return this {@code RenderSession}
   */
  RenderSession showRecursive(String... nestedTemplateNames);

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable. The variable may still occur multiple times within the template. The
   * template is going to be repeated for each value in the varargs array.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @param values the value to populate the nested template with
   * @return this {@code RenderSession}
   */
  RenderSession populate1(String nestedTemplateName, Object... values);

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable. The variable may still occur multiple times within the template. The
   * template is going to be repeated for each value in the varargs array. Note that,
   * contrary to the regular {@code populate()} method and the {@code insert()}
   * method, the {@code populate1()} method does not use
   * {@linkplain AccessorRegistry accessors} to extract values from the provided
   * object(s). The provided object really <i>is</i> the value to which the variable
   * is set.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix. May be {@code null}.
   * @param values the values to populate the instances of the specified template
   *     with
   * @return this {@code RenderSession}
   */
  RenderSession populate1(String nestedTemplateName,
      VarGroup varGroup,
      Object... values);

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables. The provided varargs array must contain an even number of elements,
   * alternating between a value for the first template variable and a value for the
   * second one. The specified template is going to be repeated for each pair of
   * values.
   *
   * @param nestedTemplateName the name of the nested template.
   * @param values an array of values, alternating between a value for the first
   *     template variable and a value for the second one
   * @return this {@code RenderSession}
   */
  RenderSession populate2(String nestedTemplateName, Object... values);

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables. The provided varargs array must contain an even number of elements,
   * alternating between a value for the first template variable and a value for the
   * second one. The specified template is going to be repeated for each pair of
   * values.
   *
   * @param nestedTemplateName the name of the nested template
   * @param varGroup the variable group to assign the variables to if they have
   *     no group name prefix
   * @param values an array of values, alternating between a value for the first
   *     template variable and a value for the second one
   * @return this {@code RenderSession}
   */
  RenderSession populate2(String nestedTemplateName,
      VarGroup varGroup, Object... values);

  /**
   * Returns the child sessions that have been created for the specified nested
   * template. This method throws a {@code RenderException} if no child sessions have
   * been created yet for the specified nested template.
   *
   * @param nestedTemplateName the nested template
   * @return A {@code List} of child sessions
   */
  List<RenderSession> getChildSessions(String nestedTemplateName);

  /**
   * Returns {@code true} if the template is fully populated. Note that you may not
   * <i>want</i> the template to be fully populated.
   *
   * @return {@code true} if the template is fully populated
   */
  boolean isFullyPopulated();

  /**
   * Renders the template.
   *
   * @param out the output stream to which to write the populated template
   */
  void render(OutputStream out);

  /**
   * Renders the template.
   *
   * @param sb the {@code StringBuilder} to which to write the populated
   *     template
   */
  void render(StringBuilder sb);

  /**
   * Renders the template.
   *
   * @return the populated template (UTF8-encoded)
   */
  String render();

}
