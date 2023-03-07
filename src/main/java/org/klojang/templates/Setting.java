package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.convert.Bool;

import static org.klojang.check.CommonChecks.numerical;

/**
 * Specifies all system properties and/or environment variables that will be picked
 * up by Klojang Templates. Environment variables take precedence over system
 * properties.
 *
 * @author Ayco Holleman
 */
public enum Setting {

  /**
   * <p>System Property: {@code org.klojang.templates.cacheSize}<br>Environment
   * Variable: {@code KJT_CACHE_SIZE}<br>Default Value: {@code -1}.
   *
   * <p>Specifies the maximum size of an internally maintained cache of
   * {@link Template} instances - used to reduce the overhead of parsing template
   * files. When the cache reaches the specified size, {@code Template} instances are
   * evicted on a least-recently-used basis. A value of -1 means the cache is allowed
   * to grow to any size. A value of 0 disables caching. The latter is useful during
   * development and/or debugging as the template file will be re-loaded and
   * re-parsed every time you press the refresh button in the browser.
   * </p>
   */
  TMPL_CACHE_SIZE("org.klojang.templates.cacheSize", "KJT_CACHE_SIZE", "-1"),

  /**
   * <p>System Property:
   * {@code org.klojang.templates.useBeanAccessor}<br>Environment Variable:
   * {@code KJT_USE_BEAN_ACCESSOR}<br>Default Value: {@code false}.
   *
   * <p>Specifying {@code true} means that if a template is
   * {@linkplain SoloSession#insert(Object, String...) populated} with an object
   * for which no dedicated {@link Accessor} implementation exists, the
   * {@code RenderSession} will use a {@link BeanAccessor} rather than a
   * {@link PathAccessor} to extract values from it. A {@code PathAccessor} can
   * handle many more types than a {@code BeanAccessor} and it can access deeply
   * nested objects. It is therefore a safer choice as a default. However, it is also
   * slightly less efficient. Moreover, nested objects generally correspond to nested
   * templates, where they appear as root-level objects. Your application may never
   * ever have to access deeply nested values to populate a template. If so, consider
   * using the {@code BeanAccessor} class as the fallback {@code Accessor}
   * implementation. See also {@link AccessorRegistry}.
   */
  USE_BEAN_ACCESSOR("org.klojang.templates.useBeanAccessor",
      "KJT_USE_BEAN_ACCESSOR",
      "false");

  private final String sysprop;
  private final String envvar;
  private final String dfault;

  Setting(String sysprop, String envvar, String dfault) {
    this.sysprop = sysprop;
    this.envvar = envvar;
    this.dfault = dfault;
  }

  /**
   * Returns the value of the system property or its default value if not specified.
   *
   * @return the value of the system property or its default value if not specified
   */
  public String get() {
    String env;
    if ((env = System.getenv(envvar)) != null) {
      return env;
    }
    return System.getProperty(sysprop, dfault);
  }

  /**
   * Returns the value of the system property as an integer or its default value if
   * not specified.
   *
   * @return the value of the system property as an integer or its default value if
   *     not specified
   */
  public int getInt() {
    return Check.that(get()).is(numerical(), int.class).ok(Integer::parseInt);
  }

  /**
   * Returns the value of the system property as a {@code boolean} or its default
   * value if not specified.
   *
   * @return the value of the system property as an {@code boolean} or its default
   *     value if not specified
   */
  public boolean getBoolean() {
    return Check.that(get(), sysprop).is(Bool::isConvertible).ok(Bool::from);
  }

  /**
   * Returns the name of the system property associated with this enum constant.
   *
   * @return the name of the system property associated with this enum constant
   */
  public String property() {
    return sysprop;
  }

  /**
   * Returns the name of the environment variable associated with this enum
   * constant.
   *
   * @return the name of the environment variable associated with this enum constant
   */
  public String envVar() {
    return sysprop.replace('.', '_').toUpperCase();
  }

  /**
   * Returns the default value for the system property.
   *
   * @return the default value for the system property
   */
  public String defaultValue() {
    return dfault;
  }
}
