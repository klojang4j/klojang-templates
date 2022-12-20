package org.klojang.templates.x.parse;

import org.klojang.check.Check;
import org.klojang.templates.ParseException;
import org.klojang.templates.Template;
import org.klojang.templates.x.TemplateId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;

import static org.klojang.templates.SysProp.*;
import static org.klojang.check.CommonChecks.gte;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

public class TemplateCache {

  private static final Logger LOG = LoggerFactory.getLogger(TemplateCache.class);

  public static final TemplateCache INSTANCE = new TemplateCache();

  private final HashMap<String, Template> cache;
  private final LinkedList<String> entries;
  private final int maxSize;

  private TemplateCache() {
    maxSize = TMPL_CACHE_SIZE.getInt();
    String s = maxSize == 0 ? " (caching disabled)" : maxSize == -1 ? " (unlimited)" : "";
    LOG.trace("Template cache size: {}{}", maxSize, s);
    Check.that(maxSize, TMPL_CACHE_SIZE.property()).is(gte(), -1);
    if (maxSize == 0) {
      cache = null;
      entries = null;
    } else if (maxSize == -1) {
      cache = new HashMap<>(32);
      entries = null;
    } else {
      cache = new HashMap<>((int) (maxSize / .75) + 1);
      entries = new LinkedList<>();
    }
  }

  public Template get(Class<?> clazz, String path) throws ParseException {
    return get(ROOT_TEMPLATE_NAME, clazz, path);
  }

  public Template get(String name, Class<?> clazz, String path) throws ParseException {
    return get(name, new TemplateId(clazz, path));
  }

  public Template get(String name, TemplateId id) throws ParseException {
    Check.notNull(name, "name");
    Check.notNull(id, "id");
    if (maxSize == 0 || id.path() == null) { // caching disabled
      logTemplateRetrieval(name, id);
      return new Parser(name, id).parse();
    }
    logCacheSearch(name, id);
    Template t = cache.get(id.path());
    if (t == null) {
      LOG.trace("Not found");
      logTemplateRetrieval(name, id);
      t = new Parser(name, id).parse();
      if (maxSize != -1 && entries.size() >= maxSize) {
        String eldest = entries.pop();
        LOG.trace("Cache overflow. Evicting {}", eldest);
        cache.remove(eldest);
        entries.add(id.path());
      }
      cache.put(id.path(), t);
    } else {
      LOG.trace("Found");
    }
    return t;
  }

  private static void logTemplateRetrieval(String name, TemplateId id) {
    if (LOG.isTraceEnabled()) {
      if (name == ROOT_TEMPLATE_NAME) {
        if (id.path() == null) {
          LOG.trace("Loading template {}", name);
        } else {
          LOG.trace("Loading template {} from {}", name, id.path());
        }
      } else if (id.path() == null) {
        LOG.trace("Loading included template \"{}\"", name);
      } else {
        LOG.trace("Loading included template \"{}\" from {}", name, id.path());
      }
    }
  }

  private static void logCacheSearch(String name, TemplateId id) {
    if (LOG.isTraceEnabled()) {
      if (name == ROOT_TEMPLATE_NAME) {
        LOG.trace("Searching cache for template {}@{})", name, id.path());
      } else {
        LOG.trace("Searching cache for included template {}@{}", name, id.path());
      }
    }
  }
}
