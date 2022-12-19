package org.klojang.templates;

import org.klojang.util.ExceptionMethods;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_TEMPLATE;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_VARIABLE;
import static org.klojang.util.ArrayMethods.implode;

/**
 * Thrown from a {@link RenderSession} under various circumstances.
 *
 * @author Ayco Holleman
 */
public class RenderException extends KlojangException {

  /**
   * Thrown when specifying a
   * {@link RenderSession#set(String, Object, VarGroup) default group} for which no
   * stringifier has been
   * {@link StringifierRegistry.Builder#registerByGroup(Stringifier, String...)
   * defined}.
   */
  public static RenderException noStringifierForGroup(VarGroup vg) {
    String fmt = "No stringifier associated with variable group \"%s\"";
    return new RenderException(format(fmt, vg));
  }

  /**
   * Thrown when specifying a non-existent variable name.
   */
  public static Supplier<RenderException> noSuchVariable(Template t, String var) {
    String fqn = TemplateUtils.getFQName(t, var);
    return () -> new RenderException(format(ERR_NO_SUCH_VARIABLE, fqn));
  }

  /**
   * Thrown when specifying a non-existent template name.
   */
  public static Function<String, RenderException> noSuchTemplate(Template t,
      String name) {
    String fqn = TemplateUtils.getFQName(t, name);
    return s -> new RenderException(format(ERR_NO_SUCH_TEMPLATE, fqn));
  }

  /**
   * Thrown if you attempt to set a variable more than once.
   */
  public static Function<String, RenderException> alreadySet(Template t,
      String var) {
    String fqn = TemplateUtils.getFQName(t, var);
    String fmt = "Variable already set: \"%s\"";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   * Thrown during multi-pass population of a template if, in the second pass, you
   * don't specify the same number of source data objects as in the first pass. The
   * {@code List} or array of source data objects you specify in the first call to
   * {@code populate} determines how often the template is going to repeat itself.
   * Obviously that fixes it for subsequent calls to {@code populate}.
   */
  public static RenderException repetitionMismatch(
      Template t, RenderSession[] sessions, int repeats) {
    String fmt =
        "Template \"%s\" has already been partially populated, but with a different number "
            + "of source data objects. When populating a template in mulitple passes you must "
            + "always provide the same number of source data objects. Received %d source data "
            + "objects in first round. Now got %d";
    String fqn = TemplateUtils.getFQName(t);
    return new RenderException(format(fmt, fqn, sessions.length, repeats));
  }

  /**
   * Thrown when attempting to populate a template after it has been rendered.
   */
  public static Function<String, RenderException> frozenSession() {
    return s -> new RenderException("Session frozen after rendering");
  }

  /**
   *
   */
  public static RenderException multiPassNotAllowed(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt =
        "show() can be called only once per text-only template (template specified: \"%s\")";
    return new RenderException(format(fmt, fqn));
  }

  /**
   *
   */
  public static Function<String, RenderException> notTextOnly(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a text-only template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   * Thrown if you call {@link RenderSession#populateWithValue(String, Object)} for a
   * nested template that does not contain exactly one variable and zero
   * doubly-nested templates.
   */
  public static Function<String, RenderException> notMonoTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a one-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   *
   */
  public static Function<String, RenderException> notTupleTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a two-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   * Thrown when the source data object for a non-text-only template is null or, if
   * the source data object is a list or array, contains one or more null elements.
   */
  public static Function<String, RenderException> missingSourceData(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Source data must not be null for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   * Thrown when the source data object for a non-text-only template is null or, if
   * the source data object is a list or array, contains one or more null elements.
   */
  public static RenderException accessException(
      Template t, String var, RuntimeException exc, Object data, Accessor<?> acc) {
    String fqn = TemplateUtils.getFQName(t, var);
    String fmt0 = "An exception occured while retrieving value for %s from %s using %s: %s.";
    String fmt1;
    String excMsg;
    if (!acc.getClass()
        .getName()
        .startsWith(KlojangException.class.getPackageName())) {
      String pkg = implode(acc.getClass().getPackageName().split("\\."), ".", 2);
      excMsg = ExceptionMethods.getDetailedMessage(exc, pkg);
      fmt1 =
          fmt0
              + " Make sure the accessor returns Accessor.UNDEFINED and does **not** accidentally"
              + " cause a RuntimeException to be thrown for variables for which it cannot provide"
              + " a value.";
    } else {
      fmt1 = fmt0;
      excMsg = exc.toString();
    }
    return new RenderException(
        format(fmt1,
            fqn,
            data.getClass().getName(),
            acc.getClass().getName(),
            excMsg));
  }

  /**
   * Thrown if you {@link RenderSession#getChildSessions(String) request} the child
   * sessions created for the specified template, but no child sessions have been
   * created yet.
   */
  public static Function<String, RenderException> noChildSessionsYet(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "No child sessions yet for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  /**
   * Generic error condition, usually akin to an {@link IllegalArgumentException}.
   */
  public static Function<String, RenderException> illegalValue(String name,
      Object value) {
    String fmt = "Illegal value for \"%s\": %s";
    return s -> new RenderException(format(fmt, name, value));
  }

  public RenderException(String message) {
    super(message);
  }

}
