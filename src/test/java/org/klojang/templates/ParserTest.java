package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.templates.x.ClassPathResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

public class ParserTest {

  @Test
  public void parseVariables00() throws ParseException {
    String src = "<tr><td>~%foo%</td></tr>";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(3, parts.size());
    assertTrue(parts.get(0) instanceof TextPart);
    assertEquals("<tr><td>", ((TextPart) parts.get(0)).getText());
    assertTrue(parts.get(1) instanceof VariablePart);
    VariablePart vp = (VariablePart) parts.get(1);
    assertTrue(vp.getVarGroup().isEmpty());
    assertEquals("foo", vp.getName());
    assertTrue(parts.get(2) instanceof TextPart);
    assertEquals("</td></tr>", ((TextPart) parts.get(2)).getText());
  }

  @Test
  public void parseVariables01() throws ParseException {
    String src = "<tr><td><!-- ~%foo% --></td></tr>";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(3, parts.size());
    assertTrue(parts.get(0) instanceof TextPart);
    assertEquals("<tr><td>", ((TextPart) parts.get(0)).getText());
    assertTrue(parts.get(1) instanceof VariablePart);
    VariablePart vp = (VariablePart) parts.get(1);
    assertTrue(vp.getVarGroup().isEmpty());
    assertEquals("foo", vp.getName());
    assertTrue(parts.get(2) instanceof TextPart);
    assertEquals("</td></tr>", ((TextPart) parts.get(2)).getText());
  }

  @Test
  public void parseVariables02() throws ParseException {
    String src = """
        <tr>
          <td>~%html:foo%</td>
          <!-- some comment -->
          <td>~%text:bar%</td>
        </tr>
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(5, parts.size());
    assertTrue(parts.get(0) instanceof TextPart);
    assertEquals("<tr>\n  <td>", ((TextPart) parts.get(0)).getText());
    assertTrue(parts.get(1) instanceof VariablePart);
    VariablePart vp = (VariablePart) parts.get(1);
    assertEquals(VarGroup.HTML, vp.getVarGroup().get());
    assertEquals("foo", vp.getName());
    assertTrue(parts.get(2) instanceof TextPart);
    assertEquals("</td>\n  <!-- some comment -->\n  <td>",
        ((TextPart) parts.get(2)).getText());
    assertTrue(parts.get(3) instanceof VariablePart);
    vp = (VariablePart) parts.get(3);
    assertEquals(VarGroup.TEXT, vp.getVarGroup().get());
    assertEquals("bar", vp.getName());
    assertTrue(parts.get(4) instanceof TextPart);
    assertEquals("</td>\n</tr>\n", ((TextPart) parts.get(4)).getText());
  }

  @Test
  public void parseVariables03() throws ParseException {
    String src = "~%foo-bar2:departments.0.employees.0.name%";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(1, parts.size());
    assertTrue(parts.get(0) instanceof VariablePart);
    VariablePart vp = (VariablePart) parts.get(0);
    assertEquals("foo-bar2", vp.getVarGroup().get().getName());
    assertEquals("departments.0.employees.0.name", vp.getName());
  }

  @Test
  public void parseInlineTemplates00() throws ParseException {
    String src = """
        <html>
        <head>
        <script type="text/javascript">
        ~%%begin:jsVars%
          var selectedName = "~%js:selectedName%";
          var selectedAge = ~%selectedAge%;
        ~%%end:jsVars%
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
            ~%%begin:tableRow%
              <tr><td>~%html:name%</td><td>~%text:age%</td></tr>
            ~%%end:tableRow%
            </tbody>
          </table>
        </body>
        </html>
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertTrue(parts.get(1) instanceof NestedTemplatePart);
    Template t = ((NestedTemplatePart) parts.get(1)).getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("selectedName"));
    assertTrue(t.getNames().contains("selectedAge"));
    assertTrue(parts.get(3) instanceof NestedTemplatePart);
    t = ((NestedTemplatePart) parts.get(3)).getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("name"));
    assertTrue(t.getNames().contains("age"));
  }

  @Test
  public void parseInlineTemplates01() throws ParseException {
    String src = """
        <html>
        <head>
        <script type="text/javascript">
        <!--
        ~%%begin:jsVars%
          var selectedName = "~%js:selectedName%";
          var selectedAge = ~%selectedAge%;
        ~%%end:jsVars%
        -->
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
            <!--
            ~%%begin:tableRow%
              <tr><td>~%html:name%</td><td>~%text:age%</td></tr>
            ~%%end:tableRow%
            -->
            </tbody>
          </table>
        </body>
        </html>
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertTrue(parts.get(1) instanceof NestedTemplatePart);
    Template t = ((NestedTemplatePart) parts.get(1)).getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("selectedName"));
    assertTrue(t.getNames().contains("selectedAge"));
    assertTrue(parts.get(3) instanceof NestedTemplatePart);
    t = ((NestedTemplatePart) parts.get(3)).getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("name"));
    assertTrue(t.getNames().contains("age"));
  }

  @Test
  public void parseInlineTemplates02() throws ParseException {
    String src = "<!--~%%begin:__foo-bar-00__%--><!--~%%end:__foo-bar-00__%-->";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(1, parts.size());
    assertTrue(parts.get(0) instanceof InlineTemplatePart);
    InlineTemplatePart itp = (InlineTemplatePart) parts.get(0);
    assertEquals("__foo-bar-00__", itp.getName());
    assertEquals(0, itp.getTemplate().parts().size());
  }

  @Test
  public void parseInlineTemplates03() throws ParseException {
    String src = "<!-- ~%%begin:21% --> FOO <!--~%%end:21%-->";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(1, parts.size());
    assertTrue(parts.get(0) instanceof InlineTemplatePart);
    InlineTemplatePart itp = (InlineTemplatePart) parts.get(0);
    assertEquals("21", itp.getName());
    assertEquals(1, itp.getTemplate().parts().size());
    assertTrue(itp.getTemplate().parts().get(0) instanceof TextPart);
    assertEquals(" FOO ", itp.getTemplate().parts().get(0).toString());
  }

  @Test
  public void parseInlineTemplates04() throws ParseException {
    String src = "~%%begin:foo%~%%begin:bar%bozo~%%end:bar%~%%end:foo%";
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.in("foo").enable("bar");
    assertEquals("bozo", rs.render());
  }

  @Test
  public void parseInlineTemplates05() throws ParseException {
    String src = """
        ~%%begin:foo%
          ~%%begin:foo%
            ~%%begin:foo%
              ~%bozo%
            ~%%end:foo%
          ~%%end:foo%
        ~%%end:foo%
        ~%%begin:bar%
          ~%%begin:foo%
            ~%%begin:foo%
              ~%bozo%
            ~%%end:foo%
          ~%%end:foo%
        ~%%end:bar%
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.in("foo").in("foo").in("foo").set("bozo", "bozo");
    rs.in("bar").in("foo").in("foo").set("bozo", "bozo");
    assertEquals("bozobozo", nospace(rs.render()));
  }

  @Test
  public void parseIncludedTemplates00() throws ParseException {
    String src = """
        <html>
        <head>
        <script type="text/javascript">
        ~%%include:jsVars.js%%
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
              ~%%include:tableRow.html%%
            </tbody>
          </table>
        </body>
        </html>
        """;
    PathResolver resolver = new ClassPathResolver(getClass());
    TemplateLocation location = new TemplateLocation(resolver);
    Parser parser = new Parser(location, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertTrue(parts.get(1) instanceof NestedTemplatePart);
    NestedTemplatePart tp = (NestedTemplatePart) parts.get(1);
    assertEquals("jsVars", tp.getName());
    Template t = tp.getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("selectedName"));
    assertTrue(t.getNames().contains("selectedAge"));
    assertTrue(parts.get(3) instanceof NestedTemplatePart);
    tp = (NestedTemplatePart) parts.get(3);
    assertEquals("tableRow", tp.getName());
    t = tp.getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("name"));
    assertTrue(t.getNames().contains("age"));
  }

  @Test
  public void parseIncludedTemplates01() throws ParseException {
    String src = """
        <html>
        <head>
        <script type="text/javascript">
        <!-- ~%%include:jsVars.js%% -->
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
              <!-- ~%%include:tableRow.html%% -->
            </tbody>
          </table>
        </body>
        </html>
        """;
    PathResolver resolver = new ClassPathResolver(getClass());
    TemplateLocation location = new TemplateLocation(resolver);
    Parser parser = new Parser(location, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertTrue(parts.get(1) instanceof NestedTemplatePart);
    NestedTemplatePart tp = (NestedTemplatePart) parts.get(1);
    assertEquals("jsVars", tp.getName());
    Template t = tp.getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("selectedName"));
    assertTrue(t.getNames().contains("selectedAge"));
    assertTrue(parts.get(3) instanceof NestedTemplatePart);
    tp = (NestedTemplatePart) parts.get(3);
    assertEquals("tableRow", tp.getName());
    t = tp.getTemplate();
    assertEquals(2, t.getNames().size());
    assertTrue(t.getNames().contains("name"));
    assertTrue(t.getNames().contains("age"));
  }

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
  public void includeTagNotTerminated00() {
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
      assertEquals(ParseErrorCode.VAR_NAME_WITH_TMPL_NAME, e.getErrorCode());
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
      assertEquals(ParseErrorCode.VAR_NAME_WITH_TMPL_NAME, e.getErrorCode());
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

  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
