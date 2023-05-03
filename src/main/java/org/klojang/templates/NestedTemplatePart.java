package org.klojang.templates;

abstract sealed class NestedTemplatePart extends AbstractPart implements
    NamedPart permits IncludedTemplatePart, InlineTemplatePart {

  final Template template;

  NestedTemplatePart(int start, Template template) {
    super(start);
    this.template = template;
  }

  @Override
  public String name() {
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
