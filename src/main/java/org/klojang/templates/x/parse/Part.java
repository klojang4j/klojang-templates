package org.klojang.templates.x.parse;

import org.klojang.templates.Template;

public sealed interface Part permits AbstractPart, NamedPart {

  // start index of this part within the template
  int start();

  Template getParentTemplate();

  void setParentTemplate(Template template);

}
