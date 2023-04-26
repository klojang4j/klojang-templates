package org.klojang.templates;

import org.klojang.check.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonChecks.inArray;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.ParseUtils.getMatcher;
import static org.klojang.templates.ParseUtils.todo;
import static org.klojang.templates.Regex.CMT_VARIABLE;
import static org.klojang.util.ArrayMethods.pack;

final class VarParser {

  private final String src;

  VarParser(String src) {
    this.src = src;
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
      String prefix = m.group(2);
      String name = m.group(3);
      String placeholder = variant == CMT_VARIABLE ? m.group(8) : null;
      checkVar(prefix, name, placeholder, names, m, offset);
      parts.add(new VariablePart(offset + m.start(), prefix, name, placeholder));
      end = m.end();
    } while (m.find());
    if (end < unparsed.text().length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;
  }

  private void checkVar(String prefix,
      String name,
      String placeholder,
      Set<String> names,
      Matcher matcher,
      int offset) throws ParseException {
    Check.that(name).isNot(in(), names,
        VAR_WITH_TMPL_NAME.getExceptionSupplier(src,
            offset + matcher.start(3),
            name));
    if (prefix != null) {
      String def = VarGroup.DEF.getName();
      Check.that(placeholder).is(notNull().orNot(prefix, EQ(), def),
          NO_PLACEHOLDER_DEFINED.getExceptionSupplier(src,
              offset + matcher.start(2),
              name));
      Check.that(prefix).isNot(inArray(), pack("begin", "end", "include"),
          ILLEGAL_VAR_PREFIX.getExceptionSupplier(src,
              offset + matcher.start(2),
              prefix));
    }
  }

}
