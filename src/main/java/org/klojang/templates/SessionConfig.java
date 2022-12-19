package org.klojang.templates;

import static org.klojang.templates.AccessorRegistry.STANDARD_ACCESSORS;
import static org.klojang.templates.StringifierRegistry.STANDARD_STRINGIFIERS;

class SessionConfig {

  private final Template template;
  private final AccessorRegistry accessors;
  private final StringifierRegistry stringifiers;

  SessionConfig(Template template) {
    this(template, STANDARD_ACCESSORS, STANDARD_STRINGIFIERS);
  }

  SessionConfig(Template template, StringifierRegistry stringifiers) {
    this(template, STANDARD_ACCESSORS, stringifiers);
  }

  SessionConfig(Template template, AccessorRegistry accessors) {
    this(template, accessors, STANDARD_STRINGIFIERS);
  }

  SessionConfig(Template template, AccessorRegistry accessors, StringifierRegistry stringifiers) {
    this.template = template;
    this.accessors = accessors;
    this.stringifiers = stringifiers;
  }

  RenderSession newRenderSession() {
    return new RenderSession(this);
  }

  Template getTemplate() {
    return template;
  }

  AccessorRegistry getAccessors() {
    return accessors;
  }

  StringifierRegistry getStringifiers() {
    return stringifiers;
  }

  Accessor<?> getAccessor(Object sourceData) {
    return accessors.getAccessor(sourceData, template);
  }

  RenderSession newChildSession(Template nested) {
    SessionConfig config = new SessionConfig(nested, accessors, stringifiers);
    return config.newRenderSession();
  }
}
