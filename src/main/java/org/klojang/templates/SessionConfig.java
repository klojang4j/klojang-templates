package org.klojang.templates;

import static org.klojang.templates.AccessorRegistry.STANDARD_ACCESSORS;
import static org.klojang.templates.StringifierRegistry.STANDARD_STRINGIFIERS;

record SessionConfig(Template template,
    AccessorRegistry accessors,
    StringifierRegistry stringifiers) {

  SessionConfig(Template template) {
    this(template, STANDARD_ACCESSORS, STANDARD_STRINGIFIERS);
  }

  SessionConfig(Template template, StringifierRegistry stringifiers) {
    this(template, STANDARD_ACCESSORS, stringifiers);
  }

  SessionConfig(Template template, AccessorRegistry accessors) {
    this(template, accessors, STANDARD_STRINGIFIERS);
  }

  RenderSession newRenderSession() {
    return new RenderSession(this);
  }

  Accessor<?> getAccessor(Object sourceData) {
    return accessors.getAccessor(sourceData, template);
  }

  RenderSession newChildSession(Template nested) {
    SessionConfig config = new SessionConfig(nested, accessors, stringifiers);
    return config.newRenderSession();
  }

}
