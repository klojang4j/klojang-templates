package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.Path;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    rs.setPath("foo0.foo1.bar", i -> "hello");
    //System.out.println(rs.getAllUnsetVariables());
  }

  @Test
  public void isSet00() throws ParseException {
    String src = """
        ~%global_var%
        ~%%begin:companies%
           ~%company_var%
            ~%%begin:departments%
               ~%department_var%
                ~%%begin:employees%
                   ~%employee_var%
                ~%%end:employees%
            ~%%end:departments%
        ~%%end:companies%
        """;
    Template tmpl = Template.fromString(src);
    SoloSession rs = (SoloSession) tmpl.newRenderSession();
    assertFalse(rs.state().isSet(Path.from("global_var")));
    assertFalse(rs.state().isSet(Path.from("companies.company_var")));
    assertFalse(rs.state().isSet(Path.from("companies.departments.department_var")));
    assertFalse(rs.state()
        .isSet(Path.from("companies.departments.employees.employee_var")));

    rs.ifNotSet("global_var", i -> "ICT");
    assertTrue(rs.state().isSet(Path.from("global_var")));
    rs.ifNotSet("companies.departments.department_var", i -> "ICT");
    assertTrue(rs.state().isSet(Path.from("companies.departments.department_var")));

  }

}
