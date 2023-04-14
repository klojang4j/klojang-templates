package org.klojang.templates;

import java.util.Optional;

/**
 * Represents a concrete occurrence of a variable within a template. Variable names
 * may occur multiple times within the same template. A {@code VariableOccurrence}
 * captures the information associated with a single occurrence.
 *
 * @param name the name of the variable
 * @param varGroup an {@code Optional} containing the
 *     {@linkplain VarGroup variable group} corresponding to the group name prefix
 *     (as in {@code ~%myVarGroup:foo%}), or an empty {@code Optional} if there was
 *     no group name prefix
 * @param placeholder an {@code Optional} containing the placeholder value for
 *     this occurrence (as in {@code <!-- ~%firstName% -->John<!--%-->}), or an empty
 *     {@code Optional} if no placeholder was specified. (See {@link VarGroup#DEF}
 *     and {@link Regex#REGEX_CMT_VARIABLE}.)
 * @param position the position (index) of this occurrence within the template
 */
public record VariableOccurrence(String name,
    Optional<VarGroup> varGroup,
    Optional<String> placeholder,
    int position) {
}
