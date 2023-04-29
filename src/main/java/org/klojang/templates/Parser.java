package org.klojang.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

import static org.klojang.templates.InlineTemplateParser.CommentType;
import static org.klojang.templates.ParseUtils.removeEmptyParts;
import static org.klojang.templates.ParseUtils.trimBoilerplate;
import static org.klojang.templates.Regex.*;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

final class Parser {

  private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

  private interface PartialParser {

    List<Part> parse(UnparsedPart unparsed, Set<String> names) throws ParseException;

  }

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
    log(name, location);
    // Accumulates template names for duplicate checks:
    Set<String> names = new HashSet<>();
    List<Part> parts = purgeDitchBlocks();
    InlineTemplateParser p1 = new InlineTemplateParser(src, location);
    parts = parse(parts, names, (x, y) -> p1.parse(x, y, CommentType.TAGS));
    parts = parse(parts, names, (x, y) -> p1.parse(x, y, CommentType.BLOCK));
    parts = parse(parts, names, (x, y) -> p1.parse(x, y, CommentType.NONE));
    IncludedTemplateParser p2 = new IncludedTemplateParser(src, location);
    parts = parse(parts, names, (x, y) -> p2.parse(x, y, CMT_INCLUDED_TEMPLATE));
    parts = parse(parts, names, (x, y) -> p2.parse(x, y, INCLUDED_TEMPLATE));
    VarParser p3 = new VarParser(src);
    parts = parse(parts, names, (x, y) -> p3.parse(x, y, CMT_VARIABLE));
    parts = parse(parts, names, (x, y) -> p3.parse(x, y, VARIABLE));
    BoilerplateCollector bc = new BoilerplateCollector(src);
    parts = bc.collectBoilerplate(parts);
    trimBoilerplate(parts);
    parts = removeEmptyParts(parts);
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
        out.addAll(parser.parse(unparsed, names));
      } else {
        out.add(p);
      }
    }
    return out;
  }

  private static void log(String name, TemplateLocation location) {
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
