package org.klojang.templates;

import org.klojang.templates.x.Lazy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.klojang.templates.RenderErrorCode.UNEXPECTED_ERROR;
import static org.klojang.templates.RenderUtil.stringify;
import static org.klojang.util.StringMethods.concat;

final class Renderer {

  private final RenderState state;

  Renderer(RenderState state) {
    this.state = state;
  }

  void render(OutputStream out) {
    Appendable x = out instanceof Appendable a ? a : new PrintStream(out);
    try {
      render(state, x);
    } catch (IOException e) {
      throw new RenderException(UNEXPECTED_ERROR, e.toString());
    }
  }

  void render(StringBuilder sb) {
    try {
      render(state, sb);
    } catch (IOException e) {
      throw new RenderException(UNEXPECTED_ERROR, e.toString());
    }
  }

  @Override
  public String toString() {
    Template t = state.getSessionConfig().template();
    if (t.path().isPresent()) {
      return concat(Renderer.class.getName(), "[source=", t.path().get(), "]");
    }
    return concat(Renderer.class.getName(), "[template=", t.getName(), "]");
  }

  private void render(RenderState state0, Appendable out) throws IOException {
    List<Part> parts = state0.getSessionConfig().template().parts();
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof TextPart tp) {
        out.append(tp.getText());
      } else if (part instanceof VariablePart vp) {
        if (state0.getVar(i) != null) {
          Object val = state0.getVar(i);
          if (val instanceof Lazy lazy) {
            out.append(eval(lazy, state0, vp));
          } else {
            out.append(val.toString());
          }
        }
      } else /* TemplatePart */ {
        renderNestedTemplate(out, (NestedTemplatePart) part, state0);
      }
    }
  }

  private void renderNestedTemplate(Appendable out,
      NestedTemplatePart part,
      RenderState state) throws IOException {
    SoloSession[] sessions = state.getChildSessions(part.getTemplate());
    if (sessions != null) {
      Template t = part.getTemplate();
      if (t.isTextOnly()) {
        // Then the RenderSession[] array will contain only null values
        // and we just want to know its length to determine the number
        // of repetitions
        String text = ((TextPart) t.parts().get(0)).getText();
        for (int i = 0; i < sessions.length; ++i) {
          out.append(text);
        }
      } else {
        for (SoloSession s : sessions) {
          render(s.getState(), out);
        }
      }
    }
  }

  private static String eval(Lazy lazy, RenderState state, VariablePart part) {
    StringifierRegistry reg = state.getSessionConfig().stringifiers();
    Object val = lazy.value().get();
    Stringifier stringifier = reg.getStringifier(part, lazy.varGroup(), val);
    return stringify(val, stringifier, part, lazy.varGroup());
  }

}
