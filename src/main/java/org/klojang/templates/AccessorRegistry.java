package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;
import org.klojang.invoke.BeanReader;
import org.klojang.invoke.BeanReaderBuilder;
import org.klojang.path.PathWalker;
import org.klojang.templates.x.acc.ArrayAccessor;
import org.klojang.templates.x.acc.BeanAccessor;
import org.klojang.templates.x.acc.MapAccessor;
import org.klojang.templates.x.acc.PathAccessor;
import org.klojang.util.ClassMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

/**
 * <p>A registry of {@link Accessor accessors}, used by a {@link RenderSession} to
 * extract values from model objects. When using a {@code RenderSession} to
 * {@linkplain RenderSession#insert(Object, String...) insert} an object into a
 * {@link Template}, the {@code RenderSession} will ask the {@code AccessorRegistry}
 * to look up an {@code Accessor} for that particular type of object. The
 * {@code AccessorRegistry} defined by the {@link #STANDARD_ACCESSORS} constant may
 * very well contain all the accessors you will ever need. In other words, you may
 * never have to actually implement an {@code Accessor} yourself.
 *
 * <p>Any {@code AccessorRegistry}, including the ones you build yourself and
 * including the {@code STANDARD_ACCESSORS} {@code AccessorRegistry}, comes with a
 * standard set of predefined, internally maintained accessors. For example there are
 * {@code Accessor} implementation for maps, records and JavaBeans. These accessors
 * are not exposed via the API as you are only communicating with Klojang Templates
 * via accessor registries, not via individual accessors. However, you can check the
 * source code for inspiration, should you need to write enhanced versions of them.
 * You might, for example, want to provide your own {@code MapAccessor}.
 *
 * <p>This is how the {@code AccessorRegistry} decides which accessor to hand out to
 * the {@code RenderSession} for a particular type of object:
 *
 * <ol>
 *   <li>If you have {@link Builder#register(Class, Accessor) registered} your own
 *       {@code Accessor} for that particular type of object, then that is the
 *       {@code Accessor} that is going to be used.
 *   <li>If the object is an {@link Optional}, then a (non-exposed)
 *       {@code OptionalAccessor} is going to be used, which will typically defer to
 *       the appropriate {@code Accessor} for the object <i>within</i> the
 *       {@code Optional} Optionals are typically returned from (for example) the
 *       ubiquitous {@code dao.findById(id)} method and in Klojang it is perfectly
 *       legitimate to {@link RenderSession#insert(Object, String...) insert} an
 *       {@code Optional} into a template.
 *   <li>If the object is a {@code Map}, a (non-exposed) {@code MapAccessor} is
 *       going to be used. (You could easily create an enhanced version yourself,
 *       tailored to your particular needs. Check the source code.)
 *   <li>Otherwise a {@code PathAccessor} is going to be used (however, see
 *       {@link Setting#USE_BEAN_ACCESSOR}). This is a very versatile accessor that
 *       can access almost any type of object as well as deeply nested objects. It
 *       is internally backed by a {@link PathWalker}.
 * </ol>
 *
 * <p>Note that the {@link PathWalker} class does not use reflection to read bean
 * properties, but it <i>does</i> use reflection to figure out what those properties
 * are in the first place. Thus, if you use this accessor from within a Java 9+
 * module, you will have to open up the module for reflection. Alternatively, you
 * could write your own {@code Accessor} after all:
 *
 * <blockquote><pre>{@code
 * Accessor<Person> personAccessor =
 *   (person, property) -> {
 *     switch(property) {
 *       case "id" : return person.getId();
 *       case "firstName" : return person.getFirstName();
 *       case "lastName" : return person.getLastName();
 *       case "birthDate" : return person.getBirthDate();
 *       default : return Accessor.UNDEFINED;
 *     }
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
 *
 * <blockquote><pre>{@code
 * AccessorRegistry reg = AccessorRegistry
 *   .configure()
 *   .register(BeanReaderBuilder.forClass(Person.class)
 *    .withInt("id")
 *    .withString("firstName", "lastName")
 *    .with(LocalDate.class, "birthDate"))
 *   .freeze();
 * RenderSession session = template.newRenderSession(reg);
 * }</pre></blockquote>
 *
 * @author Ayco Holleman
 * @see Setting#USE_BEAN_ACCESSOR
 */
public class AccessorRegistry {

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
   * @param nameMapper the {@code NameMapper} to use when accessing model
   *     objects
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
  public static class Builder {

    private NameMapper defMapper = NameMapper.AS_IS;
    private final Map<Class<?>, Map<Template, Accessor<?>>> accs = new HashMap<>();
    private final Map<Template, NameMapper> mappers = new HashMap<>();

    private Builder() {}

    /**
     * Sets the default {@code NameMapper} used to map template variable names to
     * JavaBean properties (or {@code Map} keys). If no default {@code NameMapper} is
     * specified, the {@link NameMapper#AS_IS AS_IS} name mapper will be the default
     * name mapper.
     *
     * @param nameMapper The name mapper
     * @return This {@code Builder} instance
     */
    public Builder setDefaultNameMapper(NameMapper nameMapper) {
      defMapper = Check.notNull(nameMapper).ok();
      return this;
    }

    /**
     * Sets the {@code NameMapper} to be used for the specified template.
     *
     * @param template The template for which to use the specified name mapper
     * @param nameMapper The name mapper
     * @return This {@code Builder} instance
     */
    public Builder setNameMapper(Template template, NameMapper nameMapper) {
      Check.notNull(template, "template");
      Check.notNull(nameMapper, "nameMapper");
      mappers.put(template, nameMapper);
      return this;
    }

    /**
     * Sets the {@code Accessor} to be used for objects of the specified type.
     *
     * @param <T> The type of the objects for which to use the {@code Accessor}
     * @param forType The {@code Class} object corresponding to the type
     * @param accessor The {@code Accessor}
     * @return This {@code Builder} instance
     */
    public <T> Builder register(Class<T> forType, Accessor<? extends T> accessor) {
      accs.computeIfAbsent(forType, k -> new HashMap<>()).put(null, accessor);
      return this;
    }

    /**
     * Sets the {@code Accessor} to be used for the bean class managed by the
     * specified {@code BeanReaderBuilder}. This method will call
     * {@link BeanReaderBuilder#build() build()} on the {@code BeanReaderBuilder} to
     * obtain a {@link BeanReader}, which is then wrapped into an internally defined
     * (non-exposed) {@code BeanAccessor} instance. The {@code BeanAccessor} will use
     * the {@link NameMapper} set through
     * {@link #setDefaultNameMapper(NameMapper) setDefaultNameMapper()}, or
     * {@link NameMapper#AS_IS NameMapper.AS_IS} if no default name mapper has been
     * set yet. Use this method if you prefer 100% reflection-free bean reading.
     *
     * @param brb the {@code BeanReaderBuilder}
     * @param <T> the type of the beans
     * @return This {@code Builder} instance
     */
    public <T> Builder register(BeanReaderBuilder<T> brb) {
      return register(brb, defMapper);
    }

    /**
     * Sets the {@code Accessor} to be used for the bean class managed by the
     * specified {@code BeanReaderBuilder}. This method will call
     * {@link BeanReaderBuilder#build() build()} on the {@code BeanReaderBuilder} to
     * obtain a {@link BeanReader}, which is then wrapped into an internally defined
     * (non-exposed) {@code BeanAccessor} instance. Use this method if you prefer
     * 100% reflection-free bean reading.
     *
     * @param brb the {@code BeanReaderBuilder}
     * @param nameMapper the {@code NameMapper} to be used to map template
     *     variables to bean properties
     * @param <T> the type of the beans
     * @return This {@code Builder} instance
     */
    public <T> Builder register(BeanReaderBuilder<T> brb, NameMapper nameMapper) {
      BeanReader<T> br = brb.build();
      accs.computeIfAbsent(br.getBeanClass(),
          k -> new HashMap<>()).put(null, new BeanAccessor<>(br, nameMapper));
      return this;
    }

    /**
     * Sets the {@code Accessor} to be used for objects of the specified type,
     * destined for the specified template.
     *
     * @param <T> The type of the objects for which to use the {@code Accessor}
     * @param forType The {@code Class} object corresponding to the type
     * @param template The template for which to use the {@code Accessor}
     * @param accessor The {@code Accessor}
     * @return This {@code Builder} instance
     */
    public <T> Builder register(Class<T> forType,
        Template template,
        Accessor<? super T> accessor) {
      accs.computeIfAbsent(forType, k -> new HashMap<>()).put(template, accessor);
      return this;
    }

    /**
     * Returns a
     *
     * @return
     */
    public AccessorRegistry freeze() {
      return new AccessorRegistry(accs, defMapper, mappers);
    }

  }

  /* +++++++++++++++++++++[ END BUILDER CLASS ]++++++++++++++++++ */

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
      if (type == Optional.class) {
        return new OptionalAccessor<>(this, template);
      } else if (ClassMethods.isSubtype(type, Map.class)) {
        acc = new MapAccessor(nm);
      } else if (ClassMethods.isSubtype(type, Object[].class)) {
        acc = ArrayAccessor.getInstance(template);
      } else if (useBeanAccessor) {
        acc = new BeanAccessor<>(type, nm);
      } else {
        acc = new PathAccessor(nm);
      }
    }
    return acc;
  }

}
