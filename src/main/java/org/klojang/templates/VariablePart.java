package org.klojang.templates;

import org.klojang.templates.x.Private;

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
      group = VarGroup.createPrivileged(Private.of(prefix));
    }
    this.name = name;
    this.placeholder = placeholder;
  }

  /**
   * Returns an {@code Optional} containing the group name prefix, or an empty
   * {@code Optional} if the variable was declared without a group name prefix. For
   * example for {@code ~%html:firstName%} this method would return the
   * {@link VarGroup#HTML} variable group.
   *
   * @return An {@code Optional} containing the group name prefix
   */
  Optional<VarGroup> getVarGroup() {
    return Optional.ofNullable(group);
  }

  /**
   * Returns the name of the variable.
   *
   * @return The name of the variable
   */
  @Override
  public String getName() {
    return name;
  }

  String getPlaceholder() {
    return placeholder;
  }

  @Override
  public String toString() {
    if (placeholder == null) {
      return "~%" + name + "%";
    }
    return "<!-- ~%" + name + "% -->" + placeholder + "<!--%-->";
  }

}
