package io.bdrc.iiif.metrics;

import java.util.TimerTask;

import io.bdrc.iiif.core.EHServerCache;

public class MetricsTask extends TimerTask {

    @Override
    public void run() {
        for (String cacheName : EHServerCache.getAllCachesNames()) {
            CacheMetrics.updateCommonsCache(cacheName);
            CacheMetrics.updateIfDiskCache(cacheName);
        }
    }

}
