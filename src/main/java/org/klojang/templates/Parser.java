package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.fallible.FallibleBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.InlineTemplateParser.CommentType;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.ParseUtils.*;
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
    InlineTemplateParser itp = new InlineTemplateParser(src, location);
    List<Part> parts = purgeDitchBlocks();
    parts = parse(parts, names, (x, y) -> itp.parse(x, y, CommentType.TAGS));
    parts = parse(parts, names, (x, y) -> itp.parse(x, y, CommentType.BLOCK));
    parts = parse(parts, names, (x, y) -> itp.parse(x, y, CommentType.NONE));
    parts = parse(parts, names,
        (x, y) -> parseIncludedTemplates(x, y, CMT_INCLUDED_TEMPLATE));
    parts = parse(parts, names,
        (x, y) -> parseIncludedTemplates(x, y, INCLUDED_TEMPLATE));
    parts = parse(parts, names, (x, y) -> parseVars(x, y, CMT_VARIABLE));
    parts = parse(parts, names, (x, y) -> parseVars(x, y, VARIABLE));
    parts = collectTextParts(parts);
    parts = suppressNewLines(parts);
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

  private List<Part> parseIncludedTemplates(UnparsedPart unparsed,
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
      Check.that(name).isNot(equalTo(), ROOT_TEMPLATE_NAME,
          ILLEGAL_TMPL_NAME.getExceptionSupplier(src, offset + m.start(1), name));
      Check.that(name).isNot(in(), names,
          DUPLICATE_TMPL_NAME.getExceptionSupplier(src, offset + m.start(1), name));
      TemplateLocation loc = new TemplateLocation(path, location.resolver());
      if (!loc.isValid()) {
        throw INVALID_INCLUDE_PATH.getException(src, offset + m.start(3), path);
      }
      names.add(name);
      Template nested = TemplateCache.INSTANCE.get(loc, name);
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

  private List<Part> parseVars(UnparsedPart unparsed,
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
      Check.that(name).isNot(in(), names, VAR_NAME_WITH_TMPL_NAME
          .getExceptionSupplier(src, offset + m.start(3), name));
      if (VarGroup.DEF.getName().equals(prefix) && placeholder == null) {
        throw NO_PLACEHOLDER_DEFINED.getException(src, offset + m.start(3), name);
      }
      parts.add(new VariablePart(offset + m.start(), prefix, name, placeholder));
      end = m.end();
    } while (m.find());
    if (end < unparsed.text().length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;
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
          if (purified.contains(PLACEHOLDER_START_END)) {
            int idx = p.start() + text.indexOf(PLACEHOLDER_START_END);
            throw PLACEHOLDER_NOT_CLOSED.getException(src, idx);
          }
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
    Matcher m = INLINE_TEMPLATE_END.matcher(str);
    if (m.find()) {
      throw DANGLING_END_TAG.getException(src, off + m.start(), m.group(2));
    }
    m = DITCH_TAG.matcher(str);
    if (m.find()) {
      throw DITCH_BLOCK_NOT_CLOSED.getException(src, off + m.start());
    }
    int idx = str.indexOf("~%%begin:");
    Check.that(idx).is(eq(), -1,
        BEGIN_TAG_NOT_TERMINATED.getExceptionSupplier(src, off + idx));
    idx = str.indexOf("~%%end:");
    Check.that(idx).is(eq(), -1,
        END_TAG_NOT_TERMINATED.getExceptionSupplier(src, off + idx));
    idx = str.indexOf("~%%include:");
    Check.that(idx).is(eq(), -1,
        INCLUDE_TAG_NOT_TERMINATED.getExceptionSupplier(src, off + idx));
  }

  /*
   * Remove the last line from text parts if, in the original template, it was the
   * line containing ~%%begin:foo% (and nothing else).
   */
  private static List<Part> suppressNewLines(List<Part> parts) {
    List<Part> out = new ArrayList<>(parts.size());
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof TextPart tp) {
        if (i < parts.size() - 1
            && parts.get(i + 1) instanceof InlineTemplatePart itp
            && itp.isStartTagOnSeparateLine()
        ) {
          String trimmed = deleteEmptyLine(tp.getText());
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
