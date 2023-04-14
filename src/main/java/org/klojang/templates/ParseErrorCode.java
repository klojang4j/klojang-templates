package org.klojang.templates;

import org.klojang.util.StringMethods;

import java.util.function.Supplier;

import static org.klojang.util.ArrayMethods.prefix;

/**
 * Symbolic constants for syntax errors and other types of errors in a Klojang
 * template.
 *
 * @author Ayco Holleman
 * @see ParseException#getErrorCode()
 */
public enum ParseErrorCode {

  /**
   * The template contained two or more nested templates with the same name.
   */
  DUPLICATE_TMPL_NAME("Duplicate template name \"%s\""),

  /**
   * The template contained a nested template with an illegal name.
   */
  ILLEGAL_TMPL_NAME("Illegal name for template: \"%s\""),

  /**
   * The template contained a variable with the same name as a nested template.
   */
  VAR_NAME_WITH_TMPL_NAME("Variable cannot have same name as template: \"%s\""),

  /**
   * A template variable had the predefined {@link VarGroup#DEF def:} prefix, but no
   * placeholder value was specified in the template. If you want to use the
   * {@code def:} prefix, you must also provide a placeholder value. For example:
   * {@code ~%def:firstName%John<!--%-->}.
   */
  NO_PLACEHOLDER_DEFINED(
      "Prefix \"def:\" for variable %s not allowed without also specifying placeholder value"),

  /**
   * The path specified in an included template (e&#46;g&#46;
   * {@code ~%%include:/path/to/foo.html%%}) could not be resolved to a readable
   * resource.
   *
   * @see org.klojang.templates.PathResolver#isValidPath(String)
   */
  INVALID_INCLUDE_PATH("Invalid include path: %s"),

  /**
   * The character sequence {@code ~%%begin:} was found, but no terminating
   * percentage-sign ({@code %}) followed.
   */
  BEGIN_TAG_NOT_TERMINATED("Template begin tag (~%%%%begin:) not terminated by \"%%\""),

  /**
   * The character sequence {@code ~%%end:} was found, but no terminating
   * percentage-sign ({@code %}) followed.
   */
  END_TAG_NOT_TERMINATED("Template end tag (~%%%%end:) not terminated by \"%%\""),

  /**
   * The character sequence {@code ~%%include:} was found, but no terminating
   * {@code %%} followed.
   */
  INCLUDE_TAG_NOT_TERMINATED(
      "Template include tag(~%%%%include:) not terminated by \"%%%%\""),

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
  PLACEHOLDER_NOT_CLOSED("Placeholder not closed");

  private static final String ERR_BASE = "Error at line %d, column %d. ";

  private final String format;

  ParseErrorCode(String format) {
    this.format = format;
  }

  // No line and column number included in exception message
  ParseException getTracelessException(Object... msgArgs) {
    return new ParseException(this, String.format(format, msgArgs));
  }

  ParseException getException(String src, int pos, Object... args) {
    return new ParseException(this, createMessage(src, pos, args));
  }

  Supplier<ParseException> getExceptionSupplier(String src,
      int pos,
      Object... msgArgs) {
    return () -> getException(src, pos, msgArgs);
  }

  private String createMessage(String src, int pos, Object... args) {
    int[] x = StringMethods.getLineAndColumn(src, pos);
    return String.format(ERR_BASE + format, prefix(args, x[0] + 1, x[1] + 1));
  }

}
