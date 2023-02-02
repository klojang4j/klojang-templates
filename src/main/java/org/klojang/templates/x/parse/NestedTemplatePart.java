package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

public abstract sealed class NestedTemplatePart extends AbstractPart implements
    NamedPart permits IncludedTemplatePart, InlineTemplatePart {

  final Template template;

  public NestedTemplatePart(Template template, int start) {
    super(start);
    this.template = template;
  }

  @Override
  public String getName() {
    return template.getName();
  }

  public Template getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return template.toString();
  }

}
