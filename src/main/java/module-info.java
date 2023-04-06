/**
 * <p>
 * <i><b>Klojang Templates</b></i> is a templating API written with two goals in
 * mind:
 * </p>
 * <p>
 * &#8865; Writing templates should be so simple that there is essentially no
 * learning curve.
 * </p>
 * <p>
 * &#8865; Provide a rich en flexible API for populating the templates that
 * compensates for their simplicity.
 * </p>
 * <p>
 * In short: leverage the skills of Java programmers, rather than make them learn
 * what, in effect, amounts to a whole new language.
 * </p>
 * <p>
 * A Klojang template consists of the following elements:
 * </p>
 * <p><b>Boilerplate Text</b></p>
 * <p>This could be HTML, but it could equally well be
 * anything else. <i>Klojang Templates</i> is not biased towards any particular
 * output format. However, <i>Klojang Templates</i> does provide some extra help when
 * writing HTML templates, notable in the form of a few predefined
 * {@linkplain org.klojang.templates.VarGroup variable groups}.
 * </p>
 * <p><b>Template Variables</b></p>
 * <p>
 * Template variables look like this: {@code ~%city%}. The variable name can
 * optionally be prefixed with the name of a
 * {@linkplain org.klojang.templates.VarGroup variable group}: {@code ~%html:city%}.
 * A variable name may represent a path through an object graph, with the last path
 * segment referencing the value for the variable:
 * {@code ~%html:employee.address.city}.
 * </p>
 */
module org.klojang.templates {
  exports org.klojang.templates;
  exports org.klojang.templates.name;

  requires org.apache.commons.text;
  requires org.apache.commons.lang3;
  requires org.apache.httpcomponents.httpclient;
  requires org.slf4j;

  requires org.klojang.check;
  requires org.klojang.util;
  requires org.klojang.collections;
  requires org.klojang.convert;
  requires org.klojang.invoke;
}