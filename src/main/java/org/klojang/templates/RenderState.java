package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.util.collection.IntList;

import java.util.*;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.templates.RenderErrorCode.NO_SUCH_VARIABLE;
import static org.klojang.templates.RenderErrorCode.REPETITION_MISMATCH;
import static org.klojang.templates.TemplateUtils.getFQN;

final class RenderState {

  private static final SoloSession[] ZERO_SESSIONS = new SoloSession[0];

  final SessionConfig config;

  // variables that have not been set yet
  final Set<String> todo;

  final Map<Template, SoloSession[]> sessions;

  // variable occurrence values. A variable may occur multiple times
  // within the same template, and occurrences may end up having
  // different values due to being escaped differently. The keys in
  // the map are the indices of VarPart parts.
  final Map<Integer, Object> varValues;

  RenderState(SessionConfig config) {
    this.config = config;
    int sz = config.template().countNestedTemplates();
    this.sessions = new IdentityHashMap<>(sz);
    this.varValues = new HashMap<>(sz);
    this.todo = new HashSet<>(config.template().getVariables());
  }

  SessionConfig getSessionConfig() {
    return config;
  }

  SoloSession[] createChildSessions(Template t, int repeats) {
    SoloSession[] children;
    if (repeats == 0) {
      children = ZERO_SESSIONS;
    } else {
      children = new SoloSession[repeats];
      for (int i = 0; i < repeats; ++i) {
        children[i] = config.newChildSession(t);
      }
    }
    sessions.put(t, children);
    return children;
  }

  SoloSession[] getOrCreateChildSessions(Template t, int repeats) {
    SoloSession[] children = sessions.get(t);
    if (children == null) {
      if (repeats == 0) {
        children = ZERO_SESSIONS;
      } else {
        children = new SoloSession[repeats];
        for (int i = 0; i < repeats; ++i) {
          children[i] = config.newChildSession(t);
        }
      }
      sessions.put(t, children);
    }
    if (repeats != children.length) {
      throw REPETITION_MISMATCH.getException(getFQN(t), children.length, repeats);
    }
    return children;
  }

  boolean isProcessed(Template template) {
    return sessions.get(template) != null;
  }

  boolean isDisabled(Template template) {
    return isProcessed(template) && sessions.get(template).length == 0;
  }

  SoloSession[] getChildSessions(Template template) {
    return sessions.get(template);
  }

  Object getVar(int partIndex) {
    return varValues.get(partIndex);
  }

  void setVar(int partIndex, Object value) {
    varValues.put(partIndex, value);
  }

  void done(String var) {
    todo.remove(var);
  }

  boolean isFullyPopulated() {
    return ready(this);
  }

  private static boolean ready(RenderState state0) {
    if (state0.todo.size() > 0) {
      return false;
    }
    if (state0.config.template().countNestedTemplates() > state0.sessions.size()) {
      return false;
    }
    return state0
        .sessions
        .values()
        .stream()
        .flatMap(Arrays::stream)
        .map(SoloSession::state)
        .allMatch(RenderState::ready);
  }

  void unset(String varName) {
    IntList il = config.template().variables().get(varName);
    Check.that(il).is(notNull(), NO_SUCH_VARIABLE.getExceptionSupplier(varName));
    todo.add(varName);
    varValues.keySet().removeAll(il.toGenericList());
  }

  void clear(Template tmpl) {
    for (SoloSession session : sessions.get(tmpl)) {
      clear(session);
    }
    sessions.remove(tmpl);
  }

  private static void clear(SoloSession session) {
    session.state().config.template().getVariables().forEach(session::unset);
    session.state().sessions.keySet().forEach(tmpl -> session.state().clear(tmpl));
  }

}
