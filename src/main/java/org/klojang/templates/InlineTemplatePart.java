package org.klojang.templates;

import static org.klojang.templates.x.Regex.TMPL_START;
import static org.klojang.templates.x.Regex.VAR_END;

class InlineTemplatePart extends NestedTemplatePart {

  InlineTemplatePart(Template template, int start) {
    super(template, start);
  }

  public String toString() {
    return new StringBuilder(100)
        .append(TMPL_START)
        .append("begin:")
        .append(template.getName())
        .append(VAR_END)
        .append(template.toString())
        .append(TMPL_START)
        .append("end:")
        .append(template.getName())
        .append(VAR_END)
        .toString();
  }

}
