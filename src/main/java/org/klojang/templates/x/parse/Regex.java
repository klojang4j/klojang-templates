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

  /**
   * Regular expression for inline group name prefixes, as in: ~%prefix:varname%.
   */
  static final String REGEX_PREFIX = "([a-zA-Z][a-zA-Z0-9_\\-]*)";

  static final String REGEX_NAME = "([a-zA-Z0-9_\\-]+)";
  static final String REGEX_PATH
      = "("
      + REGEX_NAME
      + "(\\." + REGEX_NAME + ")*"
      + ")";

  static final String REGEX_VARIABLE
      = "~%"
      + "(" + REGEX_PREFIX + ":)?"
      + REGEX_PATH
      + "%";

  static final String REGEX_CMT_VARIABLE
      = REGEX_CMT_START
      + REGEX_VARIABLE
      + REGEX_CMT_END;

  static final String REGEX_INLINE_TEMPLATE
      = "~%%begin:" + REGEX_NAME + "%"
      + "(.*?)"
      + "~%%end:\\1%";

  // Used only for syntax error detection:
  static final String REGEX_INLINE_TEMPLATE_BEGIN = "~%%begin:" + REGEX_NAME + "%";

  static final String REGEX_INLINE_TEMPLATE_END = "~%%end:" + REGEX_NAME + "%";

  static final String REGEX_CMT_INLINE_TEMPLATE
      = REGEX_CMT_START
      + REGEX_INLINE_TEMPLATE
      + REGEX_CMT_END;

  static final String REGEX_INCLUDE_PATH
      = "([a-zA-Z0-9_~:;/?#!$&%,@+.=\\-\\[\\]\\(\\)]+?)";

  static final String REGEX_INCLUDED_TEMPLATE
      = "~%%include:"
      + "(" + REGEX_NAME + ":)?"
      + REGEX_INCLUDE_PATH
      + "%%";

  static final String CMT_REGEX_INCLUDED_TEMPLATE
      = REGEX_CMT_START
      + REGEX_INCLUDED_TEMPLATE
      + REGEX_CMT_END;


  // Used only for syntax error detection:
  static final String REGEX_DITCH_TAG = "<!--%%(.*?)-->";

  static final String REGEX_DITCH_BLOCK
      = REGEX_DITCH_TAG + "(.*?)" + REGEX_DITCH_TAG;

  static final String PLACEHOLDER_TAG = "<!--%-->";

  static final String REGEX_PLACEHOLDER = "<!--%-->(.*?)<!--%-->";

  public static final Pattern VARIABLE = compile(REGEX_VARIABLE);

  public static final Pattern CMT_VARIABLE = compile(REGEX_CMT_VARIABLE);

  public static final Pattern INLINE_TEMPLATE
      = compile(REGEX_INLINE_TEMPLATE, MULTILINE);

  public static final Pattern INLINE_TEMPLATE_BEGIN
      = compile(REGEX_INLINE_TEMPLATE_BEGIN);

  public static final Pattern INLINE_TEMPLATE_END
      = compile(REGEX_INLINE_TEMPLATE_END);

  public static final Pattern CMT_INLINE_TEMPLATE
      = compile(REGEX_CMT_INLINE_TEMPLATE, MULTILINE);

  public static final Pattern INCLUDE_PATH = compile(REGEX_INCLUDE_PATH);

  public static final Pattern INCLUDED_TEMPLATE = compile(REGEX_INCLUDED_TEMPLATE);

  public static final Pattern CMT_INCLUDED_TEMPLATE
      = compile(CMT_REGEX_INCLUDED_TEMPLATE);

  public static final Pattern DITCH_TAG = compile(REGEX_DITCH_TAG, MULTILINE);

  public static final Pattern DITCH_BLOCK = compile(REGEX_DITCH_BLOCK, MULTILINE);

  public static final Pattern PLACEHOLDER = compile(REGEX_PLACEHOLDER, MULTILINE);

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

  private final Pattern variable;
  private final Pattern cmtVariable;
  private final Pattern beginTag;
  private final Pattern endTag;
  private final Pattern inlineTemplate;
  private final Pattern cmtInlineTemplate;
  private final Pattern includedTemplate;
  private final Pattern cmtIncludedTemplate;
  private final Pattern ditchTag;
  private final Pattern ditchBlock;
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
