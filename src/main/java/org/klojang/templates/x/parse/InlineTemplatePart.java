package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

public final class InlineTemplatePart extends NestedTemplatePart {

  public InlineTemplatePart(Template template, int start) {
    super(template, start);
  }

  public String toString() {
    return new StringBuilder(100)
        .append("~%%begin:")
        .append(template.getName())
        .append('%')
        .append(template)
        .append("~%%end:")
        .append(template.getName())
        .append('%')
        .toString();
  }

}
