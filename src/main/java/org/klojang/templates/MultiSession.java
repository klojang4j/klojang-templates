package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.path.Path;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

record MultiSession(Template template, SoloSession[] sessions) implements RenderSession {

  @Override
  public RenderSession set(String var, Object value) {
    stream().forEach(s -> s.set(var, value));
    return this;
  }

  @Override
  public RenderSession set(String var, VarGroup group, Object value) {
    stream().forEach(s -> s.set(var, group, value));
    return this;
  }

  @Override
  public RenderSession setDelayed(String var, Supplier<Object> val) {
    stream().forEach(s -> s.setDelayed(var, val));
    return this;
  }

  @Override
  public RenderSession setDelayed(String var, VarGroup group, Supplier<Object> val) {
    stream().forEach(s -> s.setDelayed(var, group, val));
    return this;
  }

  /*
   * setPath() is about the only method where we don't immediately delegate to
   * SoloSession. We use the happy fact that we have an IntFunction to set different
   * values for different instances of the template managed by THIS MultiSession.
   */
  @Override
  public RenderSession setPath(String path, IntFunction<Object> val) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    if (p.size() == 1) {
      for (int i = 0; i < sessions.length; ++i) {
        sessions[i].set(path, val.apply(i));
      }
    } else { // do delegate to SoloSession
      stream().forEach(s -> s.setPath(path, val));
    }
    return this;
  }

  @Override
  public RenderSession setPath(
        String path,
        VarGroup group,
        boolean force,
        IntFunction<Object> val) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    if (p.size() == 1) {
      for (int i = 0; i < sessions.length; ++i) {
        sessions[i].set(path, group, val.apply(i));
      }
    } else {
      stream().forEach(s -> s.setPath(path, group, force, val));
    }
    return this;
  }

  @Override
  public RenderSession ifNotSet(String path, IntFunction<Object> val) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    if (p.size() == 1) {
      for (int i = 0; i < sessions.length; ++i) {
        if (!sessions[i].state().isSet(p)) {
          sessions[i].set(path, val.apply(i));
        }
      }
    } else {
      stream().forEach(s -> s.ifNotSet(path, val));
    }
    return this;
  }

  @Override
  public RenderSession ifNotSet(String path, VarGroup group, IntFunction<Object> val) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    if (p.size() == 1) {
      for (int i = 0; i < sessions.length; ++i) {
        if (!sessions[i].state().isSet(p)) {
          sessions[i].set(path, val.apply(i));
        }
      }
    } else {
      stream().forEach(s -> s.ifNotSet(path, group, val));
    }
    return this;
  }

  @Override
  public RenderSession insert(Object data, VarGroup group, List<String> names) {
    stream().forEach(s -> s.insert(data, group, names));
    return this;
  }

  @Override
  public RenderSession populate(
        String tmplName,
        Object data,
        VarGroup group,
        String sep,
        List<String> names) {
    stream().forEach(s -> s.populate(tmplName, data, group, sep, names));
    return this;
  }

  @Override
  public RenderSession repeat(String tmplName, String sep, int times) {
    Template nested = template.getNestedTemplate(tmplName);
    ArrayList<SoloSession> list = new ArrayList<>();
    for (RenderSession rs : sessions) {
      RenderSession ms = rs.repeat(tmplName, sep, times);
      SoloSession[] ss = ((MultiSession) ms).sessions;
      list.addAll(Arrays.asList(ss));
    }
    return new MultiSession(nested, list.toArray(SoloSession[]::new));
  }

  @Override
  public RenderSession in(String tmplName) {
    Template nested = template.getNestedTemplate(tmplName);
    ArrayList<SoloSession> list = new ArrayList<>();
    for (RenderSession rs : sessions) {
      SoloSession[] ss = ((MultiSession) rs.in(tmplName)).sessions;
      list.addAll(Arrays.asList(ss));
    }
    return new MultiSession(nested, list.toArray(SoloSession[]::new));
  }

  @Override
  public RenderSession enable(String sep, int repeats, String... tmplNames) {
    stream().forEach(s -> s.enable(sep, repeats, tmplNames));
    return this;
  }

  @Override
  public RenderSession enableRecursive(String... tmplNames) {
    stream().forEach(s -> s.enableRecursive(tmplNames));
    return this;
  }

  @Override
  public RenderSession populate1(
        String tmplName,
        VarGroup group,
        String sep,
        List<?> values) {
    stream().forEach(s -> s.populate1(tmplName, group, sep, values));
    return this;
  }

  @Override
  public RenderSession populate2(
        String tmplName,
        VarGroup group,
        String sep,
        List<?> values) {
    stream().forEach(s -> s.populate2(tmplName, group, sep, values));
    return this;
  }

  @Override
  public List<String> getUnsetVariables() {
    return sessions.length == 0 ? List.of() : sessions[0].getUnsetVariables();
  }

  @Override
  public List<String> getAllUnsetVariables() {
    return sessions.length == 0 ? List.of() : sessions[0].getAllUnsetVariables();
  }

  public List<String> getAllUnsetVariables(boolean relative) {
    return sessions.length == 0
          ? List.of()
          : sessions[0].getAllUnsetVariables(relative);
  }

  @Override
  public boolean allSet() {
    return stream().allMatch(RenderSession::allSet);
  }

  @Override
  public RenderSession unset(String... paths) {
    stream().forEach(s -> s.unset(paths));
    return this;
  }

  @Override
  public RenderSession clear(String... tmplNames) {
    stream().forEach(s -> s.clear(tmplNames));
    return this;
  }

  @Override
  public List<RenderSession> getChildSessions(String tmplName) {
    ArrayList<RenderSession> flat = new ArrayList<>();
    stream().forEach(s -> flat.addAll(s.getChildSessions(tmplName)));
    return flat;
  }

  @Override
  public void render(OutputStream out) {
    stream().forEach(s -> s.render(out));
  }

  @Override
  public void render(StringBuilder sb) {
    stream().forEach(s -> s.render(sb));
  }

  @Override
  public String render() {
    StringBuilder sb = new StringBuilder(255);
    stream().forEach(s -> s.render(sb));
    return sb.toString();
  }

  @Override
  public Template getTemplate() {
    return template;
  }

  private Stream<SoloSession> stream() {
    return Arrays.stream(sessions);
  }


}
