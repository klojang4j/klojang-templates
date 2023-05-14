package org.klojang.templates;

import org.klojang.path.Path;

import java.util.Set;
import java.util.function.IntFunction;

import static org.klojang.templates.RenderErrorCode.STRINGIFIER_NOT_NULL_RESISTENT;
import static org.klojang.templates.RenderErrorCode.STRINGIFIER_RETURNED_NULL;

final class RenderUtil {

  static String stringify(
        Object value,
        Stringifier stringifier,
        VariablePart part,
        VarGroup adhoc) {
    VarGroup group = part.varGroup().orElse(adhoc);
    if (value == null && group == VarGroup.DEF && part.placeholder() != null) {
      return part.placeholder();
    }
    String s;
    try {
      s = stringifier.stringify(value);
    } catch (NullPointerException e) {
      throw STRINGIFIER_NOT_NULL_RESISTENT.getException(part.name(), group);
    }
    if (s == null) {
      throw STRINGIFIER_RETURNED_NULL.getException(part.name(), group);
    }
    return s;
  }

  static RenderSession ifNotSet(
        SoloSession session,
        Path path,
        IntFunction<Object> valueGenerator,
        VarGroup varGroup) {
    if (!session.state().isSet(path)) {
      setPath(session, path, varGroup, true, valueGenerator);
    }
    return session;
  }

  static void setPath(
        SoloSession session,
        Path path,
        VarGroup group,
        boolean force,
        IntFunction<Object> valueGenerator) {
    if (path.size() == 1) {
      session.setVar(path.segment(0), group, valueGenerator.apply(0));
    } else {
      Template t = session.getNestedTemplate(path.segment(0));
      SoloSession[] children = session.state().getChildSessions(t);
      if (children == null) {
        if (!force) {
          return;
        }
        children = session.state().createChildSessions(t, 1);
      }
      if (path.size() == 2) {
        for (int i = 0; i < children.length; ++i) {
          children[i].setVar(path.segment(1), group, valueGenerator.apply(i));
        }
      } else {
        for (SoloSession child : children) {
          setPath(child, path.shift(), group, force, valueGenerator);
        }
      }
    }
  }

  static void enableRecursive(SoloSession s0, Template t0) {
    s0.state().createChildSessions(t0, 1);
    if (!t0.getNestedTemplates().isEmpty()) {
      SoloSession s = s0.state().getChildSessions(t0)[0];
      t0.getNestedTemplates().forEach(t -> enableRecursive(s, t));
    }
  }

  static void enableRecursive(SoloSession s0, Template t0, Set<String> names) {
    if (names.contains(t0.getName())) {
      s0.state().createChildSessions(t0, 1);
      if (!t0.getNestedTemplates().isEmpty()) {
        SoloSession s = s0.state().getChildSessions(t0)[0];
        t0.getNestedTemplates().forEach(t -> enableRecursive(s, t, names));
      }
    }
  }

}
