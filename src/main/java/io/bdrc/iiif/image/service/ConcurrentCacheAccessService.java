package io.bdrc.iiif.image.service;


import static io.bdrc.iiif.resolver.AppConstants.BDR;
import static io.bdrc.iiif.resolver.AppConstants.BDR_len;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;

public class ConcurrentCacheAccessService {

    static Logger logger = LoggerFactory.getLogger(ConcurrentResourceService.class);
    
    int INCACHE = 1;
    int ERROR = 0;

    Map<String, CompletableFuture<Integer>> futuresCache = new ConcurrentHashMap<>();

    public ConcurrentCacheAccessService() {
    }
    
    boolean isInCache(final String resId) {
        return false;
    }

    void putInCache(final String resId) throws IIIFException {
        return;
    }

    String normalizeId(final String resId) {
        if (resId.startsWith(BDR))
            return "bdr:" + resId.substring(BDR_len);
        return resId;
    }

    /*
     * Function returning a CompletableFuture with an integer value of
     * 0 or 1.
     * 
     * Use this when you want to make sure that something is ready and in
     * cache, without returning the value itself (in the case of streams for
     * instance).
     * 
     */
    public CompletableFuture<Integer> ensureCacheReady(String resId) {
        resId = normalizeId(resId);
        if (isInCache(resId)) {
            logger.debug("found cache for {}", resId);
            CompletableFuture<Integer> resCached = new CompletableFuture<>();
            resCached.complete(INCACHE);
            return resCached;
        }
        // unintuitive way to perform the (necessary) atomic operation in the
        // list
        CompletableFuture<Integer> res = new CompletableFuture<>();
        CompletableFuture<Integer> resFromList = futuresCache.putIfAbsent(resId, res);
        if (resFromList != null) {
            // this is the case of all the threads trying to access the resource
            // except the first one
            return resFromList;
        }
        try {
            putInCache(resId);
        } catch (IIIFException e) {
            res.completeExceptionally(e);
            // this means that each failed fetch will not be saved and the next
            // API call will trigger a new one... this could be optimized by
            // doing
            // that every, say 10mn, so that we don't do too many external calls
            futuresCache.remove(resId);
            return res;
        }
        res.complete(INCACHE);
        futuresCache.remove(resId);
        return res;
    }

}
