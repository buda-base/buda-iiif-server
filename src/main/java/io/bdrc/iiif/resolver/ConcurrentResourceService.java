package io.bdrc.iiif.resolver;

import static io.bdrc.iiif.resolver.AppConstants.BDR;
import static io.bdrc.iiif.resolver.AppConstants.BDR_len;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.myhymir.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;

public class ConcurrentResourceService<T> {

    static Logger logger = LoggerFactory.getLogger(ConcurrentResourceService.class);

    String cachePrefix = "";
    String cacheName;

    Map<String, CompletableFuture<T>> futures = new ConcurrentHashMap<>();

    public ConcurrentResourceService() {
    }

    public ConcurrentResourceService(String cacheName, String cachePrefix) {
        this.cachePrefix = cachePrefix;
        this.cacheName = cacheName;
    }

    @SuppressWarnings("unchecked")
    T getFromCache(final String resId) {
        return (T) EHServerCache.getCache(cacheName).get(cachePrefix + resId);
    }

    @SuppressWarnings("unchecked")
    void putInCache(final String resId, final T res) {
        try {
            EHServerCache.getCache(cacheName).put(cachePrefix + resId, res);
        } catch (Exception e) {
            logger.error("putInCache, error adding to cache", e);
        }
    }

    public T getFromApi(final String resId) throws IIIFException {
        return null;
    }

    String normalizeId(final String resId) {
        if (resId.startsWith(BDR))
            return "bdr:" + resId.substring(BDR_len);
        return resId;
    }

    @SuppressWarnings("unchecked")
    public T getSync(String resId) throws IIIFException {
        resId = normalizeId(resId);
        T resT = getFromCache(resId);
        if (resT != null) {
            logger.debug("found cache for " + resId);
            return resT;
        }
        resT = getFromApi(resId);
        try {
            EHServerCache.getCache(cacheName).put(resId, resT);
        } catch (Exception e) {
            logger.error("getSync, error adding to cache", e);
        }
        return resT;
    }

    /*
     * Function returning a CompletableFuture and thus allowing many different calls
     * to iiif-presentation to try to access a non-cached WorkInfo at the same time
     * while doing only one request.
     * 
     * What it prevents is the following case. We assume that we're using a naive
     * function, not this one. let's say an API call to lds-pdi takes 100ms. Let's
     * say we ask for all the volumes of the Kangyur at the same time to
     * iiif-presentation (something which actually happens). This means that the
     * following happens: - first request to iiif-presentation: no workinfo cached,
     * making a request to lds-pdi, takes 100ms - second request to
     * iiif-presentation (2ms after the first one): the call to lds-pdi hasn't
     * finished yet, so there is still no cache, so making another request to
     * lds-pdi - etc.
     * 
     * which can lead to a large load of requests to lds-pdi, making all the
     * requests much longer and stressing lds-pdi.
     * 
     * With this function the following happens (conceptually): - first request to
     * iiif-presentation: no workinfo cached, making adding the request to lds-pdi
     * in the map of requests - second request to iiif-presentation: no workinfo
     * cached, but we see that the maps of requests contains one request for this
     * resource, so we just wait for it to return - etc.
     * 
     * which means just one request is made instead of 100.
     * 
     */
    public CompletableFuture<T> getAsync(String resId) {
        resId = normalizeId(resId);
        T resT = getFromCache(resId);
        if (resT != null) {
            logger.debug("found cache for {}", resId);
            CompletableFuture<T> resCached = new CompletableFuture<>();
            resCached.complete(resT);
            return resCached;
        }
        // unintuitive way to perform the (necessary) atomic operation in the list
        CompletableFuture<T> res = new CompletableFuture<>();
        CompletableFuture<T> resFromList = futures.putIfAbsent(resId, res);
        if (resFromList != null) {
            // this is the case of all the threads trying to access the resource
            // except the first one
            return resFromList;
        }
        try {
            resT = getFromApi(resId);
        } catch (IIIFException e) {
            res.completeExceptionally(e);
            // this means that each failed fetch will not be saved and the next
            // API call will trigger a new one... this could be optimized by doing
            // that every, say 10mn, so that we don't do too many external calls
            futures.remove(resId);
            return res;
        }
        putInCache(resId, resT);
        res.complete(resT);
        futures.remove(resId);
        return res;
    }

}