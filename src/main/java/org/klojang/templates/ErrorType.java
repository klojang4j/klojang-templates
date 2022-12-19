package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.IntCheck;
import org.klojang.check.ObjectCheck;
import org.klojang.util.StringMethods;

import java.util.function.Function;

import static org.klojang.util.ArrayMethods.prefix;

/**
 * Symbolic constants for all possible errors in a Klojang template.
 *
 * @author Ayco Holleman
 */
public enum ErrorType {

  /**
   * Illegal whitespace-only variable name.
   */
  EMPTY_VAR_NAME("Empty variable name"),

  /**
   * Illegal whitespace-only include path.
   */
  EMPTY_INCLUDE_PATH("Empty include path"),

  /**
   * Invalid include path.
   */
  INVALID_INCLUDE_PATH("Invalid include path: %s"),

  /**
   * Illegal whitespace-only template name.
   */
  EMPTY_TMPL_NAME("Empty template name"),

  /**
   * Duplicate template name.
   */
  DUPLICATE_TMPL_NAME("Duplicate template name \"%s\""),

  /**
   * {@link Template#fromString(String) Template.fromString} was called without the
   * Class argument and hence included templates cannot be loaded using {@code
   * Class.getResourceAsStream}.
   */
  MISSING_CLASS_OBJECT(
      "No Class object provided. Cannot call getResourceAsStream(\"%s\")"),

  /**
   * Variable cannot have same name as template.
   */
  VAR_NAME_WITH_TMPL_NAME("Variable cannot have same name as template: \"%s\""),

  /**
   * The character sequence {@code ~%%begin:} was found, but no terminating {@code %}
   * followed.
   */
  BEGIN_TAG_NOT_TERMINATED("Template \"begin\" tag not terminated"),

  /**
   * The character sequence {@code ~%%end:} was found, but no terminating {@code %}
   * followed.
   */
  END_TAG_NOT_TERMINATED("Template \"end\" tag not terminated"),

  /**
   * The character sequence {@code ~%%include:} was found, but no terminating {@code
   * %} followed.
   */
  INCLUDE_TAG_NOT_TERMINATED("Template \"include\" tag not terminated"),

  /**
   * An inline template template did not close properly. For example ~%%begin:foo%
   * was found, but ~%%end:foo% was not.
   */
  MISSING_END_TAG("Missing end tag for inline template \"%s\""),

  /**
   * A dangling end-of-template was found. For example ~%%end:foo% was found, but not
   * preced by ~%%begin:foo%.
   */
  DANGLING_END_TAG("Dangling end tag with template name \"%s\""),

  /**
   * A ditch block was not closed. (There was an uneven number of {@code <!--%%-->}
   * tokens.)
   */
  DITCH_BLOCK_NOT_CLOSED("Ditch block not closed"),

  /**
   * A placeholder block was not closed. (There was an uneven number of {@code
   * <!--%-->} tokens.)
   */
  PLACEHOLDER_NOT_CLOSED("Placeholder block not closed");

  private static final String ERR_BASE = "Error at line %d, column %d. ";

  private String format;

  private ErrorType(String format) {
    this.format = ERR_BASE + format;
  }

  String asMessage(String src, int pos, Object... args) {
    int[] x = StringMethods.getLineAndColumn(src, pos);
    return String.format(format, prefix(args, x[0] + 1, x[1] + 1));
  }

  ParseException asException(String src, int pos, Object... args) {
    return new ParseException(asMessage(src, pos, args));
  }

  Function<String, ParseException> asExceptionProvider(String src,
      int pos,
      Object... args) {
    return s -> asException(src, pos, args);
  }

  IntCheck<ParseException> checkInt(int arg, String src, int pos, Object... args) {
    return Check.on(asExceptionProvider(src, pos, args), arg);
  }

  <T> ObjectCheck<T, ParseException> check(T arg,
      String src,
      int pos,
      Object... args) {
    return Check.on(asExceptionProvider(src, pos, args), arg);
  }
}
