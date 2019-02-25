package de.digitalcollections.iiif.myhymir;

import static io.bdrc.pdf.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;


public class ServerCache {

    public final static Logger log=LoggerFactory.getLogger(ServerCache.class.getName());

    private static final CacheAccess<Object,Object> IIIF=JCS.getInstance("IIIF");
    private static final CacheAccess<Object,Object> IIIF_IMG=JCS.getInstance("IIIF_IMG");
    private static final CacheAccess<Object,Object> IIIF_ZIP=JCS.getInstance("IIIF_ZIP");
    private static final CacheAccess<Object,Object> INFO=JCS.getInstance("info");
    private static final CacheAccess<Object,Object> PDF_JOBS=JCS.getInstance("pdfjobs");
    private static final CacheAccess<Object,Object> ZIP_JOBS=JCS.getInstance("zipjobs");
    private static final CacheAccess<Object,Object> DEFAULT=JCS.getInstance("default");
    private static final CacheAccess<Object,Object> IDENTIFIER=JCS.getInstance("identifier");


    public static void addToCache(String cacheName,String name,Object res) throws BDRCAPIException{
        try{
            CacheAccess<Object,Object> access=getCacheAccess(cacheName);
            log.debug("Added "+res+ " name :"+ name+" to "+cacheName);
            access.put(name, res );
            res=null;
        }
        catch (CacheException e ){
            log.error("Problem putting object -->"+name+" in the cache >> "+cacheName+" Exception: "+e.getMessage());
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

    public static Object getObjectFromCache(String cacheName,String name) {
        CacheAccess<Object,Object> access=getCacheAccess(cacheName);
        log.debug("Got "+access.get(name)+ " with name :"+ name+" from "+cacheName);
        return access.get(name);
    }

    public static CacheAccess<Object,Object> getCacheAccess(String cacheName){
        CacheAccess<Object,Object> access=null;
        switch(cacheName) {
            case "IIIF":
                access=IIIF;
            case "IIIF_ZIP":
                access=IIIF_ZIP;
            case "IIIF_IMG":
                access=IIIF_IMG;
            case "info":
                access=INFO;
            case "default":
                access=DEFAULT;
            case "pdfjobs":
                access=PDF_JOBS;
            case "zipjobs":
                access=ZIP_JOBS;
            case "identifier":
                access=IDENTIFIER;
        }
        return access;
    }
}
