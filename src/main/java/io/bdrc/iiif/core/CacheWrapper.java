package io.bdrc.iiif.core;

import org.ehcache.Cache;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.metrics.CacheMetrics;

public class CacheWrapper<K, V> {

    private Cache<K, V> cache;
    private String cacheName;

    public CacheWrapper(Cache<K, V> cache, String cacheName) {
        super();
        this.cache = cache;
        this.cacheName = cacheName;
    }

    public V get(K key) throws IIIFException {
        CacheMetrics.cacheGet(cacheName);
        return cache.get(key);
    }

    public void put(K key, V value) throws IIIFException {
        CacheMetrics.cachePut(cacheName);
        cache.put(key, value);
    }

    public void clear() {
        cache.clear();
    }

}
