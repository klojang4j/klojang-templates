package org.klojang.templates;

import org.klojang.templates.x.parse.ParseError;

/**
 * Thrown if the template source could not be parsed into a {@link Template}.
 *
 * @author Ayco Holleman
 */
public class ParseException extends KlojangException {

  private final ParseError error;

  public ParseException(String message) {
    super(message);
    this.error = ParseError.UNEXPECTED;
  }

  public ParseException(ParseError error, String message) {
    super(message);
    this.error = error;
  }

  public ParseError getError() {
    return error;
  }

}
