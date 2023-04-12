/**
 * <p>
 * <i>Klojang Templates</i> is a templating API written with two goals in
 * mind:
 * </p>
 * <p>
 * &#x25c6; Writing templates should be so simple that there is essentially no
 * learning curve.<br/> &#x25c6; Provide a rich en flexible API for populating the
 * templates that compensates for their simplicity.
 * </p>
 * <p>
 * In short: leverage the skills of Java programmers, rather than make them learn
 * what, in effect, amounts to a whole new language.
 * </p>
 * <h2>Syntax</h2>
 * <p>
 * A Klojang template consists of the following elements:
 * </p>
 * <p>
 * <b>Boilerplate Text</b><br/>
 * This could be HTML, but it could equally well be anything else. <i>Klojang
 * Templates</i> is not biased towards any particular output format. However,
 * <i>Klojang Templates</i> does provide some extra help when writing HTML
 * templates, notable in the form of a few predefined
 * {@linkplain org.klojang.templates.VarGroup variable groups}.
 * </p>
 * <p>
 * <b>Template Variables</b><br/>
 * Template variables look like this: {@code ~%city%}. The variable name can
 * optionally be prefixed with the name of a
 * {@linkplain org.klojang.templates.VarGroup variable group}: {@code ~%html:city%}.
 * When populating a template through
 * {@link org.klojang.templates.RenderSession#insert(Object, String...)
 * RenderSession.insert()} or
 * {@link org.klojang.templates.RenderSession#populate(String, Object, String...)
 * RenderSession.populate()}, a variable name may be a
 * {@linkplain org.klojang.path.Path path} through the source data object passed to
 * these methods. For example: {@code ~%employee.address.city%}. Since you may be
 * populating your templates with hash maps, in which case variable names would
 * correspond to map keys, there are hardly any constraints on what constitutes a
 * valid variable name (or path segment):<br/>&#x25c6; it must contain at least one
 * character<br/>&#x25c6; it must not contain any of the following characters:
 * {@code ~%.:\r\n\0}.<br/>Obviously, when using records or JavaBeans as source data,
 * you will be more constrained in your choice of variable names: they must be valid
 * Java identifiers.
 * </p>
 * <p>
 * <b>Nested Templates</b><br/>
 * In <i>Klojang Templates</i> you can embed templates in other templates.
 * Syntactically, this happens either either via <i><b>inline templates</b></i> or
 * via <i><b>included templates</b></i>.
 * </p>
 * <p>
 * Within an HTML template, inline templates would look like this:
 * </p>
 * <blockquote><pre>{@code
 * <html>
 * <body>
 *   ~%%begin:employees%
 *     <p>First name: ~%firstName%</p>
 *     <p>Last name: ~%lastName%</p>
 *   ~%%end:employees%
 * </body>
 * </html>
 * }</pre></blockquote>
 * <p>
 * Included templates look like this:
 * </p>
 * <blockquote><pre>{@code
 * <html>
 * <body>
 *   ~%%include:/static/views/employees.html%%
 * </body>
 * </html>
 * }</pre></blockquote>
 * <p>
 * And the contents of employees.html would then be something like this:
 * </p>
 * <blockquote><pre>{@code
 *   <p>First name: ~%firstName%</p>
 *   <p>Last name: ~%lastName%</p>
 * }</pre></blockquote>
 * <p>
 * The name of an included template by default is the basename of the include path
 * ("employees" in the example above). However, you can also use the following
 * syntax:
 * </p>
 * <blockquote><pre>{@code
 * ~%%include:employees:/static/views/employees-2023-01-01.html%%
 * }</pre></blockquote>
 * <p>
 * <b>Placeholders</b><br/>
 * A placeholder is a block of text inside a pair of {@code <!--%-->} tokens. The
 * tokens and the text are removed when the template is rendered. However, since
 * {@code <!--%-->} is a self-closed HTML comment, the placeholder text between a
 * pair of these tokens will be visible when viewing the raw template in a browser. A
 * nested template may contain placeholders, but you cannot place a pair of
 * {@code <!--%-->} tokens around a nested template.
 * <p>
 * <b>Ditch Blocks</b><br/>
 * A ditch block is a block of text inside a pair of {@code <!--%%-->} tokens. The
 * tokens and the text are removed when the template is rendered. Ditch blocks really
 * are comments, comparable to slash-star comments in Java. You can place
 * {@code <!--%%-->} tokens around variables or nested templates, causing them to be
 * "commented out", but you cannot place {@code <!--%%-->} tokens
 * <i>inside</i> any of the other syntax elements listed here.
 * </p>
 * <h3>HTML Comment Wrapping</h3>
 * <p>
 * <i>(NB This section is only relevant if your goal is to create templates that
 * will render flawlessly in a browser in their raw, unprocessed state</i>.)
 * </p>
 * <p>
 * Variables, inline templates and included templates can all be placed in HTML
 * comments with out this making a difference in how the template is rendered. For
 * example <code>&lt;!--&nbsp;~%city%&nbsp;--&gt;</code> renders just like
 * {@code ~%city%}. If {@code city} is set to "Paris", then the entire string
 * "&lt;!--&nbsp;~%city%&nbsp;--&gt;" is replaced with "Paris" when the template is
 * rendered. You can have either one space character on either side of the variable,
 * or none at all ({@code <!--~%city%-->}). Multiple spaces or other characters are
 * not allowed. The same applies when wrapping inline templates and included
 * templates in HTML comments.
 * </p>
 * <p>
 * For included templates, HTML comment wrapping would look like this:
 * {@code <!-- ~%%include:/path/to/foo.html%% -->}.
 * </p>
 * <p>
 * For inline templates you can either wrap just the begin and and tags of the inline
 * template, or you can wrap the entire inline template in HTML comments:
 * </p>
 * <blockquote><pre>{@code
 * <table>
 *     <!-- ~%%begin:employee% -->
 *     <tr><td>~%firstName%</td><td>~%lastName%</td></tr>
 *     <!-- ~%%end:employee% -->
 * </table>
 * }</pre></blockquote>
 * <p>
 * Versus:
 * </p>
 * <blockquote><pre>{@code
 * <table>
 *     <!-- ~%%begin:employee%
 *     <tr><td>~%firstName%</td><td>~%lastName%</td></tr>
 *     ~%%end:employee% -->
 * </table>
 * }</pre></blockquote>
 *
 * <h2>Using the API</h2>
 * <p>
 * Here are two fairly typical examples of using <i>Klojang Templates</i> within
 * JAX-RS. They produce the same result by different means.
 * </p>
 * <blockquote><pre>{@code
 * public class EmployeeResource {
 *
 *  EmployeeDao dao;
 *
 *  @GET
 *  @Path("/{id}")
 *  public String find(@PathParam("id") int id) {
 *    Employee emp = dao.find(id);
 *    Template template = Template.fromResource(getClass(), "/views/employee.html");
 *    RenderSession session = template.newRenderSession();
 *    return session
 *      .set("firstName", emp.getFirstName())
 *      .set("lastName", emp.getLastName())
 *      .render();
 *  }
 * }
 * }</pre></blockquote>
 *
 * <blockquote><pre>{@code
 * public class EmployeeResource {
 *
 *  EmployeeDao dao;
 *
 *  @GET
 *  @Path("/{id}")
 *  public StreamingOutput find(@PathParam("id") int id) {
 *    Employee employee = dao.find(id);
 *    Template template = Template.fromResource(getClass(), "/views/employee.html");
 *    RenderSession session = template.newRenderSession()
 *    session.insert(employee);
 *    return session::render;
 *  }
 * }
 * }</pre></blockquote>
 */
module org.klojang.templates {
  exports org.klojang.templates;
  exports org.klojang.templates.name;

  requires org.apache.commons.text;
  requires org.apache.commons.lang3;
  requires org.apache.httpcomponents.httpclient;
  requires org.slf4j;

  requires org.klojang.check;
  requires org.klojang.util;
  requires org.klojang.collections;
  requires org.klojang.convert;
  requires org.klojang.invoke;
}