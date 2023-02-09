package org.klojang.templates.x.parse;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

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

  public static final String PLACEHOLDER_START_END = "<!--%-->";

}
