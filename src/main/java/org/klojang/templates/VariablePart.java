package org.klojang.templates;

import java.util.Optional;

/**
 * A {@link Part} implementation for representing template variables.
 *
 * @author Ayco Holleman
 */
final class VariablePart extends AbstractPart implements NamedPart {

  private final VarGroup group;
  private final String name;

  private final String placeholder;

  VariablePart(int start, String prefix, String name, String placeholder) {
    super(start);
    if (prefix == null) {
      group = null;
    } else {
      group = VarGroup.withName(prefix);
    }
    this.name = name;
    this.placeholder = placeholder;
  }

  Optional<VarGroup> varGroup() {
    return Optional.ofNullable(group);
  }

  @Override
  public String name() {
    return name;
  }

  String placeholder() {
    return placeholder;
  }

  @Override
  public String toString() {
    if (placeholder == null) {
      return "~%" + name + "%";
    }
    return "<!-- ~%" + name + "% -->" + placeholder + "<!--%-->";
  }

  VariableOccurrence toOccurrence() {
    return new VariableOccurrence(name,
        Optional.ofNullable(group),
        Optional.ofNullable(placeholder),
        start());
  }

}
