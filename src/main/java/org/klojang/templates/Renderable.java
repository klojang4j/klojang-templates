package org.klojang.templates;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Defines methods for rendering a populated {@link Template}. {@code Renderable}
 * instances are obtained via {@link RenderSession#createRenderable()}.
 *
 * @author Ayco Holleman
 * @see RenderSession#paste(String, Renderable)
 */
public sealed interface Renderable permits RenderableImpl {

  /**
   * Renders the template to the specified {@code OutputStream}.
   *
   * @param out the {@code OutputStream} to which to write
   * @implNote The implementation returned by
   *     {@link RenderSession#createRenderable()} wraps the {@code OutputStream} into
   *     a {@link PrintStream} if it is not already a {@code PrintStream} but does
   *     not apply any buffering.
   */
  void render(OutputStream out);

  /**
   * Renders the template to the specified {@code StringBuilder} using its
   * {@link StringBuilder#append(String) append()} method.
   *
   * @param sb the {@code StringBuilder} to which to write
   */
  void render(StringBuilder sb);

}
