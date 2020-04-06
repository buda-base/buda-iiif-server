package io.bdrc.iiif.metrics;

import java.util.Map;

import org.ehcache.core.statistics.TierStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class CacheMetrics {

    public static final String DISK_ALLOCATED = "Disk:AllocatedByteSize";
    public static final String DISK_OCCUPIED = "Disk:OccupiedByteSize";

    private static final Logger log = LoggerFactory.getLogger(CacheMetrics.class);

    public static void cacheGet(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".cache", "action", "get");
            cnt.increment();
            log.debug("Incremented cache get counter {}; it's value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void cachePut(String cacheName) throws IIIFException {
        if ("true".equals(Application.getProperty("metricsEnabled"))) {
            Counter cnt = Metrics.counter(cacheName + ".cache", "action", "put");
            cnt.increment();
            log.debug("Incremented cache put counter {}; it's value is now {}", cnt.getId(), cnt.count());
        }
    }

    public static void updateIfDiskCache(String cacheName) {
        if (EHServerCache.getDiskCachesNames().contains(cacheName)) {
            Map<String, TierStatistics> stats = EHServerCache.getTierStatistics(cacheName);
            Counter cnt = Metrics.counter(cacheName + ".disk_allocated");
            cnt.increment(stats.get(DISK_ALLOCATED).getOccupiedByteSize() - cnt.count());
            Counter cnt1 = Metrics.counter(cacheName + ".disk_occupied");
            cnt1.increment(stats.get(DISK_OCCUPIED).getOccupiedByteSize() - cnt1.count());
        }
    }

}
