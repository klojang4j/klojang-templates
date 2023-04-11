package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.x.ClassPathResolver;
import org.klojang.templates.x.FilePathResolver;
import org.klojang.templates.x.MTag;
import org.klojang.util.collection.IntArrayList;
import org.klojang.util.collection.IntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_TEMPLATE;
import static org.klojang.util.CollectionMethods.implode;

/**
 * The {@code Template} class is responsible for loading and parsing templates. It
 * also functions as a factory for {@link RenderSession} objects. {@code Template}
 * instances are unmodifiable, expensive-to-create and heavy-weight objects.
 * Generally though you should not cache them as this is already done by
 * <i>Klojang Templates</i>. You can disable template caching by means of a
 * system property. See {@link Setting#TMPL_CACHE_SIZE}. This can be useful during
 * development and/or debugging as the template file will be re-loaded and re-parsed
 * every time you press the refresh button in the browser, allowing you to edit the
 * template in between.
 *
 * @author Ayco Holleman
 */
public final class Template {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(Template.class);

  /**
   * The name given to the root template: "{root}". Any {@code Template} that is
   * explicitly instantiated by calling one of the {@code fromXXX()} methods gets
   * this name.
   */
  public static final String ROOT_TEMPLATE_NAME = "{root}";

  /**
   * Parses the specified string into a {@code Template}. If the string contains any
   * {@code include} tags (like {@code ~%%include:/path/to/foo.html%%}), the path
   * will be interpreted as a file system resource. Templates created from a string
   * are never cached.
   *
   * @param source the source code for the {@code Template}
   * @return a new {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromString(String source) throws ParseException {
    Check.notNull(source, Tag.SOURCE);
    return new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, source).parse();
  }

  /**
   * Parses the specified string into a {@code Template}. The specified class will be
   * used to include other templates using {@code Class.getResourceAsStream()}.
   * Templates created from a string are never cached.
   *
   * @param clazz a {@code Class} object that provides access to the included
   *     template file by calling {@code getResourceAsStream} on it
   * @param source the source code for the {@code Template}
   * @return a {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromString(Class<?> clazz, String source)
      throws ParseException {
    Check.notNull(clazz, Tag.CLASS);
    Check.notNull(source, Tag.SOURCE);
    PathResolver resolver = new ClassPathResolver(clazz);
    TemplateLocation location = new TemplateLocation(resolver);
    return new Parser(location, ROOT_TEMPLATE_NAME, source).parse();
  }

  /**
   * Parses the specified resource into a {@code Template}. Templates created from a
   * classpath resource are always cached. Thus, calling this method multiple times
   * with the same {@code clazz} and {@code path} arguments will always return the
   * same instance. <b>Make sure the provided class is publicly accessible</b>.
   * Otherwise <i>Klojang Templates</i> cannot use it to open an {@code InputStream}
   * to the resource.
   *
   * @param clazz a {@code Class} object that provides access to the template
   *     file by calling {@code getResourceAsStream} on it
   * @param path the location of the template file
   * @return a {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromResource(Class<?> clazz, String path)
      throws ParseException {
    Check.notNull(clazz, Tag.CLASS);
    Check.that(path).has(clazz::getResource, notNull(), "No such resource: ${arg}");
    PathResolver resolver = new ClassPathResolver(clazz);
    TemplateLocation location = new TemplateLocation(path, resolver);
    return TemplateCache.INSTANCE.get(location, ROOT_TEMPLATE_NAME);
  }

  /**
   * Parses the specified file into a {@code Template}. Templates created from file
   * are always cached. Thus, calling this method multiple times with the same
   * {@code path} argument will always return the same instance.
   *
   * @param path the path of the file to be parsed
   * @return a {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromFile(String path) throws ParseException {
    Check.notNull(path).has(File::new, regularFile());
    TemplateLocation location = new TemplateLocation(path, new FilePathResolver());
    return TemplateCache.INSTANCE.get(location, ROOT_TEMPLATE_NAME);
  }

  /**
   * Creates a {@code Template} from the source provided by the specified
   * {@link PathResolver}. Templates created using a {@code PathResolver} are always
   * cached. Thus, calling this method multiple times with the same
   * {@code PathResolver} (as per its {@code equals()} method) and the same path will
   * always return the same instance.
   *
   * @param resolver the {@code PathResolver}
   * @param path the path to be resolved by the {@code PathResolver}
   * @return a {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromResolver(PathResolver resolver, String path)
      throws ParseException {
    Check.notNull(resolver, "resolver");
    Check.notNull(path, Tag.PATH);
    TemplateLocation location = new TemplateLocation(path, resolver);
    return TemplateCache.INSTANCE.get(location, ROOT_TEMPLATE_NAME);
  }

  private final String name;
  private final TemplateLocation location;
  private final List<Part> parts;
  private final Map<String, IntList> varIndices;
  private final IntList textIndices;
  private final Map<String, Integer> tmplIndices;
  // All variable names and nested template together
  private final List<String> names;

  private Template parent;

  Template(String name, TemplateLocation location, List<Part> parts) {
    parts.forEach(p -> {
      p.setParentTemplate(this);
      if (p instanceof NestedTemplatePart ntp) {
        ntp.getTemplate().parent = this;
      }
    });
    this.name = name;
    this.location = location;
    this.parts = parts;
    this.varIndices = getVarIndices(parts);
    this.tmplIndices = getTmplIndices(parts);
    this.names = getNames(parts);
    this.textIndices = getTextIndices(parts);
  }

  Template(Template cached, String name) {
    this.name = name;
    this.location = cached.location;
    this.parts = cached.parts;
    this.varIndices = cached.varIndices;
    this.tmplIndices = cached.tmplIndices;
    this.nestedTemplates = cached.nestedTemplates;
    this.names = cached.names;
    this.textIndices = cached.textIndices;
  }

  /**
   * Returns the name of this {@code Template}. It this {@code Template} was
   * explicitly instantiated using one of the {@code fromXXX()} methods, its name
   * will be "{root}"; otherwise it is a nested template and its name will be
   * extracted from the source code (e.g. {@code ~%%begin:foo%}).
   *
   * @return the name of this {@code Template}
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the template inside which this {@code Template} is nested. If this
   * {@code Template} is the root template (the template that was explicitly created
   * by a call to one of the {@code fromXXX()} methods), this method returns
   * {@code null}.
   *
   * @return the template inside which this {@code Template} is nested
   */
  public Template getParent() {
    return parent;
  }

  /**
   * Returns the root template of this (nested) {@code Template}. That is, the
   * {@code Template} that was explicitly created by a call to one of the
   * {@code fromXXX()} methods of this class. If this {@code Template} <i>is</i> the
   * root template, then this method returns {@code this}.
   *
   * @return the root template of this {@code Template}
   */
  public Template getRootTemplate() {
    if (parent == null) {
      return this;
    }
    Template t = parent;
    while (t.getParent() != null) {
      t = t.getParent();
    }
    return t;
  }

  /**
   * Returns an {@code Optional} containing the path to the source code of this
   * template, or an empty {@code Optional} if the template was
   * {@linkplain #fromString(String) created from a string}. In other words, for
   * {@code included} templates this method (by definition) returns a non-empty
   * {@code Optional}. For inline templates this method (by definition) returns an
   * empty {@code Optional}. For templates that were explicitly created using one of
   * the {@code fromXXX()} methods, the return value depends on whether it was
   * {@code fromString()} or one of the other {@code fromXXX()} methods.
   *
   * @return the path to the source code for this {@code Template}
   */
  public Optional<String> path() {
    return Optional.ofNullable(location.path());
  }

  /**
   * Returns the names of the variables in this {@code Template}, in order of their
   * first appearance in the template. The returned set only contains the names of
   * variables that reside <i>directly</i> inside this {@code Template}. Variables
   * inside nested templates are ignored. The returned {@code Set} is unmodifiable.
   *
   * @return the names of all variables in this {@code Template}
   */
  public Set<String> getVariables() {
    return varIndices.keySet();
  }

  /**
   * Returns all occurrences of all variables within this {@code Template}. Note that
   * a variable name may occur multiple times within the same template.
   *
   * @return all occurrences of all variables within this {@code Template}
   */
  public List<VariableOccurrence> getVariableOccurrences() {
    return parts.stream()
        .filter(VariablePart.class::isInstance)
        .map(VariablePart.class::cast)
        .map(VariablePart::toOccurrence)
        .collect(toList());
  }

  /**
   * Returns the total number of variables in this {@code Template}. Note that a
   * variable name may occur multiple times within the same template. This method
   * does not count the number of <i>unique</i> variable names. To get that number,
   * call {@link #getVariables() getVariables().size()}.
   *
   * @return the total number of variables in this {@code Template}
   */
  public int countVariableOccurrences() {
    return (int) parts.stream().filter(VariablePart.class::isInstance).count();
  }

  /**
   * Returns {@code true} if this {@code Template} <i>directly</i> contains a
   * variable with the specified name. Variables inside nested templates are not
   * considered.
   *
   * @param name the name of the variable
   * @return {@code true} if  this {@code Template} contains a variable with the
   *     specified name
   */
  public boolean hasVariable(String name) {
    return Check.notNull(name).ok(varIndices::containsKey);
  }

  private List<Template> nestedTemplates;

  /**
   * Returns all templates nested inside this {@code Template} (non-recursive). The
   * returned {@code List} is unmodifiable.
   *
   * @return all templates nested inside this {@code Template}
   */
  public List<Template> getNestedTemplates() {
    if (nestedTemplates == null) {
      return nestedTemplates = tmplIndices.values()
          .stream()
          .map(parts::get)
          .map(NestedTemplatePart.class::cast)
          .map(NestedTemplatePart::getTemplate)
          .collect(toUnmodifiableList());
    }
    return nestedTemplates;
  }

  /**
   * Returns the names of all templates nested inside this {@code Template}
   * (non-recursive). The returned {@code Set} is unmodifiable.
   *
   * @return the names of all nested templates
   */
  public Set<String> getNestedTemplateNames() {
    return tmplIndices.keySet();
  }

  /**
   * Returns {@code true} if this {@code Template} contains a nested template with
   * the specified name.
   *
   * @param name the name of the nested template
   * @return {@code true} if this {@code Template} contains a nested template with
   *     the specified name
   */
  public boolean hasNestedTemplate(String name) {
    return Check.notNull(name).ok(tmplIndices::containsKey);
  }

  /**
   * Returns the number of templates nested inside this {@code Template}
   * (non-recursive).
   *
   * @return the number of nested templates
   */
  public int countNestedTemplates() {
    return tmplIndices.size();
  }

  /**
   * Returns the nested template identified by the specified name. This method throws
   * an {@link IllegalArgumentException} if no nested template has the specified
   * name.
   *
   * @param name the name of a nested template
   * @return the {@code Template} with the specified name
   */
  public Template getNestedTemplate(String name) {
    Check.notNull(name).is(keyIn(), tmplIndices, ERR_NO_SUCH_TEMPLATE);
    int partIndex = tmplIndices.get(name);
    return ((NestedTemplatePart) parts.get(partIndex)).getTemplate();
  }

  /**
   * Returns the names of all variables and nested templates within this
   * {@code Template} (non-recursive). The returned {@code List} is unmodifiable.
   *
   * @return the names of all variables and nested templates in this {@code Template}
   */
  public List<String> getNames() {
    return names;
  }

  /**
   * Returns {@code true} if this is a text-only template. In other words, if this is
   * a template without any variables or nested templates.
   *
   * @return whether this is a text-only template
   */
  public boolean isTextOnly() {
    return names.isEmpty();
  }

  /**
   * Returns a {@code RenderSession} that can be used to populate and render this
   * {@code Template}. The {@code RenderSession} uses the
   * {@link AccessorRegistry#STANDARD_ACCESSORS predefined accessors} to extract
   * values from source data objects, and the
   * {@link StringifierRegistry#STANDARD_STRINGIFIERS predefined stringifiers} to
   * stringify those values.
   *
   * @return a {@code RenderSession}
   * @see AccessorRegistry#STANDARD_ACCESSORS
   * @see StringifierRegistry#STANDARD_STRINGIFIERS
   */
  public RenderSession newRenderSession() {
    return new SessionConfig(this).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} that can be used to populate and render this
   * {@code Template}. The {@code RenderSession} will use the
   * {@link AccessorRegistry#STANDARD_ACCESSORS predefined accessors} to extract
   * values from source data objects, and the specified {@code StringifierRegistry}
   * to stringify those values.
   *
   * @param stringifiers the {@code StringifierRegistry} used to supply the
   *     {@code RenderSession} with {@link Stringifier stringifiers}
   * @return a new {@code RenderSession}
   */
  public RenderSession newRenderSession(StringifierRegistry stringifiers) {
    Check.notNull(stringifiers, MTag.STRINGIFIERS);
    return new SessionConfig(this, stringifiers).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} that can be used to populate and render this
   * {@code Template}. The {@code RenderSession} will use the specified
   * {@code AccessorRegistry} to extract values from source data, and the
   * {@link StringifierRegistry predefined stringifiers} to stringify those values.
   *
   * @param accessors the {@code AccessorRegistry} used to supply the
   *     {@code RenderSession} with {@link Accessor accessors}
   * @return a new {@code RenderSession}
   */
  public RenderSession newRenderSession(AccessorRegistry accessors) {
    Check.notNull(accessors, MTag.ACCESSORS);
    return new SessionConfig(this, accessors).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} that you can use to populate and render this
   * {@code Template}. The {@code RenderSession} will use the specified
   * {@code AccessorRegistry} to extract values from source data, and the specified
   * {@code StringifierRegistry} to stringify those values.
   *
   * @param accessors the {@code AccessorRegistry} used to supply the
   *     {@code RenderSession} with {@link Accessor accessors}
   * @param stringifiers the {@code StringifierRegistry} used to supply the
   *     {@code RenderSession} with {@link Stringifier stringifiers}
   * @return a new {@code RenderSession}
   */
  public RenderSession newRenderSession(AccessorRegistry accessors,
      StringifierRegistry stringifiers) {
    Check.notNull(accessors, MTag.ACCESSORS);
    Check.notNull(stringifiers, MTag.STRINGIFIERS);
    return new SessionConfig(this, accessors, stringifiers).newRenderSession();
  }

  /**
   * Determines whether this template is equal to the specified object. Two templates
   * are equals if they were created from the same {@linkplain #path() path} and
   * {@link PathResolver}. {@code Template} instances that were created
   * {@linkplain #fromString(String) from a string} are never equal to any other
   * {@code Template} instance (except themselves).
   *
   * @param obj the object to compare this template with
   * @return whether this template is equal to the specified object
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return !location.isString()
        && obj instanceof Template other
        && location.equals(other.location);
  }

  /**
   * Returns the hash code of this {@code Template}.
   *
   * @return the hash code of this {@code Template}
   */
  @Override
  public int hashCode() {
    return location.hashCode();
  }

  /**
   * More or less re-assembles to source code from the constituent parts of the
   * {@code Template}. Note, however, that ditch blocks are ditched early on in the
   * parsing process and there is no trace of them left in the resulting
   * {@code Template} instance.
   */
  @Override
  public String toString() {
    return implode(parts, "");
  }

  /* +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ */
  /* ++++++++++++++++++++++ END OF PUBLIC INTERFACE ++++++++++++++++++++++ */
  /* +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ */

  List<Part> parts() {
    return parts;
  }

  /*
   * Maps variable names to the indices of the parts that contain them
   */
  Map<String, IntList> variables() {
    return varIndices;
  }

  private static Map<String, IntList> getVarIndices(List<Part> parts) {
    Map<String, IntList> indices = new LinkedHashMap<>();
    for (int i = 0; i < parts.size(); ++i) {
      if (parts.get(i) instanceof VariablePart vp) {
        indices.computeIfAbsent(vp.getName(), k -> new IntArrayList()).add(i);
      }
    }
    indices.entrySet().forEach(e -> e.setValue(IntList.copyOf(e.getValue())));
    return Collections.unmodifiableMap(indices);
  }

  private static Map<String, Integer> getTmplIndices(List<Part> parts) {
    Map<String, Integer> indices = new LinkedHashMap<>();
    for (int i = 0; i < parts.size(); ++i) {
      if (parts.get(i) instanceof NestedTemplatePart ntp) {
        indices.put(ntp.getName(), i);
      }
    }
    return Collections.unmodifiableMap(indices);
  }

  private static List<String> getNames(List<Part> parts) {
    return parts.stream()
        .filter(NamedPart.class::isInstance)
        .map(NamedPart.class::cast)
        .map(NamedPart::getName)
        .collect(toUnmodifiableList());
  }

  private static IntList getTextIndices(List<Part> parts) {
    IntArrayList indices = new IntArrayList();
    for (int i = 0; i < parts.size(); ++i) {
      if (parts.get(i).getClass() == TextPart.class) {
        indices.add(i);
      }
    }
    return IntList.copyOf(indices);
  }

}
