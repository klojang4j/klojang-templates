package org.klojang.templates;

import org.klojang.check.Check;
import org.klojang.collections.WiredList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.klojang.check.CommonChecks.gte;
import static org.klojang.templates.Setting.TMPL_CACHE_SIZE;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

final class TemplateCache {

  private static final Logger LOG = LoggerFactory.getLogger(TemplateCache.class);

  public static final TemplateCache INSTANCE = new TemplateCache();

  private final HashMap<TemplateLocation, Template> cache;
  private final WiredList<TemplateLocation> entries;
  private final int maxSize;

  private TemplateCache() {
    maxSize = TMPL_CACHE_SIZE.getInt();
    String s = maxSize == 0 ? " (caching disabled)"
        : maxSize == -1 ? " (unlimited)" : "";
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
      entries = new WiredList<>();
    }
  }

  Template get(TemplateLocation location, String name) throws ParseException {
    if (maxSize == 0 || location.isString()) {
      logTemplateRetrieval(location, name);
      return new Parser(location, name).parse();
    }
    logCacheSearch(location, name);
    Template tmpl = cache.get(location);
    if (tmpl == null) {
      LOG.trace("Not found");
      logTemplateRetrieval(location, name);
      tmpl = new Parser(location, name).parse();
      if (maxSize != -1 && entries.size() >= maxSize) {
        TemplateLocation eldest = entries.removeLast();
        LOG.trace("Cache overflow. Evicting {}", eldest.path());
        cache.remove(eldest);
        entries.add(location);
      }
      cache.put(location, tmpl);
    } else {
      LOG.trace("Found");
    }
    return tmpl;
  }

  private static void logTemplateRetrieval(TemplateLocation location, String name) {
    if (LOG.isTraceEnabled()) {
      if (name == ROOT_TEMPLATE_NAME) {
        if (location.path() == null) {
          LOG.trace("Loading template {}", name);
        } else {
          LOG.trace("Loading template {} from {}", name, location.path());
        }
      } else if (location.path() == null) {
        LOG.trace("Loading included template \"{}\"", name);
      } else {
        LOG.trace("Loading included template \"{}\" from {}", name, location.path());
      }
    }
  }

  private static void logCacheSearch(TemplateLocation location, String name) {
    if (LOG.isTraceEnabled()) {
      if (name == ROOT_TEMPLATE_NAME) {
        LOG.trace("Searching cache for template {}@{})", name, location.path());
      } else {
        LOG.trace("Searching cache for included template {}@{}",
            name,
            location.path());
      }
    }
  }

}
