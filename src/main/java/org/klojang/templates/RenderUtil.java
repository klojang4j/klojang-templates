package org.klojang.templates;

import static org.klojang.templates.RenderErrorCode.STRINGIFIER_NOT_NULL_RESISTENT;
import static org.klojang.templates.RenderErrorCode.STRINGIFIER_RETURNED_NULL;

final class RenderUtil {

  static String stringify(Object value,
      Stringifier stringifier,
      VariablePart part,
      VarGroup adhoc) {
    VarGroup vg = part.getVarGroup().orElse(adhoc);
    if (value == null && vg == VarGroup.DEF && part.getPlaceholder() != null) {
      return part.getPlaceholder();
    }
    String s;
    try {
      s = stringifier.stringify(value);
    } catch (NullPointerException e) {
      throw STRINGIFIER_NOT_NULL_RESISTENT.getException(part.getName(), vg);
    }
    if (s == null) {
      throw STRINGIFIER_RETURNED_NULL.getException(part.getName(), vg);
    }
    return s;
  }

}
