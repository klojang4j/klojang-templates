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
 * {@link RenderSession} to extract values from model objects. The
 * {@link #STANDARD_ACCESSORS} constant is a ready-made {@code AccessorRegistry} that
 * may contain all the accessors you will ever need for your application. In other
 * words, you may never have to actually implement an {@code Accessor} yourself. Note
 * that you only interact with Klojang Templates via entire accessor registries, not
 * via individual accessors.
 *
 * <p>Any {@code AccessorRegistry}, including the ones you build yourself and
 * including the {@code STANDARD_ACCESSORS}, comes with a set of predefined,
 * internally maintained, and non-exposed accessors. For example there are
 * {@code Accessor} implementation for maps, records and JavaBeans.
 *
 * <p>This is how an {@code AccessorRegistry} decides which accessor to hand out to
 * the {@code RenderSession} for a particular type of object:
 *
 * <ol>
 *   <li>If you have {@linkplain Builder#register(Accessor, Class) registered} your
 *       own {@code Accessor} for that particular type of object, then that is the
 *       {@code Accessor} that is going to be used.
 *   <li>If the object is a {@code Map}, a (non-exposed) {@code MapAccessor} is going
 *       to be used. (You could easily create and register one yourself, tailored to
 *       your particular needs.)
 *   <li>Otherwise a {@code PathAccessor} is going to be used. This is a very
 *       versatile accessor that can read almost any type of object. It is internally
 *       backed by a {@link PathWalker}.
 * </ol>
 *
 * <p>Note that the {@code PathAccessor} class does not use reflection to read bean
 * properties, but it <i>does</i> use reflection to figure out what those properties
 * are in the first place. Thus, if you use this accessor from within a Java 9+
 * module, you will have to open up the module for reflection. Alternatively, you
 * could write your own {@code Accessor} after all:
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
 * BeanReader br = BeanReader.forClass(Person.class)
 *    .withInt("id")
 *    .withString("firstName", "lastName")
 *    .with(LocalDate.class, "birthDate"))
 *    .build();
 * AccessorRegistry reg = AccessorRegistry
 *   .configure()
 *   .register(br)
 *   .freeze();
 * RenderSession session = template.newRenderSession(reg);
 * }</pre></blockquote>
 *
 * @author Ayco Holleman
 * @see Template#newRenderSession(AccessorRegistry)
 * @see Setting#USE_BEAN_ACCESSOR
 */
public final class AccessorRegistry {

  /**
   * An {@code AccessorRegistry} that should be sufficient for most use cases. It
   * assumes that the names you use in your templates can be mapped as-is to your
   * model objects.
   */
  public static final AccessorRegistry STANDARD_ACCESSORS = configure().freeze();

  /**
   * Returns an {@code AccessorRegistry} that should be sufficient for most use
   * cases. It allows you to specify one global {@link NameMapper} for mapping the
   * names used in your templates to the names used in your model objects.
   *
   * @param nameMapper the {@code NameMapper} to be used to map template
   *     variables to bean properties and/or map keys.
   * @return an {@code AccessorRegistry} the should sufficient for most use cases
   */
  public static AccessorRegistry standard(NameMapper nameMapper) {
    return configure().setDefaultNameMapper(nameMapper).freeze();
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
     * Wraps the specified {@link BeanReader} into an internally defined
     * (non-exposed) {@code BeanAccessor} instance, used to access beans of the type
     * targeted by the {@code BeanReader}. Use this method if you prefer 100%
     * reflection-free bean reading. See {@link BeanReader#forClass(Class)}.
     *
     * @param br the {@code BeanReader}
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> br) {
      return register(br, defMapper);
    }

    /**
     * Wraps the specified {@link BeanReader} into an internally defined
     * (non-exposed) {@code BeanAccessor} instance, used to access beans of the type
     * targeted by the {@code BeanReader}. Use this method if you prefer 100%
     * reflection-free bean reading. See {@link BeanReader#forClass(Class)}.
     *
     * @param beanReader the {@code BeanReader}
     * @param template the template for which to use the accessor (may be a root
     *     template or a nested template)
     * @param <T> the type of the beans
     * @return this {@code Builder} instance
     */
    public <T> Builder register(BeanReader<T> beanReader,
        Template template) {
      return register(beanReader, template, defMapper);
    }

    /**
     * Wraps the specified {@link BeanReader} into an internally defined
     * (non-exposed) {@code BeanAccessor} instance, used to access beans of the type
     * targeted by the {@code BeanReader}. Use this method if you prefer 100%
     * reflection-free bean reading. See {@link BeanReader#forClass(Class)}.
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
     * Wraps the specified {@link BeanReader} into an internally defined
     * (non-exposed) {@code BeanAccessor} instance, used to access beans of the type
     * targeted by the {@code BeanReader}. Use this method if you prefer 100%
     * reflection-free bean reading. See {@link BeanReader#forClass(Class)}.
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
      return new AccessorRegistry(accs, defMapper, mappers);
    }

    private <T> Builder register0(Accessor<T> acc, Class<T> clazz, Template tmpl) {
      Map<Template, Accessor<?>> map = accs.get(clazz);
      if (map == null) {
        accs.put(clazz, map = new HashMap<>());
      } else if (tmpl == null) {
        // allowed - template-agnostic accessor
        Check.that(map.containsKey(null)).is(no(),
            TYPE_ALREADY_SET, clazz);
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

  private final boolean useBeanAccessor = Setting.USE_BEAN_ACCESSOR.getBoolean();

  private final Map<Class<?>, Map<Template, Accessor<?>>> accs;
  private final NameMapper defMapper;
  private final Map<Template, NameMapper> mappers;

  private AccessorRegistry(Map<Class<?>, Map<Template, Accessor<?>>> accs,
      NameMapper defMapper,
      Map<Template, NameMapper> mappers) {
    this.accs = accs.isEmpty() ? emptyMap() : TypeMap.fixedTypeMap(accs);
    this.defMapper = defMapper;
    this.mappers = Map.copyOf(mappers);
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
