package org.klojang.templates;

abstract class AbstractPart implements Part {

  private final int start;

  private Template parent;

  AbstractPart(int start) {
    this.start = start;
  }

  @Override
  public int start() {
    return start;
  }

  @Override
  public Template getParentTemplate() {
    return parent;
  }

  void setParentTemplate(Template parent) {
    this.parent = parent;
  }

}
