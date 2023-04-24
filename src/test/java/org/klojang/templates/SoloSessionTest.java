package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.MutableInt;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SoloSessionTest {

  @Test
  public void populate1_00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.populate1("companies", "Shell").render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>Shell</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate1_01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.populate1("companies", "MacDonald's", "Shell").render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>MacDonald's</p><p>Shell</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate1_02() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs
        .populate1("companies", VarGroup.JS_ATTR, "MacDonald's", "Shell")
        .render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>MacDonald\\&#39;s</p><p>Shell</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate2_00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
        <p>~%name% (~%country%)</p>
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.populate2("companies", "Shell", "USA").render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>Shell (USA)</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  //@Test
  public void populate2_01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
        <p>~%name% (~%country%)</p>
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.populate1("companies", "MacDonald's", "USA", "Shell", "UK")
        .render();
    //System.out.println(out);
    String expected = """
         <html><body>
         <html><body>
         <p>MacDonald's (USA)</p>
         <p>Shell (UK)</p>
         </body></html>
        </body></html>
         """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate2_02() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs
        .populate1("companies",
            VarGroup.JS_ATTR,
            "MacDonald's",
            "USA",
            "Shell",
            "UK")
        .render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>MacDonald\\&#39;s</p><p>USA</p><p>Shell</p><p>UK</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void insert00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.in("companies").insert(Map.of("name", "Shell")).render();
    //System.out.println(out);
    String expected = """
        <p>Shell</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void insert01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.in("companies")
        .insert(Map.of("name", "MacDonald's"), VarGroup.JS_ATTR)
        .render();
    //System.out.println(out);
    String expected = """
        <p>MacDonald\\&#39;s</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void repeat00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3).set("name", "Shell");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>Shell</p><p>Shell</p><p>Shell</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void setPath00() throws ParseException {
    String src = """
        <html><body>
        <p>~%greeting%</p>
        ~%%begin:companies%
            <p>Name: ~%name%</p>
            <p>Profits: ~%profits%</p>
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
                ~%%begin:employees%
                    <p>First name: ~%firstName%</p>
                    <p>Last name: ~%lastName%</p>
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.setPath("companies.departments.employees.firstName",
        i -> "John",
        VarGroup.TEXT, true);
    rs.setPath("companies.departments.employees.lastName", i -> "Smith");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p></p>
            <p>Name: </p>
            <p>Profits: </p>
                <p>Name: </p>
                <p>Description: </p>
                    <p>First name: John</p>
                    <p>Last name: Smith</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void setPath01() throws ParseException {
    String src = """
        <html><body>
         ~%%begin:companies%
            <p>Name: ~%name%</p>
            <p>Profits: ~%profits%</p>
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
                ~%%begin:employees%
                    <p>First name: ~%firstName%</p>
                    <p>Last name: ~%lastName%</p>
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.setPath("companies.departments.employees.firstName",
        i -> "John",
        VarGroup.TEXT, false);
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
         </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void setPath02() throws ParseException {
    String src = """
        <html><body>
        <p>~%greeting%</p>
        ~%%begin:companies%
            <p>Name: ~%name%</p>
            <p>Profits: ~%profits%</p>
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
                ~%%begin:employees%
                    <p>First name: ~%firstName%</p>
                    <p>Last name: ~%lastName%</p>
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies").in("departments").repeat("employees", 3);
    rs.setPath("companies.departments.employees.firstName",
        i -> "John" + i,
        VarGroup.TEXT,
        false);
    rs.setPath("companies.departments.employees.lastName", i -> "Smith" + i);
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p></p>
            <p>Name: </p>
            <p>Profits: </p>    
                <p>Name: </p>
                <p>Description: </p>        
                    <p>First name: John0</p>
                    <p>Last name: Smith0</p>       
                    <p>First name: John1</p>
                    <p>Last name: Smith1</p>        
                    <p>First name: John2</p>
                    <p>Last name: Smith2</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void setPath03() throws ParseException {
    String src = """
        ~%%begin:companies%
            ~%%begin:departments%
                ~%%begin:employees%
                    ~%%begin:roles%
                        ~%role%
                    ~%%end:roles%
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.setPath("companies.departments.employees.roles.role",
        i -> "director");
    assertEquals("director", rs.render().strip());
    rs.in("companies").setPath("departments.employees.roles.role",
        i -> "CIO");
    assertEquals("CIO", rs.render().strip());
    rs.in("companies.departments").setPath("employees.roles.role",
        i -> "programmer");
    assertEquals("programmer", rs.render().strip());
    rs.in("companies.departments.employees").setPath("roles.role",
        i -> "project manager");
    assertEquals("project manager", rs.render().strip());
    rs.in("companies.departments.employees.roles").setPath("role",
        i -> "analyst");
    assertEquals("analyst", rs.render().strip());
  }

  @Test
  public void ifNotSet00() throws ParseException {
    String src = """
        ~%greeting%
        ~%%begin:companies%
            ~%%begin:departments%
                ~%%begin:employees%
                    ~%%begin:roles%
                        ~%role%
                    ~%%end:roles%
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();

    rs.ifNotSet("greeting", i -> "Hello, World", VarGroup.TEXT);
    assertEquals("Hello, World", rs.render().strip());
    rs.ifNotSet("greeting", i -> "Hello, Moon", VarGroup.TEXT);
    assertEquals("Hello, World", rs.render().strip());
    rs.unset("greeting");
    rs.ifNotSet("greeting", i -> "Hello, Moon", VarGroup.TEXT);
    assertEquals("Hello, Moon", rs.render().strip());

    rs.unset("greeting");

    rs.ifNotSet("companies.departments.employees.roles.role",
        i -> "director");
    assertEquals("director", rs.render().strip());
    rs.ifNotSet("companies.departments.employees.roles.role",
        i -> "programmer");
    assertEquals("director", rs.render().strip());
    rs.unset("companies.departments.employees.roles.role");
    rs.ifNotSet("companies.departments.employees.roles.role",
        i -> "programmer",
        VarGroup.HTML);
    assertEquals("programmer", rs.render().strip());

  }

  @Test
  public void setDelayed00() throws ParseException {
    String src = "Hello ~%name%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    MutableInt mi = new MutableInt();
    rs.setDelayed("name", () -> "John" + mi.pp());
    assertEquals("Hello John0", rs.render());
    assertEquals("Hello John1", rs.render());
    assertEquals("Hello John2", rs.render());
  }

  @Test
  public void setDelayed01() throws ParseException {
    String src = "Hello ~%name%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    MutableInt mi = new MutableInt();
    rs.setDelayed("name", () -> "> John" + mi.pp(), VarGroup.HTML);
    assertEquals("Hello &gt; John0", rs.render());
    assertEquals("Hello &gt; John1", rs.render());
    assertEquals("Hello &gt; John2", rs.render());
  }

  @Test
  public void in00() throws ParseException {
    String src = """
        ~%%begin:companies%
          ~%%begin:departments%
            ~%%begin:employees%
              ~%foo%
            ~%%end:employees%
          ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies").in("departments").in("employees").set("foo", "bar");
    //System.out.println("*" + rs.render() + "*");
    assertEquals("bar", rs.render().strip());
  }

  @Test
  public void in01() throws ParseException {
    String src = """
        ~%%begin:companies%
          ~%%begin:departments%
            ~%%begin:employees%
              ~%foo%
            ~%%end:employees%
          ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies.departments.employees").set("foo", "bar");
    //System.out.println("*" + rs.render() + "*");
    assertEquals("bar", rs.render().strip());
  }

  @Test
  public void in02() throws ParseException {
    String src = """
        ~%%begin:companies%
          ~%%begin:companies%
            ~%%begin:companies%
              ~%foo%
            ~%%end:companies%
          ~%%end:companies%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies.companies.companies").set("foo", "bar");
    //System.out.println("*" + rs.render() + "*");
    assertEquals("bar", rs.render().strip());
  }

  @Test
  public void unset00() throws ParseException {
    String src = "~%foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", "bar");
    assertTrue(rs.allSet());
    rs.set("foo", "");
    assertTrue(rs.allSet());
    rs.unset("foo");
    assertFalse(rs.allSet());
  }

  @Test
  public void unset01() throws ParseException {
    String src = "~%foo% ~%bar%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", "foo");
    assertFalse(rs.allSet());
    rs.set("bar", "bar");
    assertTrue(rs.allSet());
    rs.unset("foo", "bar");
    assertFalse(rs.allSet());
  }

  @Test
  public void clear00() throws ParseException {
    String src = "~%%begin:foo% ~%bar% ~%%end:foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(Map.of("foo", Map.of("bar", "bozo")));
    assertTrue(rs.allSet());
    rs.clear("foo");
    assertFalse(rs.allSet());
    rs.repeat("foo", 2);
    rs.setPath("foo.bar", i -> "bar" + i);
    assertTrue(rs.allSet());
    rs.clear("foo");
    assertFalse(rs.allSet());
  }

  @Test
  public void clear01() throws ParseException {
    String src = """
        ~%%begin:foo0%
          ~%%begin:foo1% ~%bar% ~%%end:foo1%
          ~%%begin:foo2% ~%bar% ~%%end:foo2%
        ~%%end:foo0%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo0").populate("foo1", Map.of("bar", "bozo"));
    assertFalse(rs.allSet());
    assertFalse(rs.in("foo0").allSet());
    assertTrue(rs.in("foo0.foo1").allSet());
    rs.in("foo0").in("foo2").set("bar", "bozo");
    assertTrue(rs.allSet());
    rs.clear("foo0");
    assertFalse(rs.in("foo0.foo1").allSet());
    assertFalse(rs.in("foo0.foo2").allSet());
  }

  @Test
  public void getAllUnsetVariables00() throws ParseException {
    String src = """
        ~%%begin:companies%
            ~%%begin:departments%
                ~%%begin:employees%
                    ~%%begin:roles%
                        ~%role%
                    ~%%end:roles%
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    assertEquals("companies.departments.employees.roles.role",
        rs.getAllUnsetVariables().get(0));
    //System.out.println(rs.in("companies").getAllUnsetVariables());
  }

  @Test
  public void getTemplate00() throws ParseException {
    Template tmpl = Template.fromString("foo");
    RenderSession rs = tmpl.newRenderSession();
    assertSame(tmpl, rs.getTemplate());
  }

  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
