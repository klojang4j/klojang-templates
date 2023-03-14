package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.MutableInt;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  public void setNested00() throws ParseException {
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
    rs.setNested("companies.departments.employees.firstName",
        i -> "John",
        VarGroup.TEXT, true);
    rs.setNested("companies.departments.employees.lastName", i -> "Smith");
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
  public void setNested01() throws ParseException {
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
    rs.setNested("companies.departments.employees.firstName",
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
  public void setNested02() throws ParseException {
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
    rs.setNested("companies.departments.employees.firstName",
        i -> "John" + i,
        VarGroup.TEXT,
        false);
    rs.setNested("companies.departments.employees.lastName", i -> "Smith" + i);
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

  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
