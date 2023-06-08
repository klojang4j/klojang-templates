package org.klojang.templates;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EscapeTest {

  @Test
  public void test00() {
    URIBuilder ub0 = new URIBuilder();
    ub0.setPathSegments(List.of("%^#\"'=\\/"));
    System.out.println(ub0.toString().substring(1));
    assertEquals("%25%5E%23%22'=%5C%2F", ub0.toString().substring(1));
    ub0 = new URIBuilder();
    ub0.addParameter("", "%^#\"'=\\/");
    System.out.println(ub0.toString().substring(2));
    assertEquals("%25%5E%23%22%27%3D%5C%2F", ub0.toString().substring(2));
    assertEquals(escapeEcmaScript(ub0.toString().substring(2)), ub0.toString().substring(2));
    assertEquals(escapeHtml4(ub0.toString().substring(2)), ub0.toString().substring(2));
  }

  @Test
  public void test01() throws ClassNotFoundException {
    Class<?> c = Class.forName("org.klojang.check.Check");
    Method[] mm = c.getDeclaredMethods();
    for(Method m:mm) {
      System.out.println("-> "+m.getName());
    }
  }
}
