package org.klojang.templates;

import org.klojang.templates.x.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.klojang.templates.RenderErrorCode.UNEXPECTED_ERROR;
import static org.klojang.templates.RenderUtil.stringify;
import static org.klojang.templates.TemplateUtils.getFQN;

final class Renderer {

    private static final Logger LOG = LoggerFactory.getLogger(Renderer.class);


    private final RenderState state;

    Renderer(RenderState state) {
        this.state = state;
    }

    void render(OutputStream out) {
        Appendable appendable = out instanceof Appendable a ? a : new PrintStream(out);
        try {
            render(state, appendable);
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

    private void render(RenderState state0, Appendable out) throws IOException {
        if (LOG.isTraceEnabled()) {
            log(state0.getSessionConfig().template());
        }
        List<Part> parts = state0.getSessionConfig().template().parts();
        for (int i = 0; i < parts.size(); ++i) {
            Part part = parts.get(i);
            if (part instanceof TextPart tp) {
                out.append(tp.text());
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
                String text = ((TextPart) t.parts().get(0)).text();
                for (int i = 0; i < sessions.length; ++i) {
                    out.append(text);
                }
            } else {
                for (SoloSession s : sessions) {
                    render(s.state(), out);
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

    private static void log(Template t) {
        if (t.getParent() == null) {
            if (t.path().isEmpty()) {
                LOG.trace("Rendering string template");
            } else {
                LOG.trace("Rendering  {}", t.path().get());
            }
        } else if (t.location().isString()) {
            LOG.trace("Rendering inline template \"{}\"", getFQN(t));
        } else {
            LOG.trace("Rendering included template \"{}\"", getFQN(t));
        }
    }


}
