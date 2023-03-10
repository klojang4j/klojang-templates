package org.klojang.templates;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
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
      if (part.getClass() == TextPart.class) {
        TextPart tp = (TextPart) part;
        ps.append(tp.getText());
      } else if (part.getClass() == VariablePart.class) {
        if (state0.getVar(i) != null) {
          Object val = state0.getVar(i);
          ps.append(val.toString());
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
      if (part.getClass() == TextPart.class) {
        TextPart tp = (TextPart) part;
        sb.append(tp.getText());
      } else if (part.getClass() == VariablePart.class) {
        if (state0.getVar(i) != null) {
          Object val = state0.getVar(i);
          sb.append(val);
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

}
