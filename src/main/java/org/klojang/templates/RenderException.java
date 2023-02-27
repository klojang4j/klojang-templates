package org.klojang.templates;

/**
 * Thrown from a {@link RenderSession} under various circumstances.
 *
 * @author Ayco Holleman
 */
public final class RenderException extends RuntimeException {

  private final RenderErrorCode error;

  RenderException(RenderErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  /**
   * Returns a {@code RenderErrorCode} constant identifying the error.
   *
   * @return a {@code RenderErrorCode} constant identifying the error
   */
  public RenderErrorCode getErrorCode() {
    return error;
  }

}
