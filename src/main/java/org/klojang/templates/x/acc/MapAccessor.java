package org.klojang.templates.x.acc;

import org.klojang.templates.Accessor;
import org.klojang.templates.NameMapper;
import org.klojang.templates.RenderException;

import java.util.Map;

public final class MapAccessor implements Accessor<Map<String, Object>> {

  private final NameMapper nm;

  public MapAccessor() {
    this(null);
  }

  public MapAccessor(NameMapper nm) {
    this.nm = nm;
  }

  @Override
  public Object access(Map<String, Object> data, String name)
      throws RenderException {
    String key = nm == null ? name : nm.map(name);
    return data.getOrDefault(key, UNDEFINED);
  }

}
