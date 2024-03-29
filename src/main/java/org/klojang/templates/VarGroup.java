package org.klojang.templates;

import org.klojang.check.Check;

import java.util.HashMap;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_VARGROUP;

/**
 * Variable groups allow you to group template variables across one or more
 * templates. This, in turn, allows you to format or escape their values in an
 * identical manner &#8212; using a shared {@link Stringifier}. Within a template,
 * variables can be assigned to a variable group by using the variable group's name
 * as a prefix. For example: {@code ~%html:firstName%} or {@code ~%js:firstName%} or
 * {@code ~%myDateFormat:birthDate%}. The first two examples specify the predefined
 * {@link #HTML} and {@link #JS} variable groups, which provide HTML-escaping and
 * ECMASScript-escaping, respectively. The predefined variable groups mostly rely on
 * <a
 * href="https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringEscapeUtils.html">apache.commons.text</a>
 * to do the actual escaping. The third example is a custom group that you can define
 * using the {@link StringifierRegistry} class. Variable groups can also be assigned
 * in a more
 * <i>ad hoc</i> manner using the API. Many methods of the {@link RenderSession}
 * class take a {@code VarGroup} argument.
 *
 * <p>Note that variable groups are assigned at the
 * {@linkplain VariableOccurrence variable occurrence} level. For example, a template
 * may contain multiple instances of a variable named {@code firstName}. For
 * occurrences inside a {@code <script>} tag you might want to use the {@code js:}
 * prefix, for the others the {@code html:} prefix. Therefore, stringifiers
 * associated with a variable group take the highest precedence, even higher, perhaps
 * counterintuitively, than stringifiers associated with a variable.
 *
 * @author Ayco Holleman
 */
public final class VarGroup {

  private static final HashMap<String, VarGroup> VAR_GROUPS = new HashMap<>();

  /**
   * A predefined variable group corresponding to the {@code text:} prefix. Forces
   * the variable instance to be stringified using the
   * {@link Stringifier#DEFAULT default stringifier}.
   */
  public static final VarGroup TEXT = withName("text");

  /**
   * A predefined variable group corresponding to the {@code html:} prefix. Variables
   * with this prefix are HTML-escaped. Note that the fact alone that a variable
   * appears inside an HTML element, as in {@code <td>~%age%</td>}, does not mean
   * that the variable <i>has to</i> have the {@code html:} prefix. The {@code age}
   * variable likely is an integer, which does not require any HTML escaping.
   */
  public static final VarGroup HTML = withName("html");

  /**
   * A predefined variable group corresponding to the {@code js:} prefix. Variables
   * with this prefix are ECMASScript-escaped. Especially for use in {@code <script>}
   * tags.
   */
  public static final VarGroup JS = withName("js");

  /**
   * A predefined variable group corresponding to the {@code attr:} prefix. Works
   * just like the {@code html:} prefix except that single and double quote
   * characters are also escaped. Especially for use in element attribute values.
   */
  public static final VarGroup ATTR = withName("attr");

  /**
   * A predefined variable group corresponding to the {@code jsattr:} prefix.
   * Variables with this prefix are first JavaScript-escaped and then HTML-escaped
   * like the {@link #ATTR} variable group. Especially for use in HTML attributes
   * that contain JavaScript, like {@code onclick} .
   */
  public static final VarGroup JS_ATTR = withName("jsattr");

  /**
   * A predefined variable group corresponding to the {@code param:} prefix. To be
   * used for template variables placed inside a URL as the value of a query
   * parameter, like {@code http://example.com/?id=~%param:id%}. It could also be
   * used in the more unlikely case that the variable functions as the <i>name</i> of
   * the query parameter, because names and values are escaped identically in a URL.
   * Note that it does not matter whether the URL as a whole is the value of a
   * JavaScript variable or the contents of an HTML tag. Further escaping using
   * either JavaScript-escaping rules or HTML-escaping rules would not change the
   * value.
   */
  public static final VarGroup PARAM = withName("param");

  /**
   * A predefined variable group corresponding to the {@code path:} prefix. To be
   * used for template variables placed inside a URL as a path segment. For example:
   * {@code http://example.com/~%path:city%/~%path:restaurant%}.
   */
  public static final VarGroup PATH = withName("path");

  /**
   * <p>A predefined variable group corresponding to the {@code def:} prefix. This
   * prefix will cause {@code null} values to be replaced with the placeholder value
   * defined within the template. The example below will render as "Good morning,
   * John" if {@code firstName} was {@code null}.
   *
   * <blockquote><pre>{@code
   * Good morning, <!-- ~%def:firstName% -->John<!--%-->
   * }</pre></blockquote>
   *
   * <p>See {@link Regex#REGEX_CMT_VARIABLE} for an explanation of the syntax and
   * purpose of placeholder values.
   */
  public static final VarGroup DEF = withName("def");

  /**
   * Returns the {@code VarGroup} with the specified name (which is also the prefix
   * to be used inside a template). Throws an {@link IllegalArgumentException} if
   * there is no {@code VarGroup} with the specified name.
   *
   * @param name the name or prefix
   * @return the {@code VarGroup} instance corresponding to the specified name
   */
  public static VarGroup forName(String name) {
    VarGroup vg = Check.notNull(name).ok(VAR_GROUPS::get);
    return Check.that(vg).is(notNull(), ERR_NO_SUCH_VARGROUP, name).ok();
  }

  static VarGroup withName(String name) {
    return VAR_GROUPS.computeIfAbsent(name, VarGroup::new);
  }

  private final String name;

  private VarGroup(String name) {
    this.name = name;
  }

  /**
   * Returns the name of this {@code VarGroup}, which is also the prefix to be used
   * inside a template.
   *
   * @return the name of this {@code VarGroup}
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the name of this {@code VarGroup}, which is also the prefix to be used
   * inside a template.
   *
   * @return the name of this {@code VarGroup}
   */
  @Override
  public String toString() {
    return name;
  }

}
