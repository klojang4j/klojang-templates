package org.klojang.templates;

/**
 * Base class for checked exceptions emanating from the Klojang.
 *
 * @author Ayco Holleman
 */
public class KlojangException extends Exception {

  public KlojangException(String message) {
    super(message);
  }

  public KlojangException(Throwable cause) {
    super(cause);
  }

  public KlojangException(String message, Throwable cause) {
    super(message, cause);
  }
}
