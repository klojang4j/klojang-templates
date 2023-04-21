package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RenderStateTest {

  @Test
  public void getAllUnsetVariables00() throws ParseException {
    String src = """
        ~%%begin:foo0%
          ~%%begin:foo1%
              ~%bar%
              ~%%begin:foo1%
                  ~%bar%
              ~%%end:foo1%
          ~%%end:foo1%
          ~%%begin:foo2%
            ~%bar%
          ~%%end:foo2%
        ~%%end:foo0%
        """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    System.out.println(rs.getAllUnsetVariables());
  }
}
