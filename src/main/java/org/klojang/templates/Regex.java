package org.klojang.templates;

import org.klojang.templates.RenderSession;
import org.klojang.templates.VarGroup;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Tokens and regular expressions used by the template parser to parse templates. For
 * all intents and purposes, this is an internal class, and you would not miss out on
 * any functionality if it were. However, making this class and the constants defined
 * in it public makes the API documentation as a whole more self-contained, since it
 * is the only class that describes which syntactical constructs there are and what
 * they look like. This class could also be useful for tool makers (e.g. when writing
 * a syntax highlighting plugin).
 */
public final class Regex {

  private static final int MULTILINE = Pattern.MULTILINE | Pattern.DOTALL;

  private static final String REGEX_CMT_START = "<!--\\s*";

  private static final String REGEX_CMT_END = "\\s*-->";

  /**
   * Regular expression for {@linkplain VarGroup variable group} names. Variable
   * groups can be specified inline (within the template) using this syntax:
   * {@code ~%vargroup:varname%}. For example: {@code ~%html:firstName%}. Variable
   * group names must start with a letter and be followed by zero or more letters,
   * digits, underscores or hyphens.
   */
  public static final String REGEX_VAR_GROUP = "([a-zA-Z][a-zA-Z0-9_\\-]*)";

  /**
   * Regular expression for nested template names. Template ames must consists of one
   * or more letters, digits, underscores or hyphens. If the main template is going
   * to be populated with objects that mirror the structure of the template, both
   * template names and template variable names are in practice more constrained:
   * they need to be valid Java identifiers. On the other hand, if it is going to be
   * populated with a {@code Map<String, Object>} pseudo-object, there is no such
   * constraint.
   */
  public static final String REGEX_NAME = "([a-zA-Z0-9_\\-]+)";

  /**
   * <p>Regular expression for path strings. Template variable names are paths
   * through an object graph. For example: {@code ~%company.address.city%}. This
   * variable would acquire the value of the {@code city} property of the
   * {@code Address} object within the {@code Company} object within the object that
   * you populate the template with. Each of the name segments must match
   * {@link #REGEX_NAME}. In practice you are more likely to use nested and
   * doubly-nested templates, and then use simple variable names at the appropriate
   * nesting level (e.g. {@code ~%city%}).
   *
   * <p>Note that this is <i>not</i> the regular expression for the include path of
   * an included template (see {@link #REGEX_INCLUDE_PATH}).
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
   * Regular expression for a template variable that is placed between HTML comments.
   * For example: {@code <!-- ~%firstName% -->}. This is rendered in exactly the same
   * way as {@code ~%firstName%}. That is: the entire string ("&lt;!-- ~%firstName%
   * --&gt;") is removed from the template and replaced with the value
   * {@linkplain RenderSession#set(String, Object) provided} for {@code firstName}.
   * The advantage of using HTML comments is that the raw, unprocessed template still
   * renders nicely in a browser, without "odd" tilde-percent sequences spoiling the
   * HTML page. This works even better if you also provide a placeholder value, as in
   * the following example: {@code <!-- ~%firstName% -->John<!--%-->}. Here, again,
   * the <i>entire</i> string will be replaced with whatever value is provided for
   * {@code firstName}, but when the browser renders the raw, unprocessed template,
   * it will display the string "John". Note that the entire string must be on a
   * single line. If you want to provide a placeholder value that spans multiple
   * lines, use the syntax in the example below:
   *
   * <blockquote><pre>{@code
   * <tr>
   *   <td>
   *    <!-- ~%firstName% -->
   *    <!--%-->
   *      John<br>
   *      Maynard
   *    <!--%-->
   *   </td>
   * </tr>
   * }</pre></blockquote>
   *
   * @see #REGEX_PLACEHOLDER
   */
  static final String REGEX_CMT_VARIABLE
      = "<!--[ \\t]*"
      + REGEX_VARIABLE
      + "[ \\t]*-->((.*?)<!--%-->)?";

  /**
   * Regular expression for inline template blocks.
   */
  public static final String REGEX_INLINE_TEMPLATE
      = "~%%begin:" + REGEX_NAME + "%"
      + "(.*?)"
      + "~%%end:\\1%";

  // Used only for syntax error reporting:
  static final String REGEX_INLINE_TEMPLATE_BEGIN = "~%%begin:" + REGEX_NAME + "%";

  static final String REGEX_INLINE_TEMPLATE_END = "~%%end:" + REGEX_NAME + "%";

  /**
   * Regular expression for an inline template block that is placed between HTML
   * comments.
   */
  public static final String REGEX_CMT_INLINE_TEMPLATE
      = REGEX_CMT_START
      + REGEX_INLINE_TEMPLATE
      + REGEX_CMT_END;

  /**
   * Regular expression for the path specified in an included template. Templates are
   * included in another template using this syntax:
   * {@code ~%%include:/path/to/template.html%%} or
   * {@code ~%%include:template-name:/path/to/template.html%%}. The path is a
   * sequence of one more valid URL characters. So: letters, digits and:
   * {@code _-~:;/?#!$&%,@+.=[]()}. Note that valid URLs cannot contain two
   * percentage signs in a row.
   */
  public static final String REGEX_INCLUDE_PATH
      = "([a-zA-Z0-9_~:;/?#!$&%,@+.=\\-\\[\\]\\(\\)]+?)";

  /**
   * Regular expression for included templates. This is the basic pattern:
   * {@code ~%%include:[name:]path%%}. If no name is provided, the template name will
   * be the base name of the last path element. So for
   * {@code ~%%include:/path/to/foo.html%%} that would be "foo".
   */
  public static final String REGEX_INCLUDED_TEMPLATE
      = "~%%include:"
      + "(" + REGEX_NAME + ":)?"
      + REGEX_INCLUDE_PATH
      + "%%";

  /**
   * Regular expression for an included template that is placed between HTML
   * comments.
   */
  public static final String CMT_REGEX_INCLUDED_TEMPLATE
      = REGEX_CMT_START
      + REGEX_INCLUDED_TEMPLATE
      + REGEX_CMT_END;

  // Used only for syntax error detection:
  static final String REGEX_DITCH_TAG = "<!--%%(.*?)-->";

  /**
   * Regular expression for ditch blocks. A ditch block is a pair of
   * {@code &lt;--%%--&gt;} tags and any text between them. A ditch block is the
   * Klojang Templates equivalent of an HTML or Java comment. A ditch block cannot be
   * nested inside an inline template (or any other syntactical construct provided by
   * Klojang Templates for that matter).
   */
  public static final String REGEX_DITCH_BLOCK
      = REGEX_DITCH_TAG + "(.*?)" + REGEX_DITCH_TAG;

  /**
   * Regular expression for placeholders. A placeholder is a pair of
   * {@code &lt;--%--&gt;} tags and any text between them. When a template is
   * rendered by Klojang Templates, these tokens, and any text between them are
   * erased from the template. However, since they are self-closed HTML comments, a
   * browser would display what is between these tokens when rendering the raw,
   * unprocessed template.
   */
  public static final String REGEX_PLACEHOLDER = "<!--%-->(.*?)<!--%-->";

  /**
   * The compiled version of {@link #REGEX_VARIABLE}.
   */
  public static final Pattern VARIABLE = compile(REGEX_VARIABLE);

  /**
   * The compiled version of {@link #REGEX_CMT_VARIABLE}.
   */
  public static final Pattern CMT_VARIABLE = compile(REGEX_CMT_VARIABLE);

  /**
   * The compiled version of {@link #REGEX_INLINE_TEMPLATE}.
   */
  public static final Pattern INLINE_TEMPLATE
      = compile(REGEX_INLINE_TEMPLATE, MULTILINE);

  // Only used for syntax error reporting
  static final Pattern INLINE_TEMPLATE_BEGIN = compile(
      REGEX_INLINE_TEMPLATE_BEGIN);

  static final Pattern INLINE_TEMPLATE_END = compile(REGEX_INLINE_TEMPLATE_END);

  /**
   * The compiled version of {@link #REGEX_INLINE_TEMPLATE}.
   */
  public static final Pattern CMT_INLINE_TEMPLATE
      = compile(REGEX_CMT_INLINE_TEMPLATE, MULTILINE);

  /**
   * The compiled version of {@link #REGEX_INLINE_TEMPLATE}.
   */
  public static final Pattern INCLUDED_TEMPLATE = compile(REGEX_INCLUDED_TEMPLATE);

  /**
   * The compiled version of {@link #CMT_INCLUDED_TEMPLATE}.
   */
  public static final Pattern CMT_INCLUDED_TEMPLATE
      = compile(CMT_REGEX_INCLUDED_TEMPLATE);

  // Only used for syntax error reporting
  static final Pattern DITCH_TAG = compile(REGEX_DITCH_TAG, MULTILINE);

  /**
   * The compiled version of {@link #REGEX_DITCH_BLOCK}.
   */
  public static final Pattern DITCH_BLOCK = compile(REGEX_DITCH_BLOCK, MULTILINE);

  /**
   * The compiled version of {@link #REGEX_PLACEHOLDER}.
   */
  public static final Pattern PLACEHOLDER = compile(REGEX_PLACEHOLDER, MULTILINE);

  static final String PLACEHOLDER_START_END = "<!--%-->";

  /**
   * Prints the regular expressions.
   */
  public static void printAll() {
    System.out.println("VARIABLE ................: " + VARIABLE);
    System.out.println("CMT_VARIABLE ............: " + CMT_VARIABLE);
    System.out.println("INLINE_TEMPLATE .........: " + INLINE_TEMPLATE);
    System.out.println("CMT_INLINE_TEMPLATE .....: " + CMT_INLINE_TEMPLATE);
    System.out.println("INCLUDED_TEMPLATE .......: " + INCLUDED_TEMPLATE);
    System.out.println("CMT_INCLUDED_TEMPLATE ...: " + CMT_INCLUDED_TEMPLATE);
    System.out.println("DITCH_BLOCK .............: " + DITCH_BLOCK);
    System.out.println("PLACEHOLDER .............: " + PLACEHOLDER);
  }

}
