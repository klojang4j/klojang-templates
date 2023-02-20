package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.invoke.BeanReader;
import org.klojang.path.util.MapBuilder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessorRegistryTest {

  public static class Person {

    int id;
    String name;

    public Person(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

  }

  public static Accessor<Person> acc = (person, prop) -> switch (prop) {
    case "id" -> "[" + person.getId() + "]";
    case "name" -> "[" + person.getName() + "]";
    default -> Accessor.UNDEFINED;
  };

  BeanReader br = BeanReader.forClass(Person.class)
      .withInt("id")
      .withString("name")
      .build();

  @Test
  public void test00() throws ParseException {
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(br)
        .freeze();
    String src = """
        id: ~%id%
        name: ~%name%
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John", out);
  }

  @Test
  public void test01() throws ParseException {
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(br, String::toLowerCase)
        .freeze();
    String src = """
        id: ~%id%
        name: ~%name%
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John", out);
  }

  @Test
  public void test02() throws ParseException {
    String src = """
        id: ~%id%
        name: ~%name%;
        ~%%begin:foo%
          id: ~%id%
          name: ~%name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(br)
        .register(acc, Person.class, t.getNestedTemplate("foo"))
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    rs.populate("foo", new Person(12, "Mark"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John; id: [12] name: [Mark]", out);
  }

  @Test
  public void test03() throws ParseException {
    String src = """
        id: ~%id%
        name: ~%name%;
        ~%%begin:foo%
          id: ~%id%
          name: ~%name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class)
        .register(br, t, String::toLowerCase)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    rs.populate("foo", new Person(12, "Mark"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John; id: [12] name: [Mark]", out);
  }

  @Test
  public void test04() throws ParseException {
    String src = """
        id: ~%id%
        name: ~%name%;
        ~%%begin:foo%
          id: ~%id%
          name: ~%name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class)
        .register(br, t, String::toLowerCase)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(Optional.empty());
    rs.populate("foo", Optional.of(new Person(12, "Mark")));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    //System.out.println(out);
    assertEquals("id: name: ; id: [12] name: [Mark]", out);
  }

  @Test
  public void test05() throws ParseException {
    String src = """
        id: ~%id%
        name: ~%name%;
        ~%%begin:foo%
          id: ~%id%
          name: ~%name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class, t)
        .register(br)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(Optional.of(new Person(10, "John")));
    rs.populate("foo", Optional.empty());
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: [10] name: [John];", out);
  }

  @Test
  public void test06() throws ParseException {
    String src = """
        id: ~%bar.id%
        name: ~%bar.name%;
        ~%%begin:foo%
          id: ~%teapot.id%
          name: ~%teapot.name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class, t)
        .register(br)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    Map<String, Object> map = MapBuilder.begin()
        .in("bar")
        .set("id", 9)
        .set("name", "Mary")
        .jump("foo.teapot")
        .set("id", "10")
        .set("name", "John")
        .createMap();
    rs.insert(map);
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 9 name: Mary; id: 10 name: John", out);
  }

  @Test
  public void test07() throws ParseException {
    String src = """
        id: ~%bar.id%
        name: ~%bar.name%;
        ~%%begin:foo%
          id: ~%teapot.id%
          name: ~%teapot.name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class, t)
        .register(br)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    Map<String, Object> map = MapBuilder.begin()
        .in("bar")
        .set("id", 9)
        .set("name", "Mary")
        .jump("foo.teapot")
        .set("id", "10")
        .set("name", "John")
        .createMap();
    rs.insert(map, "bar.id");
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 9 name: ;", out);
  }

  @Test
  public void test08() throws ParseException {
    String src = """
        id: ~%bar.id%
        name: ~%bar.name%;
        ~%%begin:foo%
          id: ~%teapot.id%
          name: ~%teapot.name%
        ~%%end:foo%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .register(acc, Person.class)
        .register(br, t)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    Map<String, Object> map = MapBuilder.begin()
        .in("bar")
        .set("id", 9)
        .set("name", "Mary")
        .jump("foo.teapot")
        .set("id", "10")
        .set("name", "John")
        .createMap();
    rs.insert(map, "foo", "teapot.id", "teapot.name");
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: name: ; id: 10 name: John", out);
  }

  @Test
  public void test09() throws ParseException {
    AccessorRegistry ar = AccessorRegistry.configure()
        .setDefaultNameMapper(String::toLowerCase)
        .freeze();
    String src = """
        id: ~%ID%
        name: ~%NAME%
        """;
    Template t = Template.fromString(src);
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John", out);
  }

  @Test
  public void test10() throws ParseException {
    String src = """
        id: ~%ID%
        name: ~%NAME%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.configure()
        .setNameMapper(t, String::toLowerCase)
        .freeze();
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John", out);
  }

  @Test
  public void test11() throws ParseException {
    String src = """
        id: ~%ID%
        name: ~%NAME%
        """;
    Template t = Template.fromString(src);
    AccessorRegistry ar = AccessorRegistry.standard(String::toLowerCase);
    RenderSession rs = t.newRenderSession(ar);
    rs.insert(new Person(10, "John"));
    String out = rs.render();
    out = out.replaceAll("\\s+", " ").strip();
    assertEquals("id: 10 name: John", out);
  }

}
