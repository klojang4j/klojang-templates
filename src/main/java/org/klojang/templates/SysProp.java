package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.CommonChecks;
import org.klojang.convert.Bool;
import org.klojang.convert.NumberMethods;

/**
 * Specifies all system properties that will be picked up by Klojang.
 *
 * @author Ayco Holleman
 */
public enum SysProp {

  /**
   * Property: {@code org.klojang.template.cacheSize}. Default value: {@code -1}.<br>
   * Specifies the maximum size of the internally maintained {@link Template} cache.
   * When the cache reaches full capacity, {@code Template} instances are evicted on
   * a least-recently-used basis. A value of -1 means the cache is allowed to grow to
   * any size. A value of 0 effectively disables caching. This is useful during
   * development and/or debugging as the template file will be re-loaded and
   * re-parsed every time you press the refresh button in the browser, without having
   * to restart the server.
   */
  TMPL_CACHE_SIZE(Template.class, "cacheSize", "-1"),

  /**
   * Property: {@code org.klojang.template.parser.varStart}. Default value:
   * {@code ~%}.<br> Specifies the character sequence at the start of a template
   * variable.
   */
  VAR_START(Template.class, "parser.varStart", "~%"),

  /**
   * Property: {@code org.klojang.template.parser.varEnd}. Default value:
   * {@code %}.<br> Specifies the character sequence at the end of a template
   * variable.
   */
  VAR_END(Template.class, "parser.varEnd", "%"),

  /**
   * Property: {@code org.klojang.template.parser.tmplStart}. Default value:
   * {@code ~%%}.<br> Specifies the character sequence at the start of a template
   * tag. This is used for <i>both</i> the start tag ({@code ~%%begin:foo%})
   * <i>and</i> the end tag ({@code ~%%end:foo%}) of an inline template as well as
   * for included templates ({@code ~%%include:foo.html%}).
   */
  TMPL_START(Template.class, "parser.tmplStart", "~%%"),

  /**
   * Property: {@code org.klojang.template.parser.tmplEnd}. Default value:
   * {@code %}.<br> Specifies the character sequence at the end of a template tag.
   */
  TMPL_END(Template.class, "parser.tmplEnd", "%"),

  /**
   * Property: {@code org.klojang.render.useBeanAccessor}. Default value:
   * {@code false}.<br> Specifying {@code true} means that if a template is
   * {@link RenderSession#insert(Object, String...) populated} with an object for
   * which no dedicated {@link Accessor} implementation exists, the
   * {@code RenderSession} will assume it is a JavaBean and access its properties
   * using a {@link BeanAccessor}. By default such objects will be accessed using a
   * {@link PathAccessor}. A {@code PathAccessor} can handle many more types than a
   * {@code BeanAccessor} (e.g. arrays, lists, maps, sets, JavaBeans) and, more
   * importantly, can also access deeply nested data structures. It is therefore a
   * safer choice as a default. However, it is also slightly less efficient.
   * Moreover, nested data structures generally correspond to nested templates. In
   * other words, nested values are destined for nested templates (where they appear
   * as top-level values). Your application may never need to access a deeply nested
   * value from a top-level template. If so, consider using the {@code BeanAccessor}
   * class as the fallback {@code Accessor} implementation. See also
   * {@link AccessorRegistry}.
   */
  USE_BEAN_ACCESSOR(RenderSession.class, "useBeanAccessor", "false");

  private final String name;
  private final String dfault;

  private SysProp(Class<?> clazz, String name, String dfault) {
    String pkg = clazz.getPackageName();
    this.name = pkg + "." + name;
    this.dfault = dfault;
  }

  /**
   * Returns the value of the system property or its default value if not specified.
   *
   * @return The value of the system property or its default value if not specified
   */
  public String get() {
    return System.getProperty(name, dfault);
  }

  /**
   * Returns the value of the system property as an integer or its default value if
   * not specified.
   *
   * @return The value of the system property as an integer or its default value if
   *     not specified
   */
  public int getInt() {
    return Check.that(get(), name)
        .is(CommonChecks.numerical(), int.class)
        .ok(NumberMethods::parseInt);
  }

  /**
   * Returns the value of the system property as a {@code boolean} or its default
   * value if not specified.
   *
   * @return The value of the system property as an {@code boolean} or its default
   *     value if not specified
   */
  public boolean getBoolean() {
    return Check.that(get(), name).is(Bool::isConvertible).ok(Bool::from);
  }

  /**
   * Returns the property name associated with this enum constant.
   *
   * @return The property name associated with this enum constant
   */
  public String property() {
    return name;
  }

  /**
   * Returns the default value for the system property.
   *
   * @return The default value for the system property
   */
  public String defaultValue() {
    return dfault;
  }
}
