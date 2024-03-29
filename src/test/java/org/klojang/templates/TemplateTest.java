package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateTest {

  @Test
  public void fromString00() throws ParseException {
    String src = """
        <html><body>
            ~%%begin:main%
              ~%%include:contents_a:include-01.html%%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(2, tmpl.getNestedTemplates().size());
    assertEquals("main", tmpl.getNestedTemplates().get(0).getName());
    assertEquals("contents_b", tmpl.getNestedTemplates().get(1).getName());
  }

  @Test
  public void fromFile00() throws ParseException {
    assertThrows(IllegalArgumentException.class,
        () -> Template.fromFile("/foo/∞/bar/π"));
  }

  @Test
  public void getPath00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%%include:contents_a:include-01.html%%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(Optional.empty(), tmpl.path());
    Template nested = tmpl.getNestedTemplate("main");
    assertEquals(Optional.empty(), nested.path());
    nested = nested.getNestedTemplate("contents_a");
    assertEquals(Optional.of("include-01.html"), nested.path());
    nested = tmpl.getNestedTemplate("contents_b");
    assertEquals(Optional.of("include-01.html"), nested.path());
  }

  @Test
  public void countVariables00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(0, tmpl.countVariableOccurrences());
  }

  @Test
  public void countVariables01() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(1, tmpl.countVariableOccurrences());
  }

  @Test
  public void countVariables02() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(1, tmpl.getVariables().size());
    assertEquals(2, tmpl.countVariableOccurrences());
  }

  @Test
  public void countVariables03() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%~%bar%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(2, tmpl.getVariables().size());
    assertEquals(2, tmpl.countVariableOccurrences());
  }

  @Test
  public void hasVariable00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertFalse(tmpl.hasVariable("foo"));
  }

  @Test
  public void hasVariable01() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertTrue(tmpl.hasVariable("foo"));
  }

  @Test
  public void hasTemplate00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertTrue(tmpl.hasNestedTemplate("main"));
    assertTrue(tmpl.hasNestedTemplate("contents_b"));
    assertFalse(tmpl.hasNestedTemplate("include-01"));
    assertFalse(tmpl.hasNestedTemplate("foo"));
  }

  @Test
  public void getRootTemplate00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertSame(tmpl, tmpl.getRootTemplate());
  }

  @Test
  public void getRootTemplate01() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    Template nested = tmpl.getNestedTemplate("main");
    assertSame(tmpl, nested.getRootTemplate());
    nested = tmpl.getNestedTemplate("contents_b");
    assertSame(tmpl, nested.getRootTemplate());
  }

  @Test
  public void toString00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%foo%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%%
        </body></html>
        ~%foo%
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(nospace(src), nospace(tmpl.toString()));
  }

  @Test
  public void getVariableOccurrences00() throws ParseException {
    String src = """
        <html>
        <head>
          const name = '~%js:name%';
        </head>
        <body>
          <p><!--~%def:name%-->John<!--%--></p>
        </body>
        </html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    List<VariableOccurrence> occs = tmpl.getVariableOccurrences();
    assertEquals(2, occs.size());
    assertEquals("name", occs.get(0).name());
    assertEquals(Optional.of(VarGroup.JS), occs.get(0).varGroup());
    assertEquals(Optional.empty(), occs.get(0).placeholder());
    assertEquals("name", occs.get(1).name());
    assertEquals(Optional.of(VarGroup.DEF), occs.get(1).varGroup());
    assertEquals(Optional.of("John"), occs.get(1).placeholder());
  }

  @Test
  public void fromResolver00() throws ParseException {
    PathResolver resolver = s -> new ByteArrayInputStream("~%foo%".getBytes());
    Template tmpl = Template.fromResolver(resolver, "ignored");
    RenderSession rs = tmpl.newRenderSession();
    assertEquals("bar", rs.set("foo", "bar").render());
  }

  @Test
  public void equals00() throws ParseException {
    PathResolver resolver0 = s -> new ByteArrayInputStream("~%foo%".getBytes());
    Template tmpl0 = Template.fromResolver(resolver0, "include-01.html");
    Template tmpl1 = Template.fromResolver(resolver0, "include-01.html");
    Template tmpl2 = Template.fromResolver(resolver0, "something");
    Template tmpl3 = Template.fromResource(Template.class, "include-01.html");
    Template tmpl4 = Template.fromResource(Template.class, "include-01.html");
    Template tmpl5 = Template.fromString("foo");
    Template tmpl6 = Template.fromString("foo");
    assertFalse(tmpl0.equals(new Object()));
    assertTrue(tmpl0.equals(tmpl0));
    assertTrue(tmpl0.equals(tmpl1));
    assertFalse(tmpl0.equals(tmpl2));
    assertFalse(tmpl0.equals(tmpl3));
    assertFalse(tmpl0.equals(tmpl5));
    assertTrue(tmpl3.equals(tmpl4));
    assertFalse(tmpl3.equals(tmpl5));
    assertTrue(tmpl5.equals(tmpl5));
    assertFalse(tmpl5.equals(tmpl6));
  }

  @Test
  public void hasVariables00() throws ParseException {
    String src = """
        ~%foo%
        ~%%begin:companies%
            COMPANY
            ~%%begin:departments%
                DEPARTMENT
                ~%%begin:employees%
                    EMPLOYEE
                ~%%end:employees%
            ~%%end:departments%
            ~%%begin:activities%
                ~%activity%
                ~%%begin:locations%
                    AMSTERDAM
                ~%%end:locations%
            ~%%end:activities%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    assertTrue(tmpl.hasVariables());
    assertTrue(tmpl.getNestedTemplate("companies").hasVariables());
    assertFalse(tmpl.getNestedTemplate("companies")
        .getNestedTemplate("departments")
        .hasVariables());
    assertFalse(tmpl.getNestedTemplate("companies")
        .getNestedTemplate("departments")
        .getNestedTemplate("employees")
        .hasVariables());
    assertTrue(tmpl.getNestedTemplate("companies")
        .getNestedTemplate("activities")
        .hasVariables());
    assertFalse(tmpl.getNestedTemplate("companies")
        .getNestedTemplate("activities")
        .getNestedTemplate("locations")
        .hasVariables());
  }

  @Test
  public void getRootTemplate02() throws ParseException {
    String src = """
        ~%foo%
        ~%%begin:companies%
            ~%%begin:departments%
                ~%%begin:employees%
                 ~%%end:employees%
            ~%%end:departments%
            ~%%begin:activities%
                 ~%%begin:locations%
                 ~%%end:locations%
            ~%%end:activities%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    assertSame(tmpl, tmpl.getRootTemplate());
    assertSame(tmpl, tmpl.getNestedTemplate("companies").getRootTemplate());
    assertSame(tmpl, tmpl.getNestedTemplate("companies")
        .getNestedTemplate("activities")
        .getRootTemplate());
    assertSame(tmpl, tmpl.getNestedTemplate("companies")
        .getNestedTemplate("activities")
        .getNestedTemplate("locations")
        .getRootTemplate());
  }

  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
