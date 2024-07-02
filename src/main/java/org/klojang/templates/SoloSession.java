package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.path.Path;
import org.klojang.templates.x.Lazy;
import org.klojang.templates.x.MTag;
import org.klojang.util.collection.IntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonProperties.size;
import static org.klojang.templates.Accessor.UNDEFINED;
import static org.klojang.templates.RenderErrorCode.*;
import static org.klojang.templates.RenderUtil.stringify;
import static org.klojang.templates.TemplateUtils.getAllVariables;
import static org.klojang.templates.TemplateUtils.getFQN;
import static org.klojang.templates.x.MTag.VALUE_GENERATOR;
import static org.klojang.templates.x.MTag.VAR_NAME;
import static org.klojang.util.ClassMethods.simpleClassName;
import static org.klojang.util.CollectionMethods.initializeMap;
import static org.klojang.util.CollectionMethods.listify;
import static org.klojang.util.ObjectMethods.isEmpty;

final class SoloSession implements RenderSession {

  private static final Logger LOG = LoggerFactory.getLogger(SoloSession.class);

  private final SessionConfig config;
  private final RenderState state;

  SoloSession(SessionConfig config, RenderState state) {
    this.config = config;
    this.state = state;
    if (LOG.isTraceEnabled()) {
      log(config.template());
    }
  }

  SoloSession(SessionConfig config) {
    this(config, new RenderState(config));
  }

  @Override
  public RenderSession set(String varName, Object value, VarGroup varGroup) {
    Check.notNull(varName, VAR_NAME);
    return setVar(varName, varGroup, value);
  }

  RenderSession setVar(String var, VarGroup group, Object value) {
    if (mustProcess(value)) {
      Template t = config.template();
      Check.that(var).is(keyIn(), t.variables(),
            NO_SUCH_VARIABLE.getExceptionSupplier(getFQN(t, var)));
      IntList indices = t.variables().get(var);
      indices.forEachThrowing(i -> setVar(i, group, value));
      state.done(var);
    }
    return this;
  }

  private void setVar(int partIndex, VarGroup varGroup, Object value) {
    VariablePart part = (VariablePart) config.template().parts().get(partIndex);
    VarGroup group = part.varGroup().orElse(varGroup);
    StringifierRegistry reg = config.stringifiers();
    Stringifier stringifier = reg.getStringifier(part, group, value);
    String strval = stringify(value, stringifier, part, varGroup);
    state.setVar(partIndex, strval);
  }

  @Override
  public RenderSession setDelayed(
        String varName,
        VarGroup varGroup, Supplier<Object> valueGenerator) {
    Check.notNull(varName, VAR_NAME);
    Check.notNull(valueGenerator, VALUE_GENERATOR);
    return setDelayed0(varName, varGroup, valueGenerator);
  }

  private RenderSession setDelayed0(
        String var, VarGroup group, Supplier<Object> func) {
    Template t = config.template();
    Check.that(var).is(keyIn(), t.variables(), NO_SUCH_VARIABLE
          .getExceptionSupplier(getFQN(t, var)));
    IntList indices = t.variables().get(var);
    indices.forEachThrowing(i -> state.setVar(i, new Lazy(func, group)));
    state.done(var);
    return this;
  }

  @Override
  public RenderSession setPath(
        String path,
        VarGroup varGroup,
        boolean force,
        IntFunction<Object> valueGenerator) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    Check.notNull(valueGenerator, VALUE_GENERATOR);
    RenderUtil.setPath(this, p, varGroup, force, valueGenerator);
    return this;
  }

  @Override
  public RenderSession ifNotSet(
        String path,
        VarGroup varGroup,
        IntFunction<Object> valueGenerator) {
    Path p = Check.notNull(path, Tag.PATH).ok(Path::from);
    Check.notNull(valueGenerator, VALUE_GENERATOR);
    return RenderUtil.ifNotSet(this, p, valueGenerator, varGroup);
  }

  @Override
  public RenderSession insert(
        Object data,
        VarGroup group,
        List<String> names) {
    if (dontProcess(data)) {
      LOG.trace("Skipping null/undefined for insertion into template \"{}\"",
            config.template().getName());
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
      if (opt.isPresent()) {
        return insert(opt.get(), group, names);
      }
      LOG.trace("Skipping empty Optional for insertion into template \"{}\"",
            config.template().getName());
      return this;
    }
    LOG.trace("Inserting {} into template \"{}\": {}",
          simpleClassName(data),
          config.template().getName(),
          data);
    processVars(data, group, names);
    processTmpls(data, group, names);
    return this;
  }

  @SuppressWarnings("unchecked")
  private <T> void processVars(T data, VarGroup defGroup, List<String> names) {
    Set<String> varNames;
    if (isEmpty(names)) {
      varNames = config.template().getVariables();
    } else {
      varNames = HashSet.newHashSet(config.template().getVariables().size());
      varNames.addAll(config.template().getVariables());
      varNames.retainAll(names);
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String varName : varNames) {
      Object value;
      try {
        value = acc.access(data, varName);
      } catch (RuntimeException e) {
        String fqn = getFQN(config.template(), varName);
        throw ACCESS_EXCEPTION.getException(fqn, e);
      }
      setVar(varName, defGroup, value);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void processTmpls(T data, VarGroup varGroup, List<String> names) {
    Set<String> tmplNames;
    if (isEmpty(names)) {
      tmplNames = config.template().getNestedTemplateNames();
    } else {
      tmplNames = HashSet.newHashSet(config.template().getNestedTemplateNames().size());
      tmplNames.addAll(config.template().getNestedTemplateNames());
      tmplNames.retainAll(names);
    }
    Accessor<T> acc = (Accessor<T>) config.getAccessor(data);
    for (String name : tmplNames) {
      Object nestedData = acc.access(data, name);
      if (mustProcess(nestedData)) {
        doPopulate(getNestedTemplate(name), nestedData, varGroup, null, names);
      }
    }
  }

  @Override
  public RenderSession populate(
        String tmpl,
        Object data,
        String separator, VarGroup group,
        List<String> names) {
    return doPopulate(getNestedTemplate(tmpl), data, group, separator, names);
  }

  private RenderSession doPopulate(
        Template tmpl,
        Object data,
        VarGroup group,
        String separator,
        List<String> names) {
    if (dontProcess(data)) {
      return this;
    } else if (data instanceof Optional<?> opt) {
      if (opt.isPresent()) {
        return doPopulate(tmpl, opt.get(), group, separator, names);
      }
      if (!state.isProcessed(tmpl)) {
        state.createChildSessions(tmpl, separator, 0);
      }
      return this;
    }
    List<?> list = listify(data);
    if (tmpl.isTextOnly()) {
      return enable(list.size(), separator, tmpl);
    }
    SoloSession[] sessions = state.getOrCreateChildSessions(tmpl, separator, list.size());
    for (int i = 0; i < sessions.length; ++i) {
      sessions[i].insert(list.get(i), group, names);
    }
    return this;
  }

  @Override
  public RenderSession repeat(String tmpl, String separator, int times) {
    Check.that(times).isNot(negative());
    Template t = getNestedTemplate(tmpl);
    Check.that(state.getChildSessions(t)).is(NULL(),
          REPETITIONS_FIXED.getExceptionSupplier(tmpl));
    return new MultiSession(t, state.createChildSessions(t, separator, times));
  }

  @Override
  public RenderSession in(String fqn) {
    Path path = Check.that(fqn).isNot(empty()).ok(Path::from);
    RenderSession rs = in0(path.segment(0));
    for (int i = 1; i < path.size(); ++i) {
      rs = rs.in(path.segment(i));
    }
    return rs;
  }

  private RenderSession in0(String name) {
    Template t = getNestedTemplate(name);
    SoloSession[] children = state.getChildSessions(t);
    if (children == null) {
      children = state.createChildSessions(t, null, 1);
    }
    return new MultiSession(t, children);
  }

  @Override
  public RenderSession enable(String separator,
        int repeats,
        String... nestedTemplateNames) {
    Check.that(repeats, MTag.REPEATS).is(gte(), 0);
    Check.notNull(nestedTemplateNames, Tag.VARARGS);
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        Check.that(t).is(Template::isTextOnly,
              NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        if (!state.isProcessed(t)) {
          enable(repeats, separator, t);
        }
      }
    } else {
      for (String name : nestedTemplateNames) {
        Template t = getNestedTemplate(name);
        Check.that(t).is(Template::isTextOnly,
              NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        enable(repeats, separator, t);
      }
    }
    return this;
  }

  private RenderSession enable(int repeats, String separator, Template nested) {
    state.createChildSessions(nested, separator, repeats);
    return this;
  }

  @Override
  public RenderSession enableRecursive(String... nestedTemplateNames) {
    Check.that(nestedTemplateNames, Tag.VARARGS).is(deepNotNull());
    if (nestedTemplateNames.length == 0) {
      for (Template t : config.template().getNestedTemplates()) {
        if (!state.isDisabled(t) && getAllVariables(t).isEmpty()) {
          RenderUtil.enableRecursive(this, t);
        }
      }
    } else {
      Set<String> names = Set.of(nestedTemplateNames);
      for (Template t : config.template().getNestedTemplates()) {
        Check.that(getAllVariables(t)).is(empty(),
              NOT_TEXT_ONLY.getExceptionSupplier(t.getName()));
        RenderUtil.enableRecursive(this, t, names);
      }
    }
    return this;
  }

  @Override
  public RenderSession populateSolo(
        String nestedTemplateName,
        String separator,
        VarGroup group,
        List<?> values) {
    Check.that(values, Tag.VALUES).isNot(empty());
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 1,
          NOT_ONE_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    String var = t.getVariables().iterator().next();
    List<?> data = values.stream().map(v -> singletonMap(var, v)).toList();
    return doPopulate(t, data, group, separator, null);
  }

  @Override
  public RenderSession populateDuo(
        String nestedTemplateName,
        String separator,
        VarGroup varGroup,
        List<?> values) {
    Template t = getNestedTemplate(nestedTemplateName);
    Check.that(t.getVariables()).has(size(), eq(), 2,
          NOT_TWO_VAR_TEMPLATE.getExceptionSupplier(t.getName()));
    Check.that(values, Tag.VALUES).isNot(empty()).has(size(), even());
    String[] vars = t.getVariables().toArray(String[]::new);
    List<Map<String, Object>> data = new ArrayList<>(values.size() / 2);
    for (int i = 0; i < values.size(); i += 2) {
      data.add(initializeMap(vars[0], values.get(i), vars[1], values.get(i + 1)));
    }
    return doPopulate(t, data, varGroup, separator, null);
  }

  @Override
  public List<RenderSession> getChildSessions(String tmpl) {
    Template t = getNestedTemplate(tmpl);
    RenderSession[] rs = state.getChildSessions(t);
    Check.that(rs).is(notNull(), NO_CHILD_SESSIONS.getExceptionSupplier(t.getName()));
    return List.of(rs);
  }

  @Override
  public List<String> getUnsetVariables() {
    return state.todo();
  }

  @Override
  public List<String> getAllUnsetVariables() {
    return state.getAllUnsetVariables(false);
  }

  @Override
  public List<String> getAllUnsetVariables(boolean relative) {
    return state.getAllUnsetVariables(relative);
  }

  @Override
  public boolean hasUnsetVariables() {
    return !state.ready();
  }

  @Override
  public RenderSession unset(String... paths) {
    Check.notNull(paths, Tag.VARARGS);
    Arrays.stream(paths).map(Path::from).forEach(state::unset);
    return this;
  }

  @Override
  public RenderSession clear(String... tmpls) {
    Check.notNull(tmpls, Tag.VARARGS);
    Arrays.stream(tmpls).map(this::getNestedTemplate).forEach(state::clear);
    return this;
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
  public Template getTemplate() {
    return config.template();
  }

  RenderState state() {
    return state;
  }

  Template getNestedTemplate(String name) {
    Check.notNull(name, MTag.TEMPLATE_NAME);
    Template t = config.template().nested(name);
    Check.that(t).is(notNull(), NO_SUCH_TEMPLATE
          .getExceptionSupplier(getFQN(config.template(), name)));
    return t;
  }

  private boolean dontProcess(Object data) {
    return data == UNDEFINED ||
          (data == null && config.accessors().nullEqualsUndefined());
  }

  private boolean mustProcess(Object data) {
    return data != UNDEFINED &&
          (data != null || !config.accessors().nullEqualsUndefined());
  }

  private static void log(Template t) {
    if (t.getParent() == null) {
      if (t.path().isEmpty()) {
        LOG.trace("Starting render session for string template");
      } else {
        LOG.trace("Starting render session for {}", t.path().get());
      }
    } else if (t.location().isString()) {
      LOG.trace("Starting render session for inline template \"{}\"", getFQN(t));
    } else {
      LOG.trace("Starting render session for included template \"{}\"", getFQN(t));
    }
  }


}
