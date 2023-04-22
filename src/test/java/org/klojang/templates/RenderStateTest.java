package org.klojang.templates;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    assertEquals(List.of(), rs.getUnsetVariables());
    assertEquals(List.of("foo0.foo1.bar", "foo0.foo1.foo1.bar", "foo0.foo2.bar"),
        rs.getAllUnsetVariables());
    rs.setNested("foo0.foo1.bar", i -> "hello");
    System.out.println(rs.getAllUnsetVariables());
  }

}
