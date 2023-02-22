package org.klojang.templates;

import java.util.*;

import static org.klojang.check.CommonChecks.notNull;
import static org.klojang.templates.RenderException.repetitionMismatch;
import static org.klojang.templates.TemplateUtils.getFQName;
import static org.klojang.util.ObjectMethods.ifNotNull;

final class RenderState {

  private static final RenderSession[] ZERO_SESSIONS = new RenderSession[0];
  private static final RenderSession[] ONE_SESSION = new RenderSession[1];

  private final SessionConfig config;
  private final Set<String> todo; // variables that have not been set yet
  private final Map<Template, RenderSession[]> sessions;
  private final Map<Integer, Object> varValues;

  private boolean frozen;

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
    } else if (children.length != repeats) {
      throw repetitionMismatch(config.template(), children, repeats);
    }
    return children;
  }


  boolean isProcessed(Template template) {
    return sessions.get(template) != null;
  }

  boolean isEnabled(Template template) {
    return ifNotNull(sessions.get(template), x -> x.length > 0, false);
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

  void setVar(int partIndex, String[] value) {
    varValues.put(partIndex, value);
  }

  void setVar(int partIndex, Renderable value) {
    varValues.put(partIndex, value);
  }

  boolean isSet(String var) {
    return !todo.contains(var);
  }

  void done(String var) {
    todo.remove(var);
  }

  boolean isFrozen() {
    return frozen;
  }

  void freeze() {
    deepFreeze(this);
  }

  private static void deepFreeze(RenderState state0) {
    state0.frozen = true;
    state0
        .sessions
        .values()
        .stream()
        .flatMap(Arrays::stream)
        .filter(notNull()) // text-only null sessions - don't need freezing anyhow
        .map(RenderSession::getState)
        .forEach(RenderState::deepFreeze);
  }

  List<String> getUnsetVars() {
    ArrayList<String> names = new ArrayList<>();
    collectUnsetVars(this, names);
    return names;
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

  private static RenderSession[] createTextOnlySessions(int repeats) {
    switch (repeats) {
      case 0:
        return ZERO_SESSIONS;
      case 1:
        return ONE_SESSION;
      default:
        return new RenderSession[repeats];
    }
  }

}
