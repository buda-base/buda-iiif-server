package io.bdrc.iiif.image.service;


import static io.bdrc.iiif.resolver.AppConstants.BDR;
import static io.bdrc.iiif.resolver.AppConstants.BDR_len;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;

public class ConcurrentCacheAccessService {

    static Logger logger = LoggerFactory.getLogger(ConcurrentResourceService.class);
    
    static final long DEFAULT_WAIT_TIMEOUT_SECONDS = 30;

    final int INCACHE = 1;
    final int ERROR = 0;

    final Map<String, CompletableFuture<Integer>> futuresCache = new ConcurrentHashMap<>();
    private final long waitTimeout;
    private final TimeUnit waitTimeoutUnit;

    public ConcurrentCacheAccessService() {
        this(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    ConcurrentCacheAccessService(final long waitTimeout, final TimeUnit waitTimeoutUnit) {
        this.waitTimeout = waitTimeout;
        this.waitTimeoutUnit = waitTimeoutUnit;
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
        final String normalizedResId = normalizeId(resId);
        if (isInCache(normalizedResId)) {
            logger.debug("found cache for {}", normalizedResId);
            CompletableFuture<Integer> resCached = new CompletableFuture<>();
            resCached.complete(INCACHE);
            return resCached;
        }
        // unintuitive way to perform the (necessary) atomic operation in the
        // list
        CompletableFuture<Integer> res = new CompletableFuture<>();
        CompletableFuture<Integer> resFromList = futuresCache.putIfAbsent(normalizedResId, res);
        if (resFromList != null) {
            // this is the case of all the threads trying to access the resource
            // except the first one
            return resFromList.orTimeout(waitTimeout, waitTimeoutUnit).whenComplete((value, error) -> {
                if (error instanceof TimeoutException) {
                    logger.error("timed out waiting for cache resource {}", normalizedResId);
                    futuresCache.remove(normalizedResId, resFromList);
                }
            });
        }
        try {
            putInCache(normalizedResId);
            res.complete(INCACHE);
        } catch (Throwable error) {
            // Completing the future is essential: otherwise every later request for
            // this resource waits forever on an entry left behind by an unchecked
            // exception or VM error.
            res.completeExceptionally(error);
            if (error instanceof Error) {
                throw (Error) error;
            }
        } finally {
            // Remove only our own future, as a timed-out waiter may already have
            // installed a replacement fetch for this resource.
            futuresCache.remove(normalizedResId, res);
        }
        return res;
    }

}
