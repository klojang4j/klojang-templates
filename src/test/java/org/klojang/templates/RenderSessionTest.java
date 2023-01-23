package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RenderSessionTest {

  @Test
  public void set00() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", "bar");
    String out = rs.render();
    assertEquals("bar", out);
  }

  @Test
  public void set01() throws ParseException {
    Stringifier stringifier = obj -> String.valueOf(obj).toUpperCase();
    StringifierRegistry registry = StringifierRegistry.configure()
        .registerByGroup(stringifier, "upper")
        .freeze();
    String src = "~%upper:foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(registry);
    rs.set("foo", "bar");
    String out = rs.render();
    assertEquals("BAR", out);
  }

  @Test
  public void set02() throws ParseException {
    Stringifier stringifier = obj -> String.valueOf(obj).toUpperCase();
    StringifierRegistry registry = StringifierRegistry.configure()
        .registerByGroup(stringifier, "upper")
        .freeze();
    String src = "<td>~%foo%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(registry);
    rs.set("foo", "bar", VarGroup.forName("upper"));
    String out = rs.render();
    assertEquals("<td>BAR</td>", out);
  }

  @Test
  public void set03() throws ParseException {
    Stringifier stringifier = obj -> String.valueOf(obj).toUpperCase();
    StringifierRegistry registry = StringifierRegistry.configure()
        .registerByGroup(stringifier, "upper")
        .freeze();
    String src = "<td>~%foo%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(registry);
    rs.set("foo", List.of('a', 'b', 'c'), VarGroup.forName("upper"));
    String out = rs.render();
    assertEquals("<td>ABC</td>", out);
  }

  @Test
  public void set04() throws ParseException {
    Stringifier stringifier = obj -> String.valueOf(obj).toUpperCase();
    StringifierRegistry registry = StringifierRegistry.configure()
        .registerByGroup(stringifier, "upper")
        .freeze();
    String src = "<td>~%foo%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession(registry);
    rs.set("foo", List.of('a', 'b', 'c'), VarGroup.forName("upper"), "|");
    String out = rs.render();
    assertEquals("<td>A|B|C</td>", out);
  }

  @Test
  public void set05() throws ParseException {
    String src = "<td>~%foo%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of('a', 'b', 'c'), "|");
    String out = rs.render();
    assertEquals("<td>a|b|c</td>", out);
  }

  @Test
  public void set06() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of('a', 'b', 'c'), VarGroup.TEXT, "[", "|", "]");
    String out = rs.render();
    assertEquals("[a|b|c]", out);
  }

  @Test
  public void set07() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of('a'), VarGroup.TEXT, "[", "|", "]");
    String out = rs.render();
    assertEquals("[a]", out);
  }

  @Test
  public void set08() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of(), VarGroup.TEXT, "[", "|", "]");
    String out = rs.render();
    assertEquals("", out);
  }

  @Test
  public void set09() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of('<', '>'), VarGroup.HTML, "<td>", "</td><td>", "</td>");
    String out = rs.render();
    assertEquals("<td>&lt;</td><td>&gt;</td>", out);
  }

  @Test
  public void set10() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo",
        List.of('<', '>', '<'),
        VarGroup.HTML,
        "<td>",
        "</td><td>",
        "</td>");
    String out = rs.render();
    assertEquals("<td>&lt;</td><td>&gt;</td><td>&lt;</td>", out);
  }

  @Test
  public void paste() throws ParseException {
    Renderable renderable = Template.fromString("<td>~%foo%</td><td>~%bar%</td>")
        .newRenderSession()
        .set("foo", "foo")
        .set("bar", "bar")
        .createRenderable();
    String out = Template.fromString("<tr>~%foo%</tr>")
        .newRenderSession()
        .paste("foo", renderable)
        .render();
    assertEquals("<tr><td>foo</td><td>bar</td></tr>", out);
  }

}
