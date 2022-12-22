package org.klojang.templates.x.parse;

import org.klojang.templates.VarGroup;
import org.klojang.templates.x.ModulePrivate;

import java.util.Optional;

import static org.klojang.util.ObjectMethods.ifNotNull;
import static org.klojang.templates.x.parse.Regex.*;

/**
 * A {@link Part} implementation for representing template variables.
 *
 * @author Ayco Holleman
 */
public final class VariablePart extends AbstractPart implements NamedPart {

  private final VarGroup group;
  private final String name;

  VariablePart(String prefix, String name, int start) {
    super(start);
    if (prefix == null) {
      group = null;
    } else {
      group = VarGroup.createPrivileged(ModulePrivate.hide(prefix));
    }
    this.name = name;
  }

  /**
   * Returns an {@code Optional} containing the group name prefix, or an empty
   * {@code Optional} if the variable was declared without a group name prefix. For
   * example for {@code ~%html:firstName%} this method would return the
   * {@link VarGroup#HTML} variable group.
   *
   * @return An {@code Optional} containing the group name prefix
   */
  public Optional<VarGroup> getVarGroup() {
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

  @Override
  public String toString() {
    return VAR_START + name + VAR_END;
  }

}
