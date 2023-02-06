package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplateTest {

  @Test
  public void fromString00() throws ParseException {
    String src = """
        <html><body>
            ~%%begin:main%
              ~%%include:contents_a:include-01.html%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(2, tmpl.getNestedTemplates().size());
    assertEquals("main", tmpl.getNestedTemplates().get(0).getName());
    assertEquals("contents_b", tmpl.getNestedTemplates().get(1).getName());
  }

  @Test
  public void fromFile00() throws ParseException {
    assertThrows(IllegalArgumentException.class,
        () -> Template.fromFile("/foo/∞/bar/π"));
  }

  @Test
  public void getPath00() throws ParseException {
    String src = """
         <html><body>
            ~%%begin:main%
              ~%%include:contents_a:include-01.html%
            ~%%end:main%
            ~%%include:contents_b:include-01.html%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(Optional.empty(), tmpl.getPath());
    Template nested = tmpl.getNestedTemplate("main");
    assertEquals(Optional.empty(), nested.getPath());
    nested = nested.getNestedTemplate("contents_a");
    assertEquals(Optional.of("include-01.html"), nested.getPath());
    nested = tmpl.getNestedTemplate("contents_b");
    assertEquals(Optional.of("include-01.html"), nested.getPath());
  }

}
