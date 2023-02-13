package org.klojang.templates.x.parse;

import org.klojang.check.Check;
import org.klojang.check.IntCheck;
import org.klojang.check.ObjectCheck;
import org.klojang.templates.ParseException;
import org.klojang.util.StringMethods;

import java.util.function.Function;

import static org.klojang.util.ArrayMethods.prefix;

/**
 * Symbolic constants for syntax errors and other types of errors in a Klojang
 * template.
 *
 * @author Ayco Holleman
 */
public enum ParseError {

  /**
   * The path specified in an included template
   * ({@code ~%%include:/path/to/template.html%%}) did not resolve to an actual
   * physical resource.
   *
   * @see org.klojang.templates.PathResolver#isValidPath(String)
   */
  INVALID_INCLUDE_PATH("Invalid include path: %s"),

  /**
   * The template contained two or more nested templates with the same name.
   */
  DUPLICATE_TMPL_NAME("Duplicate template name \"%s\""),

  /**
   * The template contained a variable with the same name as a nested template.
   */
  VAR_NAME_WITH_TMPL_NAME("Variable cannot have same name as template: \"%s\""),

  /**
   * The character sequence {@code ~%%begin:} was found, but no terminating
   * percentage-sign ({@code %}) followed.
   */
  BEGIN_TAG_NOT_TERMINATED("Template \"begin\" tag not terminated"),

  /**
   * The character sequence {@code ~%%end:} was found, but no terminating
   * percentage-sign ({@code %}) followed.
   */
  END_TAG_NOT_TERMINATED("Template \"end\" tag not terminated"),

  /**
   * The character sequence {@code ~%%include:} was found, but no terminating
   * {@code %%} followed.
   */
  INCLUDE_TAG_NOT_TERMINATED("Template \"include\" tag not terminated"),

  /**
   * An inline template did not close properly. For example ~%%begin:foo% was found,
   * but ~%%end:foo% was not.
   */
  MISSING_END_TAG("Missing end tag for inline template \"%s\""),

  /**
   * A dangling end-of-template was found. For example ~%%end:foo% was found, but not
   * preceded by ~%%begin:foo%.
   */
  DANGLING_END_TAG("Dangling end tag with template name \"%s\""),

  /**
   * A ditch block was not closed. (There was an uneven number of {@code <!--%%-->}
   * tokens.)
   */
  DITCH_BLOCK_NOT_CLOSED("Ditch block not closed"),

  /**
   * A placeholder block was not closed. (There was an uneven number of
   * {@code <!--%-->} tokens.)
   */
  PLACEHOLDER_NOT_CLOSED("Placeholder not closed"),

  /**
   * An unexpected exception occurred while parsing the template.
   */
  UNEXPECTED("Unexpected error while parsing template");

  private static final String ERR_BASE = "Error at line %d, column %d. ";

  private final String format;

  ParseError(String format) {
    this.format = ERR_BASE + format;
  }

  public String asMessage(String src, int pos, Object... args) {
    int[] x = StringMethods.getLineAndColumn(src, pos);
    return String.format(format, prefix(args, x[0] + 1, x[1] + 1));
  }

  public ParseException asException(String src, int pos, Object... args) {
    return new ParseException(this, asMessage(src, pos, args));
  }

  public Function<String, ParseException> asExceptionProvider(String src,
      int pos,
      Object... args) {
    return s -> asException(src, pos, args);
  }

  public IntCheck<ParseException> checkInt(int arg,
      String src,
      int pos,
      Object... args) {
    return Check.on(asExceptionProvider(src, pos, args), arg);
  }

  public <T> ObjectCheck<T, ParseException> check(T arg,
      String src,
      int pos,
      Object... args) {
    return Check.on(asExceptionProvider(src, pos, args), arg);
  }
}
