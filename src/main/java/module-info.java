/**
 * <p><i><b>Klojang Templates</b></i> is a templating API written with two goals in
 * mind:
 * <ol>
 *   <li>Writing templates should be so simple that there is essentially no learning
 *       curve.
 *   <li>Provide a rich en flexible API for populating the templates that compensates
 *       for their simplicity.
 * </ol>
 * <p>In short: leverage the skills of Java programmers, rather than make them
 * learn what, in effect, amounts to a whole new language.
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