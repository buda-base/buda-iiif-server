package de.digitalcollections.iiif.myhymir;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerCache {
    
    protected static CacheAccess<String,Object> CACHE = JCS.getInstance("IIIF");
    public final static Logger log=LoggerFactory.getLogger(ServerCache.class.getName());
    
    public static void addToCache(String name,Object res) {        
        try{
            CACHE.put(name, res );            
            res=null;
        }
        catch (CacheException e ){
            log.error("Problem putting pdf -->"+name+" in the cache >> Exception: "+e.getMessage());
        }
    }
    
    public static Object getObjectFromCache(String name) {        
        return CACHE.get(name);
    }
}
