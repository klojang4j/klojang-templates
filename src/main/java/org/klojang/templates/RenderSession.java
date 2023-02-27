package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.x.MTag;
import org.klojang.util.AnyTuple2;
import org.klojang.util.collection.IntList;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonProperties.size;
import static org.klojang.check.Tag.VALUES;
import static org.klojang.templates.Accessor.UNDEFINED;
import static org.klojang.templates.RenderErrorCode.*;
import static org.klojang.templates.TemplateUtils.getFQName;
import static org.klojang.templates.x.MTag.*;
import static org.klojang.util.ArrayMethods.EMPTY_STRING_ARRAY;
import static org.klojang.util.CollectionMethods.findFirst;
import static org.klojang.util.CollectionMethods.listify;
import static org.klojang.util.ObjectMethods.isEmpty;
import static org.klojang.util.ObjectMethods.n2e;
import static org.klojang.util.StringMethods.concat;

/**
 * A {@code RenderSession} lets you populate a template and then render it. You
 * obtain a {@code RenderSession} for a template by calling
 * {@link Template#newRenderSession()}. By default template variables and nested
 * templates are not rendered. If you do not provide values for them, they will
 * simply disappear from the template upon rendering. As soon as you call
 * {@linkplain #render()} on the {@code RenderSession}, it "freezes"; you will not be
 * allowed to set or changes template variables from that point onwards.
 *
 * <p>Render sessions are throw-away objects that should go out of scope as quickly
 * as possible. They are cheap to instantiate, but can gain a lot of state as the
 * template gets populated. Therefore, when used within a JEE(-like) framework, make
 * sure they don't survive the request method.
 *
 * <h2>Thead Safety</h2>
 *
 * <p>The {@code RenderSession} class is not thread-safe. However, if different
 * threads populate different parts of the template, they cannot get in each other's
 * way. When using multiple threads to populate a template, the moment at which to
 * call {@code render()} on the shared {@code RenderSession} instance needs to be
 * carefully synchronized.
 *
 * @author Ayco Holleman
 * @see Template#newRenderSession()
 */
public final class RenderSession {

  private final SessionConfig config;
  private final RenderState state;

  RenderSession(SessionConfig config) {
    this.config = config;
    this.state = new RenderState(config);
  }

  /* METHODS FOR SETTING A SINGLE TEMPLATE VARIABLE */

  /**
   * Sets the specified variable to the specified value. Equivalent to
   * {@link #set(String, Object, VarGroup) set(varName, value, null)}.
   *
   * @param varName the name of the variable to set
   * @param value the value of the variable
   * @return this {@code RenderSession}
   */
  public RenderSession set(String varName, Object value) {
    Check.notNull(varName, VAR_NAME);
    if (value == UNDEFINED) {
      // Specifying UNDEFINED really misses the point of that constant, but
      // we'll accept that value and process it as documented, namely: not.
      return this;
    }
    return setVar(varName, listify(value), null, null, null, null);
  }

  /**
   * Sets the specified variable to the specified value, using the
   * {@link Stringifier stringifier} associated with the specified
   * {@link VarGroup variable group} to stringify the value. If the variable has an
   * inline group name prefix (e.g. ~%<b>html</b>:fullName%), the group specified
   * through the prefix will prevail. The {@code varGroup} argument is allowed to be
   * {@code null}. In that case, if the variable also doesn't have an inline group
   * name prefix, the {@code RenderSession} will attempt to find a suitable
   * stringifier by other means; for example, based on the
   * {@link StringifierRegistry.Builder#registerByType(Stringifier, Class...) data
   * type} of the variable. If that fails, the {@code RenderSession} will default to
   * using the {@link Stringifier#DEFAULT default stringifier}.
   *
   * @param varName the name of the variable to set
   * @param value the value of the variable
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix. May be {@code null}.
   * @return this {@code RenderSession}
   * @see StringifierRegistry.Builder#registerByGroup(Stringifier, String...)
   */
  public RenderSession set(String varName, Object value, VarGroup varGroup) {
    Check.notNull(varName, VAR_NAME).and(varGroup, VAR_GROUP).is(notNull());
    if (value == UNDEFINED) {
      return this;
    }
    return setVar(varName, listify(value), varGroup, null, null, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. If the {@code List} is empty, the variable will not be
   * rendered at all (that is, the variable will be replaced with an empty string).
   *
   * @param varName the name of the template variable
   * @param values the string values to concatenate
   * @return this {@code RenderSession}
   */
  public RenderSession setList(String varName, List<?> values) {
    Check.that(varName, VAR_NAME).is(notNull()).and(values, VALUES).is(notNull());
    return setVar(varName, values, null, null, null, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. The values in the {@code List} are first stringified and
   * then concatenated. If the {@code List} is empty, the variable will not be
   * rendered at all (that is, the variable will be replaced with an empty string).
   *
   * @param varName the name of the template variable
   * @param values the string values to concatenate
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix
   * @return this {@code RenderSession}
   */
  public RenderSession setList(String varName, List<?> values, VarGroup varGroup) {
    Check.that(varName, VAR_NAME).is(notNull())
        .and(values, VALUES).is(notNull())
        .and(varGroup, VAR_GROUP).is(notNull());
    return setVar(varName, values, varGroup, null, null, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}, separating them using the specified separator string.
   *
   * @param varName the name of the template variable
   * @param values the string values to concatenate
   * @param separator the suffix to use for each string
   * @return this {@code RenderSession}
   * @see #setList(String, List, VarGroup, String, String, String)
   */
  public RenderSession setList(String varName, List<?> values, String separator) {
    Check.that(varName, VAR_NAME).is(notNull())
        .and(values, VALUES).is(notNull())
        .and(separator, SEPARATOR).is(notNull());
    return setVar(varName, values, null, null, separator, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}, separating them using the specified separator string.
   *
   * @param varName the name of the template variable
   * @param values the string values to concatenate
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix.
   * @param separator the suffix to use for each string
   * @return this {@code RenderSession}
   * @see #setList(String, List, VarGroup, String, String, String)
   */
  public RenderSession setList(String varName,
      List<?> values,
      VarGroup varGroup,
      String separator) {
    Check.that(varName, VAR_NAME).is(notNull())
        .and(values, VALUES).is(notNull())
        .and(varGroup, VAR_GROUP).is(notNull())
        .and(separator, SEPARATOR).is(notNull());
    return setVar(varName, values, varGroup, null, separator, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. For example:
   *
   * <blockquote><pre>{@code
   * renderSession.set("myVar", List.of("<", "foo", ">"), VarGroup.HTML, "<tr><td>", "</td><td>", "</td></tr>");
   * }</pre></blockquote>
   *
   * <p>will be rendered as:
   *
   * <blockquote><pre>{@code
   * <tr><td>&lt;</td><td>foo</td><td>&gt;</td></tr>
   * }</pre></blockquote>
   *
   * <p>The prefix, suffix and separator will not be escaped. If the {@code List} is
   * empty, the variable, prefix, suffix and separator will not be rendered at all.
   *
   * @param varName the name of the template variable
   * @param values the values to concatenate
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix.
   * @param prefix the prefix to the first value.
   * @param separator the separator between the values.
   * @param suffix the suffix to the last value.
   * @return this {@code RenderSession}
   */
  public RenderSession setList(String varName,
      List<?> values,
      VarGroup varGroup,
      String prefix,
      String separator,
      String suffix) {
    Check.that(varName, VAR_NAME).is(notNull())
        .and(values, VALUES).is(notNull())
        .and(varGroup, VAR_GROUP).is(notNull())
        .and(prefix, PREFIX).is(notNull())
        .and(separator, SEPARATOR).is(notNull())
        .and(suffix, SUFFIX).is(notNull());
    return setVar(varName, values, varGroup, prefix, separator, suffix);
  }

  private RenderSession setVar(String varName,
      List<?> values,
      VarGroup varGroup,
      String prefix,
      String separator,
      String suffix) {
    Template t = config.template();
    Check.that(varName).is(keyIn(), t.variables(),
        NO_SUCH_VARIABLE.getExceptionSupplier(getFQName(t, varName)));
    IntList indices = t.variables().get(varName);
    if (values.isEmpty()) {
      indices.forEach(i -> state.setVar(i, EMPTY_STRING_ARRAY));
    } else {
      indices.forEachThrowing(i -> setVar(i,
          values,
          varGroup,
          prefix,
          separator,
          suffix));
    }
    state.done(varName);
    return this;
  }

  private void setVar(int partIndex,
      List<?> values,
      VarGroup varGroup,
      String prefix,
      String separator,
      String suffix) {
    VariablePart part = (VariablePart) config.template().parts().get(partIndex);
    VarGroup group = part.getVarGroup().orElse(varGroup);
    // Get first non-null element in list, so that we'll
    // find the most specific stringifier
    Object any = findFirst(values, notNull());
    StringifierRegistry sf = config.stringifiers();
    Stringifier stringifier = sf.getStringifier(part, group, any);
    String[] stringified = new String[values.size()];
    if (prefix == null && separator == null && suffix == null) {
      for (int i = 0; i < values.size(); ++i) {
        stringified[i] = stringify(stringifier, part.getName(), values.get(i));
      }
    } else {
      prefix = n2e(prefix);
      separator = n2e(separator);
      suffix = n2e(suffix);
      for (int i = 0; i < values.size(); ++i) {
        String s = stringify(stringifier, part.getName(), values.get(i));
        if (i == 0) {
          s = prefix + s;
        }
        if (i == values.size() - 1) {
          s = s + suffix;
        } else {
          s = s + separator;
        }
        stringified[i] = s;
      }
    }
    state.setVar(partIndex, stringified);
  }

  /* METHODS FOR POPULATING A SINGLE NESTED TEMPLATE */

  /**
   * Populates a template nested inside the template being rendered by this
   * {@code RenderSession}. The template is populated with values retrieved from the
   * specified source data. Only variables and doubly-nested templates whose names
   * are present in the {@code names} argument will be populated. No escaping will be
   * applied to the values retrieved from the data object.
   *
   * @param nestedTemplateName the name of the nested template
   * @param sourceData an object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param names the names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   * @return this {@code RenderSession}
   */
  public RenderSession populate(String nestedTemplateName,
      Object sourceData,
      String... names) {
    return populate(nestedTemplateName, sourceData, null, names);
  }

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
   * template will not be rendered. Note, however, that by default neither template
   * variables nor nested templates are rendered, so you can also just not call
   * {@code populate} for the nested template.
   *
   * @param nestedTemplateName the name of the nested template
   * @param sourceData an object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param varGroup the variable group to assign the variables to if they have
   *     no group name prefix. May be {@code null}.
   * @param names the names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   * @return this {@code RenderSession}
   */
  public RenderSession populate(String nestedTemplateName,
      Object sourceData,
      VarGroup varGroup,
      String... names) {
    if (sourceData == UNDEFINED) {
      return this;
    } else if (sourceData instanceof Optional<?> opt) {
      return opt.isPresent()
          ? populate(nestedTemplateName, opt.get(), varGroup, names)
          : this;
    }
    Template t = getNestedTemplate(nestedTemplateName);
    List<?> data = listify(sourceData);
    if (t.isTextOnly()) {
      return show(data.size(), t);
    }
    Check.that(data, "source data").is(deepNotNull());
    return repeat(t, data, varGroup, names);
  }

  private RenderSession repeat(Template t,
      List<?> data,
      VarGroup varGroup,
      String... names) {
    RenderSession[] sessions = state.getOrCreateChildSessions(t, data.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(data.get(i), varGroup, names);
    }
    return this;
  }

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
  public RenderSession show(String... nestedTemplateNames) {
    return show(1, nestedTemplateNames);
  }

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
  public RenderSession show(int repeats, String... nestedTemplateNames) {
    Check.that(repeats, MTag.REPEATS).is(gte(), 0);
    Check.notNull(nestedTemplateNames, Tag.VARARGS);
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        Check.that(t.isTextOnly())
            .is(yes(), NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        if (!state.isProcessed(t)) {
          show(repeats, t);
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Template t = getNestedTemplate(name);
        Check.that(t.isTextOnly())
            .is(yes(), NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        show(repeats, t);
      }
    }
    return this;
  }

  private RenderSession show(int repeats, Template nested) {
    state.getOrCreateChildSessions(nested, repeats);
    return this;
  }

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
  public RenderSession showRecursive(String... nestedTemplateNames) {
    Check.notNull(nestedTemplateNames);
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        if (!state.isDisabled(t) && TemplateUtils.getVarsPerTemplate(t).isEmpty()) {
          showRecursive(this, t);
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Template t = getNestedTemplate(name);
        Check.that(t.getVariables())
            .has(size(), zero(), NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        showRecursive(this, t);
      }
    }
    return this;
  }

  private static void showRecursive(RenderSession s0, Template t0) {
    s0.show(1, t0);
    if (!t0.getNestedTemplates().isEmpty()) {
      RenderSession s = s0.state.getOrCreateChildSession(t0);
      for (Template t : t0.getNestedTemplates()) {
        showRecursive(s, t);
      }
    }
  }

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable and zero (doubly) nested templates. The variable may still occur
   * multiple times within the template. If the specified value is an array or a
   * {@code Collection}, the template is going to be repeated for each value within
   * the array or {@code Collection}.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @param value the value to populate the nested template with
   * @return this {@code RenderSession}
   */
  public RenderSession populate1(String nestedTemplateName, Object value) {
    return populate1(nestedTemplateName, value, null);
  }

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable and zero (doubly) nested templates. The variable may still occur
   * multiple times within the template. If the specified value is an array or a
   * {@code Collection}, the template is going to be repeated for each value within
   * the array or {@code Collection}.
   *
   * @param nestedTemplateName the name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @param value the value to set the template's one and only variable to
   * @param varGroup the variable group to assign the variable to if the variable
   *     has no group name prefix. May be {@code null}.
   * @return this {@code RenderSession}
   */
  public RenderSession populate1(String nestedTemplateName,
      Object value,
      VarGroup varGroup) {
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 1,
        NOT_ONE_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    Check.that(t.getNestedTemplates()).has(size(), zero(),
        NOT_ONE_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    String var = t.getVariables().iterator().next();
    List<?> values = Stream.of(value).map(v -> singletonMap(var, v)).toList();
    RenderSession[] sessions = state.getOrCreateChildSessions(t, values.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(values.get(i), varGroup);
    }
    return this;
  }

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables and zero (doubly) nested templates.
   *
   * @param nestedTemplateName the name of the nested template.
   * @param tuples a list of value pairs
   * @param <T> the type of the first input value
   * @param <U> the type of the second input value
   * @return this {@code RenderSession}
   */
  public <T, U> RenderSession populate2(String nestedTemplateName,
      List<AnyTuple2<T, U>> tuples) {
    return populate2(nestedTemplateName, tuples, null);
  }

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables and zero (doubly) nested templates. The variables may still occur
   * multiple times within the template. The size of the list of tuples determines
   * how often the template is going to be repeated.
   *
   * @param nestedTemplateName the name of the nested template
   * @param tuples a list of value pairs
   * @param varGroup the variable group to assign the variables to if they have
   *     no group name prefix
   * @param <T> the type of the first input value
   * @param <U> the type of the second input value
   * @return this {@code RenderSession}
   */
  public <T, U> RenderSession populate2(String nestedTemplateName,
      List<AnyTuple2<T, U>> tuples,
      VarGroup varGroup) {
    Check.that(tuples, Tag.LIST).is(deepNotNull());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 2,
        NOT_TWO_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    Check.that(t.getNestedTemplates()).has(size(), zero(),
        NOT_TWO_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    String[] vars = t.getVariables().toArray(new String[2]);
    List<Map<String, Object>> data = tuples.stream()
        .map(tuple -> Map.of(vars[0], tuple.first(), vars[1], tuple.second()))
        .toList();
    RenderSession[] sessions = state.getOrCreateChildSessions(t, data.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(data.get(i), varGroup);
    }
    return this;
  }

  /* METHODS FOR POPULATING WHATEVER IS IN THE PROVIDED OBJECT */

  /**
   * Populates the template with values from the specified source data object. Only
   * template variables and nested templates whose name is in the provided
   * {@code names} array will be populated (if possible) with values from the
   * specified source data object. The {@code names} array is allowed to be
   * {@code null} or empty, in which case an attempt is made to populate the entire
   * template from the source data object. The source data object may not suffice,
   * and is not required to populate all variables and nested templates. You can call
   * this and similar methods multiple times with different source data objects until
   * you are ready to {@link #render(OutputStream) render} the template.
   *
   * @param sourceData an object that provides data for all or some of the
   *     template variables and nested templates
   * @param names the names of the variables and nested templates that must be
   *     populated. May be {@code null} or empty, in which case all variables and
   *     nested templates will be checked to see if they can be populated from the
   *     specified source data object
   * @return this {@code RenderSession}
   */
  public RenderSession insert(Object sourceData, String... names) {
    return insert(sourceData, null, names);
  }

  /**
   * Populates the template with values from the specified source data object. Only
   * template variables and nested templates whose name is in the provided
   * {@code names} array will be populated (if possible) with values from the
   * specified source data object. The {@code names} array is allowed to be
   * {@code null} or empty, in which case an attempt is made to populate the entire
   * template from the source data object. The source data object may not suffice,
   * and is not required to populate all variables and nested templates. You can call
   * this and similar methods multiple times with different source data objects until
   * you are ready to {@link #render(OutputStream) render} the template.
   *
   * @param sourceData an object that provides data for all or some of the
   *     template variables and nested templates
   * @param varGroup the variable group to assign the template variables to if
   *     they have no inline group name prefix. May be {@code null}.
   * @param names the names of the variables and nested templates that must be
   *     populated. May be {@code null} or empty, in which case all variables and
   *     nested templates will be checked to see if they can be populated from the
   *     specified source data object
   * @return this {@code RenderSession}
   */
  public RenderSession insert(Object sourceData,
      VarGroup varGroup,
      String... names) {
    if (sourceData == UNDEFINED) {
      return this;
    } else if (sourceData == null) {
      Template t = config.template();
      Check.that(t.isTextOnly())
          .is(yes(), NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
      // If we get past this check, the entire template is in fact
      // static HTML. Expensive way to render static HTML, but no
      // reason not to support it.
      return this;
    } else if (sourceData instanceof Optional<?> opt) {
      return opt.isPresent() ? insert(opt.get(), varGroup, names) : this;
    }
    processVars(sourceData, varGroup, names);
    processTmpls(sourceData, varGroup, names);
    return this;
  }

  @SuppressWarnings("unchecked")
  private <T> void processVars(T data, VarGroup defGroup, String[] names) {
    Set<String> varNames;
    if (isEmpty(names)) {
      varNames = config.template().getVariables();
    } else {
      varNames = new HashSet<>(config.template().getVariables());
      varNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String varName : varNames) {
      if (!state.isSet(varName)) {
        Object value;
        try {
          value = acc.access(data, varName);
        } catch (RuntimeException e) {
          throw ACCESS_EXCEPTION
              .getException(getFQName(config.template(), varName), e);
        }
        if (value != UNDEFINED) {
          setVar(varName, listify(value), defGroup, null, null, null);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void processTmpls(T data, VarGroup varGroup, String[] names) {
    Set<String> tmplNames;
    if (isEmpty(names)) {
      tmplNames = config.template().getNestedTemplateNames();
    } else {
      tmplNames = new HashSet<>(config.template().getNestedTemplateNames());
      tmplNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String name : tmplNames) {
      Object nestedData = acc.access(data, name);
      if (nestedData != UNDEFINED) {
        populate(name, nestedData, varGroup, names);
      }
    }
  }

  /* MISCELLANEOUS METHODS */

  /**
   * Returns a {@code RenderSession} for the specified nested template. The
   * {@code RenderSession} inherits the {@link AccessorRegistry accessors} and
   * {@link StringifierRegistry stringifiers} from the parent session (i.e.
   * <i>this</i> {@code RenderSession}). If this is the first time the nested
   * template is processed, a single child session will be created for it. This can
   * be used as follows:
   *
   * <blockquote><pre>{@code
   * Template template = Template.fromResource(getClass(), "/templates/company.html");
   * RenderSession session = template.newRenderSession();
   * session.in("employees").set("firstName", "john").set("lastName", "Smith");
   * }</pre></blockquote>
   *
   * <p>Note that just calling the {@code in()} method already has the effect that
   * the boilerplate text in the nested template becomes visible.
   *
   * @param nestedTemplateName the nested template for which to create the child
   *     session
   * @return A child session that you can (and should) populate yourself
   */
  public RenderSession in(String nestedTemplateName) {
    Template t = getNestedTemplate(nestedTemplateName);
    return state.getOrCreateChildSession(t);
  }

  /**
   * Returns the child sessions that have been created for the specified nested
   * template. This method throws a {@code RenderException} if no child sessions have
   * been created yet for the specified nested template.
   *
   * @param nestedTemplateName the nested template
   * @return A {@code List} of child sessions
   */
  public List<RenderSession> getChildSessions(String nestedTemplateName) {
    Template t = getNestedTemplate(nestedTemplateName);
    RenderSession[] sessions = state.getChildSessions(t);
    Check.that(t).is(notNull(),
        NO_CHILD_SESSIONS_YET.getExceptionSupplier(t.getName()));
    return List.of(sessions);
  }

  /* RENDER METHODS */

  /**
   * Returns {@code true} if the template is fully populated. That is, all template
   * variables (nested or not) have been. Note that you may not <i>want</i> the
   * template to be fully populated.
   *
   * @return {@code true} if the template is fully populated
   */
  public boolean isFullyPopulated() {
    return state.isFullyPopulated();
  }

  /**
   * Returns a {@code Renderable} instance that allows you to render the current
   * template over and over again. See {@link #paste(String, Renderable)}.
   *
   * @return A {@code Renderable} instance allows you to render the current template
   */
  public Renderable createRenderable() {
    state.freeze();
    return new RenderableImpl(state);
  }

  /**
   * Writes the render result to the specified {@code OutputStream}. Shortcut for
   * {@code createRenderable().render(out)}.
   *
   * @param out the output stream to which to write the render result
   */
  public void render(OutputStream out) {
    createRenderable().render(out);
  }

  /**
   * Appends the render result to the specified {@code StringBuilder}. Shortcut for
   * {@code createRenderable().render(sb)}.
   *
   * @param sb the {@code StringBuilder} to which to append the render result
   */
  public void render(StringBuilder sb) {
    createRenderable().render(sb);
  }

  /**
   * Returns the render result as a UTF-8 encoded {@code String}.
   *
   * @return the render result
   */
  public String render() {
    ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
    createRenderable().render(out);
    return out.toString(UTF_8);
  }

  @Override
  public String toString() {
    return concat(getClass().getSimpleName(),
        "[template=",
        getFQName(config.template()),
        "]");
  }

  RenderState getState() {
    return state;
  }

  private Template getNestedTemplate(String name) {
    Check.notNull(name, MTag.TEMPLATE_NAME);
    Template t = config.template();
    return Check.that(name).is(elementOf(), t.getNestedTemplateNames(),
            NO_SUCH_TEMPLATE.getExceptionSupplier(getFQName(t, name)))
        .ok(t::getNestedTemplate);
  }

  private String stringify(Stringifier stringifier, String varName, Object value) {
    String s;
    try {
      s = stringifier.stringify(value);
    } catch (NullPointerException e) {
      throw STRINGIFIER_NOT_NULL_RESISTENT
          .getException(getFQName(config.template(), varName));
    }
    return Check.that(s).is(notNull(), STRINGIFIER_RETURNED_NULL
        .getExceptionSupplier(getFQName(config.template(), varName))).ok();
  }

}
