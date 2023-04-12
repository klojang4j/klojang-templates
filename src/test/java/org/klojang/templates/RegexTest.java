package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {

  @Test
  public void printAll00() {
    Regex.printAll();
    ;
  }

  @Test
  public void variable00() throws ParseException {
    assertTrue(Regex.VARIABLE.matcher("~%person%").find());
    assertTrue(Regex.VARIABLE.matcher("foo~%person%").find());
    assertTrue(Regex.VARIABLE.matcher("foo ~%person%").find());
    assertTrue(Regex.VARIABLE.matcher("foo ~%person%bar").find());
    assertTrue(Regex.VARIABLE.matcher("foo ~%person% bar").find());
    assertTrue(Regex.VARIABLE.matcher("foo ~%person% bar").find());
    assertTrue(Regex.VARIABLE.matcher("foo\n~%person%\nbar").find());
  }

  @Test
  public void variable01() {
    assertTrue(Regex.VARIABLE.matcher("~%person.address%").find());
    assertTrue(Regex.VARIABLE.matcher("~%person.address.street%").find());
  }

  @Test
  public void variable02() {
    assertTrue(Regex.VARIABLE.matcher("~%html:person.address%").find());
    assertTrue(Regex.VARIABLE.matcher("~%js:person.address%").find());
    assertTrue(Regex.VARIABLE.matcher("~%text:person.address%").find());
  }

  @Test
  public void variable03() {
    assertTrue(Regex.CMT_VARIABLE.matcher("<!--~%person%-->").find());
    assertTrue(Regex.CMT_VARIABLE.matcher("<!-- ~%person% -->").find());
    assertFalse(Regex.CMT_VARIABLE.matcher("<!--\t~%person%\t-->").find());
    assertTrue(Regex.CMT_VARIABLE.matcher("FOO\t<!--~%person%-->BAR").find());
    assertFalse(Regex.CMT_VARIABLE.matcher("\n<!--  ~%person%  -->\n").find());
  }

  @Test
  public void variable04() throws ParseException {
    String src = "<!--~%firstName%-->John<!--%-->";
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    String s = rs.render();
    assertEquals("", s);
  }

  @Test
  public void variable05() throws ParseException {
    String src = "<!--~%firstName%-->John<!--%-->";
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession();
    rs.set("firstName", "Mark");
    String s = rs.render();
    assertEquals("Mark", s);
  }



  @Test
  public void include02() throws ParseException {
    assertTrue(Regex.INCLUDED_TEMPLATE
        .matcher("~%%include:/views/rows.html%%")
        .find());
  }

  @Test
  public void include03() {
    assertTrue(Regex.INCLUDED_TEMPLATE
        .matcher("~%%include:foo:/views/rows.html%%")
        .find());
  }

  @Test
  public void include04() {
    String src = "FOO<!-- ~%%include:/views/rows.html%% -->BAR";
    assertTrue(Regex.INCLUDED_TEMPLATE.matcher(src).find());
  }

  @Test
  public void include05() {
    String src = "FOO ******* ~%%include:foo:/views/rows.html%% ******* BAR";
    assertTrue(Regex.INCLUDED_TEMPLATE.matcher(src).find());
  }

  @Test
  public void include06() {
    String src = "FOO ******* ~%%include:foo:/views/rows.html%% ******* BAR";
    Matcher m = Regex.INCLUDED_TEMPLATE.matcher(src);
    m.find();
    assertEquals(3, m.groupCount()); // The match itself, group(0), does not count
    assertEquals("~%%include:foo:/views/rows.html%%", m.group(0));
    assertEquals("foo:", m.group(1));
    assertEquals("foo", m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void include07() {
    String src = "FOO ******* ~%%include:/views/rows.html%% ******* BAR";
    Matcher m = Regex.INCLUDED_TEMPLATE.matcher(src);
    m.find();
    assertEquals(3, m.groupCount());
    assertEquals("~%%include:/views/rows.html%%", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void cmtInclude01() {
    String src = "FOO ******* <!--~%%include:/views/rows.html%%--> ******* BAR";
    Matcher m = Regex.CMT_INCLUDED_TEMPLATE.matcher(src);
    m.find();
    assertEquals(3, m.groupCount());
    assertEquals("<!--~%%include:/views/rows.html%%-->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void cmtInclude02() {
    String src = "FOO ******* <!-- ~%%include:/views/rows.html%% --> ******* BAR";
    Matcher m = Regex.CMT_INCLUDED_TEMPLATE.matcher(src);
    m.find();
    assertEquals(3, m.groupCount());
    assertEquals("<!-- ~%%include:/views/rows.html%% -->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void cmtInclude03() {
    String src = "\n\nFOO ******* <!--~%%include:/views/rows.html%%--> ******* \nBAR";
    Matcher m = Regex.CMT_INCLUDED_TEMPLATE.matcher(src);
    m.find();
    assertEquals(3, m.groupCount());
    assertEquals("<!--~%%include:/views/rows.html%%-->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void ditch00() {
    String src = "FOO <!--%%--><tr><td>Hi!</td></tr><!--%%-->";
    Matcher m = Regex.DITCH_BLOCK.matcher(src);
    assertTrue(m.find());
  }

  @Test
  public void ditch01() throws ParseException {
    String src = "<!--%%-->foo==bar<!--%%-->";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.render();
    assertEquals("", out);
  }

  @Test
  public void ditch02() throws ParseException, RenderException {
    String src = """
        <td>
          ~%html-extra2:foo%
          <!--%%-->
          foo==bar
          ~%ignoreMe%
          <!--%%-->
          ~%bar%
        </td>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.set("foo", 'A').set("bar", 'B').render();
    out = out.replaceAll("\\s", "");
    assertEquals("<td>AB</td>", out);
  }

  @Test
  public void ditch05() throws ParseException, RenderException {
    String src = """
        <td>
          ~%foo%
          <!--%%-->
          ~%%begin:foo%
            <p>Hello world</p>
          ~%%end:foo%
          <!--%%-->
          ~%bar%
        </td>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.set("foo", 'A').set("bar", 'B').render();
    out = out.replaceAll("\\s", "");
    assertEquals("<td>AB</td>", out);
  }

  @Test
  public void cmtVar01() throws ParseException {
    Matcher m = Regex.CMT_VARIABLE.matcher("<!-- ~%person% -->");
    assertTrue(m.find());
    assertEquals("person", m.group(3));
  }

  @Test
  public void includedTemplate00() {
    Matcher m = Regex.INCLUDED_TEMPLATE.matcher("~%%include:sammy:foo.b%ar.html%%");
    assertTrue(m.find());
    assertEquals("sammy", m.group(2));
    assertEquals("foo.b%ar.html", m.group(3));
  }

  @Test
  public void includedTemplate01() {
    Matcher m = Regex.INCLUDED_TEMPLATE.matcher(
        "~%%include:main-table:foo.html?x=23&y=44#anchor1%%%");
    assertTrue(m.find());
    assertEquals("main-table", m.group(2));
    assertEquals("foo.html?x=23&y=44#anchor1", m.group(3));
  }

}
