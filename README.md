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
The latest **code coverage reports** can be found **[here](https://klojang4j.github.io/klojang-templates/1/coverage)**.

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

Note that the `john()` method returns a method reference to [RenderSession.render(OutputStream)](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.emplates/org/klojang/templates/RenderSession.html#render(java.io.OutputStream)), 
which neatly targets the JAX-RS [StreamingOutput](https://docs.oracle.com/javaee/7/api/javax/ws/rs/core/StreamingOutput.html)
interface.

## Nested Templates

In _Klojang Templates_ templates can be nested inside other templates (ad infinitum
if you like). Syntactically, this can be done in two ways: via
"inline templates" or via "included templates". Functionally, there if no 
difference between these two options. The API cannot tell you whether you are populating an inline
template or an included template. (Well, actually, it [sort of can](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/Template.html#path()),
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
```

Are these nested structures _really_ templates in their own right? Yes! You could 
even render them separately (e.g. for debugging purposes):

```java
public class CompanyResource {

   @GET
   @Path("/overview")
   @Produces("text/plain")
   public StreamingOutput john() throws ParseException {
      Template template = Template.fromResource(getClass(), "/views/company-overview.txt");
      RenderSession session = template.newRenderSession();
      // Will *only* render the employees template:
      String out = session.in("companies").in("departments").in("employees")
              .set("firstName", "John")
              .set("lastName", "Smith")
              .set("birthDate", LocalDate.of(1980, 6, 13))
              .render();
      LOG.debug(out);
      // more stuff ...
      return session::render;
   }

}
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

Nested templates, whether inline or included, are identified by their name &#8212;
"companies", "departments" and "employees" in the examples above. For included
templates, the name by default is the basename of the included file. However, you can
also use the following syntax:

```
~%%include:employees:/views/employees-2023-01-01.txt%%
```

## Using Nested Templates

The nested-template feature almost literally turns a Klojang template into a mold
into which you can sink objects with the same shape. If the structure of the template
reflects the structure of the model object, you can fill up the entire template with
a single call. The following code snippets illustrate this.

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

Notice how the address template gets mapped to the address property of `Employee`,
and how, _inside_ the address template, you have access to the `Address` 
properties. In fact, inside the address template you are in a "different 
universe" and you can **only** access the `Address` properties.

### Tables

Nested templates enable you to create tables and other repetitive structures.

```html
<!-- employees.html -->
<html>
<body>
<table>
    <thead>
        <tr><th>First name</th><th>Last name</th><th>Birth date</th></tr>
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

public class EmployeeResource {
  
  private EmployeeDao dao;

  @GET
  @Path("/")
   public String list() throws ParseException {
     Template template = Template.fromResource(getClass(), "/views/employees.html");
     List<Employee> employees = dao.list();
     return template.newRenderSession().populate("employees", employees).render();
  }

}
```

If the second argument to `RenderSession.populate()` is an array or collection, the
nested template automatically turns into a _repeating template_, repeating itself for
each element in the array or collection.

#### Newline Suppression

As an aside: when rendering a template, its structure is left completely intact.
Variables are replaced by their values and nested templates are replaced by their
contents. There is one exception: if the begin tag or end tag of an inline template
is on a separate line, that entire line will be removed from the output. Thus, the
above template would render somewhat like this:

```html
<html>
<body>
<table>
    <thead>
        <tr><th>First name</th><th>Last name</th><th>Birth date</th></tr>
    </thead>
    <tbody>
        <tr><td>John</td><td>Smith</td><td>1980-06-13</td></tr>
        <tr><td>Mary</td><td>Bear</td><td>1977-11-10</td></tr>
        <tr><td>Tracey</td><td>Peterson</td><td>2001-04-03</td></tr>
   </tbody>
</table>
</body>
</html>
```

_(If this fails to make you spill your coffee, keep in mind that the text 
enclosed by `~%%begin:employees%` and `~%%end:employees%` contains **two** 
newlines, which would ordinarily be faithfully reproduced.)_

### Complex Information

Nested templates really start to shine as the information you need to convey 
becomes more complex. Take, for example, the company-overview template again:

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
```

The corresponding model classes would probably look somewhat like this:

```java
record Company(String name, BigDecimal profits, List<Department> departments) {}

record Department(String name, String manager, List<Employee> employees) {}

record Employee(String firstName, String lastName, LocalDate birthDate) {}
```

Then, when inserting a list of Company instances into the template, the 
employees template would repeat within the departments template. which would 
repeat within the companies template, which would repeat within the 
company-overview template. All this would happen with a single call to 
[RenderSession.populate()](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/RenderSession.html#populate(java.lang.String,java.lang.Object,java.lang.String...)),
simply because the structure of the template matches the structure of the data 
model.

Note that this does not mean that the _visual_ appearance of the template must
somehow reflect the structure of the data model.
The [label template](#using-nested-templates) shown above reflects the structure of
the Employee class, but visually it actually _flattens_ the relationship between
`Employee` and `Address`.

### Conditional Rendering

Conditional rendering &#8212; that is, rendering a block of text within a template
only if a certain condition is met, is also done by means of nested templates.

The first thing to note here is that, by default, neither template variables nor
nested templates are rendered in the first place. If you don't set a variable to a
value, it will simply disappear from the template. If you don't populate a nested
template, the entire block of text it encloses will disappear from the template.
Thus, if you don't want something to be rendered, just
"don't mention its name" in the `RenderSession`.

However, you can make it more explicit that you don't want a block of text to be
rendered. If you populate a nested template with an empty array or collection, the
template is going to be repeated zero times. In other words, you prevent it from
being rendered.

## Stringifiers and Variable Groups

The chapter on [escaping](#escaping), illustrated how the`html:` and `js:` prefixes
could be used for HTML-escaping and ECMAScript-escaping, respectively. These prefixes
are, in fact, the names of two predefined 
[variable groups](https://klojang4j.github.io/klojang-templates/1/api/org.klojang.templates/org/klojang/templates/VarGroup.html).
There are a couple more of them, and you can also define your own variable groups.
For example, you might want to define a variable group for formatting date/time 
objects according to your locale.

```html
<!-- employee.html -->
<html>
<body>
<p>First name: ~%firstName%</p>
<p>Last name: ~%lastName%</p>
<p>Birthdate: ~%date-format1:birthDate%</p>
</body>
</html>
```

```java
import org.klojang.templates.Stringifier;
import org.klojang.templates.StringifierRegistry;

import java.time.LocalDate;

public class Setup {

   private static final StringifierRegistry stringifiers = configureStringifiers();

   public static StringifierRegistry getStringifiers() {
      return stringifiers;
   }

   private static StringifierRegistry configureStringifiers() {
      return StringifierRegistry.configure()
              .forVarGroup("date-format1", getDateTimeStringifier())
              .freeze();
   }

   private static Stringifier getDateTimeStringifier() {
     return obj -> obj == null 
            ? "&nbsp;" 
            : DateTimeFormatter.ofPattern("yyyy年mm月dd日") .format((LocalDate) obj);
    }

}
```

```java
public class EmployeeResource {

   @GET
   @Path("/john")
   public StreamingOutput john() throws ParseException {
      Employee employee = new Employee("John", "Smith", LocalDate.of(1980, 6, 13));
      Template template = Template.fromResource(getClass(), "/views/employee.html");
      RenderSession session = template.newRenderSession(Setup.getStringifiers());
      session.insert(employee);
      return session::render;
   }

}
```

## Evolving the Raw Template

_Klojang Templates_ allows you to write templates that will render just fine in their
raw, unprocessed state. This paragraph explains how you can achieve this.

Suppose your company's design team handed you this design:

```html
<!DOCTYPE html>
<html>
<body style="background-color: pink">
<table>
   <thead>
   <tr>
      <th>First name</th>
      <th>Last name</th>
   </tr>
   </thead>
   <tbody>
   <tr>
      <td>John</td>
      <td>Smith</td>
   </tr>
   </tbody>
</table>
</body>
</html>
```

The company fired the design team, but you plough on.

The first thing to keep in mind is that template variables can be placed in HTML
comments without this making a difference in how the template is rendered.
`<!-- ~%foo% -->` is rendered just like `~%foo%`. _Klojang Templates will replace the
_entire_ character sequence with whatever value `foo` was set to. You may
write `<!--~%foo%-->` (without the space characters), but that is about as much
syntactical freedom as you have.

So your first attempt at turning the design in a dynamically populated pages 
might look like this:

```html
<!DOCTYPE html>
<html>
<body style="background-color: pink">
<table>
   <thead>
   <tr>
      <th>First name</th>
      <th>Last name</th>
   </tr>
   </thead>
   <tbody>
   <tr>
      <td><!-- ~%firstName% --></td>
      <td><!-- ~%lastName% --></td>
   </tr>
   </tbody>
</table>
</body>
</html>
```

### Placeholders

That's nice. The user won't see ugly `~%` sequences when viewing the raw template in
a browser. Unfortunately, the example row containing "John Smith" now has effectively
disappeared from view.

This can be remedied by using _placeholders_. A placeholder is a value that is placed
inside a pairs of `<!--%-->` sequences. Since `<!--%-->` is a self-closed HTML
comment, any text inside a pair of these sequences will be visible in the browser.
However, when _Klojang Templates_ renders the template, it will remove both
the `<!--%-->` sequences and any text inside it.

```html
<td>
   <!-- ~%firstName% -->
   <!--%-->John<!--%-->
</td>
<td>
   <!-- ~%lastName% -->
   <!--%-->Smith<!--%-->
</td>
```

If you remove all HTML comments from the above HTML snippet, you are basically 
back to the original design.

If the placeholder fits into a single line, this can be contracted to:

```html
<td><!-- ~%firstName% -->John<!--%--></td>
<td><!-- ~%lastName% -->Smith<!--%--></td>
```

### Populating the Table

Now we want to introduce a nested template so we can make the table row repeat 
for each element of the `List<Employee>` we received from the data access layer.

As with template variables, you can place the begin and end tag of an inline 
template in HTML comments. _Klojang Templates_ treats `<!-- ~%%begin:foo% -->` 
just like it treats `~%%begin:foo%`

```html
<!DOCTYPE html>
<html>
<body style="background-color: pink">
<table>
   <thead>
   <tr>
      <th>First name</th>
      <th>Last name</th>
   </tr>
   </thead>
   <tbody>
   <!-- ~%%begin:employees% -->
   <tr>
      <td><!-- ~%firstName% -->John<!--%--></td>
      <td><!-- ~%lastName% -->Smith<!--%--></td>
   </tr>
   <!-- ~%%end:employees% -->
   </tbody>
</table>
</body>
</html>
```

This will still render perfectly well in a browser. Again, when you remove all HTML
comments, you are back to where you started. Yet now the page has become fully
dynamic.

### Ditch Blocks

It may have occurred to you that this won't work for included templates:

```html
   <tbody>
       <!-- ~%%include:employee:/views/employee-row.html%% -->
   </tbody>
```

Now you just have an empty table body in the raw template. Nevertheless, this, 
_too_, will be rendered by _Klojang Templates_ just like 
`~%%include:employee:/views/employee-row.html%%` (without HTML comments).

```html
<!DOCTYPE html>
<html>
<body style="background-color: pink">
<table>
   <thead>
   <tr>
      <th>First name</th>
      <th>Last name</th>
   </tr>
   </thead>
   <tbody>
   <!--%%-->
   <tr>
      <td>John</td>
      <td>Smith</td>
   </tr>
   <!--%%-->
   <!-- ~%%include:employee:/views/employee-row.html%% -->
   </tbody>
</table>
</body>
</html>
```


```html
   <tbody>
   <!--%%-->
   <tr>
      <td>John</td>
      <td>Smith</td>
   </tr>
   <!--%%-->
   
   <!-- ~%%begin:employees%
   <tr>
      <td>~%firstName%</td>
      <td>~%lastName%</td>
   </tr>
   ~%%end:employees% -->
   </tbody>
```


## About

<img src="docs/logo-groen.png" style="float:left;width:5%;padding:0 12px 12px 0"/>

Klojang Check is developed by [Naturalis](https://www.naturalis.nl/en), a
biodiversity research institute and natural history museum. It maintains one
of the largest collections of zoological and botanical specimens in the world.





