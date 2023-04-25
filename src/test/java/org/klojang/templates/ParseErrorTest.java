package org.klojang.templates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.klojang.templates.ParseErrorCode.*;

public class ParseErrorTest {

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
