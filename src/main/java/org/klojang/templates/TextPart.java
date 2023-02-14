package org.klojang.templates;

final class TextPart extends AbstractPart {

  private final String text;

  TextPart(String text, int start) {
    super(start);
    this.text = text;
  }

  String getText() {
    return text;
  }

  @Override
  public String toString() {
    return text;
  }

}
