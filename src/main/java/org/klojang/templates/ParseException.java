package org.klojang.templates;

/**
 * Thrown if the template source could not be parsed into a {@link Template}.
 *
 * @author Ayco Holleman
 */
public class ParseException extends Exception {

  private final ParseError error;

  ParseException(String message) {
    super(message);
    this.error = ParseError.UNEXPECTED;
  }

  ParseException(ParseError error, String message) {
    super(message);
    this.error = error;
  }

  /**
   * Returns a {@link ParseError} constant identifying the error.
   *
   * @return a {@code ParseError} constant identifying the error.
   */
  public ParseError getError() {
    return error;
  }

}
