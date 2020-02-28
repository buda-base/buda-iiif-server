package de.digitalcollections.iiif.myhymir;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;

public class ServerCache {

    public final static Logger log = LoggerFactory.getLogger(ServerCache.class.getName());

    private static CacheAccess<Object, Object> IIIF;
    private static CacheAccess<Object, Object> IIIF_IMG;
    private static CacheAccess<Object, Object> IIIF_ZIP;
    private static CacheAccess<Object, Object> INFO;
    private static CacheAccess<Object, Object> PDF_JOBS;
    private static CacheAccess<Object, Object> ZIP_JOBS;
    private static CacheAccess<Object, Object> DEFAULT;
    private static CacheAccess<Object, Object> IDENTIFIER;

    public static void init() {
        IIIF = JCS.getInstance("IIIF");
        IIIF_IMG = JCS.getInstance("IIIF_IMG");
        IIIF_ZIP = JCS.getInstance("IIIF_ZIP");
        INFO = JCS.getInstance("info");
        PDF_JOBS = JCS.getInstance("pdfjobs");
        ZIP_JOBS = JCS.getInstance("zipjobs");
        DEFAULT = JCS.getInstance("default");
        IDENTIFIER = JCS.getInstance("identifier");
    }

    public static void addToCache(String cacheName, String name, Object res) throws IIIFException {
        try {
            CacheAccess<Object, Object> access = getCacheAccess(cacheName);
            log.debug("Added " + res + " name :" + name + " to " + cacheName);
            access.put(name, res);
            res = null;
        } catch (CacheException e) {
            log.error("Problem putting object -->" + name + " in the cache >> " + cacheName + " Exception: " + e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    public static Object getObjectFromCache(String cacheName, String name) {
        CacheAccess<Object, Object> access = getCacheAccess(cacheName);
        log.debug("Got " + access.get(name) + " with name : " + name + " from " + cacheName);
        return access.get(name);
    }

    public static CacheAccess<Object, Object> getCacheAccess(String cacheName) {
        CacheAccess<Object, Object> access = null;
        switch (cacheName) {
        case "IIIF":
            access = IIIF;
        case "IIIF_ZIP":
            access = IIIF_ZIP;
        case "IIIF_IMG":
            access = IIIF_IMG;
        case "info":
            access = INFO;
        case "default":
            access = DEFAULT;
        case "pdfjobs":
            access = PDF_JOBS;
        case "zipjobs":
            access = ZIP_JOBS;
        case "identifier":
            access = IDENTIFIER;
        }
        return access;
    }

    public static boolean clearCache() {
        try {
            if (IIIF != null) {
                IIIF.clear();
            }
            if (IIIF_ZIP != null) {
                IIIF_ZIP.clear();
            }
            if (IIIF_IMG != null) {
                IIIF_IMG.clear();
            }
            if (INFO != null) {
                INFO.clear();
            }
            if (DEFAULT != null) {
                DEFAULT.clear();
            }
            ;
            if (PDF_JOBS != null) {
                PDF_JOBS.clear();
            }
            if (ZIP_JOBS != null) {
                ZIP_JOBS.clear();
            }
            if (IDENTIFIER != null) {
                IDENTIFIER.clear();
            }
            return true;
        } catch (Exception e) {
            log.error("There was an issue while clearing caches; Message:" + e.getMessage());
            return false;
        }
    }
}
