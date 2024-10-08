# Klojang Templates

_Klojang Templates_ is a Java templating API written with two goals in mind:

1. Writing templates should be so simple that there is essentially no learning curve.
2. The API used to populate the templates, by contrast, should be rich and flexible, and
   more than make up for the simplicity of the templates.

In short: leverage the skills of Java programmers, rather than make them learn a whole new
skill.

This solidly places _Klojang Templates_ in the _Zero-Logic Templates_ camp of templating
approaches, alongside, for example,
[Mustache](http://mustache.github.io/) &#8212; and on opposite sides of a template engine
like [Thymeleaf](https://www.thymeleaf.org/).

Yet, one appealing feature of Thymeleaf is that raw, unprocessed Thymeleaf templates
render flawlessly within a browser. This allows the static HTML produced by UI designers
to gradually evolve into fully dynamic pages while at no point entering an "unrenderable"
phase. While this was not the primary motivation for developing
_Klojang Templates_, it does let you write templates that are well-formed and valid in
their raw state. (See
[Evolving the Raw Template](#evolving-the-raw-template).)

Klojang templates arguably are even simpler than Mustache templates. There are just five
syntactical constructs. Three if you discount for the fact that two of them are
comments-like constructs. Two if you consider that, of the remaining three, two are
functionally equivalent. However, the finer details of the syntax are carefully calibrated
to give the Java API maximum leverage. In particular, _Klojang Templates_ allows you to
[nest templates](#nested-templates) inside other templates. This makes the API very
efficient and concise when it comes to populating the templates.

## Other Documentation

- The **javadocs** for _Klojang Templates_ can be
  found [here](https://klojang4j.github.io/klojang-templates/api).
- The latest **vulnerabilities report** can be
  found [here](https://klojang4j.github.io/klojang-templates/vulnerabilities/dependency-check-report.html).
- The latest **code coverage reports** can be
  found [here](https://klojang4j.github.io/klojang-templates/coverage).

## Getting Started

To get started with _Klojang Templates_, add the following dependency to you project:

**Maven**:

```xml
<dependency>
    <groupId>org.klojang</groupId>
    <artifactId>klojang-templates</artifactId>
    <version>1.1.0</version>
</dependency>
```

**Gradle**:

```
implementation group: 'org.klojang', name: 'klojang-templates', version: '1.1.0'
```

_Klojang Templates_ is agnostic about the web or application framework you use. It does
not hook into any of them in any deep way. You can use _Klojang Templates_
with any of the Jakarta/JAX-RS based frameworks, but equally well with non-Servlet based
frameworks like [Micronaut](https://micronaut.io/).

_Klojang Templates_ uses [SLF4J](https://www.slf4j.org/) to log messages about the
template parsing and rendering process. All message are logged at TRACE level.

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

_Klojang Templates_ is not tied to any particular output format. There is no reason why
you couldn't use _Klojang Templates_ to write SQL or JSON templates, for example. However,
it does provide some extra help in case you are writing HTML templates.

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
[variable groups](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/VarGroup.html),
which will be covered in greater detail later on.

By default, _Klojang Templates_ does not apply any escaping or formatting to the values
you insert into a template. However, you
can [configure](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/StringifierRegistry.Builder.html#setDefaultStringifier(org.klojang.templates.Stringifier))
_Klojang Templates_ to HTML-escape all values by default. You could then omit the `html:`
prefix while keeping the `js:` prefix to override the default behaviour.

## Inserting Beans, Records and Maps

You can set multiple template variables at once by inserting non-scalar values into the
template.

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

The resource method now returns a method reference to
[RenderSession.render(OutputStream)](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/RenderSession.html#render(java.io.OutputStream))),
which neatly targets the JAX-RS
[StreamingOutput](https://docs.oracle.com/javaee/7/api/javax/ws/rs/core/StreamingOutput.html)
interface.

## Nested Templates

In _Klojang Templates_ templates can be nested inside other templates (ad infinitum if you
like). Syntactically, this can be done in two ways: via
_inline templates_ or via _included templates_. Functionally, there is no difference
between the two options. They are populated using the exact same methods.

### Inline Templates

Inline templates are defined within the parent template itself. Here is an example of a
template which contains an inline template ("companies"), which itself contains an inline
template ("departments"), which also contains an inline template
("employees"). For clarity's sake, this is a non-HTML template.

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

Are these nested structures _really_ templates in their own right? Yes! You could even
render them separately (e.g. for debugging purposes):

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
    // And render the main, top-level template
    return session::render;
  }

}
```

### Included Templates

Included templates are defined in a separate file and are nested inside another template
using the following syntax:

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
"companies", "departments" and "employees" in the examples above. For included templates,
the name by default is the basename of the included file. However, you can also use the
following syntax:

```
~%%include:employees:/views/employees-2023-01-01.txt%%
```

#### Indentation and Newline Suppression

Ordinarily, when rendering a template, its structure is left completely intact. For inline
templates the begin tag (e.g. `~%%begin:foo%`) is removed and the text following it is
brought that much closer to the text preceding it, as though the begin tag had not been
there. The same applies to the end tag of an inline template. For included templates the
entire tag (e.g. `~%%include:foo.html%%`) is replaced with the contents of the included
file.

However, if the begin or end tag of an inline template is all by itself on a separate
line, as in the example above, then that _entire_ line will be removed from the output. If
an included template tag is all by itself on a separate line, the line is preserved, but
any whitespace on it is removed.

It may sound contrived, but it actually allows you to write elegant templates with
indentation that stays in place upon rendering. This is illustrated in
[Newline Suppression in Practice](#newline-suppression-in-practice)

## Using Nested Templates

The ability to nest templates inside other templates almost literally turns a Klojang
template into a mold into which you can sink objects with the same shape. If the structure
of the template reflects the structure of the model object, you can fill up the entire
template with a single call. The following code snippets illustrate this.

_The Model:_

```java
record Employee(int id, String firstName, String lastName, Address address) { }

record Address(String line1, String zipCode, String city, State state) { }
```

_The View:_

```html
<!-- label.html -->
<html>
<body>

<p>~%firstName% ~%lastName%</p>

~%%begin:address%
<p>~%line1%</p>
<p>~%city%, ~%state%, ~%zipCode%</p>
~%%end:address%

</body>
</html>
```

_The Controller:_

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

Looking at the template file (label.html), notice how the address template gets mapped to
the address property of `Employee`, and how, inside the address template, you have access
to the `Address` properties. In fact, inside the address template you are in a "different
universe" and you can _only_ access `Address` properties.

### Tables

Nested templates enable you to create tables and other repetitive structures.

```html
<!-- employees.html -->
<html>
<body>
<table>
    <thead>
    <tr>
        <th>First name</th>
        <th>Last name</th>
        <th>Birth date</th>
    </tr>
    </thead>
    <tbody>
    ~%%begin:employees%
    <tr>
        <td>~%firstName%</td>
        <td>~%lastName%</td>
        <td>~%birthDate%</td>
    </tr>
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
    return template.newRenderSession()
          .populate("employees", employees)
          .render();
  }

}
```

This time we use the
[populate()](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/RenderSession.html#populate(java.lang.String,java.lang.Object,java.lang.String...))
method of the `RenderSession` class, rather than the
[insert()](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/RenderSession.html#insert(java.lang.Object,java.lang.String...))
method. `populate()` is used to populate nested templates while `insert()` is used to
populate the main, top-level  template. If the second argument to `populate()` is an array
or collection, the nested template automatically turns into a **repeating template**, 
repeating itself for each element in the array or collection.

#### Newline Suppression in Practice

Tables especially benefit from
[newline suppression](#indentation-and-newline-suppression) as described above. The
template above would render somewhat like this:

```html

<html>
<body>
<table>
    <thead>
    <tr>
        <th>First name</th>
        <th>Last name</th>
        <th>Birth date</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>John</td>
        <td>Smith</td>
        <td>1980-06-13</td>
    </tr>
    <tr>
        <td>Mary</td>
        <td>Bear</td>
        <td>1977-11-10</td>
    </tr>
    <tr>
        <td>Tracey</td>
        <td>Peterson</td>
        <td>2001-04-03</td>
    </tr>
    </tbody>
</table>
</body>
</html>
```

If this fails to make you spill your coffee, notice that there are **two**
newline characters inside the employees template (one after `~%%begin:employees%` and one
after `</tr>`). Yet when rendered, the table rows stay tightly packed together.

#### Separators

<i>Klojang Templates</i> allows you to specify a separator to be inserted between the
instances of a repeating template. This can be especially useful for non-HTML templates:

```java
List<Employee> employees= ...; // got it from somewhere
String src="~%%begin:employee%~%firstName% ~%lastName%~%%end:employee%";
Template template=Template.fromString(src);
RenderSession session=template.newRenderSession();
session.populate("employee",employees,", "); // use comma-space as separator
```

### Complex Information

Nested templates really start to shine as the information you need to convey becomes more
complex. Take, for example, the company-overview template again:

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
record Company(String name, BigDecimal profits, List<Department> departments) { }

record Department(String name, String manager, List<Employee> employees) { }

record Employee(String firstName, String lastName, LocalDate birthDate) { }
```

Then, when inserting a `List<Company>` into the template, the employees template would
repeat within the departments template, which would repeat within the companies template,
which would repeat within the company-overview template. All this would happen with a
single call to
[RenderSession.populate()](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/RenderSession.html#populate(java.lang.String,java.lang.Object,java.lang.String...)),
because the structure of the template matches the structure of the data model.

### Conditional Rendering

Conditional rendering &#8212; that is, rendering a block of text within a template only if
a certain condition is met &#8212; is also done by means of nested templates.

The first thing to note here is that, by default, neither template variables nor nested
templates are rendered in the first place. If you don't set a variable to a value, it will
simply disappear from the template. If you don't populate a nested template, the entire
block of text it encloses will disappear from the template. Thus, if you don't want
something to be rendered, just "don't mention its name"
in the `RenderSession`.

However, you can make it more explicit that you don't want a block of text to be rendered.
If you populate a nested template with an empty array or collection, the template is going
to be repeated zero times. In other words, you prevent it from being rendered.

#### Optionals

`Optional` objects containing some data model object are typically returned by the
find-by-id method of data access objects. With _Klojang Templates_ it is perfectly valid
to populate a nested template with an `Optional`. If the `Optional` is empty, the template
will not be rendered. Otherwise it will be populated and rendered using the data model
object.

## Stringifiers and Variable Groups

The chapter on [escaping](#escaping), illustrated how the`html:` and `js:` prefixes could
be used for HTML-escaping and ECMAScript-escaping, respectively. These prefixes are, in
fact, the names of two predefined
[variable groups](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/VarGroup.html).
There are a couple more of them, and you can also define your own variable groups. For
example, you might want to define a variable group for formatting date/time objects.

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

// Used for and during application startup
public class Setup {

  private static final StringifierRegistry stringifiers = configureStringifiers();

  public static StringifierRegistry getStringifiers() { return stringifiers; }

  private static StringifierRegistry configureStringifiers() {
    return StringifierRegistry.configure()
          .forVarGroup("date-format1", getDateStringifier())
          .freeze();
  }

  private static Stringifier getDateStringifier() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy年mm月dd日");
    return obj -> obj == null ? "&nbsp;" : dtf.format((LocalDate) obj);
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

## Accessors and Name Mappers

When you `set` a template variable to some value, obviously it is you who provides the
value. But when you `insert` a hash map or JavaBean into a template, who or what is
responsible for extracting the values inside the hash map or JavaBean? This is done by
[accessors](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/Accessor.html).

While you are quite likely to want to write some custom
[stringifiers](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/Stringifier.html)
for your application, you may not ever need to implement an `Accessor` yourself. _Klojang
Templates_ internally uses a single `Accessor` implementation that can handle most object
types. However, should you need or want to, you can provide your own `Accessor`
implementations.

```java
import org.klojang.templates.Accessor;
import org.klojang.templates.AccessorRegistry;

// Used for and during application startup
public class Setup {

  private static final AccessorRegistry accessors = configureAccessors();

  public static AccessorRegistry getAccessors() { return accessors; }

  private static AccessorRegistry configureAccessors() {
    return AccessorRegistry.configure()
          .register(Employee.class, getEmployeeAccessor())
          .freeze();
  }

  private static Accessor<Employee> getEmployeeAccessor() {
    return (employee, property) -> switch (property) {
      case "firstName" -> employee.getFirstName();
      case "lastName" -> employee.getLastName();
      case "birtDate" -> employee.getBirthDate();
      default -> Accessor.UNDEFINED;
    };
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
    RenderSession session = template.newRenderSession(Setup.getAccessors());
    session.insert(employee);
    return session::render;
  }

}
```

### Name Mappers

By default, _Klojang Templates_ assumes that template variables can be mapped _as-is_ to
map keys or bean properties. You can use
[name mappers](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/NameMapper.html)
to loosen this up. _Klojang Templates_ provides various predefined name mappers, but you
can also implement your own `NameMapper`.

```java
import org.klojang.templates.AccessorRegistry;
import org.klojang.templates.name.SnakeCaseToCamelCase;

public class Setup {

  private static final AccessorRegistry accessors = configureAccessors();

  public static AccessorRegistry getAccessors() { return accessors; }

  private static AccessorRegistry configureAccessors() {
    // use the predefined snake-case-to-camel-case mapper
    return AccessorRegistry.configure()
          .setDefaultNameMapper(new SnakeCaseToCamelCase())
          .freeze();
  }

}
```

## Template Caching

[Template](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/Template)
instances created from a
[file system resource](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/Template.html#fromFile(java.lang.String))
or from a
[classpath resource](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/Template.html#fromResource(java.lang.Class,java.lang.String))
are by default always cached. Thus, the cost of parsing the template is paid just once.
Included templates are cached separately from the templates in which they are included.
This makes the template retrieval process even more efficient. The first time you include
a template it needs to be parsed, but the next time you include it, either in the same
template or in another template, you do so essentially for free.

#### Disabling Template Caching

During development you might want to disable caching so you can modify the template and
immediately verify the result when you hit the Refresh button in your browser. This can be
done by adding `-Dorg.klojang.templates.cacheSize=0` to the java command line.
Alternatively, you can set an environment variable named KJT_CACHE_SIZE to 0.

## Conclusion

That's it, really. The next paragraphs will only be of interest to you if your goal is to
write templates that are well-formed, valid and pleasing to the eye even in their raw,
unprocessed state.

The
[RenderSession](https://klojang4j.github.io/klojang-templates/api/org.klojang.templates/org/klojang/templates/RenderSession.html)
class contains quite a few more methods that help you populate a Klojang template, and we
have not covered every single way in which you can fine-tune the behaviour of
_Klojang Templates_, but this is all covered in great detail in
the [javadocs](https://klojang4j.github.io/klojang-templates/api).

## Evolving the Raw Template

_Klojang Templates_ allows you to write templates that will render flawlessly in their
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

The first thing to keep in mind is that template variables can be placed in HTML comments
without this making a difference in how the template is rendered.
`<!-- ~%foo% -->` is rendered just like `~%foo%`. _Klojang Templates_ will replace the
_entire_ character sequence with whatever value `foo` was set to. The space character on
either side of `~%foo%` is optional. You may also write `<!--~%foo%-->`. Multiple spaces,
or other characters, however, are not allowed.

So your first attempt at turning the design into a dynamically populated page might look
like this:

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

That's nice. The user won't see ugly `~%` tokens when viewing the raw template in a
browser. Unfortunately, the row now has disappeared from view in the raw template.

This can be remedied by using _placeholders_. A placeholder is a value that is placed
inside a pair of `<!--%-->` tokens. Since `<!--%-->` is a self-closed HTML comment, any
text inside a pair of these tokens will be visible in the browser. However, when
_Klojang Templates_ renders the template, it will remove the `<!--%-->` tokens and any
text inside it.

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

If you remove everything that is an comment from the above HTML snippet, you are back to
where you started.

If the variable and placeholder together fit on a single line, this can be contracted to:

```html

<td><!-- ~%firstName% -->John<!--%--></td>
<td><!-- ~%lastName% -->Smith<!--%--></td>
```

### Populating the Table

Now we want to introduce a nested template so we can make the table row repeat for each
element of the `List<Employee>` we receive from the data access layer.

As with template variables, you can place the begin and end tag of an inline template in
HTML comments. _Klojang Templates_ treats `<!-- ~%%begin:foo% -->`
just like it treats `~%%begin:foo%`.

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
comments, you are back to where you started. Yet now the page has become fully dynamic.

### Ditch Blocks

It may have occurred to you that this will not work for included templates:

```html

<tbody>
<!-- ~%%include:employee:/views/employee-row.html%% -->
</tbody>
```

This, too, _will_ be rendered just like
`~%%include:employee:/views/employee-row.html%%` (without the `<!--` and `-->`), but the
raw template now unfortunately simply has an empty table body.

In this case you can use _ditch blocks_ to restore renderability to the raw template:

```html

<tbody>
<!--%%-->
<tr>
    <td>John</td>
    <td>Smith</td>
</tr>
<!--%%-->

<!-- ~%%include:employee:/views/employee-row.html%% -->
</tbody>
```

Ditch blocks are pairs of `<!--%%-->` tokens and any text between them. As with
placeholders (`<!--%-->`), these tokens are self-closed HTML comments, so the text between
them will be visible in the browser. But when _Klojang Templates_ renders the template,
all ditch blocks will be removed from the template.

Ditch blocks can in fact also be combined with inline templates to produce the same
result:

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

Notice how, this time, it is not just the begin and end tags of the inline template that
are placed inside HTML comments. It is the _entire_ inline template.

#### Ditch Blocks vs. Placeholders

Ditch blocks really are just comments, comparable to HTML or Java comments. You can even
use ditch blocks to "comment out" nested templates:

```
<!--%%--> Not sure yet if this is a good idea
    <!-- ~%%begin:employees% -->
    <tr>
        <td><!-- ~%firstName% -->John<!--%--></td>
        <td><!-- ~%lastName% -->Smith<!--%--></td>
    </tr>
    <!-- ~%%end:employees% -->
<!--%%-->
```

However, you cannot have ditch blocks _inside_ an inline template. For placeholders it's
the other way round. They may appear inside nested templates, but you cannot place a pair
of placeholder tokens _around_ a nested template.

## About

<img src="docs/logo-groen.png" style="float:left;width:5%;padding:0 12px 12px 0"/>

Klojang Check is developed by [Naturalis](https://www.naturalis.nl/en), the Dutch national
biodiversity research institute and natural history museum. It maintains one of the
largest collections of zoological and botanical specimens in the world.





