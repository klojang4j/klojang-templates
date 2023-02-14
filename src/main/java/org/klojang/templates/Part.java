package org.klojang.templates;

sealed interface Part permits AbstractPart, NamedPart {

  // start index of this part within the template
  int start();

  Template getParentTemplate();

  void setParentTemplate(Template template);

}
