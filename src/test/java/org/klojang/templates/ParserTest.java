package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.templates.x.parse.NestedTemplatePart;
import org.klojang.templates.x.parse.Part;
import org.klojang.templates.x.parse.TextPart;
import org.klojang.templates.x.parse.VariablePart;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

public class ParserTest {

  @Test
  public void parseVariables00() throws ParseException {
    String src = "<tr><td>~%foo%</td></tr>";
    Parser parser = new Parser(ROOT_TEMPLATE_NAME, TemplateLocation.NONE, src);
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
    Parser parser = new Parser(ROOT_TEMPLATE_NAME, TemplateLocation.NONE, src);
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
    Parser parser = new Parser(ROOT_TEMPLATE_NAME, TemplateLocation.NONE, src);
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
  public void parseNestedTemplates00() throws ParseException {
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
    Parser parser = new Parser(ROOT_TEMPLATE_NAME, TemplateLocation.NONE, src);
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
  public void parseNestedTemplates01() throws ParseException {
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
    Parser parser = new Parser(ROOT_TEMPLATE_NAME, TemplateLocation.NONE, src);
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
  public void parseIncludedTemplates00() throws ParseException {
    String src = """
        <html>
        <head>
        <script type="text/javascript">
        ~%%include:jsVars.js%
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
              ~%%include:tableRow.html%
            </tbody>
          </table>
        </body>
        </html>
        """;
    Parser parser = new Parser(ROOT_TEMPLATE_NAME,
        new TemplateLocation(getClass()),
        src);
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
        <!-- ~%%include:jsVars.js% -->
        </script>
        </head>
        <body>
          <table>
            <thead>
              <tr><th>Name</th><th>Age</th>
            </thead>
            <tbody>
              <!-- ~%%include:tableRow.html% -->
            </tbody>
          </table>
        </body>
        </html>
        """;
    Parser parser = new Parser(ROOT_TEMPLATE_NAME,
        new TemplateLocation(getClass()),
        src);
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

}