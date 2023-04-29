package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.templates.x.ClassPathResolver;
import org.klojang.util.StringMethods;

import java.io.ByteArrayInputStream;
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
    String src = "<!--~%%begin:foo%--><!--~%%end:foo%-->";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(0, parts.size());
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
  public void parseInlineTemplates06() throws ParseException {
    String src = "~%%begin:foo%~%%end:foo%";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(0, parts.size());
  }

  @Test
  public void parseInlineTemplates07() throws ParseException {
    String src = "a~%%begin:foo%~%%end:foo%b";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(2, parts.size());
  }

  @Test
  public void parseInlineTemplates08() throws ParseException {
    String src = """
        a
        ~%%begin:foo%~%%end:foo%
        b
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(2, parts.size());
  }

  @Test
  public void parseInlineTemplates09() throws ParseException {
    String src = """
        a~%%begin:foo%~%%end:foo%
        b
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(2, parts.size());
  }

  @Test
  public void parseInlineTemplates10() throws ParseException {
    String src = """
        a
        ~%%begin:foo%~%%end:foo%b
        """;
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(2, parts.size());
  }

  @Test
  public void parseInlineTemplates11() throws ParseException {
    String src = "a \n~%%begin:foo%~%%end:foo%\n b";
    Parser parser = new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, src);
    List<Part> parts = parser.getParts();
    assertEquals(2, parts.size());
    String txt = ((TextPart) parts.get(0)).getText();
    assertEquals("a \n", ((TextPart) parts.get(0)).getText());
    assertEquals("\n b", ((TextPart) parts.get(1)).getText());
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
  public void beginAndEndTagOnSeparateLines00() throws ParseException {
    String src = """
        *****
        ~%%begin:foo%
        Foo
        ~%%end:foo%
        *****
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        Foo             
        Foo             
        Foo             
        Foo             
        Foo             
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void beginTagOnSeparateLine00() throws ParseException {
    String src = """
        *****
        ~%%begin:foo%
        Foo~%%end:foo%
        *****
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        FooFooFooFooFoo
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void beginTagOnSeparateLine01() throws ParseException {
    String src = """
        *****
        ~%%begin:foo%
        Foo
        ~%%end:foo%*****
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        Foo             
        Foo             
        Foo             
        Foo             
        Foo             
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void endTagOnSeparateLine00() throws ParseException {
    String src = """
        *****
        ~%%begin:foo%Foo
        ~%%end:foo%
        *****
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        Foo             
        Foo             
        Foo             
        Foo             
        Foo             
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void endTagOnSeparateLine01() throws ParseException {
    String src = """
        *****~%%begin:foo%
        Foo
        ~%%end:foo%
        *****
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        Foo
                
        Foo
                
        Foo
                
        Foo
                
        Foo
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void includeTagOnSeparateLine00() throws ParseException {
    String src = """
        *****
        ~%%include:foo:Foo.html%%
        *****
        """;
    Template t = Template.fromString(getClass(), src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
        FooFooFooFooFoo            
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void includeTagOnSeparateLine01() throws ParseException {
    String src = """
        *****
        ~%%include:foo:FooWithNewline.html%%
        *****
        """;
    Template t = Template.fromString(getClass(), src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****
            Foo
            Foo
            Foo
            Foo
            Foo
        
        *****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void includeTagNotOnSeparateLine00() throws ParseException {
    String src = """
        *****~%%include:foo:Foo.html%%*****
        """;
    Template t = Template.fromString(getClass(), src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****FooFooFooFooFoo*****
        """;
    assertEquals(expected, out);
  }

  @Test
  public void includeTagNotOnSeparateLine01() throws ParseException {
    String src = """
        *****~%%include:foo:FooWithNewline.html%%*****
        """;
    Template t = Template.fromString(getClass(), src);
    RenderSession rs = t.newRenderSession();
    rs.enable(5, "foo");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        *****    Foo
            Foo
            Foo
            Foo
            Foo        
        *****
        """;
    assertEquals(expected, out);
  }



  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
