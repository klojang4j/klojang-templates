package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

import static org.klojang.templates.x.parse.Regex.TMPL_START;
import static org.klojang.templates.x.parse.Regex.VAR_END;
import static org.klojang.util.StringMethods.substrAfter;
import static org.klojang.util.StringMethods.substringBefore;

/**
 * A {@link Part} implementation for representing included templates.
 *
 * @author Ayco Holleman
 */
public final class IncludedTemplatePart extends NestedTemplatePart {

  public static String basename(String path) {
    return substringBefore(substrAfter(path, "/", -1), ".", -1);
  }

  public IncludedTemplatePart(Template template, int start) {
    super(template, start);
  }

  @Override
  public String toString() {
    String basename = basename(template.getPath().get().toString());
    StringBuilder sb = new StringBuilder(32).append(TMPL_START).append("include:");
    if (!template.getName().equals(basename)) {
      sb.append(template.getName()).append(':');
    }
    return sb.append(template.getPath().get()).append(VAR_END).toString();
  }

}
