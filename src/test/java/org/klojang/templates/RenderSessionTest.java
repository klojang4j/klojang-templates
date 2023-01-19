package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RenderSessionTest {

  @Test
  public void set00() throws ParseException, RenderException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", "bar");
    String out = rs.render();
    assertEquals("bar", out);
  }

  @Test
  public void set01() throws ParseException, RenderException {
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
  public void set02() throws ParseException, RenderException {
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
  public void set03() throws ParseException, RenderException {
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
  public void set04() throws ParseException, RenderException {
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
  public void set05() throws ParseException, RenderException {
    String src = "<td>~%foo%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", List.of('a', 'b', 'c'), "|");
    String out = rs.render();
    assertEquals("<td>a|b|c</td>", out);
  }

}
