package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;
import org.klojang.templates.x.StandardStringifiers;
import org.klojang.util.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.Tag.*;
import static org.klojang.templates.TemplateUtils.getNestedTemplate;
import static org.klojang.templates.x.MTag.STRINGIFIER;
import static org.klojang.templates.x.MTag.TEMPLATE;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_VARIABLE;
import static org.klojang.util.StringMethods.*;

/**
 * A registry of {@linkplain Stringifier stringifiers} used by the
 * {@link RenderSession} to stringify the values coming back from the data access
 * layer. Stringifiers can be used, for example, to apply non-standard formatting to
 * dates and numbers, or to apply some sort of escaping (e.g.
 * {@linkplain VarGroup#HTML HTML escaping}), or to stringify objects whose
 * {@code toString()} method does not satisfy your needs. If you need to configure
 * stringifiers, your code might look something like this:
 *
 * <blockquote><pre>{@code
 * StringifierRegistry stringifiers = StringifierRegistry.configure()
 *    .forType(int.class, obj -> String.valueOf((int) obj + 10))
 *    .freeze();
 * Template template = Template.fromString("~%foo%");
 * RenderSession session = template.newRenderSession(stringifiers);
 * String out = session.set("foo", 32).render(); // 42
 * }</pre></blockquote>
 *
 * <p>In practice, you are more likely to create just a single
 * {@code StringifierRegistry} instance for your entire application, when it starts
 * up, and pass that instance to all calls to
 * {@link Template#newRenderSession(StringifierRegistry)
 * Template.newRenderSession()}.
 *
 *
 * <p>This is how a {@link StringifierRegistry} decides which stringifier to use for
 * a template variable:
 *
 * <ol>
 *   <li>If a stringifier has been registered for a
 *       {@linkplain VarGroup variable group} and the variable belongs to that group,
 *       then that is the stringifier that is going to be used.
 *   <li>If a stringifier has been registered for that particular variable in that
 *       particular template, then that is the stringifier that is going to be used.
 *   <li>If a stringifier has been registered for all variables with that particular
 *       name (irrespective of the template they belong to), then that is the
 *       stringifier that is going to be used. See
 *       {@link Builder#forName(String, Stringifier)}
 *   <li>If a stringifier has been registered for the data type of that particular
 *       variable, then that is the stringifier that is going to be used.
 *   <li>If you have
 *       {@linkplain Builder#setDefaultStringifier(Stringifier) registered} an
 *       alternative default stringifier, then that is the stringifier that is going
 *       to be used.
 *   <li>Otherwise {@link Stringifier#DEFAULT Stringifier.DEFAULT} is going to be
 *       used.
 * </ol>
 *
 * @author Ayco Holleman
 */
public final class StringifierRegistry {

  /**
   * A minimal {@code StringifierRegistry} instance. It contains stringifiers for the
   * predefined {@link VarGroup variable groups}. Variables not within these groups
   * are stringified using the {@linkplain Stringifier#DEFAULT default stringifier}.
   * This is the {@code StringifierRegistry} a {@link RenderSession} will use if you
   * called {@link Template#newRenderSession() Template.newRenderSession()} without
   * the {@code StringifierRegistry} argument.
   */
  public static final StringifierRegistry STANDARD_STRINGIFIERS = configure().freeze();

  /* ++++++++++++++++++++[ BEGIN BUILDER CLASS ]+++++++++++++++++ */

  /**
   * A builder class for {@link StringifierRegistry} instances.
   *
   * @author Ayco Holleman
   */
  public static class Builder {

    private static final String ERR_VAR_ASSIGNED = "Stringifier already set for variable \"${arg}\"";
    private static final String ERR_GROUP_ASSIGNED = "Stringifier already set for group \"${arg}\"";
    private static final String ERR_TYPE_ASSIGNED = "Stringifier already set for \"${arg}\"";
    private static final String ERR_TYPE_SET = "Data type already set for variable \"${arg}\"";

    private Stringifier defStringifier = Stringifier.DEFAULT;

    private final Map<StringifierId, Stringifier> stringifiers = new HashMap<>();
    private final Map<Class<?>, Stringifier> typeStringifiers = new HashMap<>();
    private final Map<Tuple2<Template, String>, Class<?>> typeLookup = new HashMap<>();
    private final List<Tuple2<String, Stringifier>> partialNames = new ArrayList<>();

    private Builder(boolean std) {
      if (std) {
        StandardStringifiers.get().forEach(
            (k, v) -> stringifiers.put(new StringifierId(k), v));
      }
    }

    /**
     * Lets you specify an alternative default stringifier, replacing
     * {@link Stringifier#DEFAULT}. For example, you might want the default
     * stringifier to be {@link #ESCAPE_HTML}.
     *
     * @param stringifier the stringifier to use as the default stringifier
     * @return this {@code Builder}
     */
    public Builder setDefaultStringifier(Stringifier stringifier) {
      this.defStringifier = Check.notNull(stringifier).ok();
      return this;
    }

    /**
     * Assigns the specified stringifier to one or more variables in the specified
     * template. The variable names are taken to be fully-qualified names, relative
     * to the specified template. For example:
     *
     * <blockquote><pre>{@code
     * Template template = Template.fromResource(getClass(), "/html/company.html");
     * StringifierRegistry stringifiers = StringifierRegistry.configure()
     *    .register(template,
     *        new ZipCodeFormatter(),
     *        "zipCode"
     *        "departments.employees.address.zipCode",
     *        "departments.manager.address.zipCode")
     *    .freeze();
     * }</pre></blockquote>
     *
     * <p>To assign the stringifier to <i>all</i> variables in the specified
     * template (non-recursively), specify an empty string array.
     *
     * @param template the template containing the variables
     * @param stringifier the stringifier
     * @param varNames any array of fully-qualified variable names
     * @return this {@code Builder}
     * @see TemplateUtils#getFQN(Template, String)
     * @see TemplateUtils#getContainingTemplate(Template, String)
     */
    public Builder register(Template template, Stringifier stringifier,
        String... varNames) {
      Check.notNull(stringifier, STRINGIFIER);
      Check.notNull(template, TEMPLATE);
      Check.notNull(varNames, VARARGS);
      boolean all = varNames.length == 0;
      if (all) {
        for (String name : template.getVariables()) {
          Check.that(new StringifierId(template, name))
              .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED, name)
              .then(id -> stringifiers.put(id, stringifier));
        }
      } else {
        for (String name : varNames) {
          Template tmpl = TemplateUtils.getContainingTemplate(template, name);
          Check.that(name).is(in(), tmpl.getVariables(), ERR_NO_SUCH_VARIABLE);
          Check.that(new StringifierId(template, name))
              .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED, name)
              .then(id -> stringifiers.put(id, stringifier));
        }
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to one or more variables in a nested
     * template. {@code nestedTemplateName} must be the fully-qualified name of the
     * nested template, relative to the root template. The variable names must be
     * simple names. For example:
     *
     * <blockquote><pre>{@code
     * Template template = Template.fromResource(getClass(), "/html/company.html");
     * NameFormatter nameFormatter = new NameFormatter();
     * StringifierRegistry stringifiers = StringifierRegistry.configure()
     *     .forTemplate(template,
     *         "departments.employees",
     *         nameFormatter,
     *         "firstName",
     *         "lastName")
     *     .forTemplate(
     *         template,
     *         "departments.manager",
     *         nameFormatter,
     *         "firstName",
     *         "lastName")
     *     .freeze();
     * }</pre></blockquote>
     *
     * <p>To assign the stringifier to <i>all</i> variables in the nested template,
     * specify an empty string array.
     *
     * @param root the root template
     * @param nestedTemplateName the name of a template descending from the root
     *     template, or {@code null} if you want to target the variables in the root
     *     template itself
     * @param stringifier the stringifier
     * @param varNames the names of the variables to which to assign the
     *     stringifier, or an empty string array if you want to assign the
     *     stringifier to all variables within the target template
     * @return this {@code Builder}
     * @see TemplateUtils#getNestedTemplate(Template, String)
     */
    public Builder forTemplate(Template root,
        String nestedTemplateName,
        Stringifier stringifier,
        String... varNames) {
      Check.notNull(root, TEMPLATE);
      Check.notNull(stringifier, STRINGIFIER);
      Check.that(varNames, VARARGS).is(deepNotNull());
      Template tmpl = getNestedTemplate(root, nestedTemplateName);
      boolean all = varNames.length == 0;
      if (all) {
        for (String name : tmpl.getVariables()) {
          Check.that(new StringifierId(tmpl, name))
              .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED, name)
              .then(id -> stringifiers.put(id, stringifier));
        }
      } else {
        for (String name : varNames) {
          Check.that(name).is(in(), root.getVariables(), ERR_NO_SUCH_VARIABLE);
          Check.that(new StringifierId(tmpl, name))
              .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED)
              .then(id -> stringifiers.put(id, stringifier));
        }
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified
     * {@linkplain VarGroup variable group}. Note that different instances of the
     * same variable within the same template can be assigned to different variable
     * groups (for example: {@code ~%html:fullName%} and {@code ~%js:fullName%}).
     *
     * @param groupName the name of the variable group to which to assign the
     *     stringifier
     * @param stringifier the stringifier
     * @return this {@code Builder}
     */
    public Builder forVarGroup(String groupName, Stringifier stringifier) {
      Check.notNull(groupName, "group name");
      Check.notNull(stringifier, STRINGIFIER);
      VarGroup varGroup = VarGroup.withName(groupName);
      Check.that(new StringifierId(varGroup))
          .isNot(keyIn(), stringifiers, ERR_GROUP_ASSIGNED)
          .then(id -> stringifiers.put(id, stringifier));
      return this;
    }

    /**
     * Assigns the specified stringifier to all variables with the specified name.
     * This works across all templates within the application, so be careful when
     * registering a stringifier this way. You may specify a wildcard '*' character
     * at the beginning or end of the variable name. For example to assign a number
     * formatter to all variables whose name ends with "Price", specify
     * {@code *Price} as the variable name.
     *
     * @param name the variable name to associate the stringifier with.
     * @param stringifier the stringifier
     * @return this {@code Builder}
     */
    public Builder forName(String name, Stringifier stringifier) {
      Check.notNull(stringifier, STRINGIFIER);
      Check.that(name, NAME).isNot(empty()).isNot(EQ(), "*");
      if (name.startsWith("*") || name.endsWith("*")) {
        partialNames.add(Tuple2.of(name, stringifier));
      } else {
        Check.that(new StringifierId(name))
            .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED)
            .then(x -> stringifiers.put(x, stringifier));
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified type. Internally,
     * type-based stringifiers are stored into, and looked up in a {@link TypeMap}.
     * This means that if there is no stringifier defined for, say,
     * {@code Short.class}, but there is a stringifier for {@code Number.class}, then
     * that is the stringifier that is going to be used for {@code Short} values.
     * This saves you from having to specify a stringifier for each and every
     * subclass of {@code Number} if they can all be stringified in the same way.
     *
     * @param type the type to associate the stringifier with.
     * @param stringifier the stringifier
     * @return this {@code Builder}
     */
    public Builder forType(Class<?> type, Stringifier stringifier) {
      Check.notNull(stringifier, STRINGIFIER);
      Check.notNull(type, TYPE)
          .isNot(keyIn(), typeStringifiers, ERR_TYPE_ASSIGNED)
          .then(x -> typeStringifiers.put(x, stringifier));
      return this;
    }

    /**
     * Explicitly sets the data type of the specified variables. This enables the
     * {@code StringifierRegistry} to find a type-based stringifier for a value even
     * if the value is {@code null} (in which case {@code Object.getClass()} is not
     * available to determine the variable's type). The variable names are taken to
     * be fully-qualified names, relative to the specified template.
     *
     * @param type the data type to set for the specified variables
     * @param template the template containing the variables
     * @param varNames the fully-qualified names of the variables
     * @return this {@code Builder}
     */
    public Builder setType(Class<?> type, Template template, String... varNames) {
      Check.notNull(type, TYPE);
      Check.notNull(template, TEMPLATE);
      Check.that(varNames, VARARGS).is(deepNotNull());
      for (String var : varNames) {
        Template tmpl = TemplateUtils.getContainingTemplate(template, var);
        // Make sure var is a variable name, not a nested template name
        Check.that(var).is(in(), tmpl.getVariables());
        Tuple2<Template, String> tuple = Tuple2.of(tmpl, var);
        Check.that(tuple)
            .isNot(keyIn(), typeLookup, ERR_TYPE_SET)
            .then(t -> typeLookup.put(t, type));
      }
      return this;
    }

    /**
     * Returns a new, immutable {@code StringifierRegistry} instance.
     *
     * @return A new, immutable {@code StringifierRegistry} instance
     */
    public StringifierRegistry freeze() {
      return new StringifierRegistry(stringifiers,
          typeStringifiers,
          typeLookup,
          partialNames,
          defStringifier);
    }

  }

  /* +++++++++++++++++++++[ END BUILDER CLASS ]++++++++++++++++++ */

  /**
   * Applies HTML escaping. This is one of the standard stringifiers. It is the
   * stringifier used by the {@link VarGroup#HTML HTML} variable group.
   */
  public static final Stringifier ESCAPE_HTML = StandardStringifiers.ESCAPE_HTML;

  /**
   * Applies Javascript escaping. This is one of the standard stringifiers. It is the
   * stringifier used by the {@link VarGroup#JS JS} variable group.
   */
  public static final Stringifier ESCAPE_JS = StandardStringifiers.ESCAPE_JS;

  /**
   * To be used for escaping HTML attributes. Same as {@link #ESCAPE_HTML} except
   * that single quotes and double quotes are also escaped. This is one of the
   * standard stringifiers. It is the stringifier used by the
   * {@link VarGroup#ATTR ATTR} variable group.
   */
  public static final Stringifier ESCAPE_ATTR = StandardStringifiers.ESCAPE_ATTR;

  /**
   * To be used for escaping HTML attributes containing Javascript, like
   * {@code onclick}. This is one of the standard stringifiers. It is the stringifier
   * used by the {@link VarGroup#JS_ATTR JS_ATTR} variable group.
   */
  public static final Stringifier ESCAPE_JS_ATTR = StandardStringifiers.ESCAPE_JS_ATTR;

  /**
   * To be used for escaping URL query parameter. Both parameter names and parameter
   * values can be escaped using this stringifier since they are escaped identically.
   * This is one of the standard stringifiers. It is the stringifier used by the
   * {@link VarGroup#PARAM PARAM} variable group.
   */
  public static final Stringifier ESCAPE_QUERY_PARAM = StandardStringifiers.ESCAPE_QUERY_PARAM;

  /**
   * To be used for escaping URL path segments. This is one of the standard
   * stringifiers. It is the stringifier used by the {@link VarGroup#PATH PATH}
   * variable group.
   */
  public static final Stringifier ESCAPE_PATH = StandardStringifiers.ESCAPE_PATH;

  /**
   * Returns a {@code Builder} instance that lets you configure a
   * {@code StringifierRegistry}. The {@code StringifierRegistry} will already
   * contain the {@linkplain Stringifier#DEFAULT default stringifier} and the
   * stringifiers for the standard {@linkplain VarGroup variable groups}.
   *
   * @return A {@code Builder} instance that lets you configure a
   *     {@code StringifierRegistry}
   */
  public static Builder configure() {
    return new Builder(true);
  }

  /**
   * Returns a {@code Builder} instance that lets you configure a
   * {@code StringifierRegistry}. The registry will not contain any stringifier
   * except the {@linkplain Stringifier#DEFAULT default stringifier}. Useful for
   * non-HTML templates.
   *
   * @return A {@code Builder} instance that lets you configure a
   *     {@code StringifierRegistry}
   */
  public static Builder cleanSlate() {
    return new Builder(false);
  }

  private final Map<StringifierId, Stringifier> stringifiers;
  private final Map<Class<?>, Stringifier> typeStringifiers;
  private final Map<Tuple2<Template, String>, Class<?>> typeLookup;
  private final List<Tuple2<String, Stringifier>> partialNames;
  private final Stringifier defStringifier;

  private StringifierRegistry(Map<StringifierId, Stringifier> stringifiers,
      Map<Class<?>, Stringifier> typeStringifiers,
      Map<Tuple2<Template, String>, Class<?>> typeLookup,
      List<Tuple2<String, Stringifier>> partials,
      Stringifier defStringifier) {
    this.stringifiers = Map.copyOf(stringifiers);
    this.typeStringifiers = TypeMap.fixedTypeMap(typeStringifiers);
    this.partialNames = List.copyOf(partials);
    this.typeLookup = Map.copyOf(typeLookup);
    this.defStringifier = defStringifier;
  }

  Stringifier getStringifier(VariablePart part, VarGroup varGroup, Object value)
      throws RenderException {
    Stringifier sf;
    if (part.varGroup().isPresent()) {
      VarGroup vg = part.varGroup().get();
      if (null != (sf = stringifiers.get(new StringifierId(vg)))) {
        return sf;
      }
      // else the inline group name prefix was not associated with
      // a stringifier, which is pointless but allowed (in the future
      // we might want to use variable groups for other purposes).
    }
    if (varGroup != null) {
      if (null != (sf = stringifiers.get(new StringifierId(varGroup)))) {
        return sf;
      }
    }
    Template tmpl = part.getParentTemplate();
    String var = part.name();
    if (null != (sf = stringifiers.get(new StringifierId(tmpl, var)))) {
      return sf;
    }
    if (null != (sf = stringifiers.get(new StringifierId(null, var)))) {
      return sf;
    }
    for (Tuple2<String, Stringifier> partial : partialNames) {
      String name = partial.first();
      if (name.startsWith("*")) {
        if (name.endsWith("*") && var.contains(trim(name, "*"))) {
          return partial.second();
        } else if (var.endsWith(ltrim(name, "*"))) {
          return partial.second();
        }
      } else if (var.startsWith(rtrim(name, "*"))) {
        return partial.second();
      }
    }
    Class<?> type = typeLookup.get(Tuple2.of(tmpl, var));
    if (type == null && value != null) {
      type = value.getClass();
    }
    if (type != null) {
      if (null != (sf = typeStringifiers.get(type))) {
        return sf;
      }
    }
    return defStringifier;
  }

}
