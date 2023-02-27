package org.klojang.templates;

import java.util.*;

import static org.klojang.templates.RenderErrorCode.REPETITION_MISMATCH;
import static org.klojang.templates.TemplateUtils.getFQName;
import static org.klojang.util.ObjectMethods.ifNotNull;

final class RenderState {

  private static final RenderSession[] ZERO_SESSIONS = new RenderSession[0];

  private final SessionConfig config;
  private final Set<String> todo; // variables that have not been set yet
  private final Map<Template, RenderSession[]> sessions;
  private final Map<Integer, Object> varValues;

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

  RenderSession getOrCreateChildSession(Template t) {
    return getOrCreateChildSessions(t, 1)[0];
  }

  RenderSession[] getOrCreateChildSessions(Template t, int repeats) {
    RenderSession[] children = sessions.get(t);
    if (children == null) {
      if (repeats == 0) {
        children = ZERO_SESSIONS;
      } else {
        children = new RenderSession[repeats];
        for (int i = 0; i < repeats; ++i) {
          children[i] = config.newChildSession(t);
        }
      }
      sessions.put(t, children);
    }
    if (repeats != children.length) {
      throw REPETITION_MISMATCH.getException(children.length, getFQName(t), repeats);
    }
    return children;
  }

  boolean isProcessed(Template template) {
    return sessions.get(template) != null;
  }

  boolean isDisabled(Template template) {
    return ifNotNull(sessions.get(template), x -> x.length == 0, false);
  }

  RenderSession[] getChildSessions(Template template) {
    return sessions.get(template);
  }

  Object getVar(int partIndex) {
    return varValues.get(partIndex);
  }

  void setVar(int partIndex, Object value) {
    varValues.put(partIndex, value);
  }

  boolean isSet(String var) {
    return !todo.contains(var);
  }

  void done(String var) {
    todo.remove(var);
  }

  private static void collectUnsetVars(RenderState state0, ArrayList<String> names) {
    Template t = state0.config.template();
    state0.todo.stream().map(var -> getFQName(t, var)).forEach(names::add);
    state0
        .sessions
        .values()
        .stream()
        .flatMap(Arrays::stream)
        .map(RenderSession::getState)
        .forEach(state -> collectUnsetVars(state, names));
  }

  boolean isFullyPopulated() {
    return ready(this);
  }

  private static boolean ready(RenderState state0) {
    if (state0.todo.size() > 0) {
      return false;
    }
    return state0
        .sessions
        .values()
        .stream()
        .flatMap(Arrays::stream)
        .map(RenderSession::getState)
        .allMatch(RenderState::ready);
  }

}
