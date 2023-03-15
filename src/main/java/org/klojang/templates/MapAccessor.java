package org.klojang.templates;

import java.util.Map;

final class MapAccessor implements Accessor<Map<String, Object>> {

  private final NameMapper nm;

  MapAccessor() {
    this(null);
  }

  MapAccessor(NameMapper nm) {
    this.nm = nm;
  }

  @Override
  public Object access(Map<String, Object> data, String name)
      throws RenderException {
    String key = nm == null ? name : nm.map(name);
    return data.getOrDefault(key, UNDEFINED);
  }

}
