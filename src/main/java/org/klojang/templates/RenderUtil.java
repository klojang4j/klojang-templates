package org.klojang.templates;

import static org.klojang.templates.RenderErrorCode.STRINGIFIER_NOT_NULL_RESISTENT;
import static org.klojang.templates.RenderErrorCode.STRINGIFIER_RETURNED_NULL;

final class RenderUtil {

  static String stringify(Stringifier stringifier,
      String varName,
      VarGroup varGroup,
      Object value) {
    String s;
    try {
      s = stringifier.stringify(value);
    } catch (NullPointerException e) {
      throw STRINGIFIER_NOT_NULL_RESISTENT.getException(varName, varGroup);
    }
    if (s == null) {
      throw STRINGIFIER_RETURNED_NULL.getException(varName, varGroup);
    }
    return s;
  }

}
