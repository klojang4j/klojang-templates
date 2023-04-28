package org.klojang.templates;

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

  private final boolean tagOnSeparateLine;

  IncludedTemplatePart(int start, Template template, boolean tagOnSeparateLine) {
    super(start, template);
    this.tagOnSeparateLine = tagOnSeparateLine;
  }

  @Override
  public String toString() {
    String basename = basename(template.path().get().toString());
    StringBuilder sb = new StringBuilder(32).append("~%%include:");
    if (!template.getName().equals(basename)) {
      sb.append(template.getName()).append(':');
    }
    return sb.append(template.path().get()).append("%%").toString();
  }

  boolean isTagOnSeparateLine() {
    return tagOnSeparateLine;
  }

}
