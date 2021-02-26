package io.bdrc.iiif.core;

import org.ehcache.Cache;

import io.bdrc.iiif.exceptions.IIIFException;

public class CacheWrapper<K, V> {

    private Cache<K, V> cache;
    private String cacheName;

    public CacheWrapper(Cache<K, V> cache, String cacheName) {
        super();
        this.cache = cache;
        this.cacheName = cacheName;
    }

    public V get(K key) throws IIIFException {
        return cache.get(key);
    }

    public boolean containsKey(K key) throws IIIFException {
        return cache.containsKey(key);
    }
    
    public void put(K key, V value) throws IIIFException {
        cache.put(key, value);
    }

    public void clear() {
        cache.clear();
    }

}
