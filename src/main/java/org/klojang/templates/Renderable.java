package org.klojang.templates;

import java.io.OutputStream;
import java.io.PrintStream;
/**
 * Defines methods for rendering a populated {@link Template}. {@code Renderable} instances are
 * obtained via {@link RenderSession#createRenderable()}.
 *
 * @see RenderSession#paste(String, Renderable)
 * @author Ayco Holleman
 */
public interface Renderable {

  /**
   * Writes the populated template to the specified {@code OutputStream}. The actual implementation
   * returned by {@link RenderSession#createRenderable()} wraps the {@code OutputStream} into a
   * {@link PrintStream} if it is not already a {@code PrintStream} but does not apply any
   * buffering.
   *
   * @param out The {@code OutputStream} to which to write
   */
  void render(OutputStream out);

  /**
   * Appends the populated template to the specified {@code StringBuilder}.
   *
   * @param sb The {@code StringBuilder} to which to write
   */
  void render(StringBuilder sb);
}
