package org.klojang.templates;

import org.klojang.templates.x.parse.NestedTemplatePart;
import org.klojang.templates.x.parse.Part;
import org.klojang.templates.x.parse.TextPart;
import org.klojang.templates.x.parse.VariablePart;

import java.io.PrintStream;

import static org.klojang.util.StringMethods.rpad;

final class PartsPrinter {

  private static final String HDR_TEMPLATE = "TEMPLATE";
  private static final String HDR_PART_TYPE = "PART TYPE";
  private static final String HDR_PART_NAME = "PART NAME";

  private final Template t;
  private final int w0;
  private final int w1;
  private final int w2;

  PartsPrinter(Template t) {
    this(t, getMaxTmplName(t), HDR_PART_TYPE.length(), getMaxVarName(t));
  }

  private PartsPrinter(Template t, int w0, int w1, int w2) {
    this.t = t;
    this.w0 = w0;
    this.w1 = w1;
    this.w2 = w2;
  }

  void printParts(PrintStream out) {
    printParts(out, true);
  }

  private void printParts(PrintStream out, boolean printHeader) {
    if (printHeader) {
      printDivider(out);
      printCell(out, HDR_TEMPLATE, w0);
      printCell(out, HDR_PART_TYPE, w1);
      printCell(out, HDR_PART_NAME, w2);
      out.println();
      printDivider(out);
    }
    for (Part p : t.getParts()) {
      if (p instanceof VariablePart) {
        printCell(out, t.getName(), w0);
        printCell(out, "VARIABLE", w1);
        String name = ((VariablePart) p).getName();
        printCell(out, name, w2);
        out.println(p.toString());
        printDivider(out);
      } else if (p instanceof TextPart) {
        printCell(out, t.getName(), w0);
        printCell(out, "TEXT", w1);
        printCell(out, w2);
        out.println(p.toString().replaceAll("\\s+", " "));
        printDivider(out);
      } else /* TemplatePart */ {
        printCell(out, t.getName(), w0);
        printCell(out, "TEMPLATE", w1);
        Template t2 = ((NestedTemplatePart) p).getTemplate();
        String name = t2.getName();
        printCell(out, name, w2);
        String s = "~%%begin:" + name + '%';
        out.println(s);
        printDivider(out);
        PartsPrinter printer = new PartsPrinter(t2, w0, w1, w2);
        printer.printParts(out, false);
        printCell(out, t.getName(), w0);
        printCell(out, "TEMPLATE", w1);
        printCell(out, name, w2);
        s = "~%%end:" + name + '%';
        out.println(s);
        printDivider(out);
      }
    }
  }

  private static void printCell(PrintStream out, int width) {
    printCell(out, " ", width);
  }

  private static void printCell(PrintStream out, String val, int width) {
    out.print(rpad(val, width, ' ', " | "));
  }

  private void printDivider(PrintStream out) {
    out.print(rpad("-", w0, '-', "-+-"));
    out.print(rpad("-", w1, '-', "-+-"));
    out.println(rpad("-", w2, '-', "-+"));
  }

  private static int getMaxTmplName(Template t) {
    int i = Math.max(HDR_TEMPLATE.length(), t.getName().length());
    for (Part p : t.getParts()) {
      if (p instanceof NestedTemplatePart ntp) {
        i = Math.max(i, ntp.getTemplate().getName().length());
        i = Math.max(i, getMaxTmplName(ntp.getTemplate()));
      }
    }
    return i;
  }

  private static int getMaxVarName(Template t) {
    int i = HDR_PART_NAME.length();
    for (Part p : t.getParts()) {
      if (p instanceof VariablePart vp) {
        i = Math.max(i, vp.getName().length());
      } else if (p instanceof NestedTemplatePart ntp) {
        i = Math.max(i, getMaxVarName(ntp.getTemplate()));
      }
    }
    return i;
  }

}
