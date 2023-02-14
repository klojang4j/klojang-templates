package org.klojang.templates;

final class InlineTemplatePart extends NestedTemplatePart {

  InlineTemplatePart(Template template, int start) {
    super(template, start);
  }

  @Override
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
