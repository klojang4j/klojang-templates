package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.templates.x.MTag;
import org.klojang.templates.x.Messages;
import org.klojang.util.Tuple2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
public final class TemplateUtils {

  private TemplateUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the fully-qualified name of the specified template, relative to the root
   * template. If the template <i>is</i> the root template,
   * {@link Template#ROOT_TEMPLATE_NAME} is returned. The fully-qualified name is a
   * dot-separated concatenation of name segments, with each subsequent name
   * representing a template at the next nesting level.
   *
   * @param template the template for which to retrieve the fully-qualified name
   * @return the fully-qualified name of the template
   */
  public static String getFQName(Template template) {
    Check.notNull(template);
    if (template.getParent() == null) {
      return template.getName();
    }
    int sz = 0;
    ArrayList<String> chunks = new ArrayList<>(5);
    for (Template t = template; t.getParent() != null; t = t.getParent()) {
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
   * specified template. The provided name supposedly is the name of a variable or
   * nested template. The fully-qualified name is a dot-separated concatenation of
   * name segments, with each subsequent name representing a template at the next
   * nesting level. The last segment of the fully-qualified name will be the
   * specified name itself. If the specified template is the root template
   * ({@code template.getParent()} equals {@code null}}, the name is returned as-is.
   *
   * @param template the template relative to which to get the fully-qualified
   *     name
   * @param name the name of a template variable or nested template
   * @return its fully-qualified name
   */
  public static String getFQName(Template template, String name) {
    Check.notNull(template, MTag.TEMPLATE);
    Check.notNull(name, Tag.NAME);
    if (template.getParent() == null) {
      return name;
    }
    int sz = name.length();
    ArrayList<String> chunks = new ArrayList<>(5);
    chunks.add(name);
    for (Template t = template; t.getParent() != null; t = t.getParent()) {
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
   * Returns a depth-first view of all variable occurrences within the specified
   * template.
   *
   * @param template the template to collect the variable occurrences from
   * @return a depth-first view of all variable occurrences within the specified
   *     template
   */
  public static List<VariableOccurrence> getAllVariableOccurrences(Template template) {
    Check.notNull(template);
    ArrayList<VariableOccurrence> list = new ArrayList<>();
    collectOccurences(template, list);
    return list;
  }

  public static void collectOccurences(Template template,
      ArrayList<VariableOccurrence> list) {
    for (Part part : template.parts()) {
      if (part instanceof VariablePart vp) {
        list.add(vp.toOccurrence());
      } else if (part instanceof NestedTemplatePart ntp) {
        collectOccurences(ntp.getTemplate(), list);
      }
    }
  }

  /**
   * Returns the specified template and all templates descending from it. The
   * specified template will come first in de returned list and the descendant
   * templates are retrieved in breadth-first order. The returned {@code List} is
   * created on demand and modifiable.
   *
   * @param template the template
   * @return a {@code List} containing the {@code Template} and its descendants
   */
  public static List<Template> getTemplateHierarchy(Template template) {
    Check.notNull(template);
    ArrayList<Template> tmpls = new ArrayList<>();
    tmpls.add(template);
    collectTemplates(template, tmpls);
    return tmpls;
  }

  private static void collectTemplates(Template t0, ArrayList<Template> tmpls) {
    tmpls.addAll(t0.getNestedTemplates());
    t0.getNestedTemplates().forEach(t -> collectTemplates(t, tmpls));
  }

  /**
   * Returns the nested template corresponding to the specified fully-qualified name.
   * Contrary to
   * {@link Template#getNestedTemplate(String) Template.getNestedTemplate()} this
   * method lets you retrieve nested templates at any depth (nesting level). The
   * fully-qualified name must be relative to the specified template and must not
   * start with the specified template's name itself.
   *
   * @param template the template containing the (deeply) nested template
   * @param fqName the fully qualified name of the nested template
   * @return The (possibly deeply) nested template corresponding to the specified
   *     fully-qualified name
   */
  public static Template getNestedTemplate(Template template, String fqName) {
    Check.notNull(template, MTag.TEMPLATE);
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
   * Returns the template that directly contains the variable or nested template
   * denoted by the specified fully-qualified name. The specified template is
   * supposed to be the root template, or a template at some higher level than the
   * template containing the variable. The FQName must be relative to the specified
   * template and it must not include the specified template's name.
   *
   * @param template the template relative to which the fully-qualified name
   *     should be taken
   * @param fqName the fully-qualified name
   * @return The template containing the variable or nested template denoted by the
   *     specified fully-qualified name
   */
  public static Template getContainingTemplate(Template template, String fqName) {
    Check.notNull(template, MTag.TEMPLATE);
    Check.that(fqName, "fqName").isNot(empty());
    if (count(fqName, ".") == 0) {
      Check.that(fqName).is(in(),
          template.getNames(),
          Messages.ERR_BAD_NAME,
          fqName);
      return template;
    }
    return getNestedTemplate(template, substringBefore(fqName, ".", 1));
  }

  /**
   * Returns the names of all variables in the specified template, <i>and</i> in all
   * templates descending from the specified template. Each tuple in the returned
   * {@code List} contains a {@code Template} instance and a variable name. The
   * returned {@code List} is created on demand and modifiable.
   *
   * @param template the template for which to retrieve the variable names
   * @return all variable names in this {@code Template} and the templates nested
   *     inside it
   */
  public static List<Tuple2<Template, String>> getVarsPerTemplate(Template template) {
    Check.notNull(template, MTag.TEMPLATE);
    ArrayList<Tuple2<Template, String>> tuples = new ArrayList<>(25);
    collectVarsPerTemplate(template, tuples);
    return tuples;
  }

  private static void collectVarsPerTemplate(Template t0,
      ArrayList<Tuple2<Template, String>> tuples) {
    t0.getVariables().stream().map(s -> Tuple2.of(t0, s)).forEach(tuples::add);
    t0.getNestedTemplates().forEach(t -> collectVarsPerTemplate(t, tuples));
  }

  /**
   * Prints out the constituent parts of the specified {@code Template}. Can be used
   * for debugging purposes.
   *
   * @param template the {@code Template} whose parts to print
   * @param out the {@code PrintStream} to which to print
   */
  public static void printParts(Template template, PrintStream out) {
    new PartsPrinter(template).printParts(out);
  }

}
