package io.bdrc.iiif.metrics;

import java.util.Map;

import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.TierStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

public class CacheMetrics {

    private static final Logger log = LoggerFactory.getLogger(CacheMetrics.class);

    static {
        for (MeterRegistry mr : Metrics.globalRegistry.getRegistries()) {
            log.debug("METER  REGISTRY in CacheMetrics >> {}" + mr);
        }
    }

    public static void cacheGet(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".cache", "action", "get");
            cnt.increment();
            log.info("Incremented cache get counter {}; its value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void cachePut(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".cache", "action", "put");
            cnt.increment();
            log.info("Incremented cache put counter {}; its value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void updateIfDiskCache(String cacheName) {
        Map<String, TierStatistics> stats = EHServerCache.getTierStatistics(cacheName);
        if (stats != null) {
            if (stats.get("Disk") != null) {
                Counter cnt = Metrics.counter(cacheName + ".disk_allocated");
                cnt.increment(stats.get("Disk").getOccupiedByteSize() - cnt.count());
                log.info("Incremented disk cache put counter {}; its value is now {}", cnt.getId(), cnt.count());
                Counter cnt1 = Metrics.counter(cacheName + ".disk_occupied");
                cnt1.increment(stats.get("Disk").getOccupiedByteSize() - cnt1.count());
                log.info("Incremented disk cache put counter {}; its value is now {}", cnt1.getId(), cnt1.count());
            }
        }
    }

    public static void updateCommonsCache(String cacheName) {
        CacheStatistics stats = EHServerCache.getCacheStatistics(cacheName);
        Metrics.gauge(cacheName + ".hitsPercent", Float.class, (i) -> stats.getCacheHitPercentage());
        log.info("Added gauge value for gauge {}; its value is now {}", cacheName + ".hitsPercent", stats.getCacheHitPercentage());
        Metrics.gauge(cacheName + ".missesPercent", Float.class, (i) -> stats.getCacheMissPercentage());
        log.info("Added gauge value for gauge {}; its value is now {}", cacheName + ".missesPercent", stats.getCacheMissPercentage());
        Counter removals = Metrics.counter(cacheName + ".removals");
        removals.increment(stats.getCacheRemovals() - removals.count());
        log.info("Incremented common cache put counter {}; its value is now {}", removals.getId(), removals.count());
        Counter evictions = Metrics.counter(cacheName + ".evictions");
        evictions.increment(stats.getCacheEvictions() - evictions.count());
        log.info("Incremented common cache put counter {}; its value is now {}", evictions.getId(), evictions.count());
        Counter expirations = Metrics.counter(cacheName + ".expirations");
        expirations.increment(stats.getCacheExpirations() - expirations.count());
        log.info("Incremented common cache put counter {}; its value is now {}", expirations.getId(), expirations.count());
    }

}
