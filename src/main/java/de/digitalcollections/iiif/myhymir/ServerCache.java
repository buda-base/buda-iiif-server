package de.digitalcollections.iiif.myhymir;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerCache {
    
    public final static Logger log=LoggerFactory.getLogger(ServerCache.class.getName());
    
       
    public static void addToCache(String cacheName,String name,Object res) {        
        try{
            JCS.getInstance(cacheName).put(name, res );            
            res=null;
        }
        catch (CacheException e ){
            log.error("Problem putting object -->"+name+" in the cache >> "+cacheName+" Exception: "+e.getMessage());
        }
    }
    
    public static Object getObjectFromCache(String cacheName,String name) {        
        return JCS.getInstance(cacheName).get(name);
    }
}
