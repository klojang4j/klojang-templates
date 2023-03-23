package org.klojang.templates;

final class InlineTemplatePart extends NestedTemplatePart {

  private final boolean startTagOnSeparateLine;

  InlineTemplatePart(Template template,
      int start,
      boolean startTagOnSeparateLine) {
    super(template, start);
    this.startTagOnSeparateLine = startTagOnSeparateLine;
  }

  boolean isStartTagOnSeparateLine() {
    return startTagOnSeparateLine;
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
