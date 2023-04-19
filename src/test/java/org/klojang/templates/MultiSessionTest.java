package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.MutableInt;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MultiSessionTest {

  @Test
  public void set00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            <p>Name: ~%name%</p>
            <p>Country: ~%country%</p>
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .set("name", "Apple")
        .set("country", "USA")
        .render();
    String expected = """
            <p>Name: Apple</p>
            <p>Country: USA</p>
                
            <p>Name: Apple</p>
            <p>Country: USA</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void set01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            <p>Name: ~%name%</p>
            <p>Country: ~%country%</p>
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .set("name", "Apple", VarGroup.JS)
        .set("country", "USA", VarGroup.TEXT)
        .render();
    String expected = """
            <p>Name: Apple</p>
            <p>Country: USA</p>
                
            <p>Name: Apple</p>
            <p>Country: USA</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void setDelayed00() throws ParseException {
    String src = "~%%begin:companies%~%name%~%name%~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    MutableInt mi = new MutableInt();
    String out = rs.repeat("companies", 2)
        .setDelayed("name", () -> "foo" + mi.pp())
        .render();
    // System.out.println(out);
    assertEquals("foo0foo1foo2foo3", out);
  }

  @Test
  public void setDelayed01() throws ParseException {
    String src = "~%%begin:companies%~%name%~%name%~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    MutableInt mi = new MutableInt();
    String out = rs.repeat("companies", 2)
        .setDelayed("name", () -> ">" + mi.pp(), VarGroup.HTML)
        .render();
    // System.out.println(out);
    assertEquals("&gt;0&gt;1&gt;2&gt;3", out);
  }

  @Test
  public void setNested00() throws ParseException {
    String src = """
        ~%%begin:companies%
             ~%%begin:departments%~%name%~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.setNested("companies.departments.name", i -> "foo");
    String out = rs.render();
    //System.out.println(out);
    assertEquals("foo", nospace(out));
  }

  @Test
  public void setNested01() throws ParseException {
    String src = """
        ~%%begin:companies%
             ~%%begin:departments%~%name%~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.setNested("companies.departments.name", i -> "<", VarGroup.HTML, true);
    String out = rs.render();
    //System.out.println(out);
    assertEquals("&lt;", nospace(out));
  }

  @Test
  public void setNested02() throws ParseException {
    String src = """
        ~%%begin:companies%
             ~%%begin:departments%~%name%~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3);
    rs.setNested("companies.departments.name", i -> "foo");
    String out = rs.render();
    //System.out.println("*" + out + "*");
    assertEquals("foofoofoo", nospace(out));
  }

  @Test
  public void setNested03() throws ParseException {
    String src = """
        ~%%begin:companies%
             ~%%begin:departments%~%name%~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3);
    rs.setNested("companies.departments.name", i -> "foo" + i + "|");
    String out = rs.render();
    //System.out.println("*" + out + "*");
    assertEquals("foo0|foo0|foo0|", nospace(out));
  }

  @Test
  public void setNested04() throws ParseException {
    String src = """
        ~%%begin:companies%
             ~%%begin:departments%~%name%~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies").repeat("departments", 3);
    rs.setNested("companies.departments.name", i -> "foo" + i + "|");
    String out = rs.render();
    //System.out.println("*" + out + "*");
    assertEquals("foo0|foo1|foo2|", nospace(out));
  }

  @Test
  public void repeat00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            <p>~%name%</p>
            ~%%begin:departments%<p>~%name%</p>~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3).set("name", "Shell").repeat("departments", 3).set(
        "name",
        "HR");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
                
            <p>Shell</p>
            <p>HR</p><p>HR</p><p>HR</p>
                
            <p>Shell</p>
            <p>HR</p><p>HR</p><p>HR</p>
                
            <p>Shell</p>
            <p>HR</p><p>HR</p><p>HR</p>
                
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void isFullyPopulated00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            <p>~%name%</p>
            ~%%begin:departments%<p>~%name%</p>~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3).set("name", "Shell");
    assertFalse(rs.in("companies").isFullyPopulated());
    rs.in("companies").repeat("departments", 3).set("name", "HR");
    assertTrue(rs.in("companies").isFullyPopulated());
  }

  @Test
  public void isFullyPopulated01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            <p>~%name%</p>
            ~%%begin:departments%<p>~%name%</p>~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 3).set("name", "Shell");
    assertFalse(rs.isFullyPopulated());
    assertFalse(rs.in("companies").isFullyPopulated());
    rs.in("companies").repeat("departments", 3);
    assertFalse(rs.isFullyPopulated());
    assertFalse(rs.in("companies").isFullyPopulated());
    assertFalse(rs.in("companies").in("departments").isFullyPopulated());
    rs.in("companies").in("departments").set("name", "HR");
    assertTrue(rs.isFullyPopulated());
    assertTrue(rs.in("companies").isFullyPopulated());
    assertTrue(rs.in("companies").in("departments").isFullyPopulated());
  }

  @Test
  public void populate00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .populate("departments",
            Map.of("name", "HR", "description", "Human Resources"))
        .render();
    String expected = """
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
            
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .populate("departments",
            Map.of("name", "ICT", "description", "Information Technology"),
            "name")
        .populate("departments",
            Map.of("name", "HR", "description", "Human Resources"),
            "description")
        .render();
    String expected = """
        <p>Name: ICT</p>
        <p>Description: Human Resources</p>
            
        <p>Name: ICT</p>
        <p>Description: Human Resources</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate02() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .populate("departments",
            Map.of("name", "HR", "description", "Human Resources"),
            VarGroup.HTML)
        .render();
    String expected = """
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
            
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void in00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("companies")
        .in("departments")
        .set("name", "HR");
    rs.in("companies")
        .in("departments")
        .set("description", "Human Resources");
    String out = rs.in("companies").render();
    String expected = """
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void in01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:departments%
                <p>Name: ~%name%</p>
                <p>Description: ~%description%</p>
            ~%%end:departments%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 2)
        .in("departments")
        .set("name", "HR");
    rs.in("companies")
        .in("departments")
        .set("description", "Human Resources");
    String out = rs.in("companies").render();
    String expected = """
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
            
        <p>Name: HR</p>
        <p>Description: Human Resources</p>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void show00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2).enable().render();
    //System.out.println(out);
    String expected = """

        Hello 
        World 

        Hello 
        World 

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void show01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2).enable("text-only2").render();
    //System.out.println(out);
    String expected = """

        World 

        World 

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void show02() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .enable("text-only2")
        .enable("text-only1")
        .render();
    //System.out.println(out);
    String expected = """

        Hello
        World 

        Hello
        World 

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void show03() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.repeat("companies", 2)
        .enable("text-only2", "text-only1")
        .render();
    //System.out.println(out);
    String expected = """

        Hello
        World 

        Hello
        World 

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void show04() throws ParseException {
    String src = " ~%%begin:companies% Hello ~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.enable(3, "companies").render();
    System.out.println(out);
    String expected = "Hello Hello Hello";
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void showRecursive00() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello
                ~%%begin:text-only3% Brave ~%%end:text-only3%
            ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.in("companies").enableRecursive().render();
    //System.out.println(out);
    String expected = """

        Hello
               Brave
            
        World 

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void showRecursive01() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello
                ~%%begin:text-only3% Brave ~%%end:text-only3%
            ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.in("companies").enableRecursive("text-only1").render();
    //System.out.println(out);
    String expected = """

        Hello

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void showRecursive02() throws ParseException {
    String src = """
        <html><body>
        ~%%begin:companies%
            ~%%begin:text-only1% Hello
                ~%%begin:text-only3% Brave ~%%end:text-only3%
            ~%%end:text-only1%
            ~%%begin:text-only2% World ~%%end:text-only2%
        ~%%end:companies%
        </body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.in("companies")
        .enableRecursive("text-only1", "text-only3")
        .render();
    //System.out.println(out);
    String expected = """

        Hello
            Brave

        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate1_00() throws ParseException {
    String src = """
        <html><body>~%%begin:foo%
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo").populate1("companies", "Shell");
    String out = rs.render();
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
        <html><body>~%%begin:foo%
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("foo", 1).populate1("companies", "MacDonald's", "Shell");
    String out = rs.render();
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
        <html><body>~%%begin:foo%
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo").populate1("companies", VarGroup.JS_ATTR, "MacDonald's", "Shell");
    String out = rs.render();
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
        <html><body>~%%begin:foo%
        ~%%begin:companies%
        <p>~%name% (~%country%)</p>
        ~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo").populate2("companies", "Shell", "USA");
    String out = rs.render();
    //System.out.println(out);
    String expected = """
        <html><body>
        <p>Shell (USA)</p>
        </body></html>
        """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate2_01() throws ParseException {
    String src = """
        <html><body>~%%begin:foo%
        ~%%begin:companies%
        <p>~%name% (~%country%)</p>
        ~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo").populate2("companies",
        VarGroup.HTML,
        "MacDonald<<<s",
        "USA",
        "Shell",
        "UK");
    String out = rs.render();
    System.out.println(out);
    String expected = """
        <html><body>
        <p>MacDonald&lt;&lt;&lt;s (USA)</p>
        <p>Shell (UK)</p>
        </body></html>
         """;
    assertEquals(nospace(expected), nospace(out));
  }

  @Test
  public void populate2_02() throws ParseException {
    String src = """
        <html><body>~%%begin:foo%
        ~%%begin:companies%<p>~%name%</p>~%%end:companies%
        ~%%end:foo%</body></html>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.in("foo").populate1("companies",
        VarGroup.JS_ATTR,
        "MacDonald's",
        "USA",
        "Shell",
        "UK");
    String out = rs.render();
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
  public void getChildSessions00() throws ParseException {
    String src = """
        ~%%begin:companies%
          ~%%begin:departments%
            ~%%begin:employees%
            ~%%end:employees%
          ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 2).repeat("departments", 3).repeat("employees", 4);
    assertEquals(2, rs.getChildSessions("companies").size());
    assertEquals(6, rs.in("companies").getChildSessions("departments").size());
    assertEquals(24,
        rs.in("companies").in("departments").getChildSessions("employees").size());
  }

  @Test
  public void render00() throws ParseException {
    String src = "~%%begin:companies%~%var%~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 2).set("var", 2);
    OutputStream out = new ByteArrayOutputStream();
    rs.render(out);
    assertEquals("22", out.toString());
  }

  @Test
  public void render01() throws ParseException {
    String src = "~%%begin:companies%~%var%~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("companies", 2).set("var", 2);
    StringBuilder out = new StringBuilder();
    rs.render(out);
    assertEquals("22", out.toString());
  }

  @Test
  public void enable00() throws ParseException {
    String src = "~%%begin:foo%~%%begin:bar%2~%%end:bar%~%%end:foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.repeat("foo", 2).enable("bar");
    StringBuilder out = new StringBuilder();
    rs.render(out);
    assertEquals("22", out.toString());
  }

  @Test
  public void unset00() throws ParseException {
    String src = "~%%begin:companies% ~%var% ~%%end:companies%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    RenderSession rs2 = rs.repeat("companies", 2).set("var", 2);
    assertTrue(rs.isFullyPopulated());
    assertTrue(rs2.isFullyPopulated());
    rs2.unset("var");
    assertFalse(rs.isFullyPopulated());
    assertFalse(rs2.isFullyPopulated());
  }

  @Test
  public void clear00() throws ParseException {
    String src = """
        ~%%begin:companies%
          ~%%begin:departments%
            ~%name%
          ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    RenderSession rs2 = rs.repeat("companies", 2);
    RenderSession rs3 = rs2.repeat("departments", 3);
    rs.setNested("companies.departments.name", i -> "D" + i);
    assertTrue(rs.isFullyPopulated());
    assertTrue(rs2.isFullyPopulated());
    assertTrue(rs3.isFullyPopulated());
    rs2.clear("departments");
    assertFalse(rs.isFullyPopulated());
    assertFalse(rs2.isFullyPopulated());
    assertFalse(rs3.isFullyPopulated());
  }

  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
