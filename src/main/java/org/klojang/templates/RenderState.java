package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.util.Path;
import org.klojang.util.collection.IntList;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.RenderErrorCode.*;
import static org.klojang.templates.TemplateUtils.getFQN;
import static org.klojang.util.ObjectMethods.ifNotNull;
import static org.klojang.util.ObjectMethods.nullToEmpty;



final class RenderState {

  private static final SoloSession[] ZERO_SESSIONS = new SoloSession[0];

  private final SessionConfig config;

  // variables that have not been set yet
  private final Set<String> todo;

  private final Map<Template, SessionData> children;

  // variable occurrence values. A variable may occur multiple times
  // within the same template, and occurrences may end up having
  // different values due to being escaped differently. The keys in
  // the map are the indices of VarPart parts.
  private final Map<Integer, Object> varValues;

  RenderState(SessionConfig config) {
    this.config = config;
    int sz = config.template().countNestedTemplates();
    this.children = new IdentityHashMap<>(sz);
    sz = config.template().countVariableOccurrences();
    this.varValues = HashMap.newHashMap(sz);
    this.todo = new HashSet<>(config.template().getVariables());
  }

  SessionConfig getSessionConfig() {
    return config;
  }

  SessionData getSessionData(Template tmpl) {
    return children.get(tmpl);
  }

  SoloSession[] createChildSessions(Template t, String separator, int repeats) {
    SoloSession[] sessions;
    if (repeats == 0) {
      sessions = ZERO_SESSIONS;
    } else {
      sessions = new SoloSession[repeats];
      for (int i = 0; i < repeats; ++i) {
        sessions[i] = config.newChildSession(t);
      }
    }
    this.children.put(t, new SessionData(sessions, nullToEmpty(separator)));
    return sessions;
  }

  SoloSession[] getOrCreateChildSessions(Template t, String separator, int repeats) {
    SessionData children = this.children.get(t);
    if (children == null) {
      return createChildSessions(t, separator, repeats);
    } else if (children.sessions().length == repeats) {
      return children.sessions();
    }
    throw REPETITION_MISMATCH.getException(
          getFQN(t),
          children.sessions().length,
          repeats);
  }

  boolean isProcessed(Template template) {
    return children.get(template) != null;
  }

  boolean isDisabled(Template template) {
    SessionData sd = children.get(template);
    return sd != null && sd.sessions().length == 0;
  }

  SoloSession[] getChildSessions(Template template) {
    return ifNotNull(children.get(template), SessionData::sessions);
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

  List<String> todo() {
    return List.copyOf(todo);
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

  // collects absolute paths
  private static void collectUnsetVariables(RenderState state, ArrayList<String> vars) {
    Template myTmpl = state.config.template();
    state.todo.stream().map(var -> getFQN(myTmpl, var)).forEach(vars::add);
    myTmpl.getNestedTemplates().forEach(t -> {
      SessionData children = state.children.get(t);
      if (children == null) {
        TemplateUtils.collectFQNs(t, vars);
      } else if (children.sessions().length > 0) {
        collectUnsetVariables(children.sessions()[0].state(), vars);
      }
    });
  }

  // collects relative paths
  private static void collectUnsetVariables(
        RenderState state,
        ArrayList<Path> vars,
        Path path) {
    state.todo.stream().map(path::append).forEach(vars::add);
    Template tmpl = state.config.template();
    tmpl.getNestedTemplates().forEach(t -> {
      Path next = path.append(t.getName());
      SessionData sd = state.children.get(t);
      if (sd == null) {
        TemplateUtils.collectFQNs(t, vars, next);
      } else if (sd.sessions().length > 0) {
        collectUnsetVariables(sd.sessions()[0].state(), vars, next);
      }
    });
  }

  boolean ready() {
    return ready(this);
  }

  private static boolean ready(RenderState state) {
    if (state.todo.isEmpty()) {
      for (Template t : state.config.template().getNestedTemplates()) {
        SessionData sd = state.children.get(t);
        if (sd == null) {
          if (t.hasVariables()) {
            return false;
          }
        } else if (sd.sessions().length > 0 && !ready(sd.sessions()[0].state())) {
          return false;
        }
      }
      return true;
    }
    return false;
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
      occurrences.stream().forEach(state.varValues.keySet()::remove);
    } else {
      Template tmpl = state.config.template();
      Check.that(name).is(in(), tmpl.getNestedTemplateNames(),
            NO_SUCH_TEMPLATE.getExceptionSupplier(getFQN(tmpl, name)));
      Template nested = tmpl.getNestedTemplate(name);
      SessionData children = state.children.get(nested);
      if (children != null) {
        Arrays.stream(children.sessions()).forEach(s -> unset(s.state(), path.shift()));
      }
    }
  }

  void clear(Template tmpl) {
    Arrays.stream(children.get(tmpl).sessions()).forEach(this::clear);
    children.remove(tmpl);
  }

  private void clear(SoloSession session) {
    RenderState state = session.state();
    state.varValues.clear();
    state.todo.addAll(state.config.template().getVariables());
    state.children.values()
          .stream()
          .map(SessionData::sessions)
          .flatMap(Arrays::stream)
          .forEach(this::clear);
    state.children.clear();
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
    SessionData children = state.children.get(nested);
    if (children == null) {
      return false;
    } else if (children.sessions().length == 0) {
      return true;
    }
    return isSet(children.sessions()[0].state(), path.shift());
  }

}
