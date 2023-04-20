package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;
import org.klojang.invoke.BeanReader;
import org.klojang.invoke.BeanReaderBuilder;
import org.klojang.path.PathWalker;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.klojang.check.CommonChecks.keyIn;
import static org.klojang.check.CommonChecks.no;
import static org.klojang.check.Tag.TYPE;
import static org.klojang.templates.x.MTag.ACCESSOR;
import static org.klojang.templates.x.MTag.TEMPLATE;

/**
 * <p>A registry of {@linkplain Accessor accessors}. Accessors are used by the
 * {@link RenderSession} to extract values from data provided by the data access
 * layer. This is how an {@code AccessorRegistry} decides which accessor to use for a
 * particular type of object:
 *
 * <ol>
 *   <li>If you have {@linkplain Builder#register(Accessor, Class) registered} your
 *       own {@code Accessor} for that particular type of object, then that is the
 *       {@code Accessor} that is going to be used.
 *   <li>Otherwise an internally defined, non-exposed {@code Accessor} implementation
 *       will be used. This {@code Accessor} implementation is very versatile and can
 *       read almost any type of object. It is internally backed by a
 *       {@link PathWalker}.
 * </ol>
 *
 * <p>Note that the internally defined {@code Accessor} mentioned above does not use
 * reflection to read bean properties, but it <i>does</i> use reflection to figure
 * out what those properties are in the first place. Thus, if you use
 * <i>Klojang Templates</i> from within a Java 9+ module, you must open up the module
 * for reflection. Alternatively, you could write your own {@code Accessor}:
 *
 * <blockquote><pre>{@code
 * Accessor<Person> personAccessor =
 *   (person, property) -> switch(property) {
 *       case "id" : return person.getId();
 *       case "firstName" : return person.getFirstName();
 *       case "lastName" : return person.getLastName();
 *       case "birthDate" : return person.getBirthDate();
 *       default : return Accessor.UNDEFINED;
 *   };
 * AccessorRegistry reg = AccessorRegistry
 *   .configure()
 *   .register(Person.class, new PersonAccessor())
 *   .freeze();
 * RenderSession session = template.newRenderSession(reg);
 * }</pre></blockquote>
 *
 * <p>A slightly less verbose, but still fully reflection-free alternative is to use
 * a {@link BeanReaderBuilder}:
 *
 * <blockquote><pre>{@code
 * // forClass returns a BeanReaderBuilder
 * BeanReader beanReader = BeanReader.forClass(Person.class)
 *    .withInt("id")
 *    .withString("firstName", "lastName")
 *    .with(LocalDate.class, "birthDate"))
 *    .build();
 * AccessorRegistry reg = AccessorRegistry
 *   .configure()
 *   .register(beanReader)
 *   .freeze();
 * RenderSession session = template.newRenderSession(reg);
 * }</pre></blockquote>
 *
 * @author Ayco Holleman
 * @see Template#newRenderSession(AccessorRegistry)
 */
public final class AccessorRegistry {

  /**
   * An {@code AccessorRegistry} that should be sufficient for most use cases. It
   * assumes that template variables map <i>as-is</i> to names used in source data
   * objects.
   */
  public static final AccessorRegistry STANDARD_ACCESSORS = configure().freeze();

  /**
   * Returns an {@code AccessorRegistry} that should be sufficient for most use
   * cases. It allows you to specify one global {@link NameMapper} for mapping the
   * template variables to the names used in source data objects.
   *
   * @param nameMapper the {@code NameMapper} to be used to map template
   *     variables to bean properties and/or map keys.
   * @return an {@code AccessorRegistry} the should sufficient for most use cases
   */
  public static AccessorRegistry standard(NameMapper nameMapper) {
    return configure().setDefaultNameMapper(nameMapper).freeze();
  }

  /**
   * Returns an {@code AccessorRegistry} that should be sufficient for most use
   * cases.
   *
   * @param nullEqualsUndefined whether {@code null} values should be treated the
   *     same way as {@link Accessor#UNDEFINED}
   * @return an {@code AccessorRegistry} the should sufficient for most use cases
   */
  public static AccessorRegistry standard(boolean nullEqualsUndefined) {
    return configure().nullEqualsUndefined(nullEqualsUndefined).freeze();
  }

  /**
   * Returns an {@code AccessorRegistry} that should be sufficient for most use
   * cases. It allows you to specify one global {@link NameMapper} for mapping the
   * template variables to the names used in source data objects.
   *
   * @param nameMapper the {@code NameMapper} to be used to map template
   *     variables to bean properties and/or map keys.
   * @param nullEqualsUndefined whether {@code null} values should be treated the
   *     same way as {@link Accessor#UNDEFINED}
   * @return an {@code AccessorRegistry} the should sufficient for most use cases
   */
  public static AccessorRegistry standard(NameMapper nameMapper,
      boolean nullEqualsUndefined) {
    return configure()
        .setDefaultNameMapper(nameMapper)
        .nullEqualsUndefined(nullEqualsUndefined)
        .freeze();
  }

  /* ++++++++++++++++++++[ BEGIN BUILDER CLASS ]+++++++++++++++++ */

  /**
   * A builder class for {@link AccessorRegistry} instances.
   *
   * @author Ayco Holleman
   */
  public static final class Builder {

    private static final String NAME_MAPPER = "name mapper";

    private static final String MAPPER_ALREADY_SET =
        "name mapper already set for template ${0}";

    private static final String TEMPLATE_ALREADY_SET =
        "template ${0} already has accessor for ${1}";

    private static final String TYPE_ALREADY_SET =
        "${arg} has already been associated with an accessor";

    private NameMapper defMapper = NameMapper.AS_IS;
    private boolean nullEqualsUndefined = false;
    private final Map<Class<?>, Map<Template, Accessor<?>>> accs = new HashMap<>();
    private final Map<Template, NameMapper> mappers = new HashMap<>();

    private Builder() {}

    /**
     * Sets the default {@code NameMapper} used to map template variables to bean
     * properties and/or map keys. If no default {@code NameMapper} is specified,
     * template variables will be mapped as-is to bean properties and/or map keys.
     *
     * @param nameMapper the name mapper
     * @return this {@code Builder} instance
     */
    public Builder setDefaultNameMapper(NameMapper nameMapper) {
      defMapper = Check.notNull(nameMapper).ok();
      return this;
    }

    /**
     * Determines whether {@code null} values should be treated just like
     * {@link Accessor#UNDEFINED}. By default this is not the case.
     *
     * @param b whether {@code null} values should be treated just like
     *     {@link Accessor#UNDEFINED}
     * @return this {@code Builder} instance
     * @see Accessor#UNDEFINED
     */
    public Builder nullEqualsUndefined(boolean b) {
      nullEqualsUndefined = b;
      return this;
    }

    /**
     * Sets the {@code NameMapper} to be used for the specified template.
     *
     * @param template the template for which to use the specified name mapper
     * @param nameMapper the name mapper
     * @return this {@code Builder} instance
     */
    public Builder setNameMapper(Template template, NameMapper nameMapper) {
      Check.notNull(template, TEMPLATE)
          .isNot(keyIn(), mappers, MAPPER_ALREADY_SET, template.getName());
      Check.notNull(nameMapper, NAME_MAPPER);
      mappers.put(template, nameMapper);
      return this;
    }

    /**
     * Sets the {@code Accessor} to be used for objects of the specified type.
     *
     * @param <T> the type of the objects for which to use the {@code Accessor}
     * @param accessor the {@code Accessor}
     * @param forType the {@code Class} object corresponding to the type
     * @return this {@code Builder} instance
     */
    public <T> Builder register(Accessor<T> accessor, Class<T> forType) {
      Check.notNull(accessor, ACCESSOR);
      Check.notNull(forType, TYPE);
      return register0(accessor, forType, null);
    }

    /**
     * Sets the {@code Accessor} to be used for objects of the specified type, when
     * inserted into the specified template.
     *
     * @param <T> the type of the objects for which to use the {@code Accessor}
     * @param accessor the {@code Accessor}
     * @param forType the {@code Class} object corresponding to the type
     * @param template the template for which to use the {@code Accessor}
     * @return this {@code Builder} instance
     */
    public <T> Builder register(Accessor<T> accessor, Class<T> forType,
        Template template) {
      Check.notNull(forType, TYPE);
      Check.notNull(template, TEMPLATE);
      Check.notNull(accessor, ACCESSOR);
      return register0(accessor, forType, template);
    }

    /**
     * Use the specified {@link BeanReader} to access objects of the type the
     * {@code BeanReader} can read. Use a {@link BeanReaderBuilder} to obtain the
     * {@code BeanReader} if you prefer 100% reflection-free bean reading. See
     * {@link BeanReader#forClass(Class)}.
     *
     * @param br the {@code BeanReader}
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> br) {
      return register(br, defMapper);
    }

    /**
     * Use the specified {@link BeanReader} to access objects of the type the
     * {@code BeanReader} can read. Use a {@link BeanReaderBuilder} to obtain the
     * {@code BeanReader} if you prefer 100% reflection-free bean reading. See
     * {@link BeanReader#forClass(Class)}.
     *
     * @param beanReader the {@code BeanReader}
     * @param template the template for which to use the accessor (may be a root
     *     template or a nested template)
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> beanReader, Template template) {
      return register(beanReader, template, defMapper);
    }

    /**
     * Use the specified {@link BeanReader} to access objects of the type the
     * {@code BeanReader} can read. Use a {@link BeanReaderBuilder} to obtain the
     * {@code BeanReader} if you prefer 100% reflection-free bean reading. See
     * {@link BeanReader#forClass(Class)}.
     *
     * @param br the {@code BeanReader}
     * @param nameMapper the {@code NameMapper} to be used to map template
     *     variables to bean properties
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> br, NameMapper nameMapper) {
      Check.notNull(br, "BeanReader");
      Check.notNull(nameMapper, NAME_MAPPER);
      return register0(new BeanAccessor<>(br, nameMapper), br.getBeanClass(), null);
    }

    /**
     * Use the specified {@link BeanReader} to access objects of the type the
     * {@code BeanReader} can read. Use a {@link BeanReaderBuilder} to obtain the
     * {@code BeanReader} if you prefer 100% reflection-free bean reading. See
     * {@link BeanReader#forClass(Class)}.
     *
     * @param beanReader the {@code BeanReader}
     * @param template the template for which to use the accessor (may be a root
     *     template or a nested template)
     * @param nameMapper the {@code NameMapper} to be used to map template
     *     variables to bean properties
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> beanReader,
        Template template,
        NameMapper nameMapper) {
      Check.notNull(beanReader, "BeanReader");
      Check.notNull(template, TEMPLATE);
      Check.notNull(nameMapper, NAME_MAPPER);
      return register0(new BeanAccessor<>(beanReader, nameMapper),
          beanReader.getBeanClass(),
          template
      );
    }

    /**
     * Returns an {@code AccessorRegistry} with the configured accessors.
     *
     * @return an {@code AccessorRegistry} with the configured accessors
     */
    public AccessorRegistry freeze() {
      return new AccessorRegistry(accs, defMapper, nullEqualsUndefined, mappers);
    }

    private <T> Builder register0(Accessor<T> acc, Class<T> clazz, Template tmpl) {
      Map<Template, Accessor<?>> map = accs.get(clazz);
      if (map == null) {
        accs.put(clazz, map = new HashMap<>());
      } else if (tmpl == null) {
        // allowed - template-agnostic accessor
        Check.that(map.containsKey(null)).is(no(), TYPE_ALREADY_SET, clazz);
      } else {
        Check.that(map.containsKey(tmpl)).is(no(),
            TEMPLATE_ALREADY_SET, tmpl.getName(), clazz);
      }
      map.put(tmpl, acc);
      return this;
    }

  }

  /* +++++++++++++++++++++[ END BUILDER CLASS ]++++++++++++++++++ */

  /**
   * Returns a {@code Builder} object that lets you configure an
   * {@code AccessorRegistry}.
   *
   * @return a {@code Builder} object that lets you configure an
   *     {@code AccessorRegistry}
   */
  public static Builder configure() {
    return new Builder();
  }

  private final Map<Class<?>, Map<Template, Accessor<?>>> accs;
  private final NameMapper defMapper;
  private final boolean nullEqualsUndefined;
  private final Map<Template, NameMapper> mappers;

  private AccessorRegistry(Map<Class<?>, Map<Template, Accessor<?>>> accs,
      NameMapper defMapper,
      boolean nullEqualsUndefined,
      Map<Template, NameMapper> mappers) {
    this.accs = accs.isEmpty() ? emptyMap() : TypeMap.fixedTypeMap(accs);
    this.defMapper = defMapper;
    this.nullEqualsUndefined = nullEqualsUndefined;
    this.mappers = Map.copyOf(mappers);
  }

  boolean nullEqualsUndefined() {
    return nullEqualsUndefined;
  }

  Accessor<?> getAccessor(Object obj, Template template) {
    Class<?> type = obj.getClass();
    Map<Template, Accessor<?>> m = accs.get(type);
    Accessor<?> acc = null;
    if (m != null) {
      acc = m.get(template);
      if (acc == null) {
        acc = m.get(null);
      }
    }
    if (acc == null) {
      NameMapper nm = mappers.getOrDefault(template, defMapper);
      acc = new PathAccessor(nm);
    }
    return acc;
  }

}
