package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.x.ClassPathResolver;
import org.klojang.templates.x.FilePathResolver;
import org.klojang.templates.x.MTag;
import org.klojang.templates.x.parse.*;
import org.klojang.util.collection.IntArrayList;
import org.klojang.util.collection.IntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.templates.TemplateUtils.getFQName;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_TEMPLATE;
import static org.klojang.util.CollectionMethods.implode;
import static org.klojang.util.ObjectMethods.ifNotNull;

/**
 * The {@code Template} class is responsible for loading and parsing templates and
 * functions as a factory for {@link RenderSession} objects that let you render them.
 * The {@code Template} class and the {@code RenderSession} class are the two central
 * classes of the Klojang library. {@code Template} instances are unmodifiable,
 * expensive-to-create and heavy-weight objects. Generally though you should not
 * cache them as this is already done by Klojang. You can disable template caching by
 * means of a system property. See {@link Setting#TMPL_CACHE_SIZE}. This is useful
 * during development, when you want the template to be re-loaded and re-parsed with
 * every refresh of the browser.
 *
 * @author Ayco Holleman
 */
public final class Template {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(Template.class);

  /**
   * The name given to the root template: "{root}". Any {@code Template} that is
   * explicitly instantiated by calling one of the {@code fromXXX()} methods gets
   * this name. Templates nested inside this template get their name from the source
   * code (for example: {@code ~%%begin:foo%} or {@code ~%%include:/views/foo.html%}
   * or {@code ~%%include:foo:/views/bar.html%}).
   */
  public static final String ROOT_TEMPLATE_NAME = "{root}";

  /**
   * Parses the specified string into a {@code Template} instance. If the string
   * contains any {@code include} declarations (like
   * {@code ~%%include:/path/to/foo.html%}), the path will be interpreted as a file
   * system resource. Templates created from a string are never cached.
   *
   * @param source The source code for the {@code Template}
   * @return a new {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromString(String source) throws ParseException {
    Check.notNull(source, Tag.SOURCE);
    return new Parser(TemplateLocation.STRING, ROOT_TEMPLATE_NAME, source).parse();
  }

  /**
   * Parses the specified string into a {@code Template} instance. The specified
   * class will be used to include other templates using
   * {@code clazz.getResourceAsStream("/path/to/template")}. Templates created from a
   * string are never cached.
   *
   * @param clazz Any {@code Class} object that provides access to the included
   *     template files by calling {@code getResourceAsStream} on it
   * @param source The source code for the {@code Template}
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
   * Parses the specified resource into a {@code Template} instance. The resource is
   * read using {@code clazz.getResourceAsStream(path)}. Any included templates will
   * be loaded this way as well. Templates created from a classpath resource are
   * always cached. Thus, calling this method multiple times with the same
   * {@code clazz} and {@code path} arguments will always return the same
   * {@code Template} instance. (More precisely: the cache key is the combination of
   * {@code clazz.getPackage()} and {@code path}.)
   *
   * @param clazz Any {@code Class} object that provides access to the template
   *     file by calling {@code getResourceAsStream} on it
   * @param path The location of the template file
   * @return a {@code Template} instance
   * @throws ParseException if the template source contains a syntax error
   */
  public static Template fromResource(Class<?> clazz, String path)
      throws ParseException {
    Check.notNull(clazz, Tag.CLASS);
    Check.notNull(path).is(resourceOf(), clazz);
    PathResolver resolver = new ClassPathResolver(clazz);
    TemplateLocation location = new TemplateLocation(path, resolver);
    return TemplateCache.INSTANCE.get(location, ROOT_TEMPLATE_NAME);
  }

  /**
   * Parses the specified file into a {@code Template} instance. Templates created
   * from file are always cached. Thus, calling this method multiple times with the
   * same {@code path} argument will always return the same {@code Template}
   * instance.
   *
   * @param path The path of the file to be parsed
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
   * {@link PathResolver}.
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
   * Returns the ultimate ancestor of this {@code Template}. In other words, the
   * {@code Template} that was explicitly created by a call to one of the
   * {@code fromXXX()} methods of this class. If this {@code Template} <i>is</i> the
   * root template, then this method returns {@code this}.
   *
   * @return the ultimate ancestor of this {@code Template}
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
   * If this {@code Template} was created from a file or classpath resource, this
   * method returns its path, else {@code null}. In other words, for {@code included}
   * templates this method (by definition) returns a non-null value. For inline
   * templates this method (by definition) returns {@code null}. For <i>this</i>
   * {@code Template} the return value depends on how you got hold of the instance.
   *
   * @return the file location (if any) of the source code for this {@code Template}
   */
  public String getPath() {
    return ifNotNull(location, TemplateLocation::getPath);
  }

  /**
   * Returns the names of all variables in this {@code Template} (non-recursive), in
   * order of their first appearance in the template. The returned {@code Set} is
   * unmodifiable.
   *
   * @return the names of all variables in this {@code Template}
   */
  public Set<String> getVariables() {
    return varIndices.keySet();
  }

  /**
   * Returns {@code true} if this {@code Template} contains a variable with the
   * specified name.
   *
   * @param name the name of the variable
   * @return {@code true} if  this {@code Template} contains a variable with the
   *     specified name
   */
  public boolean hasVariable(String name) {
    return Check.notNull(name).ok(varIndices::containsKey);
  }

  /**
   * Returns the total number of variables in this {@code Template}. Note that one
   * variable name may occur multiple times within the same template. This method
   * does not count the number of <i>unique</i> variable names (which would be
   * {@link #getVariables() getVars().size()}).
   *
   * @return the total number of variables in this {@code Template}
   */
  public int countVariables() {
    return (int) parts.stream().filter(VariablePart.class::isInstance).count();
  }

  private List<Template> nestedTemplates;

  /**
   * Returns all templates nested inside this {@code Template} (non-recursive). The
   * returned {@code List} is unmodifiable.
   *
   * @return All templates nested inside this {@code Template}
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
    return Check.notNull(name).ok(varIndices::containsKey);
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
    Check.notNull(name).is(keyIn(), tmplIndices, ERR_NO_SUCH_TEMPLATE, name);
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
   * @return Whether this is a text-only template
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
   * @return A {@code RenderSession}
   * @see AccessorRegistry#STANDARD_ACCESSORS
   * @see StringifierRegistry#STANDARD_STRINGIFIERS
   */
  public RenderSession newRenderSession() {
    return new SessionConfig(this).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} with which populate and render this
   * {@code Template}. The {@code RenderSession} will use the
   * {@link AccessorRegistry#STANDARD_ACCESSORS predefined accessors} to extract
   * values from source data objects, and the specified {@code StringifierRegistry}
   * to stringify those values.
   *
   * @param stringifiers The {@code StringifierRegistry} used to supply the
   *     {@code RenderSession} with {@link Stringifier stringifiers}
   * @return A new {@code RenderSession}
   */
  public RenderSession newRenderSession(StringifierRegistry stringifiers) {
    Check.notNull(stringifiers, MTag.STRINGIFIERS);
    return new SessionConfig(this, stringifiers).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} with which populate and render this
   * {@code Template}. The {@code RenderSession} that will use the specified
   * {@code AccessorRegistry} to extract values from source data, and the
   * {@link StringifierRegistry predefined stringifiers} to stringify those values.
   *
   * @param accessors The {@code AccessorRegistry} used to supply the
   *     {@code RenderSession} with {@link Accessor accessors}
   * @return A new {@code RenderSession}
   */
  public RenderSession newRenderSession(AccessorRegistry accessors) {
    Check.notNull(accessors, MTag.ACCESSORS);
    return new SessionConfig(this, accessors).newRenderSession();
  }

  /**
   * Returns a {@code RenderSession} with which populate and render this
   * {@code Template}. The {@code RenderSession} that will use the specified
   * {@code AccessorRegistry} to extract values from source data, and the specified
   * {@code StringifierRegistry} to stringify those values.
   *
   * @param accessors The {@code AccessorRegistry} used to supply the
   *     {@code RenderSession} with {@link Accessor accessors}
   * @param stringifiers The {@code StringifierRegistry} used to supply the
   *     {@code RenderSession} with {@link Stringifier stringifiers}
   * @return A new {@code RenderSession}
   */
  public RenderSession newRenderSession(AccessorRegistry accessors,
      StringifierRegistry stringifiers) {
    Check.notNull(accessors, MTag.ACCESSORS);
    Check.notNull(stringifiers, MTag.STRINGIFIERS);
    return new SessionConfig(this, accessors, stringifiers).newRenderSession();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Template other) {
      if (location.isString()) {
        return parent != null
            && getRootTemplate().equals(other.getRootTemplate())
            && getFQName(this).equals(getFQName(other));
      }
      return location.equals(other.location);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return location.hashCode();
  }

  /**
   * More or less re-assembles to source code from the constituent parts of the
   * {@code Template}. Note, however, that ditch block are ditched early on in the
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

  List<Part> getParts() {
    return parts;
  }

  @SuppressWarnings("unchecked")
  <T extends Part> T getPart(int index) {
    return (T) Check.that(index).is(indexOf(), parts).mapToObj(parts::get);
  }

  Map<String, IntList> getVarPartIndices() {
    return varIndices;
  }

  Map<String, Integer> getTemplatePartIndices() {
    return tmplIndices;
  }

  IntList getTextPartIndices() {
    return textIndices;
  }

  private static Map<String, IntList> getVarIndices(List<Part> parts) {
    Map<String, IntList> indices = new LinkedHashMap<>();
    for (int i = 0; i < parts.size(); ++i) {
      if (parts.get(i).getClass() == VariablePart.class) {
        String name = ((VariablePart) parts.get(i)).getName();
        indices.computeIfAbsent(name, k -> new IntArrayList()).add(i);
      }
    }
    indices.entrySet().forEach(e -> e.setValue(IntList.copyOf(e.getValue())));
    return Collections.unmodifiableMap(indices);
  }

  private static Map<String, Integer> getTmplIndices(List<Part> parts) {
    Map<String, Integer> indices = new LinkedHashMap<>();
    for (int i = 0; i < parts.size(); ++i) {
      if (parts.get(i) instanceof NestedTemplatePart) {
        String name = ((NestedTemplatePart) parts.get(i)).getName();
        indices.put(name, i);
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
