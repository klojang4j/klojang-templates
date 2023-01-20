package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;
import org.klojang.templates.x.Private;
import org.klojang.templates.x.StandardStringifiers;
import org.klojang.templates.x.parse.VariablePart;
import org.klojang.util.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.TemplateUtils.getNestedTemplate;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_VARIABLE;
import static org.klojang.util.ObjectMethods.ifNotNull;
import static org.klojang.util.StringMethods.*;

/**
 * A registry of {@link Stringifier stringifiers} used by the {@link RenderSession}
 * to stringify the values provided by the data access layer. In principle each and
 * every template variable must be associated with a {@code Stringifier}. In
 * practice, however, it is unlikely you will define many variable-specific
 * stringifiers, if at all. If a variable's value can be stringified by calling
 * {@code toString()} on it (or to an empty string if null), you don't need to
 * specify a stringifier for it because this is default behaviour. In addition, all
 * variables with the same data type will often should be stringified identically.
 * For example, you may want to format all {@code int} values according to your
 * country's locale. These type-based stringifiers can be configured using
 * {@link Builder#registerByType(Stringifier, Class[]) registerByType}. Only if a
 * template variable has very specific stringification requirements would you
 * {@link Builder#register(Stringifier, Template, String...) register} a
 * variable-specific stringifier for it.
 *
 * <p>Type-based stringifiers are internally kept in a {@link TypeMap}. This
 * means that if the {@code RenderSession} requests a stringifier for some type, and
 * that type is not in the {@code TypeMap}, but one of its super types is, it will
 * receive the stringifier associated with the super type. For example, if the
 * {@code TypeMap} contains a {@code Number} stringifier and the
 * {@code RenderSession} requests an {@code Integer} stringifier, it will receive the
 * {@code Number} stringifier (unless of course you have also registered an
 * {@code Integer} stringifier). This saves you from having to register a stringifier
 * for each and every subclass of {@code Number} if they are all stringified
 * identically.
 *
 * <p>Note that escaping (e.g. HTML) and formatting (e.g. numbers) are also regarded
 * as a form of stringification, albeit from {@code String} to {@code String}. The
 * stringifiers associated with the {@link VarGroup standard variable groups} are in
 * fact all escape functions.
 *
 * <p>This is how a {@link StringifierRegistry} decides which stringifier to hand
 * out for a variable in a template:
 *
 * <p>
 *
 * <ol>
 *   <li>If a stringifier has been registered for a {@link VarGroup variable group} and the variable
 *       belongs to that group, then that is the stringifier that is going to be used.
 *   <li>If a stringifier has been registered for that particular variable in that particular
 *       template, then that is the stringifier that is going to be used.
 *   <li>If a stringifier has been registered for all variables with that particular name
 *       (irrespective of the template they belong to), then that is the stringifier that is going
 *       to be used. See {@link Builder#registerByName(Stringifier, String...)} registerByName}.
 *   <li>If a stringifier has been registered for the data type of that particular variable, then
 *       that is the stringifier that is going to be used.
 *   <li>If you have {@link Builder#setDefaultStringifier(Stringifier) registered} an alternative
 *       default stringifier, then that is the stringifier that is going to be used.
 *   <li>Otherwise the {@link Stringifier#DEFAULT default stringifier} is going to be used.
 * </ol>
 *
 * @author Ayco Holleman
 */
public final class StringifierRegistry {

  /**
   * A minimal {@code StringifierRegistry} instance. It contains stringifiers for the
   * predefined {@link VarGroup variable groups}. Variables not within these groups
   * are stringified using the {@link Stringifier#DEFAULT default stringifier}. This
   * is the {@code StringifierRegistry} a {@link RenderSession} will use if you
   * called {@link Template#newRenderSession() Template.newRenderSession} without the
   * {@code StringifierRegistry} argument.
   */
  public static final StringifierRegistry STANDARD_STRINGIFIERS = configure().freeze();

  /* ++++++++++++++++++++[ BEGIN BUILDER CLASS ]+++++++++++++++++ */

  /**
   * Lets you configure a {@link StringifierRegistry}.
   *
   * @author Ayco Holleman
   */
  public static class Builder {

    private static final String ERR_VAR_ASSIGNED = "Stringifier already set for variable \"%s\"";
    private static final String ERR_GROUP_ASSIGNED = "Stringifier already set for group \"%s\"";
    private static final String ERR_TYPE_ASSIGNED = "Stringifier already set for type \"%s\"";
    private static final String ERR_TYPE_SET = "Data type already set for variable \"%s\"";

    private Stringifier defStringifier = Stringifier.DEFAULT;

    private final Map<StringifierId, Stringifier> stringifiers = new HashMap<>();
    private final Map<Class<?>, Stringifier> typeStringifiers = new HashMap<>();
    private final Map<Tuple2<Template, String>, Class<?>> typeLookup = new HashMap<>();
    private final List<Tuple2<String, Stringifier>> partialNames = new ArrayList<>();

    private Builder(boolean std) {
      if (std) {
        StandardStringifiers.get()
            .forEach((k, v) -> stringifiers.put(new StringifierId(k), v));
      }
    }

    /**
     * Lets you specify an alternative default stringifier, replacing
     * {@link Stringifier#DEFAULT}.
     *
     * @param stringifier The stringifier to use as the default stringifier
     * @return This {@code Builder}
     */
    public Builder setDefaultStringifier(Stringifier stringifier) {
      this.defStringifier = Check.notNull(stringifier).ok();
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified variables. The variable
     * names are taken to be fully-qualified names, relative to the specified
     * template. For example:
     *
     * <blockquote><pre>{@code
     * Template companyTmpl = Template.fromResource(getClass(), "/html/company.html");
     * StringifierRegistry stringifiers = StringifierRegistry
     *  .configure()
     *  .register(
     *    zipCodeFormatter,
     *    companyTmpl,
     *    "zipCode"
     *    "departments.employees.address.zipCode",
     *    "departments.manager.address.zipCode")
     *  .freeze();
     * }</pre></blockquote>
     *
     * @param stringifier The stringifier
     * @param template The template containing the variables
     * @param varNames Any array of fully-qualified variable names
     * @return This {@code Builder}
     * @see TemplateUtils#getFQName(Template, String)
     * @see TemplateUtils#getParentTemplate(Template, String)
     */
    public Builder register(Stringifier stringifier,
        Template template,
        String... varNames) {
      Check.notNull(stringifier, "stringifier");
      Check.notNull(template, "template");
      Check.that(varNames, "varNames").is(deepNotEmpty());
      for (String name : varNames) {
        Template tmpl = TemplateUtils.getParentTemplate(template, name);
        Check.that(name).is(in(), tmpl.getVariables(), ERR_NO_SUCH_VARIABLE, name);
        Check.that(new StringifierId(template, name))
            .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED)
            .then(id -> stringifiers.put(id, stringifier));
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified variables. The variables
     * are supposed to be residing in {@code nestedTemplateName}; not in some
     * template descending from it. In other words, don't used fully-qualified
     * variable names. If you want to target the variables in the root template
     * itself (the {@code template} argument), specify {@code null} for
     * {@code nestedTemplateName}. To assign the stringifier to <i>all</i> variables
     * in the target template, specify an empty string array for {@code varNames}.
     * For example:
     *
     * <blockquote>
     *
     * <pre>{@code
     * Template companyTmpl = Template.fromResource(getClass(), "/html/company.html");
     * StringifierRegistry stringifiers = StringifierRegistry
     *  .configure()
     *  .registerByTemplate(
     *    nameFormatter,
     *    companyTmpl,
     *    "departments.employees.firstName",
     *    "departments.employees.lastName")
     *  .registerByTemplate(
     *    nameFormatter,
     *    companyTmpl,
     *    "departments.manager.firstName",
     *    "departments.manager.lastName")
     *  .freeze();
     * }</pre>
     *
     * </blockquote>
     *
     * @param stringifier The stringifier
     * @param template The root template
     * @param nestedTemplateName The name of a template descending from the root
     *     template, or {@code null} if you want to target the variables in the root
     *     template itself
     * @param varNames The names of the variables to which to assign the
     *     stringifier, or an empty string array if you want to assign the
     *     stringifier to all variables within the target template
     * @return This {@code Builder}
     */
    public Builder registerByTemplate(Stringifier stringifier,
        Template template,
        String nestedTemplateName,
        String... varNames) {
      Check.notNull(stringifier, "stringifier");
      Check.notNull(template, "template");
      Check.notNull(varNames, "varNames");
      Template tmpl = ifNotNull(nestedTemplateName,
          n -> getNestedTemplate(template, n),
          template);
      boolean all = varNames.length == 0;
      String[] names;
      if (all) {
        names = tmpl.getVariables().toArray(String[]::new);
      } else {
        names = varNames;
      }
      for (String name : names) {
        if (!all) {
          Check.notNull(name, "variable name")
              .is(in(), template.getVariables(), ERR_NO_SUCH_VARIABLE, name);
        }
        Check.that(new StringifierId(template, name))
            .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED)
            .then(id -> stringifiers.put(id, stringifier));
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified
     * {@link VarGroup variable groups}. The group that a variable belongs to can be
     * specified as a prefix within the variable declaration. For example in
     * {@code ~%format2:salary%} the {@code salary} variable is assigned to variable
     * group "format2". A variable group can also be assigned via the
     * {@link RenderSession} class. See
     * {@link RenderSession#set(String, Object, VarGroup)}. Note that different
     * instances of the same variable within the same template can be assigned to
     * different variable groups (for example: {@code ~%html:fullName%} and
     * {@code ~%js:fullName%}).
     *
     * @param stringifier The stringifier
     * @param groupNames The names of the variable groups to which to assign the
     *     stringifier
     * @return This {@code Builder}
     */
    public Builder registerByGroup(Stringifier stringifier, String... groupNames) {
      Check.notNull(stringifier, "stringifier");
      Check.that(groupNames, "groupNames").isNot(empty());
      for (String name : groupNames) {
        Check.that(name, "group name").isNot(empty());
        VarGroup varGroup = VarGroup.createPrivileged(Private.of(name));
        Check.that(new StringifierId(varGroup))
            .isNot(keyIn(), stringifiers, ERR_GROUP_ASSIGNED)
            .then(id -> stringifiers.put(id, stringifier));
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to all variables with the specified name(s).
     * This works across all templates within the application, so be careful when
     * registering a stringifier this way. You may specify a wildcard '*' character
     * at the beginning or end of the variable name. For example to assign a number
     * formatter to all variables whose name ends with "Price", specify
     * {@code *Price} as the variable name.
     *
     * @param stringifier The stringifier
     * @param varNames The variable names to associate the stringifier with.
     * @return This {@code Builder}
     */
    public Builder registerByName(Stringifier stringifier, String... varNames) {
      Check.notNull(stringifier, "stringifier");
      Check.that(varNames, "varNames").isNot(empty());
      for (String var : varNames) {
        Check.that(var, "variable name").isNot(empty());
        if (var.startsWith("*") || var.endsWith("*")) {
          partialNames.add(Tuple2.of(var, stringifier));
        } else {
          StringifierId id = new StringifierId(var);
          Check.that(id)
              .isNot(keyIn(), stringifiers, ERR_VAR_ASSIGNED)
              .then(x -> stringifiers.put(x, stringifier));
        }
      }
      return this;
    }

    /**
     * Assigns the specified stringifier to the specified types. In other words, if a
     * value is an instance of one of those types, then it will be stringified using
     * the specified stringifier, whatever the variable receiving that value.
     * Internally, type-based stringifiers are stored into, and looked up in a
     * {@link TypeMap}. This means that if there is no stringifier defined for, say,
     * {@code Short.class}, but there is a stringifier for {@code Number.class}, then
     * that is the stringifier that is going to be used for {@code Short} values.
     * This saves you from having to specify a stringifier for each and every
     * subclass of {@code Number} if they can all be stringified in the same way.
     *
     * @param stringifier The stringifier
     * @param types The types to associate the stringifier with.
     * @return This {@code Builder}
     */
    public Builder registerByType(Stringifier stringifier, Class<?>... types) {
      Check.notNull(stringifier, "stringifier");
      Check.that(types, "types").isNot(empty());
      for (Class<?> t : types) {
        Check.notNull(t, "type")
            .isNot(keyIn(), typeStringifiers, ERR_TYPE_ASSIGNED, t.getName())
            .then(x -> typeStringifiers.put(x, stringifier));
      }
      return this;
    }

    /**
     * Explicitly sets the data type of the specified variables. This enables the
     * {@code StringifierRegistry} to find a type-based stringifier for a value even
     * if the value is {@code null} (in which case {@code Object.getClass()} is not
     * available to determine the variable's type). The variable names are taken to
     * be fully-qualified names, relative to the specified template.
     *
     * @param type The data type to set for the specified variables
     * @param template The template containing the variables
     * @param varNames The fully-qualified names of the variables
     * @return This {@code Builder}
     */
    public Builder setVariableType(Class<?> type,
        Template template,
        String... varNames) {
      Check.notNull(type, "type");
      Check.notNull(template, "template");
      Check.that(varNames, "varNames").isNot(empty());
      for (String var : varNames) {
        Check.that(var, "variable name").isNot(empty());
        Template tmpl = TemplateUtils.getParentTemplate(template, var);
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
   * Applies HTML escaping. This is one of the standard stringifiers.
   */
  public static final Stringifier ESCAPE_HTML = StandardStringifiers.ESCAPE_HTML;

  /**
   * Applies Javascript escaping. This is one of the standard stringifiers.
   */
  public static final Stringifier ESCAPE_JS = StandardStringifiers.ESCAPE_JS;

  /**
   * To be used for escaping HTML attributes. Same as {@link #ESCAPE_HTML} except
   * that single quotes and double quotes are also escaped. This is one of the
   * standard stringifiers.
   */
  public static final Stringifier ESCAPE_ATTR = StandardStringifiers.ESCAPE_ATTR;

  /**
   * To be used for escaping HTML attributes containing Javascript, like
   * {@code onclick}. This is one of the standard stringifiers.
   */
  public static final Stringifier ESCAPE_JS_ATTR = StandardStringifiers.ESCAPE_JS_ATTR;

  /**
   * To be used for escaping URL query parameter. Both parameter names and parameter
   * values can be escaped using this stringifier since they are escaped identically.
   * This is one of the standard stringifiers.
   */
  public static final Stringifier ESCAPE_QUERY_PARAM = StandardStringifiers.ESCAPE_QUERY_PARAM;

  /**
   * To be used for escaping URL path segments. This is one of the standard
   * stringifiers.
   */
  public static final Stringifier ESCAPE_PATH = StandardStringifiers.ESCAPE_PATH;

  /**
   * Returns a {@code Builder} instance that lets you configure a
   * {@code StringifierRegistry}. The registry will already contain stringifiers for
   * the standard {@link VarGroup variable groups}.
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
   * exception the {@link Stringifier#DEFAULT default stringifier}. Useful for
   * non-HTML templates.
   *
   * @return A {@code Builder} instance that lets you configure a
   *     {@code StringifierRegistry}
   */
  public static Builder cleanSlate() {
    return new Builder(true);
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
    StringifierId id;
    Stringifier sf;
    if (part.getVarGroup().isPresent()) {
      VarGroup vg = part.getVarGroup().get();
      id = new StringifierId(vg);
      if (null != (sf = stringifiers.get(id))) {
        return sf;
      }
    } else if (varGroup != null) {
      id = new StringifierId(varGroup);
      if (null != (sf = stringifiers.get(id))) {
        return sf;
      }
      throw RenderException.noStringifierForGroup(part, varGroup);
    }
    Template tmpl = part.getParentTemplate();
    String var = part.getName();
    id = new StringifierId(tmpl, var);
    if (null != (sf = stringifiers.get(id))) {
      return sf;
    }
    id = new StringifierId(null, var);
    if (null != (sf = stringifiers.get(id))) {
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
