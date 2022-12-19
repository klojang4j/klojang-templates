package org.klojang.templates;

interface Part {

  /** The start index of this part within the template. */
  int start();

  Template getParentTemplate();
}
