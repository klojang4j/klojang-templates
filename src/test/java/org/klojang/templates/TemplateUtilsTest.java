package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.util.CollectionMethods;
import org.klojang.util.Tuple2;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateUtilsTest {

  private static final String src = """
      <html>
      <head>
      <script>
        var message1 = '~%js:message1%';
      </script>
      </head>
      <body>
        ~%%begin:foo%
          <p>~%message1%</p>
          <p>~%message3%</p>
        ~%%end:foo%
        ~%%begin:bar%
          <p>~%message1%</p>
          ~%%begin:foo%
            <p>~%message2%</p>
            <p>~%message4%</p>
          ~%%end:foo%
        ~%%end:bar%
        <p onclick="alert('~%jsattr:message1%');">Click me</p>
        <p><!--~%message2%-->Hi there<!--%--></p>
        ~%%begin:bozo%
          <p>Hello</p>
        ~%%end:bozo%
      </body>
      </html>
      """;

  @Test
  public void getFQName0() throws ParseException {
    Template root = Template.fromString(src);
    assertEquals("{root}", TemplateUtils.getFQN(root));
  }

  @Test
  public void getFQName01() throws ParseException {
    Template root = Template.fromString(src);
    Template bar = root.getNestedTemplate("bar");
    Template foo = bar.getNestedTemplate("foo");
    assertEquals("bar.foo", TemplateUtils.getFQN(foo));
  }

  @Test
  public void getFQName02() throws ParseException {
    Template root = Template.fromString(src);
    Template bar = root.getNestedTemplate("bar");
    Template foo = bar.getNestedTemplate("foo");
    assertEquals("bar.foo.bozo", TemplateUtils.getFQN(foo, "bozo"));
  }

  @Test
  public void getTemplateHierarchy00() throws ParseException {
    Template root = Template.fromString(src);
    List<Template> l = TemplateUtils.getTemplateHierarchy(root);
    List<String> names = CollectionMethods.freeze(l, Template::getName);
    assertEquals(List.of("{root}", "foo", "bar", "bozo", "foo"), names);
  }

  @Test
  public void getNestedTemplate00() throws ParseException {
    Template root = Template.fromString(src);
    Template bozo = TemplateUtils.getNestedTemplate(root, "bozo");
    assertEquals("bozo", TemplateUtils.getFQN(bozo));
    Template barFoo = TemplateUtils.getNestedTemplate(root, "bar.foo");
    assertEquals("bar.foo", TemplateUtils.getFQN(barFoo));
  }

  @Test
  public void getContainingTemplate00() throws ParseException {
    Template root = Template.fromString(src);
    Template bar = TemplateUtils.getContainingTemplate(root, "bar.message1");
    assertEquals("bar", TemplateUtils.getFQN(bar));
    assertEquals("{root}",
        TemplateUtils.getContainingTemplate(root, "message1").getName());
    bar = TemplateUtils.getContainingTemplate(root, "bar.foo.message4");
    assertEquals("bar", TemplateUtils.getFQN(bar));
  }

  @Test
  public void getVarsPerTemplate00() throws ParseException {
    Template root = Template.fromString(src);
    List<Tuple2<Template, String>> vars = TemplateUtils.getVarsPerTemplate(root);
    List<String> names = CollectionMethods.freeze(vars,
        t -> t.first().getName() + ":" + t.second());
    assertEquals(
        List.of("{root}:message1",
            "{root}:message2",
            "foo:message1",
            "foo:message3",
            "bar:message1",
            "foo:message2",
            "foo:message4"),
        names
    );
  }

  @Test
  public void printParts00() throws ParseException {
    Template root = Template.fromString(src);
    TemplateUtils.printParts(root, System.out);
  }

  @Test
  public void getAllVariableOccurrences() throws ParseException {
    Template root = Template.fromString(src);
    List<VariableOccurrence> l = TemplateUtils.getAllVariableOccurrences(root);
    l.forEach(System.out::println);
    assertEquals("message1", l.get(0).name());
    assertEquals(VarGroup.JS, l.get(0).varGroup().get());
    assertEquals(Optional.empty(), l.get(0).placeholder());
    assertEquals("message2", l.get(7).name());
    assertEquals(Optional.empty(), l.get(7).varGroup());
    assertEquals("Hi there", l.get(7).placeholder().get());
  }

}
