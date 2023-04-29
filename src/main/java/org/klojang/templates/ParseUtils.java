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

  static void trimBoilerplate(List<Part> parts) {
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof InlineTemplatePart itp) {
        List<Part> childParts = itp.getTemplate().parts();
        if (itp.isStartTagOnSeparateLine()) {
          if (childParts.get(0) instanceof TextPart tp) {
            tp.setText(removeWhitespaceAfterTag(tp.getText(), false));
          }
          if (i > 0 && parts.get(i - 1) instanceof TextPart tp) {
            tp.setText(removeWhitespaceBeforeTag(tp.getText()));
          }
        }
        if (itp.isEndTagOnSeparateLine()) {
          if (childParts.get(childParts.size() - 1) instanceof TextPart tp) {
            tp.setText(removeWhitespaceBeforeTag(tp.getText()));
          }
          if (i < parts.size() - 1 && parts.get(i + 1) instanceof TextPart tp) {
            tp.setText(removeWhitespaceAfterTag(tp.getText(), false));
          }
        }
      } else if (part instanceof IncludedTemplatePart itp) {
        if (itp.isTagOnSeparateLine()) {
          if (i > 0 && parts.get(i - 1) instanceof TextPart tp) {
            tp.setText(removeWhitespaceBeforeTag(tp.getText()));
          }
          if (i < parts.size() - 1 && parts.get(i + 1) instanceof TextPart tp) {
            tp.setText(removeWhitespaceAfterTag(tp.getText(), true));
          }
        }
      }
    }
  }

  static List<Part> removeEmptyParts(List<Part> parts) {
    List<Part> out = new ArrayList<>(parts.size());
    for (Part p : parts) {
      if (p instanceof NestedTemplatePart ntp) {
        if (((NestedTemplatePart) p).getTemplate().parts().isEmpty()) {
          continue;
        }
      } else if (p instanceof TextPart tp) {
        if (tp.getText().isEmpty()) {
          continue;
        }
      }
      out.add(p);
    }
    return out;
  }

  private static String removeWhitespaceAfterTag(String txt, boolean keepLine) {
    String s = StringMethods.ltrim(txt, " \t");
    if (!keepLine) {
      if (s.startsWith("\r\n")) {
        return s.substring(2);
      } else if (s.startsWith("\n") || s.startsWith("\r")) {
        return s.substring(1);
      }
    }
    return s;
  }

  private static String removeWhitespaceBeforeTag(String txt) {
    return StringMethods.rtrim(txt, " \t");
  }

}
