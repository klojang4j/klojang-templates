package org.klojang.templates;

import org.klojang.util.StringMethods;

final class ParseUtils {

  /**
   * Determines whether the text between from and to finds itself on an otherwise
   * empty line. This is used to ascertain this for ~%%begin:foo% and ~%%end:foo%. If
   * the begin or end tag of an inline template finds itself on an otherwise empty
   * line, that entire line is removed from the template when it is rendered.
   */
  static boolean occupiesLine(String src, int from, int to) {
    char c;
    for (int i = from - 1; i >= 0 && (c = src.charAt(i)) != '\n'; --i) {
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

  static String deleteEmptyLine(String txt) {
    String s = StringMethods.rtrim(txt, " \t");
    if (s.endsWith("\n")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

}
