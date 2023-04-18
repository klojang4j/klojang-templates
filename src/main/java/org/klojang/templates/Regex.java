package org.klojang.templates;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Tokens and regular expressions used by the template parser to parse templates. For
 * all intents and purposes, this is an internal class. However, by making this class
 * and the constants defined in it public, the API documentation as a whole becomes
 * more self-contained as it is the only class from which you can infer which
 * syntactical constructs are available in a Klojang template. This class might also
 * be useful for toolmakers (e.g. when writing a syntax highlighting plugin).
 */
public final class Regex {

  private static final int MULTILINE = Pattern.MULTILINE | Pattern.DOTALL;

  /**
   * Regular expression for {@linkplain VarGroup variable group} names. Variable
   * groups can be specified inline (within the template) using this syntax:
   * {@code ~%vargroup:varname%}. For example: {@code ~%html:firstName%}. Variable
   * group names must start with a letter and be followed by zero or more letters,
   * digits, underscores or hyphens.
   */
  public static final String REGEX_VAR_GROUP = "([a-zA-Z][a-zA-Z0-9_\\-]*)";

  /**
   * Regular expression for nested template names and path segments within a variable
   * name. Since these names may correspond to keys in {@code Map} objects, there are
   * very few constraints on what constitutes a valid name. They must consist of at
   * least one character, and they must not contain any of the following characters:
   * {@code ~%:.\n\r\0}. Of course, if the name are to correspond to, for example,
   * bean properties, they are externally constrained: they must be valid Java
   * identifiers.
   */
  public static final String REGEX_NAME = "([^~%:.\\n\\r\u0000]+)";

  /**
   * <p>Regular expression for path strings. Variable names are paths through an
   * object graph. For example: {@code ~%company.address.city%}. This variable would
   * map to the {@code city} property of the {@code Address} object within the
   * {@code Company} object within the object that you populate the template with.
   * Each of the name segments must match {@link #REGEX_NAME}. In practice, you are
   * more likely to use nested and doubly-nested templates, and then use simple names
   * at the appropriate nesting level (e.g. {@code ~%city%}).
   *
   * <p><b>Do not confuse this regular expression with
   * {@link #REGEX_INCLUDE_PATH})</b>. The latter is used for included templates, in
   * which you specify a path to a file system or classpath resource.
   *
   * @see org.klojang.path.Path
   */
  public static final String REGEX_PATH
      = "("
      + REGEX_NAME
      + "(\\." + REGEX_NAME + ")*"
      + ")";

  /**
   * Regular expression for template variables. The pattern for a variable name is:
   * {@code ~%[vargroup:]varname%}, where {@code vargroup} is
   * {@link #REGEX_VAR_GROUP} and {@code varname} is {@link #REGEX_PATH}.
   */
  public static final String REGEX_VARIABLE
      = "~%"
      + "(" + REGEX_VAR_GROUP + ":)?"
      + REGEX_PATH
      + "%";

  /**
   * <p>Regular expression for a template variable that is placed inside an HTML
   * comment. For example: {@code <!-- ~%firstName% -->}. This is rendered just like
   * {@code ~%firstName%}. However, when using HTML comments, the raw, unprocessed
   * template still renders nicely in a browser &#8212; without "odd" tilde-percent
   * sequences spoiling the HTML page. This works even better if you also provide a
   * placeholder value, as in the following example:
   * {@code <!-- ~%firstName% -->John<!--%-->}. This, too, renders just like
   * {@code ~%firstName%}. Now, when the browser renders the raw template, it will
   * display the string "John", because it is outside any HTML comments. But when
   * <i>Klojang Templates</i> renders the template, "John" will have
   * disappeared, and the only thing that remains is the value of {@code firstName}.
   *
   * <p>Note that the entire construct ({@code <!-- ~%firstName% -->John<!--%-->})
   * <b>must</b> be on a single line. If you want to provide a placeholder value
   * that spans multiple lines, use the syntax in the example below:
   *
   * <blockquote><pre>{@code
   * <tr>
   *   <td>
   *   <!-- ~%firstName% -->
   *   <!--%-->
   *       This entire piece of text, and
   *       the placeholder tags on either
   *       side of it, will be gone when
   *       the template is rendered
   *   <!--%-->
   *   </td>
   * </tr>
   * }</pre></blockquote>
   *
   * <p>However, contrary to the single-line syntax, this value is not recorded
   * <i>as the placeholder for</i> the preceding variable. It is just something that
   * will be visible in the raw template, but gone in the rendered version.
   *
   * <p>The space character surrounding the variable (as in
   * {@code <!-- ~%firstName% -->}) is optional. You may also omit it
   * ({@code <!--~%firstName%-->}). Multiple spaces or other characters are not
   * allowed.
   *
   * @see VarGroup#DEF
   * @see #REGEX_PLACEHOLDER
   */
  public static final String REGEX_CMT_VARIABLE
      = "<!-- ?"
      + REGEX_VARIABLE
      + " ?-->((.*?)<!--%-->)?";

  /**
   * <p>
   * Regular expression for inline templates begin tags. The following examples are
   * all valid begin tags:
   * </p>
   * <p>
   * {@code ~%%begin:foo%}<br/>{@code <!-- ~%%begin:foo%}<br/>
   * {@code <!-- ~%%begin:foo% -->}
   * </p>
   * <p>
   * However, the parser enforces an extra symmetry:
   * </p>
   * <ul>
   * <li><span style="background-color:#dfdfdf">{@code <!-- ~%%begin:foo% -->}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code <!-- ~%%end:foo% -->}</span>
   * <li><span style="background-color:#dfdfdf">{@code <!-- ~%%begin:foo%}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code ~%%end:foo% -->}</span>
   * <li><span style="background-color:#dfdfdf">{@code ~%%begin:foo%}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code ~%%end:foo%}</span>
   * </ul>
   * <p>
   * The space character following "&lt;!--" and/or preceding "--&gt;" is optional.
   * Multiple spaces or other characters are not allowed.
   * </p>
   */
  public static final String REGEX_INLINE_TEMPLATE_BEGIN
      = "(<!-- ?)?~%%begin:" + REGEX_NAME + "%( ?-->)?";

  /**
   * <p>
   * Regular expression for inline templates end tags. The following examples are all
   * valid end tags:
   * </p>
   * <p>
   * {@code ~%%end:foo%}<br/>{@code ~%%end:foo% -->}<br/>
   * {@code <!-- ~%%end:foo% -->}
   * </p>
   * <p>
   * However, the parser enforces an extra symmetry:
   * </p>
   * <ul>
   * <li><span style="background-color:#dfdfdf">{@code <!-- ~%%begin:foo% -->}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code <!-- ~%%end:foo% -->}</span>
   * <li><span style="background-color:#dfdfdf">{@code <!-- ~%%begin:foo%}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code ~%%end:foo% -->}</span>
   * <li><span style="background-color:#dfdfdf">{@code ~%%begin:foo%}</span> <b>must</b> terminate with <span style="background-color:#dfdfdf">{@code ~%%end:foo%}</span>
   * </ul>
   * <p>
   * The space character following "&lt;!--" and/or preceding "--&gt;" is optional.
   * Multiple spaces or other characters are not allowed.
   * </p>
   */
  public static final String REGEX_INLINE_TEMPLATE_END
      = "(<!-- ?)?~%%end:" + REGEX_NAME + "%( ?-->)?";

  /**
   * Regular expression for the path specified in an included template. Templates are
   * included in another template using this syntax:
   * {@code ~%%include:/path/to/template.html%%} or
   * {@code ~%%include:template-name:/path/to/template.html%%}. The path is a
   * sequence of one more valid URL characters. So: letters, digits and:
   * {@code _-~:;/?#!$&%,@+.=[]()}.
   */
  public static final String REGEX_INCLUDE_PATH
      = "([a-zA-Z0-9_~:;/?#!$&%,@+.=\\-\\[\\]()]+?)";

  /**
   * Regular expression for included templates. This is the basic pattern:
   * {@code ~%%include:[template-name:]path%%}. If no name is provided, the template
   * name will be the base name of the last path element. So for
   * {@code ~%%include:/path/to/foo.html%%} that would be "foo".
   */
  public static final String REGEX_INCLUDED_TEMPLATE
      = "~%%include:"
      + "(" + REGEX_NAME + ":)?"
      + REGEX_INCLUDE_PATH
      + "%%";

  /**
   * Regular expression for an included template that is placed inside an HTML
   * comment. For example: {@code <!-- ~%%include:/path/to/foo.html%% -->}.
   */
  public static final String REGEX_CMT_INCLUDED_TEMPLATE
      = "<!-- ?"
      + REGEX_INCLUDED_TEMPLATE
      + " ?-->";

  // Used only for syntax error detection:
  static final String REGEX_DITCH_TAG = "<!--%%-->";

  /**
   * Regular expression for ditch blocks. A ditch block is a pair of
   * {@code <!--%%-->} tags and any text between them. A ditch block is the
   * <i>Klojang Templates</i> equivalent of an HTML or Java comment. Ditch blocks
   * can be used to "comment out" nested templates, template variables, or anything
   * else. They cannot themselves be nested inside any syntactical construct provided
   * by <i>Klojang Templates</i>, including nested templates.
   */
  public static final String REGEX_DITCH_BLOCK = "<!--%%-->(.*?)<!--%%-->";

  /**
   * Regular expression for placeholders. A placeholder is a pair of {@code <!--%-->}
   * tags and any text between them. When a template is rendered by <i>Klojang
   * Templates</i>, these tokens, and any text between them are erased from the
   * template. However, since {@code <!--%-->} is a self-closed HTML comment, a
   * browser would display what is between these tokens when rendering the raw,
   * unprocessed template. Contrary to {@link #REGEX_DITCH_BLOCK ditch blocks},
   * placeholders may appear inside a nested template.
   */
  public static final String REGEX_PLACEHOLDER = "<!--%-->(.*?)<!--%-->";
  static final Pattern VARIABLE = compile(REGEX_VARIABLE);
  static final Pattern CMT_VARIABLE = compile(REGEX_CMT_VARIABLE);
  static final Pattern INLINE_TEMPLATE_BEGIN = compile(REGEX_INLINE_TEMPLATE_BEGIN);
  static final Pattern INLINE_TEMPLATE_END = compile(REGEX_INLINE_TEMPLATE_END);
  static final Pattern INCLUDED_TEMPLATE = compile(REGEX_INCLUDED_TEMPLATE);
  static final Pattern CMT_INCLUDED_TEMPLATE = compile(REGEX_CMT_INCLUDED_TEMPLATE);
  // Only used for syntax error reporting
  static final Pattern DITCH_TAG = compile(REGEX_DITCH_TAG, MULTILINE);
  static final Pattern DITCH_BLOCK = compile(REGEX_DITCH_BLOCK, MULTILINE);
  static final Pattern PLACEHOLDER = compile(REGEX_PLACEHOLDER, MULTILINE);
  static final String PLACEHOLDER_START_END = "<!--%-->";

  /**
   * Prints the regular expressions.
   */
  public static void printAll() {
    System.out.println("VARIABLE ................: " + VARIABLE);
    System.out.println("CMT_VARIABLE ............: " + CMT_VARIABLE);
    System.out.println("INLINE_TEMPLATE_BEGIN ...: " + INLINE_TEMPLATE_BEGIN);
    System.out.println("INLINE_TEMPLATE_END .....: " + INLINE_TEMPLATE_END);
    System.out.println("INCLUDED_TEMPLATE .......: " + INCLUDED_TEMPLATE);
    System.out.println("CMT_INCLUDED_TEMPLATE ...: " + CMT_INCLUDED_TEMPLATE);
    System.out.println("DITCH_BLOCK .............: " + DITCH_BLOCK);
    System.out.println("PLACEHOLDER .............: " + PLACEHOLDER);
  }

  private Regex() {
    throw new UnsupportedOperationException();
  }

}
