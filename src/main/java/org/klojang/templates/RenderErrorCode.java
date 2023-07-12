package org.klojang.templates;

import java.util.List;
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
   * A non-existent variable name was specified when calling
   * {@link RenderSession#set(String, Object) RenderSession.set()}, or other methods
   * taking a variable name as argument.
   */
  NO_SUCH_VARIABLE("No such variable: \"%s\""),

  /**
   * A non-existent nested template name was specified.
   */
  NO_SUCH_TEMPLATE("No such nested template: \"%s\""),

  /**
   * The child sessions for a nested template were requested, but none were created
   * yet.
   *
   * @see RenderSession#getChildSessions(String)
   */
  NO_CHILD_SESSIONS("Template %s not instantiated yet"),

  /**
   * An error occurred while retrieving the value for a template variable.
   */
  ACCESS_EXCEPTION("Error while retrieving value for %s: %s"),

  /**
   * {@code RenderSession.enable()} was called on a nested template, but it was not a
   * text-only template.
   *
   * @see RenderSession#enable(String...)
   */
  NOT_TEXT_ONLY("Not a text-only template: %s"),

  /**
   * {@code RenderSession.populate1()} was called on a nested template, but it was
   * not a template with exactly one variable.
   *
   * @see RenderSession#populate1(String, List) 
   */
  NOT_ONE_VAR_TEMPLATE("Not a one-variable template: %s"),

  /**
   * {@code RenderSession.populate2()} was called on a nested template, but it was
   * not a template with exactly two variables.
   *
   * @see RenderSession#populate2(String, List)
   */
  NOT_TWO_VAR_TEMPLATE("Not a two-variable template: %s"),

  /**
   * A stringifier's {@link Stringifier#stringify(Object) stringify} method returned
   * {@code null}, but stringifiers must <i>never</i> return {@code null}.
   */
  STRINGIFIER_RETURNED_NULL(
      "Stringifier for variable %s in variable group %s returned null"),

  /**
   * A stringifier's {@link Stringifier#stringify(Object) stringify} method threw a
   * {@code NullPointerException}, but stringifiers <i>must</i> be capable of
   * stringifying {@code null}.
   */
  STRINGIFIER_NOT_NULL_RESISTANT(
      "Stringifier for variable %s in variable group %s threw NullPointerException"),

  /**
   * A call to {@link RenderSession#repeat(String, int) RenderSession.repeat()} was
   * made, but the number of repetitions had already been fixed, either by a previous
   * call to {@code repeat()}, or implicitly, via the
   * {@link RenderSession#populate(String, Object) popupate()} method.
   */
  REPETITIONS_FIXED("Number of repetitions already fixed for template %s"),

  /**
   * The number of elements in the array or {@code Collection} passed to
   * {@link RenderSession#populate(String, Object)
   * RenderSession.populate()} differed from the number of repetitions fixed by the
   * first call to {@code populate()} or {@code repeat()}.
   */
  REPETITION_MISMATCH("Error while populating %s. When populating a nested template "
      + "in multiple passes you must always provide the same number of source data "
      + "objects. Received %d source data object(s) in first round. Now got %d."),

  /**
   * An unexpected error occurred while rendering the template.
   */
  UNEXPECTED_ERROR(null);

  private String format;

  RenderErrorCode(String format) {
    this.format = format;
  }

  RenderException getException(Object... msgArgs) {
    return new RenderException(this, String.format(format, msgArgs));
  }

  Supplier<RenderException> getExceptionSupplier(Object... msgArgs) {
    return () -> getException(msgArgs);
  }

}
