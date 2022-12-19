package org.klojang.templates;

import static org.klojang.util.ObjectMethods.isEmpty;

/**
 * Base class for runtime exceptions emanating from the Klojang.
 *
 * @author Ayco Holleman
 */
public class KlojangRTException extends RuntimeException {

  public KlojangRTException(String message, Object... msgArgs) {
    super(isEmpty(msgArgs) ? message : String.format(message, msgArgs));
  }

  public KlojangRTException(Throwable cause) {
    super(cause);
  }

  public KlojangRTException(String message, Throwable cause) {
    super(message, cause);
  }
}
