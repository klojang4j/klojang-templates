package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.templates.x.parse.VariablePart;
import org.klojang.util.CollectionMethods;
import org.klojang.util.LaxTuple2;
import org.klojang.util.collection.IntList;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.Accessor.UNDEFINED;
import static org.klojang.templates.RenderException.*;
import static org.klojang.templates.TemplateUtils.getFQName;
import static org.klojang.templates.x.Messages.ERR_TEMPLATE_NAME_NULL;
import static org.klojang.util.ArrayMethods.EMPTY_STRING_ARRAY;
import static org.klojang.util.ObjectMethods.*;
import static org.klojang.util.StringMethods.concat;

/**
 * A {@code RenderSession} lets you populate a template and then render it. By
 * default template variables and nested templates are not rendered. That is, unless
 * you provide them with values, they will just disappear from the template upon
 * rendering. As soon as you render the template (by calling any of the
 * {@link #render()} methods), the {@code RenderSession} effectively becomes
 * immutable. You can render the template again using that {@code RenderSession}, as
 * often as you like, but it won't allow you to set or changes template variables any
 * longer.
 *
 * <p>Render sessions are throw-away objects that should go out of scope as quickly
 * as possible. They are cheap to instantiate, but can gain a lot of state as the
 * template gets populated. Therefore, when used within a JEE(-like) framework, make
 * sure they don't survive the request method. A possible exception could be
 * templates that render static but expensive-to-create content. However, in that
 * case it is better to {@link #createRenderable() obtain} a {@link Renderable}
 * object from the {@code RenderSession} and cache that, rather than the
 * {@code RenderSession} itself.
 *
 * <h2>Thead Safety</h2>
 * <p>
 * A {@code RenderSession} carries a lot of state across its methods and is therefore
 * in principle not thread-safe. However, as long as different threads populate
 * different parts of the template (e.g. one thread populates the main table and
 * another thread does the rest), they cannot get in each other's way.
 *
 * @author Ayco Holleman
 */
public class RenderSession {

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
   * @param varName The name of the variable to set
   * @param value The value of the variable
   * @throws RenderException
   */
  public RenderSession set(String varName, Object value) throws RenderException {
    return set(varName, value, (VarGroup) null);
  }

  /**
   * Sets the specified variable to the specified value, using the
   * {@link Stringifier stringifier} associated with the specified
   * {@link VarGroup variable group} to stringify the value. If the variable has an
   * inline group name prefix (e.g. ~%<b>html</b>:fullName%), the group specified
   * through the prefix will prevail. The {@code defaultGroup} argument is allowed to
   * be {@code null}. In that case, if the variable also doesn't have an inline group
   * name prefix, the {@code RenderSession} will attempt to find a suitable
   * stringifier by other means; for example, based on the
   * {@link StringifierRegistry.Builder#registerByType(Stringifier, Class...) data
   * type} of the variable. If that fails, the {@code RenderSession} will default to
   * using the {@link Stringifier#DEFAULT default stringifier}.
   *
   * @param varName The name of the variable to set
   * @param value The value of the variable
   * @param defaultGroup The variable group to assign the variable to if the
   *     variable has no group name prefix. May be {@code null}.
   * @return This {@code RenderSession}
   * @throws RenderException
   * @see StringifierRegistry.Builder#registerByGroup(Stringifier, String...)
   */
  public RenderSession set(String varName, Object value, VarGroup defaultGroup)
      throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.notNull(varName, "varName");
    Template t = config.getTemplate();
    Check.that(t.getVariables()).is(contains(), varName, noSuchVariable(t, varName));
    Check.on(alreadySet(t, varName), state.isSet(varName)).is(no());
    if (value == UNDEFINED) {
      // Unless the user is manually going through, and accessing the properties
      // of some source data object, specifying UNDEFINED misses the point of that
      // constant, but since we can't know his, we'll have to accept that value
      // and process it as it is meant to be processed (namely: not).
      return this;
    }
    IntList indices = config.getTemplate().getVarPartIndices().get(varName);
    StringifierRegistry sf = config.getStringifiers();
    for (int i = 0; i < indices.size(); ++i) {
      int partIndex = indices.get(i);
      VariablePart part = t.getPart(partIndex);
      Stringifier stringifier = sf.getStringifier(part, defaultGroup, value);
      String stringified = stringify(stringifier, varName, value);
      state.setVar(partIndex, new String[] {stringified});
    }
    state.done(varName);
    return this;
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. Unless the variable was declared with an inline group
   * name prefix, the values wil be stringified using the
   * {@link Stringifier#DEFAULT default stringifier}. The values are first
   * stringified, then escaped, then concatenated. If the {@code List} is empty, the
   * variable will not be rendered at all (that is, an empty string will be inserted
   * at the location of the variable within the template).
   *
   * @param varName The name of the variable to set
   * @param values The string values to concatenate
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession set(String varName, List<?> values) throws RenderException {
    return set(varName, values, (VarGroup) null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. The values in the {@code List} are first stringified and
   * then concatenated. If the {@code List} is empty, the variable will not be
   * rendered at all (that is, an empty string will be inserted at the location of
   * the variable within the template).
   *
   * @param varName The name of the variable to set
   * @param values The string values to concatenate
   * @param defaultGroup The variable group to assign the variable to if the
   *     variable has no group name prefix
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession set(String varName, List<?> values, VarGroup defaultGroup)
      throws RenderException {
    return set(varName,
        values,
        defaultGroup,
        (String) null,
        (String) null,
        (String) null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}, separating them using the specified separator string.
   *
   * @param varName The name of the variable to set
   * @param values The string values to concatenate
   * @param separator The suffix to use for each string
   * @return This {@code RenderSession}
   * @throws RenderException
   * @see #set(String, List, VarGroup, String, String, String)
   */
  public RenderSession set(String varName, List<?> values, String separator)
      throws RenderException {
    return set(varName,
        values,
        (VarGroup) null,
        (String) null,
        separator,
        (String) null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}, separating them using the specified separator string.
   *
   * @param varName The name of the variable to set
   * @param values The string values to concatenate
   * @param defaultGroup The variable group to assign the variable to if the
   *     variable has no group name prefix. May be {@code null}.
   * @param separator The suffix to use for each string
   * @return This {@code RenderSession}
   * @throws RenderException
   * @see #set(String, List, VarGroup, String, String, String)
   */
  public RenderSession set(String varName,
      List<?> values,
      VarGroup defaultGroup,
      String separator) throws RenderException {
    return set(varName, values, defaultGroup, null, separator, null);
  }

  /**
   * Sets the specified variable to the concatenation of the values within the
   * specified {@code List}. Each value will be prefixed with the specified prefix,
   * suffixed with the specified suffix, and separated from the previous one by the
   * specified separator. The values in the {@code List} are <i>first</i>
   * stringified, <i>then</i> enriched with prefix, suffix and separator, and
   * <i>then</i> concatenated. Thus, prefix, suffix and separator will <i>not</i> be
   * put through the stringifier. This allows you, for example, to use "&lt;br&gt;"
   * as a separator without worrying that it might get HTML-escaped into
   * "&amp;lt;br;&amp;gt;".
   *
   * <p>If the {@code List} is empty, the variable will not be rendered at all.
   *
   * @param varName The name of the variable to set
   * @param values The string values to concatenate
   * @param defaultGroup The variable group to assign the variable to if the
   *     variable has no group name prefix. May be {@code null}.
   * @param prefix The prefix to use for each string
   * @param separator The suffix to use for each string
   * @param suffix The separator to use between the stringd
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession set(String varName,
      List<?> values,
      VarGroup defaultGroup,
      String prefix,
      String separator,
      String suffix) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.notNull(varName, "varName");
    Check.notNull(values, "values");
    Template t = config.getTemplate();
    Check.that(t.getVariables()).is(contains(),
        varName,
        noSuchVariable(t, varName));
    Check.on(alreadySet(t, varName), state.isSet(varName)).is(no());
    IntList indices = config.getTemplate().getVarPartIndices().get(varName);
    if (values.isEmpty()) {
      indices.forEach(i -> state.setVar(i, EMPTY_STRING_ARRAY));
    } else {
      indices.forEachThrowing(i -> setVar(i,
          values,
          defaultGroup,
          prefix,
          separator,
          suffix));
    }
    state.done(varName);
    return this;
  }

  private void setVar(int partIndex,
      List<?> values,
      VarGroup defGroup,
      String prefix,
      String separator,
      String suffix) throws RenderException {
    VariablePart part = config.getTemplate().getPart(partIndex);
    VarGroup varGroup = part.getVarGroup().orElse(defGroup);
    prefix = n2e(prefix);
    separator = n2e(separator);
    suffix = n2e(suffix);
    boolean enrich = !prefix.isEmpty() || !separator.isEmpty() || !suffix.isEmpty();
    StringifierRegistry sf = config.getStringifiers();
    // Find first non-null value to increase the chance that we find a suitable
    // stringifier:
    Object any = values.stream().filter(notNull()).findFirst().orElse(null);
    Stringifier stringifier = sf.getStringifier(part, varGroup, any);
    String[] stringified = new String[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      String s = stringify(stringifier, part.getName(), values.get(i));
      if (enrich) {
        if (i == 0) {
          s = prefix + s + suffix;
        } else {
          s = separator + prefix + s + suffix;
        }
      }
      stringified[i] = s;
    }
    state.setVar(partIndex, stringified);
  }

  /**
   * Sets the specified variable to the entire output of the specified
   * {@code Renderable}. This allows you to create and populate a template for an
   * HTML snippet once, and then repeatedly (for each render session of the current
   * template) "paste" its output into the current template. See
   * {@link #createRenderable()}.
   *
   * @param varName The template variable to set
   * @param renderable The {@code Renderable}
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession paste(String varName, Renderable renderable)
      throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.on(illegalValue("varName", varName), varName).is(notNull());
    Check.on(illegalValue("renderable", renderable), renderable).is(notNull());
    Template t = config.getTemplate();
    Check.that(t.getVariables()).is(contains(),
        varName,
        noSuchVariable(t, varName));
    Check.on(alreadySet(t, varName), state.isSet(varName)).is(no());
    IntList indices = config.getTemplate().getVarPartIndices().get(varName);
    indices.forEach(i -> state.setVar(i, renderable));
    return this;
  }

  /* METHODS FOR POPULATING A SINGLE NESTED TEMPLATE */

  /**
   * Populates a <i>nested</i> template. The template is populated with values
   * retrieved from the specified source data. Only variables and (doubly) nested
   * templates whose name is present in the {@code names} argument will be populated.
   * No escaping will be applied to the values retrieved from the data object.
   *
   * @param nestedTemplateName The name of the nested template
   * @param sourceData An object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param names The names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   */
  public RenderSession populate(String nestedTemplateName,
      Object sourceData,
      String... names) throws RenderException {
    return populate(nestedTemplateName, sourceData, null, names);
  }

  /**
   * Populates a <i>nested</i> template. The template is populated with values
   * retrieved from the specified source data. Only variables and (doubly) nested
   * templates whose name is present in the {@code names} argument will be
   * populated.
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
   * the template will not be rendered at all. This is the mechanism for conditional
   * rendering: "populate" the nested template with an empty array or
   * {@code Collection} and the template will not be rendered. Note, however, that
   * the same can be achieved more easily by just not calling the {@code populate}
   * method for that template as by default neither template variables nor nested
   * templates are rendered.
   *
   * @param nestedTemplateName The name of the nested template
   * @param sourceData An object that provides data for all or some of the nested
   *     template's variables and nested templates
   * @param defaultGroup The variable group to assign the variables to if they
   *     have no group name prefix. May be {@code null}.
   * @param names The names of the variables and doubly-nested templates that you
   *     want to be populated using the specified data object
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession populate(String nestedTemplateName,
      Object sourceData,
      VarGroup defaultGroup,
      String... names) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    if (sourceData == UNDEFINED) {
      return this;
    }
    Template t = getNestedTemplate(nestedTemplateName);
    List<?> data = CollectionMethods.listify(sourceData);
    if (t.isTextOnly()) {
      return show(data.size(), t);
    }
    Check.on(missingSourceData(t), data).is(deepNotNull());
    return repeat(t, data, defaultGroup, names);
  }

  private RenderSession repeat(Template t,
      List<?> data,
      VarGroup defGroup,
      String... names) throws RenderException {
    RenderSession[] sessions = state.getOrCreateChildSessions(t, data.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(data.get(i), defGroup, names);
    }
    return this;
  }

  /**
   * Enables or disables the rendering of the specified templates. The specified
   * templates must all be text-only templates, otherwise a {@link RenderException}
   * is thrown. Equivalent to
   * {@link #show(int, String...) show(1, nestedTemplateNames)}.
   *
   * @param nestedTemplateNames The names of the nested templates to be
   *     rendered.
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession show(String... nestedTemplateNames) throws RenderException {
    return show(1, nestedTemplateNames);
  }

  /**
   * Enabled or disables the rendering of the specified templates. Each of the
   * specified templates will be repeated the specified number of times. The
   * specified templates must all be text-only templates, otherwise a
   * {@link RenderException} is thrown. A text-only template is a template that does
   * not contain any variables or nested templates. Some reasons you might want to
   * have such a template are:
   *
   * <p>
   *
   * <ul>
   *   <li>You want to conditionally render the (static) HTML inside it
   *   <li>You want to include it in multiple parent templates
   *   <li>You want to reduce clutter in your main template
   * </ul>
   *
   * <p>To <i>disable</i> rendering of a text-only template, specify 0 (zero) for the {@code
   * repeats} argument. Note, however, that by default template variables and nested templates are
   * not rendered in the first place, so you could also just not call this method for the template
   * in question.
   *
   * <p>Specify an empty {@code String} array to enable <i>all</i> text-only templates that have not
   * been explicitly enabled or disabled yet. In that case it <i>does</i> make sense to first
   * explicitly disable the text-only templates that should not be rendered.
   *
   * <p>You could achieve the same by calling {@code populate(nestedTemplateName, null} or {@code
   * populate(nestedTemplateName, new Object[6]} (repeat six times) or {@code
   * populate(nestedTemplateName, new Object[0]} (disable the template). However, the {@code show}
   * method bypasses some code that is irrelevant to text-only templates.
   *
   * @param nestedTemplateNames The names of the nested text-only templates you
   *     want to be rendered
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession show(int repeats, String... nestedTemplateNames)
      throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.that(repeats, "repeats").is(gte(), 0);
    Check.notNull(nestedTemplateNames, "nestedTemplateNames");
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.getTemplate().getNestedTemplates()) {
        if (t.isTextOnly() && !state.isProcessed(t)) {
          show(repeats, t);
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Check.that(name).is(notNull(), "Template name must not be null");
        Template t = getNestedTemplate(name);
        Check.on(notTextOnly(t), t.isTextOnly()).is(yes());
        show(repeats, t);
      }
    }
    return this;
  }

  private RenderSession show(int repeats, Template nested) throws RenderException {
    state.getOrCreateTextOnlyChildSessions(nested, repeats);
    return this;
  }

  /**
   * Enables all nested text-only templates that have not been explicitly disabled.
   * The nested templates may in fact contain nested templates themselves, but they
   * must not contain variables at any nesting level beneath them. A
   * {@code RenderException} if they do.
   *
   * @param nestedTemplateNames The names of the nested text-only templates you
   *     want to be rendered
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession showRecursive(String... nestedTemplateNames)
      throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.notNull(nestedTemplateNames, "nestedTemplateNames");
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.getTemplate().getNestedTemplates()) {
        if (!state.isDisabled(t)) {
          if (TemplateUtils.getVarsPerTemplate(t).isEmpty()) {
            showRecursive(this, t);
          }
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Check.that(name).is(notNull(), ERR_TEMPLATE_NAME_NULL);
        Template t = getNestedTemplate(name);
        if (TemplateUtils.getVarsPerTemplate(t).isEmpty()) {
          showRecursive(this, t);
        }
      }
    }
    return this;
  }

  private static void showRecursive(RenderSession s0, Template t0)
      throws RenderException {
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
   * @param nestedTemplateName The name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession populateWithValue(String nestedTemplateName, Object value)
      throws RenderException {
    return populateWithValue(nestedTemplateName, value, null);
  }

  /**
   * Convenience method for populating a nested template that contains exactly one
   * variable and zero (doubly) nested templates. The variable may still occur
   * multiple times within the template. If the specified value is an array or a
   * {@code Collection}, the template is going to be repeated for each value within
   * the array or {@code Collection}.
   *
   * @param nestedTemplateName The name of the nested template. <i>Must</i>
   *     contain exactly one variable
   * @param value The value to set the template's one and only variable to
   * @param defaultGroup The variable group to assign the variable to if the
   *     variable has no group name prefix. May be {@code null}.
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession populateWithValue(String nestedTemplateName,
      Object value,
      VarGroup defaultGroup) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.on(notMonoTemplate(t), t)
        .has(tmpl -> tmpl.getVariables().size(), eq(), 1)
        .has(tmpl -> tmpl.countNestedTemplates(), eq(), 0);
    String var = t.getVariables().iterator().next();
    List<?> values = Arrays.asList(value)
        .stream()
        .map(v -> singletonMap(var, v))
        .collect(toList());
    RenderSession[] sessions = state.getOrCreateChildSessions(t, values.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(values.get(i), defaultGroup);
    }
    return this;
  }

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables and zero (doubly) nested templates.
   *
   * @param nestedTemplateName The name of the nested template.
   * @param tuples A list of value pairs
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public <T, U> RenderSession populateWithTuple(String nestedTemplateName,
      List<LaxTuple2<T, U>> tuples) throws RenderException {
    return populateWithTuple(nestedTemplateName, tuples, null);
  }

  /**
   * Convenience method for populating a nested template that contains exactly two
   * variables and zero (doubly) nested templates. The variables may still occur
   * multiple times within the template. The size of the list of tuples determines
   * how often the template is going to be repeated.
   *
   * @param nestedTemplateName The name of the nested template
   * @param tuples A list of value pairs
   * @param defaultGroup The variable group to assign the variables to if they
   *     have no group name prefix
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public <T, U> RenderSession populateWithTuple(String nestedTemplateName,
      List<LaxTuple2<T, U>> tuples,
      VarGroup defaultGroup) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Check.on(illegalValue("tuples", tuples), tuples).is(deepNotNull());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.on(notTupleTemplate(t), t)
        .has(tmpl -> tmpl.getVariables().size(), eq(), 2)
        .has(tmpl -> tmpl.countNestedTemplates(), eq(), 0);
    String[] vars = t.getVariables().toArray(new String[2]);
    List<Map<String, Object>> data = tuples.stream()
        .map(tuple -> Map.of(vars[0], tuple.first(), vars[1], tuple.second()))
        .collect(toList());
    RenderSession[] sessions = state.getOrCreateChildSessions(t, data.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(data.get(i), defaultGroup);
    }
    return this;
  }

  /* METHODS FOR POPULATING WHATEVER IS IN THE PROVIDED OBJECT */

  /**
   * Populates the <i>current</i> template (the template for which this
   * {@code RenderSession} was created). No escaping will be applied to the values
   * extracted from the source data.
   *
   * @param sourceData An object that provides data for all or some of the
   *     template variables and nested templates
   * @param names The names of the variables nested templates names that must be
   *     populated. Not specifying any name (or {@code null}) indicates that you want
   *     all variables and nested templates to be populated.
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession insert(Object sourceData, String... names)
      throws RenderException {
    return insert(sourceData, null, names);
  }

  /**
   * Populates the <i>current</i> template (the template for which this
   * {@code RenderSession} was created) using the provided source data object. The
   * source data object is not required to populate the entire template in one shot.
   * You can call this and similar methods multiple times until you are satisfied and
   * ready to {@link #render(OutputStream) render} the template.
   *
   * @param sourceData An object that provides data for all or some of the
   *     template variables and nested templates
   * @param defaultGroup The variable group to assign the variables to if they
   *     have no group name prefix. May be {@code null}.
   * @param names The names of the variables nested templates names that must be
   *     populated. Not specifying any name (or {@code null}) indicates that you want
   *     all variables and nested templates to be populated.
   * @return This {@code RenderSession}
   * @throws RenderException
   */
  public RenderSession insert(Object sourceData,
      VarGroup defaultGroup,
      String... names) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    if (sourceData == UNDEFINED) {
      return this;
    } else if (sourceData == null) {
      Template t = config.getTemplate();
      Check.on(notTextOnly(t), t.isTextOnly()).is(yes());
      // If we get past this check, the entire template is in fact
      // static HTML. Pretty expensive way to render static HTML,
      // but no reason not to support it.
      return this;
    }
    processVars(sourceData, defaultGroup, names);
    processTmpls(sourceData, defaultGroup, names);
    return this;
  }

  @SuppressWarnings("unchecked")
  private <T> void processVars(T data, VarGroup defGroup, String[] names)
      throws RenderException {
    Set<String> varNames;
    if (isEmpty(names)) {
      varNames = config.getTemplate().getVariables();
    } else {
      varNames = new HashSet<>(config.getTemplate().getVariables());
      varNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String varName : varNames) {
      if (!state.isSet(varName)) {
        Object value;
        try {
          value = acc.access(data, varName);
        } catch (RuntimeException e) {
          throw accessException(config.getTemplate(), varName, e, data, acc);
        }
        if (value != UNDEFINED) {
          set(varName, value, defGroup);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void processTmpls(T data, VarGroup defaultGroup, String[] names)
      throws RenderException {
    Set<String> tmplNames;
    if (isEmpty(names)) {
      tmplNames = config.getTemplate().getNestedTemplateNames();
    } else {
      tmplNames = new HashSet<>(config.getTemplate().getNestedTemplateNames());
      tmplNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String name : tmplNames) {
      Object nestedData = acc.access(data, name);
      if (nestedData != UNDEFINED) {
        populate(name, nestedData, defaultGroup, names);
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
   * be used as illustrated in the following example (assuming the presence of a
   * nested template named {@code employees}:
   *
   * <blockquote>
   *
   * <code>{@code
   * session.in("employees").set("firstName", "john").set("lastName", "Smith");
   * }</code>
   *
   * </blockquote>
   *
   * <p>Note that just calling the {@code in} method already has the effect of the
   * template becoming visible.
   *
   * @param nestedTemplateName The nested template for which to create the child
   *     session
   * @return A child session that you can (and should) populate yourself
   * @throws RenderException
   */
  public RenderSession in(String nestedTemplateName) throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Template t = getNestedTemplate(nestedTemplateName);
    return state.getOrCreateChildSession(t);
  }

  /**
   * Returns the child sessions that have been created for the specified nested
   * template. This method throws a {@code RenderException} if no child sessions have
   * been created yet for the specified nested template.
   *
   * @param nestedTemplateName The nested template
   * @return A {@code List} of child sessions
   * @throws RenderException
   */
  public List<RenderSession> getChildSessions(String nestedTemplateName)
      throws RenderException {
    Check.on(frozenSession(), state.isFrozen()).is(no());
    Template t = getNestedTemplate(nestedTemplateName);
    RenderSession[] sessions = state.getChildSessions(t);
    Check.on(noChildSessionsYet(t), t).is(notNull());
    return List.of(sessions);
  }

  /* RENDER METHODS */

  /**
   * Returns whether or not the template is fully populated. That is, all variables
   * have been set and all nested templates have been populated. Note that you may
   * not <i>want</i> the template to be fully populated.
   *
   * @return Whether or not the template is fully populated
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
    return new Renderer(state);
  }

  /**
   * Writes the render result to the specified {@code OutputStream}. Shortcut for
   * {@code createRenderable().render(out)}.
   *
   * @param out The output stream to which to write the render result
   * @throws RenderException
   */
  public void render(OutputStream out) {
    createRenderable().render(out);
  }

  /**
   * Appends the render result to the specified {@code StringBuilder}. Shortcut for
   * {@code createRenderable().render(sb)}.
   *
   * @param sb The {@code StringBuilder} to which to append the render result
   * @throws RenderException
   */
  public void render(StringBuilder sb) {
    createRenderable().render(sb);
  }

  /**
   * Returns the render result as a UTF-8 encoded {@code String}.
   *
   * @return The render result
   */
  public String render() {
    ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
    createRenderable().render(out);
    return out.toString(UTF_8);
  }

  @Override
  public String toString() {
    return concat(getClass().getSimpleName(),
        " ",
        System.identityHashCode(this),
        " for template ",
        getFQName(config.getTemplate()),
        " (",
        ifNull(config.getTemplate().getPath(), "inline"),
        ")");
  }

  RenderState getState() {
    return state;
  }

  private Template getNestedTemplate(String name) throws RenderException {
    Check.notNull(name, "nestedTemplateName");
    Check.on(noSuchTemplate(config.getTemplate(), name), name)
        .is(validTemplateName());
    return config.getTemplate().getNestedTemplate(name);
  }

  private Predicate<String> validTemplateName() {
    return s -> config.getTemplate().getNestedTemplateNames().contains(s);
  }

  private String stringify(Stringifier stringifier, String varName, Object value)
      throws RenderException {
    try {
      String s = stringifier.toString(value);
      if (s == null) {
        throw BadStringifierException.stringifierReturnedNull(config.getTemplate(),
            varName);
      }
      return s;
    } catch (NullPointerException e) {
      throw BadStringifierException.stringifierNotNullResistant(config.getTemplate(),
          varName);
    }
  }

}
