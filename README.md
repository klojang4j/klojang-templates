# Klojang Templates

_Klojang Templates_ is a Java templating API written with two goals in mind:

1. Writing templates should be so simple that there is essentially no learning curve.
2. Provide a rich en flexible API for populating the templates that more than
   compensates for their simplicity.

In short: leverage the skills of Java programmers, rather than make them learn what,
in effect, amounts to a whole new language. This solidly places _Klojang Templates_
in the _Zero Logic Templates_ camp of templating approaches, alongside, for example,
[Mustache](http://mustache.github.io/) and on opposite sides of a template engine
like [Thymeleaf](https://www.thymeleaf.org/).

Klojang templates arguably are even simpler than Mustache templates. There are just
five syntactical constructs. Three if you discount for the fact that two of them are
comments-like constructs. Two if you consider that of those three, two are
functionally equivalent (the API user cannot tell whether one construct was used or
the other). Nevertheless, populating a template can be done very efficiently and
elegantly due to a rather unique feature: nested templates.

One appealing feature of Thymeleaf is that the raw, unprocessed templates render just
as well within a browser as the output generated by the Thymeleaf engine. This allows
the static HTML mockups produced by designers to evolve into fully dynamic pages
while at no point entering an "unrenderable" phase. If your company has the financial
and human resources to care about this, _Klojang Templates_ also allows you to create
templates that will render just fine in their raw state.

# Getting Started

To get started with _Klojang Templates_, add the following dependency to you project:

**Maven**:

```xml
<dependency>
    <groupId>org.klojang</groupId>
    <artifactId>klojang-templates</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle**:

```
implementation group: 'org.klojang', name: 'klojang-templates', version: '1.0.0'
```

The **javadocs** for _Klojang Templates_ can be
found [https://klojang4j.github.io/klojang-templates/1/api](https://klojang4j.github.io/klojang-templates/1/api).

## Hello World

```html
<!-- hello.html -->
<html>
<body>
<p>~%greeting%</p>
</body>
</html>
```

```java
import org.klojang.templates.ParseException;
import org.klojang.templates.RenderSession;
import org.klojang.templates.Template;

public class HelloWorld {

  @GET
  @Path("/hello")
  public String hello() throws ParseException {
    Template template = Template.fromResource(getClass(), "/views/hello.html");
    RenderSession session = template.newRenderSession();
    return session.set("greeting", "Hello World").render();
  }

}
```

## Escaping

_Klojang Templates_ is, in fact, not tied to any particular output format. However,
it does provide some extra help in case you are working with HTML templates. This
happens through so-called _variable groups_, which will be covered in greater detail
later on.

```html
<html>
<head>
    <script>
        const currentGreeting = '~%js:greeting%';
    </script>
</head>
<body>
<p>~%html:greeting%</p>
</body>
</html>
```

By default, _Klojang Templates_ does not apply any escaping or formatting to the
values you insert into the template, but you can configure _Klojang Templates_ to
HTML-escape all values by default. You can then omit the `html:` prefix while 
keeping the `js:` prefix to override the default behaviour.

## Inserting POJOs, Records and Maps

You can set multiple template variables at once by "inserting" non-scalar values into
the template.

```html
<!-- employee.html -->
<html>
<body>
<table>
    <tr>
        <td>First name:</td>
        <td>~%firstName%</td>
    </tr>
    <tr>
        <td>Last name:</td>
        <td>~%lastName%</td>
    </tr>
    <tr>
        <td>Birthdate:</td>
        <td>~%birthDate%</td>
    </tr>
</table>
</body>
</html>
```

```java
import java.time.LocalDate;

public record Employee(String firstName, String lastName, LocalDate birtDate) {

}
```

```java
public class EmployeeResource {

  @GET
  @Path("/john")
  public StreamingOutput example() throws ParseException {
    Employee employee = new Employee("John", "Smith", LocalDate.of(1980, 6, 13));
    Template template = Template.fromResource(getClass(), "/views/employee.html");
    RenderSession session = template.newRenderSession();
    session.insert(employee);
    return session::render;
  }

}
```

