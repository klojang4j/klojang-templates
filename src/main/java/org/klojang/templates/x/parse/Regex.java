package org.klojang.templates.x.parse;

import org.klojang.check.Check;
import org.klojang.check.ObjectCheck;
import org.klojang.templates.ParseException;

import java.util.Set;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static org.klojang.check.CommonChecks.EQ;
import static org.klojang.check.CommonChecks.blank;

public final class Regex {

  private static final int MULTILINE = Pattern.MULTILINE | Pattern.DOTALL;

  private static final String REGEX_CMT_START = "<!--\\s*";
  private static final String REGEX_CMT_END = "\\s*-->";

  private static final String REGEX_VAR_GROUP_PREFIX = "([a-zA-Z][a-zA-Z0-9_\\-]*)";

  private static final String REGEX_SEGMENT = "(([a-zA-Z_]\\w*)|\\d+)";
  private static final String REGEX_PATH
      = "("
      + REGEX_SEGMENT
      + "(\\." + REGEX_SEGMENT + ")*"
      + ")";

  private static final String REGEX_VARIABLE
      = "~%"
      + "(" + REGEX_VAR_GROUP_PREFIX + ":)?"
      + REGEX_PATH
      + "%";

  private static final String REGEX_CMT_VARIABLE
      = REGEX_CMT_START
      + REGEX_VARIABLE
      + REGEX_CMT_END;
  private static final String REGEX_INLINE_TEMPLATE
      = "~%%begin:" + REGEX_PATH + "%"
      + "(.*?)"
      + "~%%end:\\1%";

  private static final String REGEX_CMT_INLINE_TEMPLATE
      = REGEX_CMT_START
      + REGEX_INLINE_TEMPLATE
      + REGEX_CMT_END;

  public static final Pattern VARIABLE = compile(REGEX_VARIABLE);
  public static final Pattern CMT_VARIABLE = compile(REGEX_CMT_VARIABLE);
  public static final Pattern INLINE_TEMPLATE
      = compile(REGEX_INLINE_TEMPLATE, MULTILINE);
  public static final Pattern CMT_INLINE_TEMPLATE
      = compile(REGEX_CMT_INLINE_TEMPLATE, MULTILINE);

  private static final String ERR_ILLEGAL_VAL = "Illegal value for system property %s: \"%s\"";
  private static final String ERR_IDENTICAL = "varStart and tmplStart must be different";

  public static final String VAR_START = "~%";
  public static final String VAR_END = "%";
  public static final String TMPL_START = "~%%";
  public static final String TMPL_END = VAR_END;

  public static final String PLACEHOLDER_START_END = "<!--%-->";

  private static Regex instance;

  public static Regex of() throws ParseException {
    if (instance == null) {
      return instance = new Regex();
    }
    return instance;
  }

  public final Pattern variable;
  public final Pattern cmtVariable;
  public final Pattern beginTag;
  public final Pattern endTag;
  public final Pattern inlineTemplate;
  public final Pattern cmtInlineTemplate;
  public final Pattern includedTemplate;
  public final Pattern cmtIncludedTemplate;
  public final Pattern ditchTag;
  public final Pattern ditchBlock;
  public final Pattern placeholder;

  private Regex() throws ParseException {

    checkSysProps();

    String vStart = quote(VAR_START);
    String vEnd = quote(VAR_END);
    String tStart = quote(TMPL_START);
    String tEnd = quote(TMPL_END);

    String name = "([a-zA-Z0-9_\\.\\-]+)";

    String cmtStart = "<!--\\s*";

    String cmtEnd = "\\s*-->";

    //String ptnVariable = vStart + "(" + name + ":)?(.+?)" + vEnd;
    String ptnVariable = "~%(([a-zA-Z_]\\w*):)?([a-zA-Z0-9_\\.\\-]+)%";
    String ptnCmtVariable = cmtStart + ptnVariable + cmtEnd;

    String ptnInlineBegin = tStart + "begin:" + name + tEnd;

    String ptnInlineEnd = tStart + "end:" + name + tEnd;

    String ptnInlineTmpl = ptnInlineBegin + "(.*?)" + tStart + "end:\\1" + tEnd;

    String ptnCmtInlineTmpl =
        cmtStart
            + ptnInlineBegin
            + cmtEnd
            + "(.*?)"
            + cmtStart
            + tStart
            + "end:\\1"
            + tEnd
            + cmtEnd;

    String ptnIncludedTmpl = tStart + "include:(" + name + ":)?(.+?)" + tEnd;

    String ptnCmtIncludedTmpl = cmtStart + ptnIncludedTmpl + cmtEnd;

    String ptnDitchTag = "<!--%%(.*?)-->";

    String ptnDitchBlock = ptnDitchTag + "(.*?)" + ptnDitchTag;

    String ptnPlaceholder = PLACEHOLDER_START_END + "(.*?)" + PLACEHOLDER_START_END;

    // Equivalent to prefixing the regular expression with "(?ms)"
    int msModifiers = Pattern.MULTILINE | Pattern.DOTALL;

    this.variable = compile(ptnVariable);
    this.cmtVariable = compile(ptnCmtVariable);
    this.beginTag = compile(ptnInlineBegin);
    this.endTag = compile(ptnInlineEnd);
    this.inlineTemplate = compile(ptnInlineTmpl, msModifiers);
    this.cmtInlineTemplate = compile(ptnCmtInlineTmpl, msModifiers);
    this.includedTemplate = compile(ptnIncludedTmpl);
    this.cmtIncludedTemplate = compile(ptnCmtIncludedTmpl);
    this.ditchTag = compile(ptnDitchTag);
    this.ditchBlock = compile(ptnDitchBlock, msModifiers);
    this.placeholder = compile(ptnPlaceholder, msModifiers);
  }

  private static void checkSysProps() throws ParseException {
    checkThat(VAR_START).isNot(blank(), ERR_ILLEGAL_VAL, VAR_START);
    checkThat(VAR_END).isNot(blank(), ERR_ILLEGAL_VAL, VAR_END);
    checkThat(TMPL_START)
        .isNot(blank(), ERR_ILLEGAL_VAL, TMPL_START)
        .isNot(EQ(), VAR_START, ERR_IDENTICAL);
    checkThat(TMPL_END).isNot(blank(), ERR_ILLEGAL_VAL, TMPL_END);
  }

  public void printAll() {
    System.out.println("VARIABLE .......: " + variable);
    System.out.println("VARIABLE_CMT ...: " + cmtVariable);
    System.out.println("NESTED .........: " + inlineTemplate);
    System.out.println("NESTED_CMT .....: " + cmtInlineTemplate);
    System.out.println("INCLUDE ........: " + includedTemplate);
    System.out.println("INCLUDE_CMT ....: " + cmtIncludedTemplate);
    System.out.println("DITCH_BLOCK: ...: " + ditchBlock);
    System.out.println("PLACEHOLDER: ...: " + placeholder);
  }

  private static ObjectCheck<String, ParseException> checkThat(String sysprop) {
    return Check.on(ParseException::new, sysprop);
  }

  private static String quote(String token) {
    String special = "\\^$.|?*+()[]{}";
    Set<Integer> specialChars = special.codePoints().boxed().collect(toSet());
    Set<Integer> tokenChars = token.codePoints().boxed().collect(toSet());
    return tokenChars.stream().anyMatch(specialChars::contains)
        ? Pattern.quote(token)
        : token;
  }

}
