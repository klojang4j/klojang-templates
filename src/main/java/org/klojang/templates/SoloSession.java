package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.path.Path;
import org.klojang.templates.x.Lazy;
import org.klojang.templates.x.MTag;
import org.klojang.util.collection.IntList;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonProperties.length;
import static org.klojang.check.CommonProperties.size;
import static org.klojang.templates.Accessor.UNDEFINED;
import static org.klojang.templates.RenderErrorCode.*;
import static org.klojang.templates.TemplateUtils.getFQN;
import static org.klojang.templates.x.MTag.VAR_GROUP;
import static org.klojang.templates.x.MTag.VAR_NAME;
import static org.klojang.util.CollectionMethods.listify;
import static org.klojang.util.ObjectMethods.isEmpty;

final class SoloSession implements RenderSession {

  private final SessionConfig config;
  private final RenderState state;

  SoloSession(SessionConfig config) {
    this.config = config;
    this.state = new RenderState(config);
  }

  /* METHODS FOR SETTING A SINGLE TEMPLATE VARIABLE */

  @Override
  public RenderSession set(String varName, Object value) {
    Check.notNull(varName, VAR_NAME);
    return setVar(varName, value, null);
  }

  @Override
  public RenderSession set(String varName, Object value, VarGroup varGroup) {
    Check.notNull(varName, VAR_NAME);
    Check.notNull(varGroup, VAR_GROUP);
    return setVar(varName, value, varGroup);
  }

  private RenderSession setVar(String varName, Object value, VarGroup varGroup) {
    if (value != UNDEFINED) {
      Template t = config.template();
      Check.that(varName).is(keyIn(), t.variables(),
          NO_SUCH_VARIABLE.getExceptionSupplier(TemplateUtils.getFQN(t, varName)));
      IntList indices = t.variables().get(varName);
      indices.forEachThrowing(i -> setVar(i, value, varGroup));
      state.done(varName);
    }
    return this;
  }

  private void setVar(int partIndex, Object value, VarGroup varGroup) {
    VariablePart part = (VariablePart) config.template().parts().get(partIndex);
    VarGroup group = part.getVarGroup().orElse(varGroup);
    StringifierRegistry reg = config.stringifiers();
    Stringifier stringifier = reg.getStringifier(part, group, value);
    String strval = RenderUtil.stringify(value, stringifier, part, varGroup);
    state.setVar(partIndex, strval);
  }

  @Override
  public RenderSession setDelayed(String varName, Supplier<Object> valueGenerator) {
    return setDelayed(varName, valueGenerator, VarGroup.TEXT);
  }

  @Override
  public RenderSession setDelayed(String varName,
      Supplier<Object> valueGenerator,
      VarGroup varGroup) {
    Check.notNull(varName, VAR_NAME);
    Check.notNull(valueGenerator, "valueGenerator");
    Check.notNull(varGroup, VAR_GROUP);
    Template t = config.template();
    Check.that(varName).is(keyIn(), t.variables(),
        NO_SUCH_VARIABLE.getExceptionSupplier(TemplateUtils.getFQN(t, varName)));
    IntList indices = t.variables().get(varName);
    indices.forEachThrowing(i -> state.setVar(i,
        new Lazy(valueGenerator, varGroup)));
    state.done(varName);
    return this;
  }

  @Override
  public RenderSession setNested(String path, IntFunction<Object> valueGenerator) {
    Check.notNull(path, Tag.PATH);
    Check.notNull(valueGenerator, "valueGenerator");
    Path p = Path.from(path);
    Check.that(p).has(Path::size, gt(), 1, "Not a nested variable: ${arg}");
    setNested(this, p, valueGenerator, null, true);
    return this;
  }

  @Override
  public RenderSession setNested(String path,
      IntFunction<Object> valueGenerator,
      VarGroup varGroup,
      boolean force) {
    Check.notNull(path, Tag.PATH);
    Check.notNull(valueGenerator, "valueGenerator");
    Path p = Path.from(path);
    Check.that(p).has(Path::size, gt(), 1, "Not a nested variable: ${arg}");
    setNested(this, p, valueGenerator, varGroup, force);
    return this;
  }

  private static void setNested(
      SoloSession session,
      Path path,
      IntFunction<Object> valueGenerator,
      VarGroup varGroup,
      boolean force) {
    Template t = session.getNestedTemplate(path.segment(0));
    SoloSession[] children = session.state.getChildSessions(t);
    if (children == null) {
      if (!force) {
        return;
      }
      children = session.state.createChildSessions(t, 1);
    }
    if (path.size() == 2) {
      for (int i = 0; i < children.length; ++i) {
        children[i].setVar(path.segment(1), valueGenerator.apply(i), varGroup);
      }
    } else {
      for (SoloSession child : children) {
        setNested(child, path.shift(), valueGenerator, varGroup, force);
      }
    }
  }

  @Override
  public RenderSession insert(Object data, String... names) {
    return insert(data, null, names);
  }

  @Override
  public RenderSession insert(Object data,
      VarGroup varGroup,
      String... names) {
    if (data == UNDEFINED) {
      return this;
    } else if (data == null) {
      Template t = config.template();
      Check.that(t.isTextOnly())
          .is(yes(), NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
      // If we get past this check, the entire template is in fact
      // static HTML. Expensive way to render static HTML, but no
      // reason not to support it.
      return this;
    } else if (data instanceof Optional<?> opt) {
      return opt.isPresent() ? insert(opt.get(), varGroup, names) : this;
    }
    processVars(data, varGroup, names);
    processTmpls(data, varGroup, names);
    return this;
  }

  @SuppressWarnings("unchecked")
  private <T> void processVars(T data, VarGroup defGroup, String[] names) {
    Set<String> varNames;
    if (isEmpty(names)) {
      varNames = config.template().getVariables();
    } else {
      varNames = new HashSet<>(config.template().getVariables());
      varNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String varName : varNames) {
      Object value;
      try {
        value = acc.access(data, varName);
      } catch (RuntimeException e) {
        String fqn = TemplateUtils.getFQN(config.template(), varName);
        throw ACCESS_EXCEPTION.getException(fqn, e);
      }
      if (value != UNDEFINED) {
        setVar(varName, value, defGroup);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void processTmpls(T data, VarGroup varGroup, String[] names) {
    Set<String> tmplNames;
    if (isEmpty(names)) {
      tmplNames = config.template().getNestedTemplateNames();
    } else {
      tmplNames = new HashSet<>(config.template().getNestedTemplateNames());
      tmplNames.retainAll(List.of(names));
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String name : tmplNames) {
      Object nestedData = acc.access(data, name);
      if (nestedData != UNDEFINED) {
        populate(name, nestedData, varGroup, names);
      }
    }
  }

  @Override
  public RenderSession populate(String nestedTemplateName,
      Object data,
      String... names) {
    return populate(nestedTemplateName, data, null, names);
  }

  @Override
  public RenderSession populate(String nestedTemplateName,
      Object sourceData,
      VarGroup varGroup,
      String... names) {
    if (sourceData == UNDEFINED) {
      return this;
    } else if (sourceData instanceof Optional<?> opt) {
      return opt.isPresent()
          ? populate(nestedTemplateName, opt.get(), varGroup, names)
          : this;
    }
    Template t = getNestedTemplate(nestedTemplateName);
    List<?> data = listify(sourceData);
    if (t.isTextOnly()) {
      return enable(data.size(), t);
    }
    Check.that(data, Tag.DATA).is(deepNotNull());
    return populate(t, data, varGroup, names);
  }

  public RenderSession repeat(String nestedTemplateName, int times) {
    Check.that(times).isNot(negative());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(state.getChildSessions(t)).is(NULL(),
        REPETITIONS_FIXED.getExceptionSupplier(nestedTemplateName));
    SoloSession[] sessions = state.createChildSessions(t, times);
    return new MultiSession(sessions);
  }

  @Override
  public RenderSession in(String fqn) {
    Path path = Check.that(fqn).isNot(empty()).ok(Path::from);
    RenderSession rs = in1(path.segment(0));
    for (int i = 1; i < path.size(); ++i) {
      rs = rs.in(path.segment(i));
    }
    return rs;
  }

  private RenderSession in1(String name) {
    Template t = getNestedTemplate(name);
    SoloSession[] children = state.getChildSessions(t);
    if (children == null) {
      children = state.createChildSessions(t, 1);
    }
    return new MultiSession(children);
  }

  @Override
  public RenderSession enable(String... nestedTemplateNames) {
    return enable(1, nestedTemplateNames);
  }

  @Override
  public RenderSession enable(int repeats, String... nestedTemplateNames) {
    Check.that(repeats, MTag.REPEATS).is(gte(), 0);
    Check.notNull(nestedTemplateNames, Tag.VARARGS);
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        Check.that(t).is(Template::isTextOnly,
            NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        if (!state.isProcessed(t)) {
          enable(repeats, t);
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Template t = getNestedTemplate(name);
        Check.that(t).is(Template::isTextOnly,
            NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        enable(repeats, t);
      }
    }
    return this;
  }

  private RenderSession enable(int repeats, Template nested) {
    state.createChildSessions(nested, repeats);
    return this;
  }

  @Override
  public RenderSession enableRecursive(String... nestedTemplateNames) {
    Check.notNull(nestedTemplateNames);
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        if (!state.isDisabled(t) && TemplateUtils.getVarsPerTemplate(t).isEmpty()) {
          showAllRecursive(this, t);
        }
      }
    } else {
      Set<String> names = Set.of(nestedTemplateNames);
      for (Template t : config.template().getNestedTemplates()) {
        Check.that(TemplateUtils.getVarsPerTemplate(t)).is(empty(),
            NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        showSelectedRecursive(this, t, names);
      }
    }
    return this;
  }

  private static void showAllRecursive(SoloSession s0, Template t0) {
    s0.enable(1, t0);
    if (!t0.getNestedTemplates().isEmpty()) {
      SoloSession s = s0.state.getChildSessions(t0)[0];
      t0.getNestedTemplates().forEach(t -> showAllRecursive(s, t));
    }
  }

  private static void showSelectedRecursive(SoloSession s0,
      Template t0,
      Set<String> names) {
    if (names.contains(t0.getName())) {
      s0.enable(1, t0);
      if (!t0.getNestedTemplates().isEmpty()) {
        SoloSession s = s0.state.getChildSessions(t0)[0];
        t0.getNestedTemplates().forEach(t -> showSelectedRecursive(s, t, names));
      }
    }
  }

  @Override
  public RenderSession populate1(String nestedTemplateName, Object... values) {
    return populate1(nestedTemplateName, null, values);
  }

  @Override
  public RenderSession populate1(String nestedTemplateName,
      VarGroup varGroup, Object... values) {
    Check.that(values, Tag.VARARGS).isNot(empty());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 1,
        NOT_ONE_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    String var = t.getVariables().iterator().next();
    List<?> data = Arrays.stream(values).map(v -> singletonMap(var, v)).toList();
    return populate(t, data, varGroup);
  }

  @Override
  public RenderSession populate2(String nestedTemplateName, Object... values) {
    return populate2(nestedTemplateName, null, values);
  }

  @Override
  public RenderSession populate2(String nestedTemplateName,
      VarGroup varGroup, Object... values) {
    Check.that(values, Tag.VARARGS).isNot(empty()).has(length(), even());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 2,
        NOT_TWO_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    String[] vars = t.getVariables().toArray(String[]::new);
    List<Map<String, Object>> data = new ArrayList<>(values.length / 2);
    for (int i = 0; i < values.length; i += 2) {
      data.add(Map.of(vars[0], values[i], vars[1], values[i + 1]));
    }
    return populate(t, data, varGroup);
  }

  @Override
  public List<RenderSession> getChildSessions(String nestedTemplateName) {
    Template t = getNestedTemplate(nestedTemplateName);
    RenderSession[] sessions = state.getChildSessions(t);
    return Check.that(sessions).is(notNull(),
        TEMPLATE_NOT_INSTANTIATED.getExceptionSupplier(t.getName())).ok(List::of);
  }

  /* RENDER METHODS */

  @Override
  public boolean isFullyPopulated() {
    return state.isFullyPopulated();
  }

  @Override
  public void render(OutputStream out) {
    Check.notNull(out).then(x -> new Renderer(state).render(x));
  }

  @Override
  public void render(StringBuilder sb) {
    Check.notNull(sb).then(x -> new Renderer(state).render(x));
  }

  @Override
  public String render() {
    ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
    new Renderer(state).render(out);
    return out.toString(UTF_8);
  }

  @Override
  public String toString() {
    return String.format("RenderSession[template=%s]", getFQN(config.template()));
  }

  RenderState getState() {
    return state;
  }

  private RenderSession populate(Template t,
      List<?> data,
      VarGroup varGroup,
      String... names) {
    SoloSession[] sessions = state.getOrCreateChildSessions(t, data.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(data.get(i), varGroup, names);
    }
    return this;
  }

  private Template getNestedTemplate(String name) {
    Check.notNull(name, MTag.TEMPLATE_NAME);
    Template t = config.template();
    return Check.that(name).is(elementOf(), t.getNestedTemplateNames(),
            NO_SUCH_TEMPLATE.getExceptionSupplier(TemplateUtils.getFQN(t, name)))
        .ok(t::getNestedTemplate);
  }

}
