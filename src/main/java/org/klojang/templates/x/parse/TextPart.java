package org.klojang.templates.x.parse;

public final class TextPart extends AbstractPart {

  private final String text;

  TextPart(String text, int start) {
    super(start);
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return text;
  }
}
