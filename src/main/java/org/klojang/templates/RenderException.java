package org.klojang.templates;

import org.klojang.util.ExceptionMethods;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
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
    String fmt = "variable %s has already been set";
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

  static Function<String, RenderException> frozenSession() {
    return s -> new RenderException("Session frozen after rendering");
  }

  static Function<String, RenderException> isTextOnly(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "not a text-only template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> isOneVarTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "not a one-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> isTwoVarTemplate(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "not a two-variable template: %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static Function<String, RenderException> missingSourceData(Template t) {
    String fqn = TemplateUtils.getFQName(t);
    String fmt = "source data must not be null for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  static RenderException accessException(
      Template t, String var, RuntimeException exc, Object data, Accessor<?> acc) {
    String fqn = TemplateUtils.getFQName(t, var);
    String fmt0 = "Error while retrieving value for %s from %s using %s: %s.";
    String fmt1;
    String excMsg;
    if (!acc.getClass()
        .getName()
        .startsWith(Template.class.getPackageName())) {
      String pkg = implode(acc.getClass().getPackageName().split("\\."), ".", 2);
      excMsg = ExceptionMethods.getDetailedMessage(exc, pkg);
      fmt1 = fmt0
          + " Make sure the accessor **never** throws an exception if it cannot"
          + " provide a value for a variable. Return Accessor.UNDEFINED instead.";
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
    String fmt = "no child sessions yet for template %s";
    return s -> new RenderException(format(fmt, fqn));
  }

  public RenderException(String message) {
    super(message);
  }

}
