package org.klojang.templates.x.parse;

import org.klojang.check.Check;
import org.klojang.collections.WiredList;
import org.klojang.templates.ParseException;
import org.klojang.templates.Template;
import org.klojang.templates.x.TemplateLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.klojang.check.CommonChecks.gte;
import static org.klojang.templates.Setting.TMPL_CACHE_SIZE;
import static org.klojang.templates.Template.ROOT_TEMPLATE_NAME;

public final class TemplateCache {

  private static final Logger LOG = LoggerFactory.getLogger(TemplateCache.class);

  public static final TemplateCache INSTANCE = new TemplateCache();

  private final HashMap<TemplateLocation, Template> cache;
  private final WiredList<String> entries;
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

  public Template get(TemplateLocation location, String name) throws ParseException {
    if (maxSize == 0 || location.path() == null) { // caching disabled
      logTemplateRetrieval(name, location);
      return new Parser(name, location).parse();
    }
    logCacheSearch(name, location);
    Template tmpl = cache.get(location.path());
    if (tmpl == null) {
      LOG.trace("Not found");
      logTemplateRetrieval(name, location);
      tmpl = new Parser(name, location).parse();
      if (maxSize != -1 && entries.size() >= maxSize) {
        String eldest = entries.removeLast();
        LOG.trace("Cache overflow. Evicting {}", eldest);
        cache.remove(eldest);
        entries.add(location.path());
      }
      cache.put(location, tmpl);
    } else {
      LOG.trace("Found");
    }
    return tmpl;
  }

  private static void logTemplateRetrieval(String name, TemplateLocation location) {
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

  private static void logCacheSearch(String name, TemplateLocation id) {
    if (LOG.isTraceEnabled()) {
      if (name == ROOT_TEMPLATE_NAME) {
        LOG.trace("Searching cache for template {}@{})", name, id.path());
      } else {
        LOG.trace("Searching cache for included template {}@{}", name, id.path());
      }
    }
  }

}
