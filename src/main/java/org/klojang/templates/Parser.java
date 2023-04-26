package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.fallible.FallibleBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

import static org.klojang.check.CommonChecks.eq;
import static org.klojang.templates.InlineTemplateParser.CommentType;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.ParseUtils.deleteEmptyLastLine;
import static org.klojang.templates.Regex.*;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

final class Parser {

  private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

  private interface PartialParser extends
      FallibleBiFunction<UnparsedPart, Set<String>, List<Part>, ParseException> {}

  private final String name; // template name
  private final TemplateLocation location;
  private final String src;

  Parser(TemplateLocation location, String name) throws ParseException {
    this(location, name, location.read());
  }

  Parser(TemplateLocation location, String name, String src) {
    this.name = name;
    this.location = location;
    this.src = src;
  }

  Template parse() throws ParseException {
    return new Template(name, location, List.copyOf(getParts()));
  }

  List<Part> getParts() throws ParseException {
    logParsing(name, location);
    // Accumulates template names for duplicate checks:
    Set<String> names = new HashSet<>();
    List<Part> parts = purgeDitchBlocks();
    InlineTemplateParser pp1 = new InlineTemplateParser(src, location);
    parts = parse(parts, names, (x, y) -> pp1.parse(x, y, CommentType.TAGS));
    parts = parse(parts, names, (x, y) -> pp1.parse(x, y, CommentType.BLOCK));
    parts = parse(parts, names, (x, y) -> pp1.parse(x, y, CommentType.NONE));
    IncludedTemplateParser pp2 = new IncludedTemplateParser(src, location);
    parts = parse(parts, names, (x, y) -> pp2.parse(x, y, CMT_INCLUDED_TEMPLATE));
    parts = parse(parts, names, (x, y) -> pp2.parse(x, y, INCLUDED_TEMPLATE));
    VarParser pp3 = new VarParser(src);
    parts = parse(parts, names, (x, y) -> pp3.parse(x, y, CMT_VARIABLE));
    parts = parse(parts, names, (x, y) -> pp3.parse(x, y, VARIABLE));
    parts = collectTextParts(parts);
    parts = deleteEmptyLastLine(parts);
    return parts;
  }

  private List<Part> purgeDitchBlocks() {
    Matcher m = Regex.DITCH_BLOCK.matcher(src);
    if (!m.find()) {
      return Collections.singletonList(new UnparsedPart(src, 0));
    }
    List<Part> parts = new ArrayList<>();
    int end = 0;
    do {
      int start = m.start();
      if (start > end) {
        parts.add(new UnparsedPart(src.substring(end, start), end));
      }
      end = m.end();
    } while (m.find());
    if (end < src.length()) {
      parts.add(new UnparsedPart(src.substring(end), end));
    }
    return parts;
  }

  private static List<Part> parse(List<Part> in,
      Set<String> names,
      PartialParser parser)
      throws ParseException {
    List<Part> out = new ArrayList<>(in.size() + 10);
    for (Part p : in) {
      if (p instanceof UnparsedPart unparsed) {
        out.addAll(parser.apply(unparsed, names));
      } else {
        out.add(p);
      }
    }
    return out;
  }

  /*
   * Text parts are all unparsed parts that remain after everything else has been
   * parsed out
   */
  private List<Part> collectTextParts(List<Part> in) throws ParseException {
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

  static void logParsing(String name, TemplateLocation location) {
    if (LOG.isTraceEnabled()) {
      if (name.equals(ROOT_TEMPLATE_NAME)) {
        LOG.trace("Parsing root template");
      } else if (location.isString()) {
        LOG.trace("Parsing inline template \"{}\"", name);
      } else {
        LOG.trace("Parsing included template \"{}\"", name);
      }
    }
  }

}
