package org.klojang.templates.x.parse;

import org.junit.jupiter.api.Test;
import org.klojang.templates.ParseException;
import org.klojang.templates.RenderException;
import org.klojang.templates.RenderSession;
import org.klojang.templates.Template;
import org.klojang.util.IOMethods;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {

  @Test
  public void print() throws ParseException {
    Regex.of().printAll();
  }

  @Test
  public void variable00() throws ParseException {
    assertTrue(Regex.of().variable.matcher("~%person%").find());
    assertTrue(Regex.of().variable.matcher("foo~%person%").find());
    assertTrue(Regex.of().variable.matcher("foo ~%person%").find());
    assertTrue(Regex.of().variable.matcher("foo ~%person%bar").find());
    assertTrue(Regex.of().variable.matcher("foo ~%person% bar").find());
    assertTrue(Regex.of().variable.matcher("foo ~%person% bar").find());
    assertTrue(Regex.of().variable.matcher("foo\n~%person%\nbar").find());
  }

  @Test
  public void variable01() throws ParseException {
    assertTrue(Regex.of().variable.matcher("~%person.address%").find());
    assertTrue(Regex.of().variable.matcher("~%person.address.street%").find());
  }

  @Test
  public void variable02() throws ParseException {
    assertTrue(Regex.of().variable.matcher("~%html:person.address%").find());
    assertTrue(Regex.of().variable.matcher("~%js:person.address%").find());
    assertTrue(Regex.of().variable.matcher("~%text:person.address%").find());
  }

  @Test
  public void variable03() throws ParseException {
    assertTrue(Regex.of().cmtVariable.matcher("<!--~%person%-->").find());
    assertTrue(Regex.of().cmtVariable.matcher("<!-- ~%person% -->").find());
    assertTrue(Regex.of().cmtVariable.matcher("<!--\t~%person%\t-->").find());
    assertTrue(Regex.of().cmtVariable.matcher("FOO\t<!--~%person%-->BAR").find());
    assertTrue(Regex.of().cmtVariable.matcher("\n<!--      \t~%person%\t\t   -->\n")
        .find());
  }

  @Test
  public void test04() throws ParseException, IOException {
    try (InputStream is = getClass().getResourceAsStream("RegexTest.test04.html")) {
      String s = IOMethods.getContents(is);
      assertTrue(Regex.of().inlineTemplate.matcher(s).find());
    }
  }

  @Test
  public void test05() throws ParseException, IOException {
    try (InputStream is = getClass().getResourceAsStream("RegexTest.test05.html")) {
      String s = IOMethods.getContents(is);
      assertTrue(Regex.of().cmtInlineTemplate.matcher(s).find());
    }
  }

  @Test
  public void include01() throws ParseException, IOException {
    try (InputStream is = getClass().getResourceAsStream("RegexTest.test06.html")) {
      String s = IOMethods.getContents(is);
      assertTrue(Regex.of().cmtInlineTemplate.matcher(s).find());
    }
  }

  @Test
  public void include02() throws ParseException {
    assertTrue(Regex.of().includedTemplate.matcher("~%%include:/views/rows.html%")
        .find());
  }

  @Test
  public void include03() throws ParseException {
    assertTrue(Regex.of().includedTemplate.matcher("~%%include:foo:/views/rows.html%")
        .find());
  }

  @Test
  public void include04() throws ParseException {
    assertTrue(
        Regex.of()
            .cmtIncludedTemplate
            .matcher("FOO<!-- ~%%include:/views/rows.html% -->BAR")
            .find());
  }

  @Test
  public void include05() throws ParseException {
    assertTrue(
        Regex.of()
            .cmtIncludedTemplate
            .matcher("FOO\n<!-- \t ~%%include:foo:/views/rows.html%\t--> BAR")
            .find());
  }

  @Test
  public void include06() throws ParseException {
    Matcher m =
        Regex.of()
            .includedTemplate
            .matcher("FOO ******* ~%%include:foo:/views/rows.html% ******* BAR");
    m.find();
    assertEquals(3, m.groupCount()); // The match itself, group(0), does not count
    assertEquals("~%%include:foo:/views/rows.html%", m.group(0));
    assertEquals("foo:", m.group(1));
    assertEquals("foo", m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void include07() throws ParseException {
    Matcher m =
        Regex.of().includedTemplate.matcher(
            "FOO ******* ~%%include:/views/rows.html% ******* BAR");
    m.find();
    assertEquals(3,
        m.groupCount()); // Number of groups defined by regex, not by input
    assertEquals("~%%include:/views/rows.html%", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void hiddenInclude01() throws ParseException {
    Matcher m =
        Regex.of()
            .cmtIncludedTemplate
            .matcher("FOO ******* <!--~%%include:/views/rows.html%--> ******* BAR");
    m.find();
    assertEquals(3,
        m.groupCount()); // Number of groups defined by regex, not by input
    assertEquals("<!--~%%include:/views/rows.html%-->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void hiddenInclude02() throws ParseException {
    Matcher m =
        Regex.of()
            .cmtIncludedTemplate
            .matcher(
                "FOO ******* <!--\n\t~%%include:/views/rows.html% \t\n  --> ******* BAR");
    m.find();
    assertEquals(3,
        m.groupCount()); // Number of groups defined by regex, not by input
    assertEquals("<!--\n\t~%%include:/views/rows.html% \t\n  -->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void hiddenInclude03() throws ParseException {
    Matcher m =
        Regex.of()
            .cmtIncludedTemplate
            .matcher(
                "\n\nFOO ******* <!--\n\t~%%include:/views/rows.html% \t\n  --> ******* \nBAR");
    m.find();
    assertEquals(3,
        m.groupCount()); // Number of groups defined by regex, not by input
    assertEquals("<!--\n\t~%%include:/views/rows.html% \t\n  -->", m.group(0));
    assertNull(m.group(1));
    assertNull(m.group(2));
    assertEquals("/views/rows.html", m.group(3));
  }

  @Test
  public void ditch00() throws ParseException {
    Matcher m = Regex.of().ditchBlock.matcher(
        "FOO <!--%%--><tr><td>Hi!</td></tr><!--%%-->");
    assertTrue(m.find());
  }

  @Test
  public void ditch01() throws ParseException {
    String src = "<!--%% this is a comment -->foo==bar<!--%%-->";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.render();
    assertEquals("", out);
  }

  @Test
  public void ditch02() throws ParseException {
    String src = "<td><!--%% this is a comment -->foo==bar<!--%%--></td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.render();
    assertEquals("<td></td>", out);
  }

  @Test
  public void ditch03() throws ParseException, RenderException {
    String src = "<td>~%foo%<!--%% this is a comment -->foo==bar<!--%%-->~%bar%</td>";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.set("foo", 'A').set("bar", 'B').render();
    assertEquals("<td>AB</td>", out);
  }

  @Test
  public void ditch04() throws ParseException, RenderException {
    String src = """
        <td>
          ~%foo%
          <!--%% ~%ignoreMe% -->
          foo==bar
          ~%ignoreMe%
          <!--%%-->
          ~%bar%
        </td>
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    String out = rs.set("foo", 'A').set("bar", 'B').render();
    out=out.replaceAll("\\s","");
    assertEquals("<td>AB</td>", out);
  }

  @Test
  public void hiddenVar01() throws ParseException {
    Matcher m = Regex.of().cmtVariable.matcher("<!-- ~%person% -->");
    assertTrue(m.find());
    assertEquals("person", m.group(3));
  }

}
