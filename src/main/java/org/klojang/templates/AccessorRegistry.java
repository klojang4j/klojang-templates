package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.TypeMap;
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
 * A registry of {@link Accessor accessors} used by the {@link RenderSession} to
 * extract values from model objects. For example, if you want to populate a template
 * with a {@code Person} object, the {@link RenderSession} needs to know how to read
 * the {@code Person} properties that correspond to template variables. In most cases
 * the {@code AccessorRegistry} defined by the {@link #STANDARD_ACCESSORS} variable
 * is probably all you need, without you actually having to implement any
 * {@code Accessor} yourself.
 *
 * <p>Any {@code AccessorRegistry}, including the ones you build yourself and
 * including the {@code STANDARD_ACCESSORS} {@code AccessorRegistry} comes with a set
 * of predefined accessors. These accessors are not exposed via the API because you
 * are only communicating with Klojang via accessor registries, not via individual
 * accessors. However, you can check the source code for inspiration, should you need
 * to write enhanced versions of them. This is how the {@code AccessorRegistry}
 * decides which accessor to hands out to the {@code RenderSession} for any
 * particular type of object:
 *
 * <p>
 *
 * <ol>
 *   <li>If you have {@link Builder#register(Class, Accessor) registered} your own
 *       {@code Accessor} for that particular type of object, then that is the {@code Accessor} that
 *       is going to be used.
 *   <li>If the object is an {@link Optional}, then and {@code OptionalAccessor} is going to be
 *       used. If the {@code Optional} turns out to be empty, the {@code OptionalAccessor} forwards
 *       to a {@code NullAccessor}. This accessor simply always returns {@link Accessor#UNDEFINED},
 *       effectively causing the render session to do nothing. Otherwise the {@code
 *       OptionalAccessor} defers to another {@code Accessor}, based on the type of the object
 *       within the {@code Optional}. Optionals are typically returned from (for example) the
 *       ubiquitous {@code dao.findById(id)} method and in Klojang it is perfectly legitimate to
 *       {@link RenderSession#insert(Object, String...) insert} them into a template.
 *   <li>If the object is a {@code Map}, a {@code MapAccessor} is going to be used. (You could
 *       easily create an enhanced version yourself, tailored to your particular needs. Check the
 *       source code.)
 *   <li>If the object is a {@link Row}, a {@code RowAccessor} is going to be used.
 *   <li>In any other case the object is taken to be a JavaBean and a {@code PathAccessor} is going
 *       to be used (however, see {@link SysProp#USE_BEAN_ACCESSOR}). This is a very versatile
 *       accessor that can access almost any type of object as well as deeply nested objects. See
 *       {@link PathWalker}.
 * </ol>
 *
 * <p>Note that the accessor used to read JavaBean properties makes use of a {@link PathWalker}.
 * This class does not use reflection to read bean properties, but it does use reflection to figure
 * out what the properties are in the first place. Thus, if you use this accessor from within a Java
 * module, you will have to open up the module for reflection. Alternatively, you could just write
 * your own {@code Accessor}:
 *
 * <blockquote>
 *
 * <pre>{@code
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
 * AccessorRegistry aReg = AccessorRegistry
 *   .configure()
 *   .register(Person.class, new PersonAccessor())
 *   .freeze();
 * RenderSession session = template.newRenderSession(aReg);
 * }</pre>
 *
 * </blockquote>
 *
 * @author Ayco Holleman
 * @see SysProp#USE_BEAN_ACCESSOR
 */
public class AccessorRegistry {

  /**
   * An {@code AccessorRegistry} the should be sufficent for most use cases. It
   * assumes that the names you use in your templates can be mapped as-is to your
   * model objects.
   */
  public static final AccessorRegistry STANDARD_ACCESSORS = configure().freeze();

  /**
   * Returns an {@code AccessorRegistry} that should be sufficient for most use
   * cases. It allows you to specify one global {@link NameMapper} for mapping the
   * names used in your templates to the names used in your model objects.
   *
   * @param nameMapper The {@code NameMapper} to use when accessing model
   *     objects
   * @return An {@code AccessorRegistry} the should sufficent for most use cases
   */
  public static AccessorRegistry standard(NameMapper nameMapper) {
    return configure().setDefaultNameMapper(nameMapper).freeze();
  }

  /* ++++++++++++++++++++[ BEGIN BUILDER CLASS ]+++++++++++++++++ */

  /**
   * Lets you configure an {@link AccessorRegistry}.
   *
   * @author Ayco Holleman
   */
  public static class Builder {

    private NameMapper defMapper;
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

  private final boolean useBeanAccessor = SysProp.USE_BEAN_ACCESSOR.getBoolean();

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
