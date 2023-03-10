package org.klojang.templates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.klojang.templates.RenderErrorCode.*;

public class RenderErrorTest {

  @Test
  public void noSuchVariable00() throws ParseException {
    String src = "~%name%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.set("foo", "bar");
    } catch (RenderException e) {
      assertEquals("No such variable: \"foo\"", e.getMessage());
      assertEquals(NO_SUCH_VARIABLE, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void noSuchTemplate00() throws ParseException {
    String src = "~%name%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.populate("foo", new Object());
    } catch (RenderException e) {
      assertEquals("No such nested template: \"foo\"", e.getMessage());
      assertEquals(NO_SUCH_TEMPLATE, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void templateNotInstantiated00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
        ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.getChildSessions("foo");
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals("Template foo not instantiated yet", e.getMessage());
      assertEquals(TEMPLATE_NOT_INSTANTIATED, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void accessException00() throws ParseException {
    Accessor<Object> acc = (x, y) -> {
      throw new RuntimeException();
    };
    AccessorRegistry reg = AccessorRegistry.configure()
        .register(acc, Object.class)
        .freeze();
    String src = """
        ~%%begin:foo%
          ~%name%
        ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(reg);
    try {
      rs.populate("foo", new Object());
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Error while retrieving value for foo.name: java.lang.RuntimeException",
          e.getMessage());
      assertEquals(ACCESS_EXCEPTION, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void notTextOnly00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
        ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.show("foo");
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Not a text-only template: foo",
          e.getMessage());
      assertEquals(NOT_TEXT_ONLY, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void notOneVarTemplate00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
          ~%age%
        ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.populate1("foo", 23);
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Not a one-variable template: foo",
          e.getMessage());
      assertEquals(NOT_ONE_VAR_TEMPLATE, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void notTwoVarTemplate00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
         ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.populate2("foo", "john", "smith");
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Not a two-variable template: foo",
          e.getMessage());
      assertEquals(NOT_TWO_VAR_TEMPLATE, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void stringifierReturnedNull00() throws ParseException {
    String src = "~%xyz:name%";
    Stringifier sf = obj -> null;
    StringifierRegistry reg = StringifierRegistry.configure()
        .registerByGroup(sf, "xyz")
        .freeze();
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(reg);
    try {
      rs.set("name", new Object());
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Stringifier for variable name in variable group xyz returned null",
          e.getMessage());
      assertEquals(STRINGIFIER_RETURNED_NULL, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void stringifierNotNullResistent00() throws ParseException {
    String src = "~%xyz:name%";
    Stringifier sf = obj -> {
      throw new NullPointerException();
    };
    StringifierRegistry reg = StringifierRegistry.configure()
        .registerByGroup(sf, "xyz")
        .freeze();
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(reg);
    try {
      rs.set("name", new Object());
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Stringifier for variable name in variable group xyz threw NullPointerException",
          e.getMessage());
      assertEquals(STRINGIFIER_NOT_NULL_RESISTENT, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void repetitionsFixed00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
         ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.repeat("foo", 2);
      rs.repeat("foo", 2);
    } catch (RenderException e) {
      //System.out.println(e.getMessage());
      assertEquals(
          "Number of repetitions already fixed for template foo",
          e.getMessage());
      assertEquals(REPETITIONS_FIXED, e.getErrorCode());
      return;
    }
    fail();
  }

  @Test
  public void repetitionMismatch00() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%name%
         ~%%end:foo%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    try {
      rs.repeat("foo", 2);
      rs.populate1("foo", "bar");
    } catch (RenderException e) {
      System.out.println(e.getMessage());
      assertEquals(
          "Error while populating foo. When populating a nested template in multiple passes you must always provide the same number of source data objects. Received 2 source data object(s) in first round. Now got 1.",
          e.getMessage());
      assertEquals(REPETITION_MISMATCH, e.getErrorCode());
      return;
    }
    fail();
  }

}
