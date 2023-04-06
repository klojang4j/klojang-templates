package org.klojang.templates;

import java.util.Optional;

/**
 * Represents a concrete occurrence of a variable within a template. Note that a
 * variable name may occur multiple times within the same template.
 *
 * @param name the name of the variable
 * @param varGroup an {@code Optional} containing the
 *     {@linkplain VarGroup variable group} to which this occurrence was assigned, or
 *     an empty {@code Optional} if this occurrence did not have a variable group
 *     prefix
 * @param placeholder an {@code Optional} containing the placeholder value for
 *     this occurrence, or an empty {@code Optional} if no placeholder was specified
 *     (see {@link VarGroup#DEF} and {@link Regex#REGEX_CMT_VARIABLE}).
 * @param position the position (index) of this occurrence within the template
 */
public record VariableOccurrence(String name,
    Optional<VarGroup> varGroup,
    Optional<String> placeholder,
    int position) {
}
