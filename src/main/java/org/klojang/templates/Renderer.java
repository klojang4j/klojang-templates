package org.klojang.templates;

import org.klojang.templates.x.Lazy;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static org.klojang.templates.ParseUtils.trimToFristNewline;
import static org.klojang.templates.RenderUtil.stringify;
import static org.klojang.util.StringMethods.concat;

final class Renderer {

  private final RenderState state;

  Renderer(RenderState state) {
    this.state = state;
  }

  public void render(OutputStream out) {
    PrintStream ps = out instanceof PrintStream
        ? (PrintStream) out
        : new PrintStream(out);
    render(state, ps);
  }

  public void render(StringBuilder sb) {
    render(state, sb);
  }

  @Override
  public String toString() {
    Template t = state.getSessionConfig().template();
    if (t.path().isPresent()) {
      return concat(Renderer.class.getName(), "[source=", t.path().get(), "]");
    }
    return concat(Renderer.class.getName(), "[template=", t.getName(), "]");
  }

  private void render(RenderState state0, PrintStream ps) {
    List<Part> parts = state0.getSessionConfig().template().parts();
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof TextPart tp) {
        ps.append(tp.getText());
      } else if (part instanceof VariablePart vp) {
        if (state0.getVar(i) != null) {
          Object val = state0.getVar(i);
          if (val instanceof Lazy lazy) {
            ps.append(eval(lazy, state0, vp));
          } else {
            ps.append(val.toString());
          }
        }
      } else /* TemplatePart */ {
        NestedTemplatePart ntp = (NestedTemplatePart) part;
        SoloSession[] sessions = state0.getChildSessions(ntp.getTemplate());
        if (sessions != null) {
          Template t = ntp.getTemplate();
          if (t.isTextOnly()) {
            // The RenderSession[] array will contain only null values
            // and we just want to know its length to determine the
            // number of repetitions
            String text = ((TextPart) t.parts().get(0)).getText();
            IntStream.range(0, sessions.length).forEach(x -> ps.append(text));
          } else {
            stream(sessions)
                .map(SoloSession::getState)
                .forEach(s -> render(s, ps));
          }
        }
      }
    }
  }

  private void render(RenderState state0, StringBuilder sb) {
    List<Part> parts = state0.getSessionConfig().template().parts();
    for (int i = 0; i < parts.size(); ++i) {
      Part part = parts.get(i);
      if (part instanceof TextPart tp) {
        sb.append(tp.getText());
      } else if (part instanceof VariablePart vp) {
        if (state0.getVar(i) != null) {
          Object val = state0.getVar(i);
          if (val instanceof Lazy lazy) {
            sb.append(eval(lazy, state0, vp));
          } else {
            sb.append(val);
          }
        }
      } else /* TemplatePart */ {
        NestedTemplatePart ntp = (NestedTemplatePart) part;
        SoloSession[] sessions = state0.getChildSessions(ntp.getTemplate());
        if (sessions != null) {
          Template t = ntp.getTemplate();
          if (t.isTextOnly()) {
            String text = ((TextPart) t.parts().get(0)).getText();
            IntStream.range(0, sessions.length).forEach(x -> sb.append(text));
          } else {
            stream(sessions)
                .map(SoloSession::getState)
                .forEach(s -> render(s, sb));
          }
        }
      }
    }
  }

  private static String eval(Lazy lazy, RenderState state, VariablePart part) {
    StringifierRegistry reg = state.getSessionConfig().stringifiers();
    Object val = lazy.value().get();
    Stringifier stringifier = reg.getStringifier(part, lazy.varGroup(), val);
    return stringify(stringifier, part.getName(), lazy.varGroup(), val);
  }

}
