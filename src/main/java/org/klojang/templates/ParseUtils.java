package org.klojang.templates;

import org.klojang.util.StringMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ParseUtils {

  /*
   * Determines whether the text between from and to finds itself on an otherwise
   * empty line. This is used to ascertain this for ~%%begin:foo% and ~%%end:foo%. If
   * the begin or end tag of an inline template finds itself on an otherwise empty
   * line, then that entire line is removed from the template when it is rendered.
   */
  static boolean onSeparateLine(String src, int from, int to) {
    char c;
    for (int i = from - 1; i >= 0 && (c = src.charAt(i)) != '\n' && c != '\r'; --i) {
      if (c != ' ' && c != '\t') {
        return false;
      }
    }
    int len = src.length();
    for (int i = to; i < len && (c = src.charAt(i)) != '\n' && c != '\r'; ++i) {
      if (c != ' ' && c != '\t') {
        return false;
      }
    }
    return true;
  }

  static Matcher getMatcher(Pattern pattern, UnparsedPart unparsed) {
    return pattern.matcher(unparsed.text());
  }

  static UnparsedPart todo(UnparsedPart p, int from, int to) {
    String s = p.text().substring(from, to);
    return new UnparsedPart(s, p.start() + from);
  }

  /*
   * Remove the first line of an inline template if it was the containing
   * ~%%begin:foo%, followed by a newline character (which would belong to the inline
   * template).
   */
  static String deleteEmptyLastLine(String txt) {
    String s = StringMethods.rtrim(txt, " \t");
    if (s.endsWith("\r\n")) {
      return s.substring(0, s.length() - 2);
    } else if (s.endsWith("\n")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

  /*
   * Remove the last line from text parts if, in the original template source, it was
   * a line containing a nested template tag (and nothing else but whitespace).
   */
  static List<Part> deleteEmptyLastLine(List<Part> parts) {
    List<Part> out = new ArrayList<>(parts.size());
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof TextPart tp) {
        if (i < parts.size() - 1 && mustTrim(parts.get(i + 1))) {
          String trimmed = deleteEmptyLastLine(tp.getText());
          if (!trimmed.isEmpty()) {
            out.add(new TextPart(trimmed, part.start()));
          }
        } else {
          out.add(part);
        }
      } else {
        out.add(part);
      }
    }
    return out;
  }

  private static boolean mustTrim(Part part) {
    return (part instanceof InlineTemplatePart x && x.isStartTagOnSeparateLine())
        || (part instanceof IncludedTemplatePart y && y.isTagOnSeparateLine());
  }

}
