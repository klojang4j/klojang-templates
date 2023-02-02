package org.klojang.templates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateTest {

  @Test
  public void fromString() throws ParseException {
    String src = """
        <html><body>
            ~%%begin:main-table%
              ~%%include:contents1:include-01.html%
            ~%%end:main-table%
            ~%%include:contents2:include-01.html%
        </body></html>
        """;
    Template tmpl = Template.fromString(getClass(), src);
    assertEquals(2, tmpl.getNestedTemplates().size());
  }

}
