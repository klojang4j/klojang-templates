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
import static org.klojang.templates.ParseUtils.getMatcher;
import static org.klojang.templates.ParseUtils.todo;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

final class IncludedTemplateParser {

  private final String src;
  private final TemplateLocation loc;

  IncludedTemplateParser(String src, TemplateLocation loc) {
    this.src = src;
    this.loc = loc;
  }

  List<Part> parse(UnparsedPart unparsed,
      Set<String> names,
      Pattern variant)
      throws ParseException {
    Matcher m = getMatcher(variant, unparsed);
    if (!m.find()) {
      return Collections.singletonList(unparsed);
    }
    List<Part> parts = new ArrayList<>();
    int offset = unparsed.start(), end = 0;
    do {
      if (m.start() > end) {
        parts.add(todo(unparsed, end, m.start()));
      }
      String name = m.group(2);
      String path = m.group(3);
      if (name == null) {
        name = IncludedTemplatePart.basename(path);
      }
      TemplateLocation myLoc = new TemplateLocation(path, loc.resolver());
      validate(path, name, myLoc, names, m, offset);
      names.add(name);
      Template nested = TemplateCache.INSTANCE.get(myLoc, name);
      if (!nested.getName().equals(name)) {
        nested = new Template(nested, name);
      }
      parts.add(new IncludedTemplatePart(offset + m.start(), nested));
      end = m.end();
    } while (m.find());
    if (end < unparsed.text().length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;
  }

  private void validate(String path,
      String name,
      TemplateLocation location,
      Set<String> names,
      Matcher matcher,
      int offset) throws ParseException {
    Check.that(name).isNot(equalTo(), ROOT_TEMPLATE_NAME,
        ILLEGAL_TMPL_NAME.getExceptionSupplier(src,
            offset + matcher.start(1),
            name));
    Check.that(name).isNot(in(), names,
        DUPLICATE_TMPL_NAME.getExceptionSupplier(src,
            offset + matcher.start(1),
            name));
    Check.that(location.isValid()).is(yes(),
        INVALID_INCLUDE_PATH.getExceptionSupplier(src,
            offset + matcher.start(3),
            path));
  }

}

