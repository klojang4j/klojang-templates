package org.klojang.templates;

import java.util.function.Supplier;

/**
 * Symbolic constants for errors that may occur while populating or rendering a
 * template.
 *
 * @author Ayco Holleman
 * @see RenderException#getErrorCode()
 */
public enum RenderErrorCode {

  /**
   * A non-existent variable name was specified while populating a template.
   */
  NO_SUCH_VARIABLE("No such variable: %s"),

  /**
   * A non-existent nested template name was specified while populating a template.
   */
  NO_SUCH_TEMPLATE("No such nested template: %s"),

  /**
   * The child sessions for a nested template were requested, but the nested template
   * had not been populated yet.
   *
   * @see RenderSession#getChildSessions(String)
   */
  NO_CHILD_SESSIONS_YET("No child sessions yet for template %s"),

  /**
   * An error occurred while retrieving the value for a template variable.
   */
  ACCESS_EXCEPTION("Error while retrieving value for %s: %s"),

  /**
   * One or more template variables were placed in a
   * {@linkplain VarGroup variable group} for which no {@link Stringifier} was
   * defined.
   */
  VAR_GROUP_WITHOUT_STRINGIFIER(
      "Error while processing ~%%1$s:%2$s%: no stringifier associated with variable group \"%1$s\""),

  /**
   * {@code RenderSession.show()} was called on a nested template, but it was not a
   * text-only template.
   */
  NOT_TEXT_ONLY("Not a text-only template: %s"),

  /**
   * {@code RenderSession.populate1()} was called on a nested template, but it was
   * not a template with exactly one variable and zero doubly-nested templates.
   */
  NOT_ONE_VAR_TEMPLATE("Not a one-variable template: %s"),

  /**
   * {@code RenderSession.populate2()} was called on a nested template, but it was
   * not a template with exactly two variables and zero doubly-nested templates.
   */
  NOT_TWO_VAR_TEMPLATE("Not a two-variable template: %s"),

  /**
   * A stringifier's {@link Stringifier#stringify(Object) stringify} method returned
   * {@code null}, which it should never do.
   */
  STRINGIFIER_RETURNED_NULL("Stringifier for %s must not return null"),

  /**
   * A stringifier's {@link Stringifier#stringify(Object) stringify} method threw a
   * {@code NullPointerException}, but stringifiers <i>must</i> be capable of
   * stringifying {@code null}.
   */
  STRINGIFIER_NOT_NULL_RESISTENT("Stringifier for %s threw NullPointerException"),

  /**
   * A nested template was populated in multiple passes, but with a different number
   * of source data objects.
   */
  REPETITION_MISMATCH("Error while populating %s. When populating a nested template "
      + "in multiple passes you must always provide the same number of source data "
      + "objects. Received %d source data object(s) in first round. Now got %d.");

  private String format;

  RenderErrorCode(String format) {
    this.format = format;
  }

  RenderException getException(Object... msgArgs) {
    return new RenderException(this, String.format(format, msgArgs));
  }

  Supplier<RenderException> getExceptionSupplier(Object... msgArgs) {
    return () -> getException(this, msgArgs);
  }

}
