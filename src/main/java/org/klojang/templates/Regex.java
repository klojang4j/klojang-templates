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
   * name. Names must consists of one or more letters, digits, underscores or
   * hyphens. If the main template is going to be populated with beans or records
   * (rather than maps), both template names and variable names are in practice more
   * constrained: they need to be valid Java identifiers.
   */
  public static final String REGEX_NAME = "([a-zA-Z0-9_\\-]+)";

  /**
   * <p>Regular expression for path strings. Variable names are paths through an
   * object graph. For example: {@code ~%company.address.city%}. This variable would
   * map to the {@code city} property of the {@code Address} object within the
   * {@code Company} object within the object that you populate the template with.
   * Each of the name segments must match {@link #REGEX_NAME}. In practice, you are
   * more likely to use nested and doubly-nested templates, and then use simple
   * variable names at the appropriate nesting level (e.g. {@code ~%city%}).
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
   * <p>Regular expression for a template variable that is placed between HTML
   * comments. For example: {@code <!-- ~%firstName% -->}. This is rendered in
   * exactly the same way as {@code ~%firstName%}. However, when using HTML comments,
   * the raw, unprocessed template still renders nicely in a browser &#8212; without
   * "odd" tilde-percent sequences spoiling the HTML page. This works even better if
   * you also provide a placeholder value, as in the following example:
   * {@code <!-- ~%firstName% -->John<!--%-->}. Now, when the browser renders the raw
   * template, it will display the string "John", because it is outside any HTML
   * comments. But when <i><b>Klojang Templates</b></i> renders the template, "John"
   * will have disappeared, and the only thing that remains is the value of
   * {@code firstName}.
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
   * <p>The space characters surrounding the variable (as in
   * {@code <!-- ~%firstName% -->}) are optional. You may also omit them
   * ({@code <!--~%firstName%-->}), but you cannot insert multiple spaces or any
   * other characters.
   *
   * @see VarGroup#DEF
   * @see #REGEX_PLACEHOLDER
   */
  public static final String REGEX_CMT_VARIABLE
      = "<!-- ?"
      + REGEX_VARIABLE
      + " ?-->((.*?)<!--%-->)?";

  /**
   * Regular expression for inline templates.
   */
  public static final String REGEX_INLINE_TEMPLATE
      = "(~%%begin:" + REGEX_NAME + "%)"
      + "(.*?)"
      + "(~%%end:\\2%)";

  // Used only for syntax error reporting:
  static final String REGEX_INLINE_TEMPLATE_BEGIN = "~%%begin:" + REGEX_PATH + "%";

  static final String REGEX_INLINE_TEMPLATE_END = "~%%end:" + REGEX_PATH + "%";

  /**
   * Regular expression for inline templates of which the begin and end tags are
   * placed inside HTML comments:
   *
   * <blockquote><pre>{@code
   * <!-- ~%%begin:foo% -->
   *   <p>bar</p>
   * <!-- ~%%end:foo% -->
   * }</pre></blockquote>
   *
   * <p>The space characters surrounding {@code ~%%begin:foo%} and
   * {@code ~%%end:foo%} are optional. You may also omit them, but you cannot insert
   * multiple spaces or any other characters.
   */
  public static final String REGEX_CMT_TAGS_INLINE_TEMPLATE
      = "(<!-- ?~%%begin:" + REGEX_NAME + "% ?-->)"
      + "(.*?)"
      + "(<!-- ?~%%end:\\2% ?-->)";

  /**
   * Regular expression for inline templates that are placed entirely inside HTML
   * comments:
   *
   * <blockquote><pre>{@code
   * <!-- ~%%begin:foo%
   *   <p>bar</p>
   * ~%%end:foo% -->
   * }</pre></blockquote>
   *
   * <p>The space characters before {@code ~%%begin:foo%} and after
   * {@code ~%%end:foo%} are optional. optional. You may also omit them, you cannot
   * insert multiple spaces or any other characters.
   */
  public static final String REGEX_CMT_ALL_INLINE_TEMPLATE
      = "(<!-- ?~%%begin:" + REGEX_NAME + "%)"
      + "(.*?)"
      + "(~%%end:\\2% ?-->)";

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
   * Regular expression for an included template that is placed between HTML
   * comments. For example: {@code <!-- ~%%include:/path/to/foo.html%% -->}.
   */
  public static final String REGEX_CMT_INCLUDED_TEMPLATE
      = "<!-- ?"
      + REGEX_INCLUDED_TEMPLATE
      + " ?-->";

  // Used only for syntax error detection:
  static final String REGEX_DITCH_TAG = "<!--%%-->";

  /**
   * Regular expression for ditch blocks. A ditch block is a pair of
   * {@code <!--%%-->} tags and any text between them. A ditch block is the Klojang
   * Templates equivalent of an HTML or Java comment. Ditch blocks cannot be nested
   * inside any syntactical construct provided by Klojang Templates.
   */
  public static final String REGEX_DITCH_BLOCK = "<!--%%-->(.*?)<!--%%-->";

  /**
   * Regular expression for placeholders. A placeholder is a pair of {@code <!--%-->}
   * tags and any text between them. When a template is rendered by Klojang
   * Templates, these tokens, and any text between them are erased from the template.
   * However, since {@code <!--%-->} is a self-closed HTML comment, a browser would
   * display what is between these tokens when rendering the raw, unprocessed
   * template.
   */
  public static final String REGEX_PLACEHOLDER = "<!--%-->(.*?)<!--%-->";

  static final Pattern VARIABLE = compile(REGEX_VARIABLE);

  static final Pattern CMT_VARIABLE = compile(REGEX_CMT_VARIABLE);

  static final Pattern INLINE_TEMPLATE = compile(REGEX_INLINE_TEMPLATE, MULTILINE);

  // Only used for syntax error reporting
  static final Pattern INLINE_TEMPLATE_BEGIN =
      compile(REGEX_INLINE_TEMPLATE_BEGIN);

  static final Pattern INLINE_TEMPLATE_END = compile(REGEX_INLINE_TEMPLATE_END);

  static final Pattern CMT_TAGS_INLINE_TEMPLATE =
      compile(REGEX_CMT_TAGS_INLINE_TEMPLATE, MULTILINE);

  static final Pattern CMT_ALL_INLINE_TEMPLATE =
      compile(REGEX_CMT_ALL_INLINE_TEMPLATE, MULTILINE);

  static final Pattern INCLUDED_TEMPLATE = compile(REGEX_INCLUDED_TEMPLATE);

  static final Pattern CMT_INCLUDED_TEMPLATE
      = compile(REGEX_CMT_INCLUDED_TEMPLATE);

  // Only used for syntax error reporting
  static final Pattern DITCH_TAG = compile(REGEX_DITCH_TAG, MULTILINE);

  /**
   * The compiled version of {@link #REGEX_DITCH_BLOCK}.
   */
  static final Pattern DITCH_BLOCK = compile(REGEX_DITCH_BLOCK, MULTILINE);

  /**
   * The compiled version of {@link #REGEX_PLACEHOLDER}.
   */
  static final Pattern PLACEHOLDER = compile(REGEX_PLACEHOLDER, MULTILINE);

  static final String PLACEHOLDER_START_END = "<!--%-->";

  /**
   * Prints the regular expressions.
   */
  public static void printAll() {
    System.out.println("VARIABLE ................: " + VARIABLE);
    System.out.println("CMT_VARIABLE ............: " + CMT_VARIABLE);
    System.out.println("INLINE_TEMPLATE .........: " + INLINE_TEMPLATE);
    System.out.println("CMT_TAGS_INLINE_TEMPLATE : " + CMT_TAGS_INLINE_TEMPLATE);
    System.out.println("CMT_ALL_INLINE_TEMPLATE .: " + CMT_ALL_INLINE_TEMPLATE);
    System.out.println("INCLUDED_TEMPLATE .......: " + INCLUDED_TEMPLATE);
    System.out.println("CMT_INCLUDED_TEMPLATE ...: " + CMT_INCLUDED_TEMPLATE);
    System.out.println("DITCH_BLOCK .............: " + DITCH_BLOCK);
    System.out.println("PLACEHOLDER .............: " + PLACEHOLDER);
  }

  private Regex() {
    throw new UnsupportedOperationException();
  }

}
