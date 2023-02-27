package org.klojang.templates;

/**
 * Thrown if the template source could not be parsed into a {@link Template}.
 *
 * @author Ayco Holleman
 */
public final class ParseException extends Exception {

  private final ParseErrorCode error;

  ParseException(ParseErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  /**
   * Returns a {@link ParseErrorCode} constant identifying the error.
   *
   * @return a {@code ParseError} constant identifying the error.
   */
  public ParseErrorCode getErrorCode() {
    return error;
  }

}
