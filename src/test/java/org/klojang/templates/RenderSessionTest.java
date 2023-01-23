package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.path.util.MapBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

  @Test
  public void populate() throws ParseException {
    String src = """
        <html></body>
          <table>
            ~%%begin:companies%
            <tr>
              <td>Name</td><td>~%name%</td>
              <td>Profits</td><td>~%profits%</td>
              <td>Departments</td>
              <td><table>
                ~%%begin:departments%
                <tr>
                  <td>Name</td><td>~%name%</td>
                  <td>Boss</td><td>~%boss%</td>
                  <td>Employees</td>
                  <td><table>~%%begin:employees%
                    <tr>
                      <td>Name</td><td>~%name%</td>
                      <td>Age</td><td>~%age%</td>
                    </tr>
                  ~%%end:employees%</table></td>
                </tr>
                ~%%end:departments%
              </table></td>
            </tr>
            ~%%end:companies%
          </table>
        </body></html>
        """;
    //@formatter:off
    List<Map<String,Object>> companies = List.of(
    new MapBuilder()
        .set("name", "Shell")
        .set("profits", 5_029_872.80)
        .set("departments", List.of(
            new MapBuilder()
                .set("name", "ICT")
                .set("boss", "John")
                .set("employees", List.of(
                    new MapBuilder()
                        .set("name", "Pete")
                        .set("age", 27)
                        .createMap(),
                    new MapBuilder()
                        .set("name", "Jake")
                        .set("age", 52)
                        .createMap()))
                .createMap(),
            new MapBuilder()
                .set("name", "HR")
                .set("boss", "Joanna")
                .set("employees", List.of(
                    new MapBuilder()
                        .set("name", "Mary")
                        .set("age", 27)
                        .createMap(),
                    new MapBuilder()
                        .set("name", "John")
                        .set("age", 52)
                        .createMap()))
                .createMap()))
        .createMap(),
    new MapBuilder()
        .set("name", "Goggle")
        .set("profits", 386_325_345.78)
        .set("departments", List.of(
            new MapBuilder()
                .set("name", "ICT")
                .set("boss", "Jane")
                .set("employees", List.of(
                    new MapBuilder()
                        .set("name", "Capote")
                        .set("age", 66)
                        .createMap(),
                    new MapBuilder()
                        .set("name", "Joan")
                        .set("age", 51)
                        .createMap()))
                .createMap(),
            new MapBuilder()
                .set("name", "HR")
                .set("boss", "Eric")
                .set("employees", List.of(
                    new MapBuilder()
                        .set("name", "Mary")
                        .set("age", 54)
                        .createMap(),
                    new MapBuilder()
                        .set("name", "John")
                        .set("age", 46)
                        .createMap()))
                .createMap()))
        .createMap()
    );
    //@formatter:on
    //System.out.println(companies);
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate("companies", companies);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    String expected = "<html></body><table><tr><td>Name</td><td>Shell</td><td>Profits</td><td>5029872.8</td><td>Departments</td><td><table><tr><td>Name</td><td>ICT</td><td>Boss</td><td>John</td><td>Employees</td><td><table><tr><td>Name</td><td>Pete</td><td>Age</td><td>27</td></tr><tr><td>Name</td><td>Jake</td><td>Age</td><td>52</td></tr></table></td></tr><tr><td>Name</td><td>HR</td><td>Boss</td><td>Joanna</td><td>Employees</td><td><table><tr><td>Name</td><td>Mary</td><td>Age</td><td>27</td></tr><tr><td>Name</td><td>John</td><td>Age</td><td>52</td></tr></table></td></tr></table></td></tr><tr><td>Name</td><td>Goggle</td><td>Profits</td><td>3.8632534578E8</td><td>Departments</td><td><table><tr><td>Name</td><td>ICT</td><td>Boss</td><td>Jane</td><td>Employees</td><td><table><tr><td>Name</td><td>Capote</td><td>Age</td><td>66</td></tr><tr><td>Name</td><td>Joan</td><td>Age</td><td>51</td></tr></table></td></tr><tr><td>Name</td><td>HR</td><td>Boss</td><td>Eric</td><td>Employees</td><td><table><tr><td>Name</td><td>Mary</td><td>Age</td><td>54</td></tr><tr><td>Name</td><td>John</td><td>Age</td><td>46</td></tr></table></td></tr></table></td></tr></table></body></html>";
    assertEquals(expected, out);
  }

}
