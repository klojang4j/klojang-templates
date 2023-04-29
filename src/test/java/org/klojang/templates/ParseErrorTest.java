package org.klojang.templates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.klojang.templates.ParseErrorCode.*;

public class ParseErrorTest {

  @Test
  public void ditchTagNotClosed00() throws ParseException {
    String src = """
        <table>
        <!--%%-->
        </table>
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.DITCH_BLOCK_NOT_CLOSED, e.getErrorCode());
    }
  }

  @Test
  public void missingEndTag00() {
    String src = """
        ~%%begin:foo%
          <p>Hello world
        ~%%end:bar%
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.MISSING_END_TAG, e.getErrorCode());
    }
  }

  @Test
  public void missingEndTag0100() {
    String src = """
        ~%%begin:foo%
          <p>Hello world
        ~%%end:foo
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.MISSING_END_TAG, e.getErrorCode());
    }
  }

  @Test
  public void danglingEndTag00() {
    String src = """
        ~%%begin:foo%
          <p>Hello world
        ~%%end:foo%
        ~%%end:bar%
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.DANGLING_END_TAG, e.getErrorCode());
    }
  }

  @Test
  public void beginTagNotTerminated00() {
    String src = """
        ~%%begin:foo
          <p>Hello world
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.BEGIN_TAG_NOT_TERMINATED, e.getErrorCode());
    }
  }

  @Test
  public void endTagNotTerminated00() {
    String src = """
        ~%%begin:foo%
          <p>Hello world
        ~%%end:foo%
        ~%%end:bar
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.END_TAG_NOT_TERMINATED, e.getErrorCode());
    }
  }

  @Test
  public void duplicateTmplName00() {
    String src = """
         <p>
         ~%%begin:foo%
           <p>Hello world
         ~%%end:foo%
        ~%%begin:foo%
           <p>Hi there
         ~%%end:foo%
         </p>
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.DUPLICATE_TMPL_NAME, e.getErrorCode());
    }
  }

  @Test
  public void varNameWithTmplName00() {
    String src = """
         <p>
         ~%%begin:foo%
           <p>Hello world
         ~%%end:foo%
        ~%foo%
         </p>
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.VAR_WITH_TMPL_NAME, e.getErrorCode());
    }
  }

  @Test
  public void varNameWithTmplName01() {
    String src = """
         <p>
        ~%foo%
        ~%%begin:foo%
           <p>Hello world
         ~%%end:foo%
          </p>
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.VAR_WITH_TMPL_NAME, e.getErrorCode());
    }
  }

  @Test
  public void ditchTagNotClosed01() throws ParseException {
    String src = "<!--%%-->";
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.DITCH_BLOCK_NOT_CLOSED, e.getErrorCode());
    }
  }

  @Test
  public void placeholderNotClosed00() {
    String src = """
        <p>
        <!--%-->Hello, world
        </p>
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.PLACEHOLDER_NOT_CLOSED, e.getErrorCode());
    }
  }

  @Test
  public void placeholderNotClosed01() {
    String src = "<!--%-->";
    try {
      Template t = Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.PLACEHOLDER_NOT_CLOSED, e.getErrorCode());
    }
  }

  @Test
  public void noPlaceholderDefined00() {
    String src = """
        ~%%begin:foo%
          ~%def:bar%
        ~%%end:foo%
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(NO_PLACEHOLDER_DEFINED, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void includeTagNotTerminated00() {
    String src = "~%%include:foo";
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(INCLUDE_TAG_NOT_TERMINATED, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void includeTagNotTerminated01() {
    String src = "~%%include:foo%";
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(INCLUDE_TAG_NOT_TERMINATED, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void includeTagNotTerminated02() {
    String src = """
        ~%%include:/foo/bar%
        ~%%begin:foo%
          <p>Hello world
        ~%%end:foo%
         """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ParseErrorCode.INCLUDE_TAG_NOT_TERMINATED, e.getErrorCode());
    }
  }

  @Test
  public void invalidIncludePath00() {
    String src = "~%%include:/a/b/c/d/e/f/g/h/i/0/1/2%%";
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(INVALID_INCLUDE_PATH, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void twoPercentages00() {
    String src = """
        ~%begin:foo%
          bar
        ~%end:foo%
        """;
    try {
      Template.fromString(src);
    } catch (ParseException e) {
      assertEquals(ILLEGAL_VAR_PREFIX, e.getErrorCode());
      return;
    }
    fail();
  }

}
