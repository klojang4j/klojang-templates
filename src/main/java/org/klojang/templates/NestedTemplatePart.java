package org.klojang.templates;

abstract sealed class NestedTemplatePart extends AbstractPart implements
    NamedPart permits IncludedTemplatePart, InlineTemplatePart {

  final Template template;

  NestedTemplatePart(Template template, int start) {
    super(start);
    this.template = template;
  }

  @Override
  public String getName() {
    return template.getName();
  }

  Template getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return template.toString();
  }

}
