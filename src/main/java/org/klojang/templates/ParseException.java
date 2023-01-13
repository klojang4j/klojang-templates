package org.klojang.templates;

/**
 * Thrown if the template source could not be parsed into a {@link Template}.
 *
 * @author Ayco Holleman
 */
public class ParseException extends KlojangException {

  public ParseException(String message) {
    super(message);
  }

}
