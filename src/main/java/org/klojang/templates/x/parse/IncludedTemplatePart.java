package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

import static org.klojang.templates.x.Regex.TMPL_START;
import static org.klojang.templates.x.Regex.VAR_END;
import static org.klojang.util.StringMethods.substrAfter;
import static org.klojang.util.StringMethods.substringBefore;

/**
 * A {@link Part} implementation for representing included templates.
 *
 * @author Ayco Holleman
 */
final class IncludedTemplatePart extends NestedTemplatePart {

  static String basename(String path) {
    return substringBefore(substrAfter(path, "/", -1), ".", -1);
  }

  IncludedTemplatePart(Template template, int start) {
    super(template, start);
  }

  @Override
  public String toString() {
    String basename = basename(template.getPath().toString());
    StringBuilder sb = new StringBuilder(32).append(TMPL_START).append("include:");
    if (!template.getName().equals(basename)) {
      sb.append(template.getName());
    }
    return sb.append(template.getPath()).append(VAR_END).toString();
  }

}
