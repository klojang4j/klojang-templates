package org.klojang.templates;

import org.klojang.check.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.klojang.check.CommonChecks.eq;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.Regex.*;

/*
 * Converts all parts of the template that remain unparsed after all variables and
 * nested templates have been extracted to TextPart instances. In other words,
 * anything that's not a variable or nested template is a TextPart.
 */
final class BoilerplateCollector {

  private final String src;

  BoilerplateCollector(String src) {
    this.src = src;
  }

  List<Part> collectBoilerplate(List<Part> in) throws ParseException {
    List<Part> out = new ArrayList<>(in.size());
    for (Part p : in) {
      if (p instanceof UnparsedPart unparsed) {
        String text = unparsed.text();
        if (!text.isEmpty()) {
          checkGarbage(unparsed);
          String purified = PLACEHOLDER.matcher(text).replaceAll("");
          int idx = text.indexOf(PLACEHOLDER_TOKEN);
          Check.that(idx).is(eq(), -1, PLACEHOLDER_NOT_CLOSED
              .getExceptionSupplier(src, p.start() + idx));
          out.add(new TextPart(purified, p.start()));
        }
      } else {
        out.add(p);
      }
    }
    return out;
  }

  private void checkGarbage(UnparsedPart unparsed) throws ParseException {
    String str = unparsed.text();
    int off = unparsed.start();
    Matcher matcher = INLINE_TEMPLATE_END.matcher(str);
    if (matcher.find()) {
      throw DANGLING_END_TAG
          .getException(src, off + matcher.start(), matcher.group(2));
    }
    int idx = str.indexOf("~%%begin:");
    Check.that(idx).is(eq(), -1, BEGIN_TAG_NOT_TERMINATED
        .getExceptionSupplier(src, off + idx));
    idx = str.indexOf("~%%end:");
    Check.that(idx).is(eq(), -1, END_TAG_NOT_TERMINATED
        .getExceptionSupplier(src, off + idx));
    idx = str.indexOf("~%%include:");
    Check.that(idx).is(eq(), -1, INCLUDE_TAG_NOT_TERMINATED
        .getExceptionSupplier(src, off + idx));
    idx = str.indexOf(DITCH_BLOCK_TOKEN);
    Check.that(idx).is(eq(), -1, DITCH_BLOCK_NOT_CLOSED
        .getExceptionSupplier(src, off + idx));
  }

}
