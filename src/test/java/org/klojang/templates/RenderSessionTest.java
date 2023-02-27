package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.path.util.MapBuilder;
import org.klojang.util.AnyTuple2;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
  public void set12() throws ParseException {
    String src = "[~%foo%]";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", Accessor.UNDEFINED);
    String out = rs.render();
    // System.out.println(out);
    assertEquals("[]", out);
  }

  @Test
  public void populate01() throws ParseException {
    String src = """
        <html><body>
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
    String expected = "<html><body><table><tr><td>Name</td><td>Shell</td><td>Profits</td><td>5029872.8</td><td>Departments</td><td><table><tr><td>Name</td><td>ICT</td><td>Boss</td><td>John</td><td>Employees</td><td><table><tr><td>Name</td><td>Pete</td><td>Age</td><td>27</td></tr><tr><td>Name</td><td>Jake</td><td>Age</td><td>52</td></tr></table></td></tr><tr><td>Name</td><td>HR</td><td>Boss</td><td>Joanna</td><td>Employees</td><td><table><tr><td>Name</td><td>Mary</td><td>Age</td><td>27</td></tr><tr><td>Name</td><td>John</td><td>Age</td><td>52</td></tr></table></td></tr></table></td></tr><tr><td>Name</td><td>Goggle</td><td>Profits</td><td>3.8632534578E8</td><td>Departments</td><td><table><tr><td>Name</td><td>ICT</td><td>Boss</td><td>Jane</td><td>Employees</td><td><table><tr><td>Name</td><td>Capote</td><td>Age</td><td>66</td></tr><tr><td>Name</td><td>Joan</td><td>Age</td><td>51</td></tr></table></td></tr><tr><td>Name</td><td>HR</td><td>Boss</td><td>Eric</td><td>Employees</td><td><table><tr><td>Name</td><td>Mary</td><td>Age</td><td>54</td></tr><tr><td>Name</td><td>John</td><td>Age</td><td>46</td></tr></table></td></tr></table></td></tr></table></body></html>";
    assertEquals(expected, out);
  }

  @Test
  public void populate02() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            BAR
            ~%%end:companies%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate("companies", null);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOBARFOO", out);
  }

  @Test
  public void populate03() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            BAR
            ~%%end:companies%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate("companies", Accessor.UNDEFINED);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOFOO", out);
  }

  @Test
  public void populate04() throws ParseException {
    String src = """
        <html><body>
            ~%%begin:main-table%
              FOO
            ~%%end:main-table%
            ~%%include:contents2:include-01.html%%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertNotNull(tmpl);
  }

  @Test
  public void show01() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            BAR
            ~%%end:companies%
            ~%%begin:teapots%
            BOZO
            ~%%end:teapots%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.show(2);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOBARBARBOZOBOZOFOO", out);
  }

  @Test
  public void show02() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            BAR
            ~%%end:companies%
            ~%%begin:teapots%
            BOZO
            ~%%end:teapots%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.show(2, "companies");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOBARBARFOO", out);
  }

  @Test
  public void show03() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            BAR
            ~%%end:companies%
            ~%%begin:teapots%
            BOZO
            ~%%end:teapots%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.show("teapots");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOBOZOFOO", out);
  }

  @Test
  public void showRecursive00() throws ParseException {
    String src = """
        Foo
            ~%%begin:companies%
            Bar
              ~%%begin:departments%
                Kitchen
                ~%%begin:employees%
                  Sink
                ~%%end:employees%
              ~%%end:departments%
            ~%%end:companies%
            ~%%begin:teapots%
            Bozo
            ~%%end:teapots%
        Foo
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.showRecursive();
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FooBarKitchenSinkBozoFoo", out);
  }

  @Test
  public void showRecursive01() throws ParseException {
    String src = """
        Foo
            ~%%begin:companies%
            Bar
              ~%%begin:departments%
                Kitchen
                ~%%begin:employees%
                  Sink
                ~%%end:employees%
              ~%%end:departments%
            ~%%end:companies%
            ~%%begin:teapots%
            Bozo
            ~%%end:teapots%
            ~%%begin:chairs%
              Cheerio
            ~%%end:chairs%
        Foo
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.showRecursive("chairs");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FooCheerioFoo", out);
  }

  @Test
  public void populate1_00() throws ParseException {
    String src = """
        FOO
            ~%%begin:companies%
            ~%myVar%
            ~%%end:companies%
        FOO
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate1("companies", "BAR");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FOOBARFOO", out);
  }

  @Test
  public void populate2_00() throws ParseException {
    String src = """
        Foo
            ~%%begin:companies%
            ~%foo%
            ~%bar%
            ~%%end:companies%
        Foo
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate2("companies", List.of(AnyTuple2.of("Pig", "Pony")));
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FooPigPonyFoo", out);
  }

  @Test
  public void populate2_01() throws ParseException {
    String src = """
        Foo
            ~%%begin:companies%
            ~%foo%
            ~%bar%
            ~%%end:companies%
        Foo
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate2("companies",
        List.of(AnyTuple2.of("Pig", "Pony"), AnyTuple2.of("Horse", "Cat")));
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("FooPigPonyHorseCatFoo", out);
  }

  @Test
  public void insert00() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", "hello")
        .set("foo.bar", "teapot")
        .createMap();
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p>hello</p><p>teapot</p></body></html>", out);
  }

  @Test
  public void insert01() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", "1 < 2")
        .set("foo.bar", "teapot")
        .createMap();
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data, VarGroup.HTML);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p>1&lt;2</p><p>teapot</p></body></html>", out);
  }

  @Test
  public void insert02() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", "hello")
        .createMap();
    data.put("foo", List.of(Map.of("bar", "tea"), Map.of("bar", "pot")));
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data, VarGroup.HTML);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p>hello</p><p>tea</p><p>pot</p></body></html>", out);
  }

  @Test
  public void insert03() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", Accessor.UNDEFINED)
        .createMap();
    data.put("foo", Accessor.UNDEFINED);
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data, VarGroup.HTML);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p></p></body></html>", out);
  }

  @Test
  public void insert04() throws ParseException {
    String src = "foo";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(null);
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("foo", out);
  }

  @Test
  public void insert05() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", "hello")
        .createMap();
    data.put("foo", List.of(Map.of("bar", "tea"), Map.of("bar", "pot")));
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data, "message");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p>hello</p></body></html>", out);
  }

  @Test
  public void insert06() throws ParseException {
    String src = """
        <html><body>
        <p>~%message%</p>
        ~%%begin:foo%
          <p>~%bar%</p>
        ~%%end:foo%
        </body></html>
        """;
    Map<String, Object> data = new MapBuilder()
        .set("message", "hello")
        .createMap();
    data.put("foo", List.of(Map.of("bar", "tea"), Map.of("bar", "pot")));
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(data, "foo", "bar");
    String out = rs.render();
    out = out.replaceAll("\\s+", "");
    //System.out.println(out);
    assertEquals("<html><body><p></p><p>tea</p><p>pot</p></body></html>", out);
  }

}
