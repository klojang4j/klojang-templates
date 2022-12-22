package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

import static org.klojang.templates.x.parse.Regex.TMPL_START;
import static org.klojang.templates.x.parse.Regex.VAR_END;

final class InlineTemplatePart extends NestedTemplatePart {

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
