package org.klojang.templates;

final class TextPart extends AbstractPart {

  private String text;

  TextPart(String text, int start) {
    super(start);
    this.text = text;
  }

  String getText() {
    return text;
  }

  void setText(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }

}
