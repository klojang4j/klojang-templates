package org.klojang.templates;

import java.util.Objects;

class StringifierId {

  private final VarGroup varGroup;
  private final Template template;
  private final String varName;
  private final int hash;

  StringifierId(VarGroup varGroup) {
    this(varGroup, null, null);
  }

  StringifierId(Template template, String varName) {
    this(null, template, varName);
  }

  StringifierId(String varName) {
    this(null, null, varName);
  }

  StringifierId(VarGroup varGroup, Template template, String varName) {
    this.varGroup = varGroup;
    this.template = template;
    this.varName = varName;
    this.hash = Objects.hash(template, varGroup, varName);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    StringifierId other = (StringifierId) obj;
    return Objects.equals(template, other.template)
        && varGroup == other.varGroup
        && Objects.equals(varName, other.varName);
  }
}
