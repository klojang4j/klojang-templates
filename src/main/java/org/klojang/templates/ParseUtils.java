package org.klojang.templates;

import org.klojang.util.StringMethods;

final class ParseUtils {

  static boolean occupiesLine(String src, int from, int to) {
    char c;
    for (int i = from - 1;
        i >= 0 && (c = src.charAt(i)) != '\n' && c != '\r'; --i) {
      if (c != ' ' && c != '\t') {
        return false;
      }
    }
    for (int i = to;
        i < src.length() && (c = src.charAt(i)) != '\n' && c != '\r'; ++i) {
      if (c != ' ' && c != '\t') {
        return false;
      }
    }
    return true;
  }

  static String trimToFristNewline(String txt) {
    String s = StringMethods.rtrim(txt, " \t");
    if (s.endsWith("\n")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

}
