package org.klojang.templates.x.parse;

import org.klojang.templates.x.parse.Part;

public sealed interface NamedPart extends Part
    permits NestedTemplatePart, VariablePart {

  String getName();

}
