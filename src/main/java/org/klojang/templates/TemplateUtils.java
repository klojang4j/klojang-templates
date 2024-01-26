package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.check.Tag;
import org.klojang.collections.WiredList;
import org.klojang.path.Path;
import org.klojang.templates.x.MTag;
import org.klojang.util.Tuple2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toList;
import static org.klojang.check.CommonChecks.empty;
import static org.klojang.check.CommonChecks.in;
import static org.klojang.templates.x.Messages.ERR_BAD_NAME;
import static org.klojang.templates.x.Messages.ERR_NO_SUCH_TEMPLATE;
import static org.klojang.util.CollectionMethods.implode;
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
   * {@link Template#ROOT_TEMPLATE_NAME} is returned. Otherwise the fully-qualified name
   * is the {@linkplain org.klojang.path.Path path} from the specified template to the
   * root template, in reverse direction.
   *
   * <blockquote><pre>{@code
   * String src = """
   *      ~%%begin:companies%
   *          ~%foo%
   *          ~%%begin:departments%
   *              ~%bar%
   *          ~%%end:departments%
   *      ~%%end:companies%
   *      """;
   * Template t = Template.fromString(src);
   * System.out.println(TemplateUtils.getFQN(t); // "{root}"
   * t = t.getNestedTemplate("companies");
   * System.out.println(TemplateUtils.getFQN(t); // "companies"
   * t = t.getNestedTemplate("departments");
   * System.out.println(TemplateUtils.getFQN(t); // "companies.departments"
   * }</pre></blockquote>
   *
   * @param template the template for which to retrieve the fully-qualified name
   * @return the fully-qualified name of the template
   */
  public static String getFQN(Template template) {
    Check.notNull(template);
    if (template.getParent() == null) {
      return template.getName();
    }
    WiredList<String> wl = new WiredList<>();
    for (Template t = template; t.getParent() != null; t = t.getParent()) {
      wl.prepend(t.getName());
    }
    return implode(wl, ".");
  }

  /**
   * Returns the fully-qualified name of the specified name. The provided name supposedly
   * is the name of a variable or nested template inside the specified template. If the
   * specified template is the root template, this method simply returns the name as-is.
   * Otherwise it prefixes the template's FQN (and a dot character) to the name.
   *
   * @param template the template for which to retrieve the fully-qualified name
   * @param name the name of a template variable or nested template
   * @return its fully-qualified name
   */
  public static String getFQN(Template template, String name) {
    Check.notNull(template, MTag.TEMPLATE);
    Check.notNull(name, Tag.NAME);
    return (template.getParent() == null) ? name : getFQN(template) + '.' + name;
  }

  /**
   * Returns a depth-first view of all variable occurrences within the specified template.
   * The returned {@code List} is created on demand and modifiable.
   *
   * @param template the template to collect the variable occurrences from
   * @return a depth-first view of all variable occurrences within the specified template
   */
  public static List<VariableOccurrence> getAllVariableOccurrences(Template template) {
    Check.notNull(template);
    ArrayList<VariableOccurrence> list = new ArrayList<>();
    collectOccurrences(template, list);
    return list;
  }

  private static void collectOccurrences(Template template,
        ArrayList<VariableOccurrence> list) {
    for (Part part : template.parts()) {
      if (part instanceof VariablePart vp) {
        list.add(vp.toOccurrence());
      } else if (part instanceof NestedTemplatePart ntp) {
        collectOccurrences(ntp.getTemplate(), list);
      }
    }
  }

  /**
   * Returns the specified template and all templates descending from it. The specified
   * template will come first in de returned list and the descendant templates are
   * retrieved in breadth-first order. The returned {@code List} is created on demand and
   * modifiable.
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
   * Contrary to {@link Template#getNestedTemplate(String) Template.getNestedTemplate()}
   * this method lets you retrieve deeply nested templates. The fully-qualified name must
   * be relative to the specified template and must not start with the specified
   * template's name itself.
   *
   * @param template the template containing the (deeply) nested template
   * @param FQN the fully qualified name of the nested template
   * @return The (possibly deeply) nested template corresponding to the specified
   *       fully-qualified name
   */
  public static Template getNestedTemplate(Template template, String FQN) {
    Check.notNull(template, MTag.TEMPLATE);
    Check.that(FQN, MTag.FQN).isNot(empty());
    return getNestedTemplate(template, FQN, FQN.split("\\."));
  }

  private static Template getNestedTemplate(Template t0,
        String FQN,
        String[] names) {
    if (names.length == 0) {
      return t0;
    }
    Check.that(names[0]).is(in(), t0.getNestedTemplateNames(),
          ERR_NO_SUCH_TEMPLATE, FQN);
    t0 = t0.getNestedTemplate(names[0]);
    return getNestedTemplate(t0, FQN, copyOfRange(names, 1, names.length));
  }

  /**
   * Returns the template that directly contains the variable or nested template denoted
   * by the specified fully-qualified name. The specified template is supposed to be the
   * root template, or a template at some higher level than the template containing the
   * variable. The FQName must be relative to the specified template and it must not
   * include the specified template's name.
   *
   * @param template the template relative to which the fully-qualified name should
   *       be taken
   * @param FQN the fully-qualified name
   * @return The template containing the variable or nested template denoted by the
   *       specified fully-qualified name
   */
  public static Template getContainingTemplate(Template template, String FQN) {
    Check.notNull(template, MTag.TEMPLATE);
    Check.that(FQN, MTag.FQN).isNot(empty());
    if (count(FQN, ".") == 0) {
      Check.that(FQN).is(in(), template.getNames(), ERR_BAD_NAME, FQN);
      return template;
    }
    return getNestedTemplate(template, substringBefore(FQN, ".", 1));
  }

  /**
   * Returns the names of all variables in the specified template and all templates
   * descending from the specified template. Each tuple in the returned {@code List}
   * contains a {@code Template} instance and a variable name. The {@code Template}
   * instance is either the specified template itself or one of its descendants. The
   * returned {@code List} is created on demand and modifiable.
   *
   * @param template the template for which to retrieve the variable names
   * @return all variable names in this {@code Template} and the templates nested inside
   *       it
   */
  public static List<Tuple2<Template, String>> getAllVariables(Template template) {
    Check.notNull(template, MTag.TEMPLATE);
    ArrayList<Tuple2<Template, String>> tuples = new ArrayList<>();
    collectVars(template, tuples);
    return tuples;
  }

  private static void collectVars(Template t0,
        ArrayList<Tuple2<Template, String>> tuples) {
    t0.getVariables().stream().map(s -> Tuple2.of(t0, s)).forEach(tuples::add);
    t0.getNestedTemplates().forEach(t -> collectVars(t, tuples));
  }

  /**
   * Returns the fully-qualified names of all variables in the specified template and all
   * templates descending from the specified template. The names are fully-qualified
   * relative to the root template of the specified template (not to the specified
   * template itself).
   *
   * @param template the template for which to retrieve the variable names
   * @return the fully-qualified variable names in this {@code Template} and the templates
   *       nested inside it
   */
  public static List<String> getAllVariableFQNames(Template template) {
    Check.notNull(template, MTag.TEMPLATE);
    ArrayList<String> fqns = new ArrayList<>();
    collectFQNs(template, fqns);
    return fqns;
  }

  static void collectFQNs(Template t0, ArrayList<String> vars) {
    t0.getVariables().stream().map(s -> getFQN(t0, s)).forEach(vars::add);
    t0.getNestedTemplates().forEach(t -> collectFQNs(t, vars));
  }

  /**
   * Returns the fully-qualified names of all variables in the specified template and all
   * templates descending from the specified template.
   *
   * @param template the template for which to retrieve the variable names
   * @param relative if {@code true}, the names are fully-qualified relative to the
   *       specified template, otherwise relative to the root template of the specified
   *       template
   * @return the fully-qualified variable names in this {@code Template} and the templates
   *       nested inside it
   */
  public static List<String> getAllVariableFQNames(Template template,
        boolean relative) {
    if (relative) {
      Check.notNull(template, MTag.TEMPLATE);
      ArrayList<Path> paths = new ArrayList<>();
      collectFQNs(template, paths, Path.empty());
      return paths.stream().map(Path::toString).collect(toList());
    }
    return getAllVariableFQNames(template);
  }

  static void collectFQNs(Template t0, ArrayList<Path> vars, Path path) {
    t0.getVariables().stream().map(path::append).forEach(vars::add);
    t0.getNestedTemplates().forEach(
          t -> collectFQNs(t, vars, path.append(t.getName())));
  }

  /**
   * Prints out the constituent parts of the specified {@code Template}. Can be used for
   * debugging purposes.
   *
   * @param template the {@code Template} whose parts to print
   */
  public static void printParts(Template template) {
    printParts(template, System.out);
  }

  /**
   * Prints out the constituent parts of the specified {@code Template}. Can be used for
   * debugging purposes.
   *
   * @param template the {@code Template} whose parts to print
   * @param out the {@code PrintStream} to which to print
   */
  public static void printParts(Template template, PrintStream out) {
    new PartsPrinter(template).printParts(out);
  }

}
