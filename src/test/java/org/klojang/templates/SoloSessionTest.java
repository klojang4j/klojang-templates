package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.MutableInt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.klojang.templates.VarGroup.JS_ATTR;

@SuppressWarnings("MissingJavadoc")
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
    String out = rs.populateSolo("companies", List.of("Shell")).render();
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
    String out = rs.populateSolo("companies", List.of("MacDonald's", "Shell")).render();
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
          .populateSolo("companies", null, JS_ATTR, List.of("MacDonald's", "Shell"))
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
    String out = rs.populateDuo("companies", List.of("Shell", "USA")).render();
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
    String out = rs
          .populateSolo("companies", List.of("MacDonald's", "USA", "Shell", "UK"))
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
          .populateSolo("companies",
                null, JS_ATTR,
                List.of(
                      "MacDonald's",
                      "USA",
                      "Shell",
                      "UK"))
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
          .insert(Map.of("name", "MacDonald's"), JS_ATTR, null)
          .render();
    //System.out.println(out);
    String expected = """
          <p>MacDonald\\&#39;s</p>
          """;
    assertEquals(nospace(expected), nospace(out));
  }

  public record Address(String line1, String city, String zip) { }

  public record Employee(String firstName, String lastName, Address address) { }

  public record Department(String name, List<Employee> employees) { }

  public record Company(String name, double profits, List<Department> departments) { }

  // From the documentation - only print to std out to make sure it works
  @Test
  public void populate00() throws ParseException {
    Address addr = new Address("Main st 7", "New York", "NY12345");
    Employee emp = new Employee("John", "Smith", addr);
    Department dept = new Department("ICT", List.of(emp, emp, emp, emp));
    Company comp = new Company("Shell", 1_200_000_000, List.of(dept, dept, dept));

    String src = """
          ~%%begin:companies%
              Name ......: ~%name%
              Profits....: ~%profits%
              ~%%begin:departments%
                  Name ......: ~%name%
                  ~%%begin:employees%
                      ~%firstName% ~%lastName%
                      ~%%begin:address%
                      ~%line1%, ~%city%, ~%zip%
                      ~%%end:address%
                  ~%%end:employees%
              ~%%end:departments%
          ~%%end:companies%
          """;

    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate("companies", List.of(comp, comp));
    System.out.println(rs.render());
  }

  @Test
  public void populate01() throws ParseException {
    Address addr = new Address("Main st 7", "New York", "NY12345");
    Employee emp = new Employee("John", "Smith", addr);
    Department dept = new Department("ICT", List.of(emp, emp, emp, emp));
    Company comp = new Company("Shell", 1_200_000_000, List.of(dept, dept, dept));

    String src = """
          ~%%begin:companies%
              Name ......: ~%name%
              Profits....: ~%profits%
              ~%%begin:departments%
                  Name ......: ~%name%
                  ~%%begin:employees%
                      ~%firstName% ~%lastName%
                      ~%%begin:address%
                      ~%line1%, ~%city%, ~%zip%
                      ~%%end:address%
                  ~%%end:employees%
              ~%%end:departments%
          ~%%end:companies%
          """;

    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.populate("companies", List.of(comp, comp), "*****************\n", null, null);
    System.out.println(rs.render());
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
          VarGroup.TEXT, true, i -> "John"
    );
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
          VarGroup.TEXT, false, i -> "John"
    );
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
          VarGroup.TEXT, false, i -> "John" + i
    );
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

    rs.ifNotSet("greeting", VarGroup.TEXT, i -> "Hello, World");
    assertEquals("Hello, World", rs.render().strip());
    rs.ifNotSet("greeting", VarGroup.TEXT, i -> "Hello, Moon");
    assertEquals("Hello, World", rs.render().strip());
    rs.unset("greeting");
    rs.ifNotSet("greeting", VarGroup.TEXT, i -> "Hello, Moon");
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
          VarGroup.HTML, i -> "programmer"
    );
    assertEquals("programmer", rs.render().strip());

  }

  @Test
  public void setDelayed00() throws ParseException {
    String src = "Hello ~%name%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    MutableInt mi = new MutableInt();
    rs.setDelayed("name", () -> "John" + mi.increment());
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
    rs.setDelayed("name", VarGroup.HTML, () -> "> John" + mi.increment());
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
    assertFalse(rs.hasUnsetVariables());
    rs.set("foo", "");
    assertFalse(rs.hasUnsetVariables());
    rs.unset("foo");
    assertTrue(rs.hasUnsetVariables());
  }

  @Test
  public void unset01() throws ParseException {
    String src = "~%foo% ~%bar%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.set("foo", "foo");
    assertTrue(rs.hasUnsetVariables());
    rs.set("bar", "bar");
    assertFalse(rs.hasUnsetVariables());
    rs.unset("foo", "bar");
    assertTrue(rs.hasUnsetVariables());
  }

  @Test
  public void clear00() throws ParseException {
    String src = "~%%begin:foo% ~%bar% ~%%end:foo%";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    rs.insert(Map.of("foo", Map.of("bar", "bozo")));
    assertFalse(rs.hasUnsetVariables());
    rs.clear("foo");
    assertTrue(rs.hasUnsetVariables());
    rs.repeat("foo", 2);
    rs.setPath("foo.bar", i -> "bar" + i);
    assertFalse(rs.hasUnsetVariables());
    rs.clear("foo");
    assertTrue(rs.hasUnsetVariables());
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
    assertTrue(rs.hasUnsetVariables());
    assertTrue(rs.in("foo0").hasUnsetVariables());
    assertFalse(rs.in("foo0.foo1").hasUnsetVariables());
    rs.in("foo0").in("foo2").set("bar", "bozo");
    assertFalse(rs.hasUnsetVariables());
    rs.clear("foo0");
    assertTrue(rs.in("foo0.foo1").hasUnsetVariables());
    assertTrue(rs.in("foo0.foo2").hasUnsetVariables());
  }

  @Test
  public void disable00() throws ParseException {
    String src = "A~%%begin:foo%This is Foo~%%end:foo%B";
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    assertEquals("AB", rs.render());
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
  public void allSet00() throws ParseException {
    String src = """
          FOO
          ~%%begin:companies%
              COMPANY
              ~%%begin:departments%
                  DEPARTMENT
                  ~%%begin:employees%
                      EMPLOYEE
                  ~%%end:employees%
              ~%%end:departments%
          ~%%end:companies%
          """;
    Template tmpl = Template.fromString(src);
    RenderSession rs = tmpl.newRenderSession();
    assertFalse(rs.hasUnsetVariables());
    assertFalse(rs.enableRecursive("companies").hasUnsetVariables());
  }

  @Test
  public void getTemplate00() throws ParseException {
    Template tmpl = Template.fromString("foo");
    RenderSession rs = tmpl.newRenderSession();
    assertSame(tmpl, rs.getTemplate());
  }

  public record Person(String firstName, String lastName, int age) { }


  private static String nospace(String s) {
    return s.replaceAll("\\s+", "");
  }

}
