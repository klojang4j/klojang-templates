package org.klojang.templates;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MultiSession implements RenderSession {

  final SoloSession[] sessions;

  MultiSession(SoloSession[] sessions) {
    this.sessions = sessions;
  }

  @Override
  public RenderSession set(String varName, Object value) {
    Arrays.stream(sessions).forEach(s -> s.set(varName, value));
    return this;
  }

  @Override
  public RenderSession set(String varName, Object value, VarGroup varGroup) {
    Arrays.stream(sessions).forEach(s -> s.set(varName, value));
    return this;
  }

  @Override
  public RenderSession populate(String nestedTemplateName,
      Object data,
      String... names) {
    Arrays.stream(sessions).forEach(s -> s.populate(nestedTemplateName,
        data,
        names));
    return this;
  }

  @Override
  public RenderSession populate(String nestedTemplateName,
      Object data,
      VarGroup varGroup,
      String... names) {
    Arrays.stream(sessions).forEach(s -> s.populate(nestedTemplateName,
        data,
        names));
    return this;
  }

  @Override
  public RenderSession repeat(String nestedTemplateName, int times) {
    List<SoloSession> list = new ArrayList<>();
    for (RenderSession rs : sessions) {
      RenderSession ms = rs.repeat(nestedTemplateName, times);
      SoloSession[] ss = ((MultiSession) ms).sessions;
      list.addAll(Arrays.asList(ss));
    }
    return new MultiSession(list.toArray(SoloSession[]::new));
  }

  @Override
  public RenderSession in(String nestedTemplateName) {
    List<SoloSession> list = new ArrayList<>();
    for (RenderSession rs : sessions) {
      SoloSession[] ss = ((MultiSession) rs.in(nestedTemplateName)).sessions;
      list.addAll(Arrays.asList(ss));
    }
    return new MultiSession(list.toArray(SoloSession[]::new));
  }

  @Override
  public RenderSession show(String... nestedTemplateNames) {
    Arrays.stream(sessions).forEach(s -> s.show(nestedTemplateNames));
    return this;
  }

  @Override
  public RenderSession show(int repeats, String... nestedTemplateNames) {
    Arrays.stream(sessions).forEach(s -> s.show(repeats, nestedTemplateNames));
    return this;
  }

  @Override
  public RenderSession showRecursive(String... nestedTemplateNames) {
    Arrays.stream(sessions).forEach(s -> s.showRecursive(nestedTemplateNames));
    return this;
  }

  @Override
  public RenderSession populate1(String nestedTemplateName, Object... values) {
    Arrays.stream(sessions).forEach(s -> s.populate1(nestedTemplateName, values));
    return this;
  }

  @Override
  public RenderSession populate1(String nestedTemplateName,
      VarGroup varGroup,
      Object... values) {
    Arrays.stream(sessions).forEach(
        s -> s.populate1(nestedTemplateName, varGroup, values));
    return this;
  }

  @Override
  public RenderSession populate2(String nestedTemplateName,
      Object... values) {
    Arrays.stream(sessions).forEach(s -> s.populate2(nestedTemplateName, values));
    return this;
  }

  @Override
  public RenderSession populate2(String nestedTemplateName,
      VarGroup varGroup, Object... values) {
    Arrays.stream(sessions).forEach(
        s -> s.populate2(nestedTemplateName, varGroup, values));
    return this;
  }

  @Override
  public RenderSession insert(Object sourceData, String... names) {
    Arrays.stream(sessions).forEach(s -> s.insert(sourceData, names));
    return this;
  }

  @Override
  public RenderSession insert(Object sourceData,
      VarGroup varGroup,
      String... names) {
    Arrays.stream(sessions).forEach(s -> s.insert(sourceData, varGroup, names));
    return this;
  }

  @Override
  public boolean isFullyPopulated() {
    return Arrays.stream(sessions).allMatch(RenderSession::isFullyPopulated);
  }

  @Override
  public List<RenderSession> getChildSessions(String nestedTemplateName) {
    List<RenderSession> flat = new ArrayList<>();
    Arrays.stream(sessions).forEach(
        s -> flat.addAll(s.getChildSessions(nestedTemplateName)));
    return flat;
  }

  @Override
  public void render(OutputStream out) {
    Arrays.stream(sessions).forEach(s -> s.render(out));
  }

  @Override
  public void render(StringBuilder sb) {
    Arrays.stream(sessions).forEach(s -> s.render(sb));
  }

  @Override
  public String render() {
    StringBuilder sb = new StringBuilder(255);
    Arrays.stream(sessions).forEach(s -> s.render(sb));
    return sb.toString();
  }

}
