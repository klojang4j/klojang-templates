package org.klojang.templates;

final class InlineTemplatePart extends NestedTemplatePart {

  private final boolean startTagOnSeparateLine;
  private final boolean endTagOnSeparateLine;

  InlineTemplatePart(int start,
      Template template,
      boolean startTagOnSeparateLine,
      boolean endTagOnSeparateLine) {
    super(start, template);
    this.startTagOnSeparateLine = startTagOnSeparateLine;
    this.endTagOnSeparateLine = endTagOnSeparateLine;
  }

  boolean isStartTagOnSeparateLine() {
    return startTagOnSeparateLine;
  }

  boolean isEndTagOnSeparateLine() {
    return endTagOnSeparateLine;
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
