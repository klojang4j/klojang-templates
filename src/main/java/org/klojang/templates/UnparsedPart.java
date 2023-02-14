package org.klojang.templates;

final class UnparsedPart extends AbstractPart {

  private final String text;

  UnparsedPart(String text, int start) {
    super(start);
    this.text = text;
  }

  String text() {
    return text;
  }

  @Override
  public String toString() {
    return text;
  }

}
