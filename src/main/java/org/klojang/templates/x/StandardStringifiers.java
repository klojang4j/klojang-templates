package org.klojang.templates.x;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;
import org.apache.http.client.utils.URIBuilder;
import org.klojang.templates.Stringifier;
import org.klojang.templates.VarGroup;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript;
import static org.klojang.templates.VarGroup.*;
import static org.klojang.util.StringMethods.EMPTY_STRING;

public class StandardStringifiers {

  // Copied from StringEscapeUtils and added the 4th LookupTranslator
  private static final CharSequenceTranslator HTML_ATTR_TRANSLATOR =
      new AggregateTranslator(
          new LookupTranslator(EntityArrays.BASIC_ESCAPE),
          new LookupTranslator(EntityArrays.ISO8859_1_ESCAPE),
          new LookupTranslator(EntityArrays.HTML40_EXTENDED_ESCAPE),
          new LookupTranslator(Map.of("'", "&#39;", "\"", "&#34;")));

  public static final Stringifier ESCAPE_HTML = wrap(StringEscapeUtils::escapeHtml4);

  public static final Stringifier ESCAPE_JS = wrap(StringEscapeUtils::escapeEcmaScript);

  public static final Stringifier ESCAPE_ATTR = wrap(HTML_ATTR_TRANSLATOR::translate);

  public static final Stringifier ESCAPE_JS_ATTR = wrap(StandardStringifiers::escapeJsAttr);

  public static final Stringifier ESCAPE_QUERY_PARAM = wrap(StandardStringifiers::escapeParam);

  public static final Stringifier ESCAPE_PATH = wrap(StandardStringifiers::escapePath);

  public static Map<VarGroup, Stringifier> get() {
    return Map.of(
        TEXT, Stringifier.DEFAULT,
        HTML, ESCAPE_HTML,
        JS, ESCAPE_JS,
        ATTR, ESCAPE_ATTR,
        JS_ATTR, ESCAPE_JS_ATTR,
        PARAM, ESCAPE_QUERY_PARAM,
        PATH, ESCAPE_PATH);
  }

  private static String escapeJsAttr(String s) {
    return HTML_ATTR_TRANSLATOR.translate(escapeEcmaScript(s));
  }

  private static String escapeParam(String s) {
    return new URIBuilder().setPathSegments(s).toString().substring(1);
  }

  private static String escapePath(String s) {
    return new URIBuilder().addParameter("x", s).toString().substring(3);
  }

  private static Stringifier wrap(UnaryOperator<String> stringifier) {
    return x -> x == null ? EMPTY_STRING : stringifier.apply(x.toString());
  }

}
