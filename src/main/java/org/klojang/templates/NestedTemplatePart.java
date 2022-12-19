package org.klojang.templates;

abstract class NestedTemplatePart extends AbstractPart implements NamedPart {

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

  @Override
  public void setParentTemplate(Template parent) {
    super.setParentTemplate(parent);
    template.parent = parent;
  }
}
