package org.klojang.templates;

import org.klojang.check.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.ParseUtils.*;
import static org.klojang.templates.Regex.REGEX_NAME;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

final class InlineTemplateParser {

  enum CommentType {
    NONE, TAGS, BLOCK;

    private Pattern beginPattern() {
      return switch (this) {
        case NONE -> Pattern.compile("~%%begin:" + REGEX_NAME + "%");
        case TAGS -> Pattern.compile("<!-- ?~%%begin:" + REGEX_NAME + "% ?-->");
        case BLOCK -> Pattern.compile("<!-- ?~%%begin:" + REGEX_NAME + "%");
      };
    }

    private Pattern beginPattern(String tmplName) {
      return switch (this) {
        case NONE -> Pattern.compile("~%%begin:" + tmplName + "%");
        case TAGS -> Pattern.compile("<!-- ?~%%begin:" + tmplName + "% ?-->");
        case BLOCK -> Pattern.compile("<!-- ?~%%begin:" + tmplName + "%");
      };
    }

    private Pattern endPattern(String tmplName) {
      return switch (this) {
        case NONE -> Pattern.compile("~%%end:" + tmplName + "%");
        case TAGS -> Pattern.compile("<!-- ?~%%end:" + tmplName + "% ?-->");
        case BLOCK -> Pattern.compile("~%%end:" + tmplName + "% ?-->?");
      };
    }

  }

  private record EndTag(int start, int end) {}

  private final String src;
  private final TemplateLocation loc;

  InlineTemplateParser(String src, TemplateLocation loc) {
    this.src = src;
    this.loc = loc;
  }

  List<Part> parse(UnparsedPart unparsed, Set<String> names, CommentType type)
      throws ParseException {
    Matcher m = getMatcher(type.beginPattern(), unparsed);
    if (!m.find()) {
      return Collections.singletonList(unparsed);
    }
    List<Part> parts = new ArrayList<>();
    int offset = unparsed.start(), end = 0;
    do {
      if (m.start() > end) {
        parts.add(todo(unparsed, end, m.start()));
      }
      String name = m.group(1);
      validate(name, names, m, offset);
      names.add(name);
      EndTag endTag = getEndTag(type, unparsed, name, m.end(), 0);
      if (m.end() != endTag.start()) {
        String mySrc = unparsed.text().substring(m.end(), endTag.start());
        TemplateLocation myLoc = new TemplateLocation(loc.resolver());
        Parser parser = new Parser(myLoc, name, mySrc);
        parts.add(new InlineTemplatePart(offset + m.start(),
            parser.parse(),
            onSeparateLine(unparsed.text(), m.start(), m.end()),
            onSeparateLine(unparsed.text(), endTag.start(), endTag.end())));
      }
      end = endTag.end();
    } while (m.find(end));
    if (end < unparsed.text().length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;

  }

  private EndTag getEndTag(
      CommentType type,
      UnparsedPart unparsed,
      String tmplName,
      int offset,
      int level)
      throws ParseException {
    Matcher mEnd = type.endPattern(tmplName).matcher(unparsed.text());
    Check.that(mEnd.find(offset)).is(yes(),
        MISSING_END_TAG.getExceptionSupplier(src, offset, tmplName));
    Matcher mStart = type.beginPattern(tmplName).matcher(unparsed.text());
    if (!mStart.find(offset)) {
      if (level == 0) {
        return new EndTag(mEnd.start(), mEnd.end());
      }
      return getEndTag(type, unparsed, tmplName, mEnd.end(), --level);
    }
    if (mEnd.start() < mStart.start()) {
      if (level == 0) {
        return new EndTag(mEnd.start(), mEnd.end());
      }
      return getEndTag(type, unparsed, tmplName, mEnd.end(), --level);
    }
    return getEndTag(type, unparsed, tmplName, mStart.end(), ++level);
  }

  private void validate(String name, Set<String> names, Matcher matcher, int offset)
      throws ParseException {
    Check.that(name).isNot(equalTo(), ROOT_TEMPLATE_NAME,
        ILLEGAL_TMPL_NAME.getExceptionSupplier(src,
            offset + matcher.start(1),
            name));
    Check.that(name).isNot(in(), names,
        DUPLICATE_TMPL_NAME.getExceptionSupplier(src,
            offset + matcher.start(1),
            name));
  }

}
