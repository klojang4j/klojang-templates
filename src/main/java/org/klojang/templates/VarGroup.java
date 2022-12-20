package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.templates.x.ModulePrivate;

import java.util.HashMap;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_VARGROUP;

/**
 * A {@code VarGroup} lets you group template variables across one or more templates.
 * This enables you to
 * {@link StringifierRegistry.Builder#registerByGroup(Stringifier, String...)
 * provide} them with a shared stringifier. Variables can be assigned to a variable
 * group using a group name prefix. For example: {@code ~%html:firstName%} or
 * {@code ~%js:firstName%} or {@code ~%myDateFormat:birthDate%}. The first two
 * examples use the predefined {@link #HTML} and {@link #JS} groups, which provide
 * HTML-escaping and Ecmascript-escaping, respectively. The third example is a custom
 * group that you can define and associate with a stringifier using the
 * {@link StringifierRegistry} class. Variable groups can also be assigned
 * {@link RenderSession#set(String, Object, VarGroup) programmatically}.
 *
 * <p>Note that variable groups are assigned at the variable <i>occurrence</i>
 * level. For example, you may have a variable named "firstName" occuring multiple
 * times in one and the same template. For the occurrences inside a &lt;script&gt;
 * you might want to use the "js" prefix, for the others the "html" prefix (or no
 * prefix at all). Therefore stringifiers associated with a variable group take the
 * highest precedence when deciding which stringifier to use, even higher (perhaps
 * paradoxically) than stringifiers associated with a variable!
 *
 * @author Ayco Holleman
 */
public class VarGroup {

  public static final HashMap<String, VarGroup> VAR_GROUPS = new HashMap<>();

  /**
   * A predefined variable group corresponding to the {@code text:} prefix. Forces
   * the variable instance to be stringified using the
   * {@link Stringifier#DEFAULT default stringifier}.
   */
  public static final VarGroup TEXT = withName("text");

  /**
   * A predefined variable group corresponding to the {@code html:} prefix. Variables
   * with this prefix are HTML-escaped. Note that the fact alone that a variable
   * appears inside an HTML element, as in <code>&lt;td&gt;~%age%&lt;/td&gt;</code>,
   * does not mean that the variable <i>has to</i> have the {@code html:} prefix. The
   * {@code age} variable likely is an integer, which does not require any HTML
   * escaping.
   */
  public static final VarGroup HTML = withName("html");

  /**
   * A predefined variable group corresponding to the {@code js:} prefix. Variables
   * with this prefix are JavaScript-escaped. Especially for use in
   * <code>&lt;script&gt;</code> tags.
   */
  public static final VarGroup JS = withName("js");

  /**
   * A predefined variable group corresponding to the {@code attr:} prefix. Works
   * just like the {@code html:} prefix except that single and double quote
   * characters are also escaped.
   */
  public static final VarGroup ATTR = withName("attr");

  /**
   * A predefined variable group corresponding to the {@code jsattr:} prefix.
   * Variables with this prefix are first JavaScript-escaped and then HTML-escaped
   * like the {@link #ATTR} variable group. Especially for use in HTML attributes
   * that contain JavaScript, like <code>onclick</code> .
   */
  public static final VarGroup JS_ATTR = withName("jsattr");

  /**
   * A predefined variable group corresponding to the {@code param:} prefix. To be
   * used for template variables placed inside a URL as the value of a query
   * parameter. It could also be used in the more unlikely case that the variable
   * functions as the <i>name</i> of the query parameter, because names and values
   * are escaped in the same way in a URL. Note that it does not matter whether the
   * URL as a whole is the value of a JavaScript variable or the contents of an HTML
   * tag. Further escaping using either JavaScript-escaping rules or HTML-escaping
   * rules will not change the value.
   */
  public static final VarGroup PARAM = withName("param");

  /**
   * A predefined variable group corresponding to the {@code path:} prefix. To be
   * used for template variables placed inside a URL as a path segment.
   */
  public static final VarGroup PATH = withName("path");

  /**
   * Returns the {@code VarGroup} instance corresponding to the specified name (which
   * also functions as the prefix). Throws an {@link IllegalArgumentException} if
   * there is no {@code VarGroup} with the specified name.
   *
   * @param name The name or prefix
   * @return The {@code VarGroup} instance corresponding to the specified name
   */
  public static VarGroup forName(String name) {
    VarGroup vg = Check.notNull(name).ok(VAR_GROUPS::get);
    return Check.that(vg).is(notNull(), ERR_NO_SUCH_VARGROUP, name).ok();
  }

  /**
   * For internal use only.
   */
  public static VarGroup createPrivileged(ModulePrivate<String> name) {
    return withName(name.get());
  }

  private static VarGroup withName(String name) {
    return VAR_GROUPS.computeIfAbsent(name, VarGroup::new);
  }

  private final String name;

  private VarGroup(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
