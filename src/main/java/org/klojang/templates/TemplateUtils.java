package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.templates.x.Messages;
import org.klojang.templates.x.parse.Part;
import org.klojang.templates.x.parse.VariablePart;
import org.klojang.util.LaxTuple2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.copyOfRange;
import static org.klojang.check.CommonChecks.empty;
import static org.klojang.check.CommonChecks.in;
import static org.klojang.util.StringMethods.count;
import static org.klojang.util.StringMethods.substringBefore;

/**
 * Utility class extending the functionality of the {@link Template} class.
 *
 * @author Ayco Holleman
 */
public class TemplateUtils {

  private TemplateUtils() {}

  /**
   * Returns the fully-qualified name of the specified template, relative to the root
   * template. If the template <i>is</i> the root template,
   * {@link Template#ROOT_TEMPLATE_NAME} is returned. The fully-qualified name is a
   * dot-separated concatenation of template names, with each subsequent name
   * representing a template at the next nesting level.
   *
   * @param template
   * @return
   */
  public static String getFQName(Template template) {
    Check.notNull(template);
    if (template.getParent() == null) {
      return template.getName();
    }
    int sz = 0;
    ArrayList<String> chunks = new ArrayList<>(5);
    for (Template t = template;
        t != null && t.getParent() != null;
        t = t.getParent()) {
      chunks.add(t.getName());
      sz += t.getName().length() + 1;
    }
    StringBuilder sb = new StringBuilder(sz);
    for (int i = chunks.size() - 1; i >= 0; --i) {
      if (sb.length() != 0) {
        sb.append('.');
      }
      sb.append(chunks.get(i));
    }
    return sb.toString();
  }

  /**
   * Returns the fully-qualified name of the specified name, relative to the
   * specified template. The fully-qualified name is a dot-separated concatenation of
   * template names, with each subsequent name representing a template at the next
   * nesting level. The last segment of the fully-qualified name will be the
   * specified name itself.
   *
   * @param template
   * @param name
   * @return
   */
  public static String getFQName(Template template, String name) {
    Check.notNull(template, "template");
    Check.notNull(name, "name");
    int sz = name.length();
    ArrayList<String> chunks = new ArrayList<>(5);
    chunks.add(name);
    for (Template t = template;
        t != null && t.getParent() != null;
        t = t.getParent()) {
      chunks.add(t.getName());
      sz += t.getName().length() + 1;
    }
    StringBuilder sb = new StringBuilder(sz);
    for (int i = chunks.size() - 1; i >= 0; --i) {
      if (sb.length() != 0) {
        sb.append('.');
      }
      sb.append(chunks.get(i));
    }
    return sb.toString();
  }

  /**
   * Returns the names of all variables and all nested templates within the specified
   * template and all templates descending from it. The returned {@code Set} is
   * created on demand and modifiable.
   *
   * @param template The {@code Template} to extract the names from
   * @return The names of all variables and nested templates within the specified
   *     template and all templates descending from it
   */
  public static Set<String> getAllNames(Template template) {
    Check.notNull(template, "template");
    Set<String> names = new HashSet<>();
    collectNames(template, names);
    return names;
  }

  private static void collectNames(Template template, Set<String> names) {
    names.addAll(template.getNames());
    for (Template t : template.getNestedTemplates()) {
      collectNames(t, names);
    }
  }

  /**
   * Returns a {@code List} containing the specified {@code Template} and all
   * templates descending from it. The specified {@code Template} will be the first
   * element of the {@code List}. The {@code List} is created on demand and
   * modifiable.
   *
   * @return A {@code List} containing the {@code Template} and all templates
   *     descending from it
   */
  public static List<Template> getTemplateHierarchy(Template template) {
    Check.notNull(template, "template");
    ArrayList<Template> tmpls = new ArrayList<>(20);
    tmpls.add(template);
    collectTemplates(template, tmpls);
    return tmpls;
  }

  private static void collectTemplates(Template t0, ArrayList<Template> tmpls) {
    List<Template> myTmpls = t0.getNestedTemplates();
    tmpls.addAll(myTmpls);
    myTmpls.forEach(t -> collectTemplates(t, tmpls));
  }

  /**
   * Returns the nested template corresponding to the specified fully-qualified name.
   * Contrary to
   * {@link Template#getNestedTemplate(String) Template.getNestedTemplate} this
   * method lets you retrieve nested templates at any depth (nesting level). The
   * fully-qualified name must be relative to the specified template and must not
   * start with the specified template's name itself.
   *
   * @param template The template containing the (deeply) nested template
   * @param fqName The fully qualified name of the nested template
   * @return The (possibly deeply) nested template corresponding to the specified
   *     fully-qualified name
   */
  public static Template getNestedTemplate(Template template, String fqName) {
    Check.notNull(template, "template");
    Check.that(fqName, "fqName").isNot(empty());
    return getNestedTemplate(template, fqName, fqName.split("\\."));
  }

  private static Template getNestedTemplate(Template t0,
      String fqName,
      String[] names) {
    if (names.length == 0) {
      return t0;
    }
    Check.that(names[0]).is(in(),
        t0.getNestedTemplateNames(),
        Messages.ERR_NO_SUCH_TEMPLATE,
        fqName);
    t0 = t0.getNestedTemplate(names[0]);
    return getNestedTemplate(t0, fqName, copyOfRange(names, 1, names.length));
  }

  /**
   * Returns the template containing the variable or nested template denoted by the
   * specified fully-qualified name, relative to the specified template.
   *
   * @param template The template relative to which the fully-qualified name
   *     should be taken
   * @param fqName The fully-qualified name
   * @return The template containing the variable or nested template denoted by the
   *     specified fully-qualified name
   */
  public static Template getParentTemplate(Template template, String fqName) {
    Check.notNull(template, "template");
    Check.that(fqName, "fqName").isNot(empty());
    if (count(fqName, ".") == 1) {
      Check.that(fqName).is(in(),
          template.getNames(),
          Messages.ERR_BAD_NAME,
          fqName);
      return template;
    }
    return getNestedTemplate(template, substringBefore(fqName, ".", 1));
  }

  /**
   * Returns, for this {@code Template} and all templates descending from it, the
   * names of their variables. Each tuple in the returned {@code List} contains a
   * {@code Template} instance and a variable name. The returned {@code List} is
   * created on demand and modifiable.
   *
   * @return All variable names in this {@code Template} and the templates nested
   *     inside it
   */
  public static List<LaxTuple2<Template, String>> getVarsPerTemplate(Template template) {
    Check.notNull(template, "template");
    ArrayList<LaxTuple2<Template, String>> tuples = new ArrayList<>(25);
    collectVarsPerTemplate(template, tuples);
    return tuples;
  }

  private static void collectVarsPerTemplate(
      Template t0, ArrayList<LaxTuple2<Template, String>> tuples) {
    t0.getVariables().stream().map(s -> LaxTuple2.of(t0, s)).forEach(tuples::add);
    t0.getNestedTemplates().forEach(t -> collectVarsPerTemplate(t, tuples));
  }

  /**
   * Returns all instances of variables within the specified template and with the
   * specified prefix.
   *
   * @param template The {@code Template} in which to search
   * @param prefix The variable prefix (a.k.a. the variable group)
   * @return All instances of variables with the specified name and prefix
   */
  public static List<VariablePart> getVariableInstances(Template template,
      String prefix) {
    Check.notNull(template, "template");
    Check.notNull(prefix, "prefix");
    List<VariablePart> vars = new ArrayList<>(template.getParts().size());
    for (Part p : template.getParts()) {
      if (p.getClass() == VariablePart.class) {
        VariablePart vp = (VariablePart) p;
        if (vp.getVarGroup().isPresent() && vp.getVarGroup().get().getName().equals(
            prefix)) {
          vars.add(vp);
        }
      }
    }
    return vars;
  }

  /**
   * Returns all instances of variables within the specified template, with the
   * specified name, and with the specified prefix.
   *
   * @param template The {@code Template} in which to search
   * @param prefix The variable prefix (a.k.a. the variable group)
   * @param varName The variableName
   * @return All instances of variables with the specified name and prefix
   */
  public static List<VariablePart> getVariableInstances(
      Template template, String prefix, String varName) {
    Check.notNull(template, "template");
    Check.notNull(prefix, "prefix");
    Check.notNull(varName, "varName");
    List<VariablePart> vars = new ArrayList<>(template.getParts().size());
    for (Part p : template.getParts()) {
      if (p.getClass() == VariablePart.class) {
        VariablePart vp = (VariablePart) p;
        if (vp.getName().equals(varName)) {
          if (vp.getVarGroup().isPresent() && vp.getVarGroup()
              .get()
              .getName()
              .equals(prefix)) {
            vars.add(vp);
          }
        }
      }
    }
    return vars;
  }

  /**
   * Prints out the constituent parts of the specified {@code Template}. Can be used
   * for debugging purposes.
   *
   * @param template The {@code Template} whose parts to print
   * @param out The {@code PrintStream} to which to print
   */
  public static void printParts(Template template, PrintStream out) {
    new PartsPrinter(template).printParts(out);
  }

}
