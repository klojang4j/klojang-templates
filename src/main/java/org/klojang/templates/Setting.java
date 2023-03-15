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
   * to grow to any size. A value of 0 disables caching. This is useful during
   * development and/or debugging as the template file will be re-loaded and
   * re-parsed every time you press the refresh button in the browser.
   * </p>
   */
  TMPL_CACHE_SIZE("org.klojang.templates.cacheSize", "KJT_CACHE_SIZE", "-1");

  private final String sysprop;
  private final String envvar;
  private final String dfault;

  Setting(String sysprop, String envvar, String dfault) {
    this.sysprop = sysprop;
    this.envvar = envvar;
    this.dfault = dfault;
  }

  /**
   * Returns the name of the system property for this setting.
   *
   * @return the name of the system property for this setting
   */
  public String property() {
    return sysprop;
  }

  /**
   * Returns the name of the environment variable for this setting.
   *
   * @return the name of the environment variable for this setting
   */
  public String envVar() {
    return sysprop.replace('.', '_').toUpperCase();
  }

  /**
   * Returns the default value for this setting.
   *
   * @return the default value for this setting
   */
  public String defaultValue() {
    return dfault;
  }

  /**
   * Returns the value of this setting or its default value if not specified.
   *
   * @return the value of this setting or its default value if not specified
   */
  public String get() {
    String env;
    if ((env = System.getenv(envvar)) != null) {
      return env;
    }
    return System.getProperty(sysprop, dfault);
  }

  int getInt() {
    return Check.that(get()).is(numerical(), int.class).ok(Integer::parseInt);
  }

  boolean getBoolean() {
    return Check.that(get(), sysprop).is(Bool::isConvertible).ok(Bool::from);
  }

  }
