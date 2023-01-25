package org.klojang.templates;

import org.klojang.templates.x.parse.VariablePart;
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
public sealed class RenderException extends RuntimeException permits
    BadStringifierException {

  static RenderException noStringifierForGroup(VariablePart part, VarGroup group) {
    String msg = "Cannot render variable %s at line %d, column %d. The specified "
        + "variable group (%s) is not associated with a stringifier.";
    String fmt = "No stringifier associated with variable group \"%s\"";
    return new RenderException(format(fmt, group));
  }

  static Supplier<RenderException> noSuchVariable(Template t, String var) {
    String fqn = TemplateUtils.getFQName(t, var);
    return () -> new RenderException(format(ERR_NO_SUCH_VARIABLE, fqn));
  }

  static Supplier<RenderException> noSuchTemplate(Template t, String name) {
    return () -> new RenderException(
        format("no such template: \"%s\"", TemplateUtils.getFQName(t, name)));
  }

  static Supplier<RenderException> alreadySet(Template t, String var) {
    String fqn = TemplateUtils.getFQName(t, var);
    String fmt = "Variable already set: \"%s\"";
    return () -> new RenderException(format(fmt, fqn));
  }

  static RenderException repetitionMismatch(
      Template t, RenderSession[] sessions, int repeats) {
    String fmt =
        "When populating a repeating template in multiple passes you must always "
            + "provide the same number of source data objects. Received %d source "
            + "data objects in first round. Now got %d";
    String fqn = TemplateUtils.getFQName(t);
    return new RenderException(format(fmt, fqn, sessions.length, repeats));
  }

  public static Function<String, RenderException> frozenSession() {
    return s -> new RenderException("Session frozen after rendering");
  }

  static Supplier<RenderException> frozen() {
    return () -> new RenderException("Session frozen after rendering");
  }

  public static RenderException multiPassNotAllowed(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt =
        "show() can be called only once per text-only template (template specified: \"%s\")";
    return new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> textOnly(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a text-only template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Supplier<RenderException> notTextOnly(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a text-only template: %s";
    return () -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> notMonoTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a one-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> notTupleTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Not a two-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> missingSourceData(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "Source data must not be null for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static RenderException accessException(
      Template t, String var, RuntimeException exc, Object data, Accessor<?> acc) {
    String fqn = TemplateUtils.getFQName(t, var);
    String fmt0 = "An exception occurred while retrieving a value for %s from %s using %s: %s.";
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

  static Function<String, RenderException> noChildSessionsYet(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "No child sessions yet for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> illegalValue(String name,
      Object value) {
    String fmt = "Illegal value for \"%s\": %s";
    return s -> new RenderException(format(fmt, name, value));
  }

  public RenderException(String message) {
    super(message);
  }

}
