package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.fallible.FallibleBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.ParseErrorCode.*;
import static org.klojang.templates.ParseUtils.deleteEmptyLine;
import static org.klojang.templates.ParseUtils.onSeparateLine;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;
import static org.klojang.util.StringMethods.EMPTY_STRING;

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

  // visible for testing
  List<Part> getParts() throws ParseException {
    logParsing(name, location);
    // Accumulates template names for duplicate checks:
    Set<String> names = new HashSet<>();
    List<Part> parts = List.of(new UnparsedPart(src, 0));
    parts = purgeDitchBlocks(parts);
    parts = parse(parts, names, (x, y) -> parseInlineTemplates(x, y, true));
    parts = parse(parts, names, (x, y) -> parseInlineTemplates(x, y, false));
    parts = parse(parts, names, (x, y) -> parseIncludedTemplates(x, y, true));
    parts = parse(parts, names, (x, y) -> parseIncludedTemplates(x, y, false));
    parts = parse(parts, names, (x, y) -> parseVars(x, y, true));
    parts = parse(parts, names, (x, y) -> parseVars(x, y, false));
    parts = collectTextParts(parts);
    parts = suppressNewLines(parts);
    return parts;
  }

  private static List<Part> parse(List<Part> in,
      Set<String> names,
      PartialParser parser)
      throws ParseException {
    List<Part> out = new ArrayList<>(in.size() + 10);
    for (Part p : in) {
      if (p.getClass() == UnparsedPart.class) {
        out.addAll(parser.apply((UnparsedPart) p, names));
      } else {
        out.add(p);
      }
    }
    return out;
  }

  private static List<Part> purgeDitchBlocks(List<Part> parts) {
    List<Part> out = new ArrayList<>();
    // There will always be just 1 part, and it will always be
    // an UnparsedPart. Nevertheless, let's act as if we don't
    // know this
    for (Part p : parts) {
      if (p instanceof UnparsedPart) {
        out.addAll(purgeDitchBlocksInPart((UnparsedPart) p));
      } else {
        out.add(p);
      }
    }
    return out;
  }

  private static List<Part> purgeDitchBlocksInPart(UnparsedPart unparsed) {
    String src = unparsed.text();
    Matcher m = Regex.DITCH_BLOCK.matcher(src);
    if (!m.find()) {
      return Collections.singletonList(unparsed);
    }
    List<Part> parts = new ArrayList<>();
    int end = 0;
    do {
      int start = m.start();
      if (start > end) {
        parts.add(todo(unparsed, end, m.start()));
      }
      end = m.end();
    } while (m.find());
    if (end < src.length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;
  }

  private List<Part> parseInlineTemplates(UnparsedPart unparsed,
      Set<String> names,
      boolean inComments)
      throws ParseException {
    Pattern p = inComments ? Regex.CMT_INLINE_TEMPLATE : Regex.INLINE_TEMPLATE;
    Matcher m = match(p, unparsed);
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
      String mySrc = m.group(3);
      Check.that(name).isNot(equalTo(), ROOT_TEMPLATE_NAME,
          ILLEGAL_TMPL_NAME.getExceptionSupplier(src, offset + m.start(2), name));
      Check.that(name).isNot(in(), names,
          DUPLICATE_TMPL_NAME.getExceptionSupplier(src, offset + m.start(2), name));
      names.add(name);
      // No path is associated with an inline template, but it inherits the
      // PathResolver of the template in which it is nested
      TemplateLocation loc = new TemplateLocation(location.resolver());
      // If ~%%end:foo% is all by itself on a separate line, except possibly
      // surrounded by whitespace, then that whole line will be removed.
      if (onSeparateLine(src, m.start(4), m.end(4))) {
        mySrc = deleteEmptyLine(mySrc);
      }
      Parser parser = new Parser(loc, name, mySrc);
      parts.add(
          new InlineTemplatePart(offset + m.start(), parser.parse(),
              onSeparateLine(src, m.start(1), m.end(1))));
      end = m.end();
    } while (m.find());
    if (end < unparsed.text().length()) {
      parts.add(todo(unparsed, end, unparsed.text().length()));
    }
    return parts;
  }

  private List<Part> parseIncludedTemplates(UnparsedPart unparsed,
      Set<String> names,
      boolean inComments)
      throws ParseException {
    Pattern p = inComments ? Regex.CMT_INCLUDED_TEMPLATE : Regex.INCLUDED_TEMPLATE;
    Matcher m = match(p, unparsed);
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
      if (loc.isInvalid()) {
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
      boolean inComments)
      throws ParseException {
    Pattern p = inComments ? Regex.CMT_VARIABLE : Regex.VARIABLE;
    Matcher m = match(p, unparsed);
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
      String placeholder = inComments ? m.group(8) : null;
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
      if (p.getClass() == UnparsedPart.class) {
        UnparsedPart unparsed = (UnparsedPart) p;
        if (unparsed.text().length() != 0) {
          checkGarbage(unparsed);
          String text = Regex.PLACEHOLDER
              .matcher(unparsed.text())
              .replaceAll(EMPTY_STRING);
          if (text.contains(Regex.PLACEHOLDER_START_END)) {
            int idx = p.start()
                + unparsed.text().indexOf(Regex.PLACEHOLDER_START_END);
            throw PLACEHOLDER_NOT_CLOSED.getException(text, idx);
          }
          out.add(new TextPart(text, p.start()));
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
    Matcher m = Regex.INLINE_TEMPLATE_BEGIN.matcher(str);
    if (m.find()) {
      throw MISSING_END_TAG.getException(src, off + m.start(), m.group(1));
    }
    m = Regex.INLINE_TEMPLATE_END.matcher(str);
    if (m.find()) {
      throw DANGLING_END_TAG.getException(src, off + m.start(), m.group(1));
    }
    m = Regex.DITCH_TAG.matcher(str);
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

  private static Matcher match(Pattern pattern, UnparsedPart unparsed) {
    return pattern.matcher(unparsed.text());
  }

  private static UnparsedPart todo(UnparsedPart p, int from, int to) {
    String s = p.text().substring(from, to);
    return new UnparsedPart(s, from + p.start());
  }

  private static void logParsing(String name, TemplateLocation location) {
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
