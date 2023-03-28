# Klojang Templates

_Klojang Templates_ is a Java templating API written with two goals in mind:

1. Writing templates should be so simple that there is essentially no learning curve.
2. Provide a rich en flexible API for populating the templates that more than
   compensates for their simplicity.

In short: leverage the skills of Java programmers, rather than make them learn a
whole new skill. This solidly places _Klojang Templates_ in the _Zero Logic 
Templates_ camp of templating approaches, alongside, for example,
[Mustache](http://mustache.github.io/) &#8212; and on opposite sides of a template 
engine like [Thymeleaf](https://www.thymeleaf.org/).

Nevertheless, one appealing feature of Thymeleaf is that raw, unprocessed Thymeleaf
templates render perfectly well within a browser. This allows the static HTML
produced by UI designers to gradually evolve into fully dynamic pages while at no
point entering an "unrenderable" phase. While this was not the prime objective for
_Klojang Templates_, it _does_ let you create templates that render just fine in
their raw state.

Klojang templates arguably are even simpler than Mustache templates. There are just
five syntactical constructs. Three if you discount for the fact that two of them are
comments-like constructs. Two if you consider that of those three, two are
functionally equivalent. Nevertheless, we think (and hope) that you'll find 
populating a Klojang template surprisingly efficient.

The **javadocs** for _Klojang Templates_ can be found **[here](https://klojang4j.github.io/klojang-templates/1/api)**.
The latest **vulnerabilities report** can be found **[here](https://klojang4j.github.io/klojang-templates/1/vulnerabilities/dependency-check-report.html)**.
The latest **code coverage reports** can be found **[here](https://klojang4j. github.io/klojang-templates/1/coverage)**.

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

## Hello, World

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
    return session.set("greeting", "Hello, World").render();
  }

}
```

## Escaping

_Klojang Templates_ is not tied to any particular output format. However, it does
provide some extra help in case you are writing HTML templates.

```html
<!-- hello.html -->
<html>
<head>
<script>
  const greeting = '~%js:greeting%';
</script>
</head>
<body>
<p>~%html:greeting%</p>
</body>
</html>
```

The `html:` and `js:` prefixes are examples of 
[variable groups](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/VarGroup.html),
which will be covered in greater detail later on.

By default, _Klojang Templates_ does not apply any escaping or formatting to the
values you insert into a template. However, you can [configure](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/StringifierRegistry.Builder.html#setDefaultStringifier(org.klojang.templates.Stringifier)) _Klojang Templates_ to
HTML-escape all values by default. You could then omit the `html:` prefix while 
keeping the `js:` prefix to override the default behaviour.

## Inserting Java Beans, Records and Maps

You can set multiple template variables at once by inserting non-scalar values into
the template.

```html
<!-- employee.html -->
<html>
<body>
<p>First name: ~%firstName%</p>
<p>Last name: ~%lastName%</p>
<p>Birthdate: ~%birthDate%</p>
</body>
</html>
```

```java
public class EmployeeResource {

   @GET
   @Path("/john")
   public StreamingOutput john() throws ParseException {
      Employee employee = new Employee("John", "Smith", LocalDate.of(1980, 6, 13));
      Template template = Template.fromResource(getClass(), "/views/employee.html");
      RenderSession session = template.newRenderSession();
      session.insert(employee);
      return session::render;
   }

}
```

Note that the `john()` method now returns a method reference to [RenderSession.render(OutputStream)](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.emplates/org/klojang/templates/RenderSession.html#render(java.io.OutputStream)), 
which neatly targets the JAX-RS [StreamingOutput](https://docs.oracle.com/javaee/7/api/javax/ws/rs/core/StreamingOutput.html)
interface.

## Nested Templates

In _Klojang Templates_ templates can be nested inside other templates (ad infinitum
if you like). Why you would want that is explained in the next chapter. This chapter 
details the syntax for nested templates.

There are, in fact, two ways to nest one template inside another: via 
"inline templates" or via "included templates". Functionally 
they are completely equivalent. The API cannot tell you whether you are 
populating an inline template or an included template. (Well, actually, it [sort of can](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/Template.html#path()),
but there's not much you can do with this knowledge.)

### Inline Templates

Inline templates are defined within the parent template itself. Here is an example of
a template which contains an inline template ("companies"), which itself contains an
inline template ("departments"), which also contains an inline template ("employees"). 
For clarity's sake, this is a non-HTML template.

_/views/company-overview.txt:_
```
This is an overview of our customers:
~%%begin:companies%
    Name .......: ~%name%
    Profits ....: ~%profits%
    Departments:
    ~%%begin:departments%
        Name .......: ~%name%
        Manager ....: ~%manager%
        Employees:
            ~%%begin:employees%
                ~%firstName% ~%lastName% (~%birthDate%)                             
            ~%%end:employees%                   
   ~%%end:departments%  
~%%end:companies%
Not bad, ey!
```

### Included Templates
Included templates are defined in a separate file and are nested inside another 
template using the following syntax:

_/views/company-overview.txt:_
```
This is an overview of our customers:
~%%begin:companies%
    Name .......: ~%name%
    Profits ....: ~%profits%
    Departments:
        ~%%include:/views/departments.txt%%
~%%end:companies%
Not bad, ey!
```

_/views/departments.txt:_
```
        Name .......: ~%name%
        Manager ....: ~%manager%
        Employees:
            ~%%begin:employees%
                ~%firstName% ~%lastName% (~%birthDate%)                             
            ~%%end:employees%                   
```

Note that inline template tags end with a single percentage sign (%) while included 
template tags end with a double percentage sign (%%).

### Template Names

Nested templates, whether inline or included, are identified by their name &#8212;
"companies", "departments" and "employees" in the examples above. For included
templates, the name by default is the basename of the included file. However, you can
also use the following syntax:

```
~%%include:employees:/views/employees-2023-01-01.txt%%
```

The template that you explicitly instantiate using [Template.fromResource()](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/Template.html#fromResource(java.lang.Class,java.lang.String))
(and the other `fromXXX` methods) is called the "root" or "main" template. When 
inserting non-scalar values (e.g. hash maps) directly into the root template, you 
use the [insert()](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/RenderSession.html#insert(java.lang.Object,java.lang.String...))
method of the [RenderSession](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/RenderSession.html) class.
When inserting them into a nested template, you use the [populate()](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/RenderSession.html#populate(java.lang.String,java.lang.Object,java.lang.String...))
method. The `insert()` method does not require you to specify the name of the 
template you want to populate. Yet, for reporting purposes, the root template 
still also has a name. It is always **{root}**.

## Using Nested Templates

Nested templates add an extra dimension to a Klojang template ("depth"). As a
consequence, a Klojang template almost literally becomes a mold into which you can
sink objects with the same shape. If the structure of the template reflects the
structure of the model object, you can fill up the entire template with a single
call.

```java
record Employee(int id, String firstName, String lastName, Address address) {}

record Address(String line1, String zipCode, String city, State state) {}
```

```html
<!-- label.html -->
<html><body>
<p>~%firstName% ~%lastName%</p>
~%%begin:address%
<p>~%line1%</p>
<p>~%city%, ~%state%, ~%zipCode%</p>
~%%end:address%
</body></html>
```

```java
@Path("/print")
public class LabelPrintResource {

   EmployeeDao dao;

   @Path("/{id}")
   public StreamingOutput printLabel(@PathParam("id") int id) throws ParseException {
      Employee employee = dao.find(id);
      Template template = Template.fromResource(getClass(), "/views/label.html");
      RenderSession session = template.newRenderSession();
      session.insert(employee);
      return session::render;
   }

}
```

Note how the address template gets mapped to the address property of `Employee`, 
and how, _inside_ the address template, you have direct access to the `Address` 
properties.

### Tables

Nested templates enable you to create tables and other repetitive structures.

```html
<!-- home.html -->
<html>
<body>
<table>
   <thead>
   <tr>
      <th>First name</th><th>Last name</th><th>Birth date</th>
   </tr>
   </thead>
   <tbody>
   ~%%begin:employees%
   <tr><td>~%firstName%</td><td>~%lastName%</td><td>~%birthDate%</td></tr>
   ~%%end:employees%
   </tbody>
</table>
</body>
</html>
```

```java
import org.klojang.templates.ParseException;
import org.klojang.templates.RenderSession;
import org.klojang.templates.Template;

public class HomeResource {
  
  private EmployeeDao dao;

  @GET
  @Path("/")
   public String list() throws ParseException {
     Template template = Template.fromResource(getClass(), "/views/home.html");
     List<Employee> employees = dao.list();
     return template.newRenderSession()
             .populate("employees", employees)
             .render();
  }

}
```

If the second argument to `RenderSession.populate()` is an array or collection, the
nested template automatically turns into a _repeating template_, repeating itself for
each element in the array or collection.

### Complex Structures

The templates and the objects to populate them with can be arbitrarily complex.

Take, for example, the company-overview template again:

```
This is an overview of our customers:
~%%begin:companies%
    Name .......: ~%name%
    Profits ....: ~%profits%
    Departments:
    ~%%begin:departments%
        Name .......: ~%name%
        Manager ....: ~%manager%
        Employees:
            ~%%begin:employees%
                ~%firstName% ~%lastName% (~%birthDate%)                             
            ~%%end:employees%                   
   ~%%end:departments%  
~%%end:companies%
Not bad, ey!
```

The corresponding model classes might look like this:

```java
record Company(String name, BigDecimal profits, List<Department> departments) {}

record Department(String name, String manager, List<Employee> employees) {}

record Employee(String firstName, String lastName, LocalDate birthDate) {}
```

Then, when inserting a list of Company instances into the template, the 
employees template would repeat within the departments template, which would 
repeat within the companies template, which would again repeat within the 
company-overview template. All this would happen with a single call to 
[RenderSession.populate()](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/RenderSession.html#populate(java.lang.String,java.lang.Object,java.lang.String...)),
simply because the structure of the template reflects the structure of the data 
model.

Note that this does not mean that the _visual layout_ of the template must somehow
reflect the structure of the data model.
The [label template](#using-nested-templates) shown above reflects the structure of
the `Employee` class, but visually it actually flattens the relationship between
`Employee` and `Address`.

### Conditional Rendering

Conditional rendering &#8212; that is, rendering a block of text within a template
only if a certain condition is met, is an unremarkable affair in _Klojang Templates_.
By default, neither template variables nor nested templates are rendered in the first
place. If you don't set a variable to a value, it will simply disappear from the
template. If you don't populate a nested template, the entire block of text it
encloses will disappear from the template. Thus, if you don't want something to be
rendered, just "don't mention its name".

However, you can make it more explicit that you don't want a block of text to be
rendered:

```java
public class HomeResource {

   @GET
   @Path("/no-data")
   public String list() throws ParseException {
      Template template = Template.fromResource(getClass(), "/views/home.html");
      return template.newRenderSession()
              .populate("employees", Collections.emptyList())
              .render();
   }

}
```

If you populate a nested template with an empty array or collection, the template is
going to be repeated zero times. In other words, you prevent it from being rendered.

## Stringifiers and Variable Groups

What gets inserted into a template ultimately needs to be a `String`. However, 

