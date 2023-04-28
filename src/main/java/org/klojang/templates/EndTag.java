package org.klojang.templates;

record EndTag(int start, int end) {

  boolean isOnSeparateLine(UnparsedPart src) {
    return ParseUtils.onSeparateLine(src.text(), start, end);
  }

}
