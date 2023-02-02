package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

public abstract sealed class AbstractPart implements Part permits
    NestedTemplatePart, TextPart, UnparsedPart, VariablePart {

  private final int start;

  private Template parent;

  public AbstractPart(int start) {
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

  @Override
  public void setParentTemplate(Template parent) {
    this.parent = parent;
  }

}
