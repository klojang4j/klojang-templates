package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.path.Path;
import org.klojang.util.collection.IntList;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.RenderErrorCode.*;
import static org.klojang.templates.TemplateUtils.getAllVariableFQNames;
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
    sz = config.template().countVariableOccurrences();
    this.varValues = HashMap.newHashMap(sz);
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

  List<String> getAllUnsetVariables(boolean relative) {
    if (relative) {
      ArrayList<Path> paths = new ArrayList<>();
      collectUnsetVariables(this, paths, Path.empty());
      return paths.stream().map(Path::toString).collect(toList());
    }
    ArrayList<String> vars = new ArrayList<>();
    collectUnsetVariables(this, vars);
    return vars;
  }

  private static void collectUnsetVariables(RenderState state,
      ArrayList<String> vars) {
    Template myTmpl = state.config.template();
    state.todo.stream().map(var -> getFQN(myTmpl, var)).forEach(vars::add);
    myTmpl.getNestedTemplates().forEach(t -> {
      if (state.sessions.containsKey(t)) {
        Arrays.stream(state.sessions.get(t))
            .limit(1)
            .map(SoloSession::state)
            .forEach(s -> collectUnsetVariables(s, vars));
      } else {
        vars.addAll(getAllVariableFQNames(t));
      }
    });
  }

  private static void collectUnsetVariables(RenderState state0,
      ArrayList<Path> vars,
      Path path) {
    Template myTmpl = state0.config.template();
    state0.todo.stream().map(path::append).forEach(vars::add);
    myTmpl.getNestedTemplates().forEach(t -> {
      if (state0.sessions.containsKey(t)) {
        Arrays.stream(state0.sessions.get(t))
            .limit(1)
            .map(SoloSession::state)
            .forEach(s -> collectUnsetVariables(s, vars, path.append(t.getName())));
      } else {
        TemplateUtils.collectFQNs(t, vars, path.append(t.getName()));
      }
    });
  }

  boolean allSet() {
    return ready(this);
  }

  private static boolean ready(RenderState state) {
    if (state.todo.size() > 0) {
      return false;
    }
    if (state.config.template().countNestedTemplates() > state.sessions.size()) {
      return false;
    }
    return state.sessions.values().stream()
        .flatMap(Arrays::stream)
        .map(SoloSession::state)
        .allMatch(RenderState::ready);
  }

  void unset(Path path) {
    unset(this, path);
  }

  private static void unset(RenderState state, Path path) {
    String name = path.segment(0);
    if (path.size() == 1) {
      IntList occurrences = state.config.template().variables().get(name);
      Check.that(occurrences).is(notNull(),
          NO_SUCH_VARIABLE.getExceptionSupplier(name));
      state.todo.add(name);
      occurrences.toGenericList().forEach(state.varValues.keySet()::remove);
    } else {
      Template tmpl = state.config.template();
      Check.that(name).is(in(), tmpl.getNestedTemplateNames(),
          NO_SUCH_TEMPLATE.getExceptionSupplier(getFQN(tmpl, name)));
      Template nested = tmpl.getNestedTemplate(name);
      SoloSession[] childSessions = state.sessions.get(nested);
      if (childSessions != null) {
        Arrays.stream(childSessions).forEach(s -> unset(s.state(), path.shift()));
      }
    }
  }

  void clear(Template tmpl) {
    for (SoloSession session : sessions.get(tmpl)) {
      clear(session);
    }
    sessions.remove(tmpl);
  }

  private void clear(SoloSession session) {
    RenderState state = session.state();
    state.varValues.clear();
    state.todo.addAll(state.config.template().getVariables());
    state.sessions.values().stream().flatMap(Arrays::stream).forEach(this::clear);
    state.sessions.clear();
  }

  boolean isSet(Path path) {
    return isSet(this, path);
  }

  private static boolean isSet(RenderState state, Path path) {
    String name = path.segment(0);
    if (path.size() == 1) {
      if (state.todo.contains(name)) {
        return false;
      }
      Template tmpl = state.config.template();
      Check.that(name).is(keyIn(), tmpl.variables(),
          NO_SUCH_VARIABLE.getExceptionSupplier(getFQN(tmpl, name)));
      return true;
    }
    Template tmpl = state.config.template();
    Check.that(name).is(in(), tmpl.getNestedTemplateNames(),
        NO_SUCH_TEMPLATE.getExceptionSupplier(getFQN(tmpl, name)));
    Template nested = tmpl.getNestedTemplate(name);
    SoloSession[] childSessions = state.sessions.get(nested);
    if (childSessions == null) {
      return false;
    } else if (childSessions.length == 0) {
      return true;
    }
    return isSet(childSessions[0].state(), path.shift());
  }

}
