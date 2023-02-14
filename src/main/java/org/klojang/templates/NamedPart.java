package org.klojang.templates;

sealed interface NamedPart extends Part permits NestedTemplatePart, VariablePart {

  String getName();

}
