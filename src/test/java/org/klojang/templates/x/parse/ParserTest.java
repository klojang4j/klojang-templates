package org.klojang.templates.x.parse;

import org.junit.jupiter.api.Test;
import org.klojang.templates.ParseException;
import org.klojang.templates.Template;
import org.klojang.templates.VarGroup;
import org.klojang.templates.x.TemplateLocation;

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
    String src1 = "<tr>\n<td>~%html:foo%</td>\n<!-- some comment -->\n<td>~%text:bar%</td>\n</tr>";
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
    String path = "ParserTest.parseNestedTemplates01.html";
    Parser parser = new Parser(ROOT_TEMPLATE_NAME,
        new TemplateLocation(getClass(), path));
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
    String path = "ParserTest.parseIncludedTemplates00.html";
    Parser parser = new Parser(ROOT_TEMPLATE_NAME,
        new TemplateLocation(getClass(), path));
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
    String path = "ParserTest.parseIncludedTemplates01.html";
    Parser parser = new Parser(ROOT_TEMPLATE_NAME,
        new TemplateLocation(getClass(), path));
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

  //  @Test
  //  public void testDitchBlock00() throws ParseException {
  //    String path = "ParserTest.parseDitchBlock00.html";
  //    Template template = Template.fromResource(getClass(), path);
  //    String expected = IOMethods.getContents(getClass(),
  //        "ParserTest.parseDitchBlock00.expected.html");
  //    String actual = template.newRenderSession().render();
  //    assertEquals(expected, actual);
  //  }
  //
  //  @Test
  //  public void testDitchBlock01() throws ParseException, RenderException {
  //    String path = "ParserTest.parseDitchBlock01.html";
  //    Template template = Template.fromResource(getClass(), path);
  //    String expected = IOMethods.getContents(getClass(),
  //        "ParserTest.parseDitchBlock01.expected.html");
  //    String actual = template.newRenderSession().set("foo", "bar").render();
  //    assertEquals(expected, actual);
  //  }
  //
  //  public void testDitchBlock02() {
  //    String path = "ParserTest.parseDitchBlock02.html";
  //    assertThrows(
  //        ParseException.class, // Ditch block not terminated
  //        () -> {
  //          Template.fromResource(getClass(), path);
  //        });
  //  }
  //
  //  @Test
  //  public void allTogetherNow00() throws ParseException {
  //    String path = "ParserTest.allTogetherNow00.html";
  //    Template template = Template.fromResource(getClass(), path);
  //    TemplateUtils.printParts(template, System.out);
  //  }

}
