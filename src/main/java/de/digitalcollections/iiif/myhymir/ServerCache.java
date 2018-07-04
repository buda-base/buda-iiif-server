package de.digitalcollections.iiif.myhymir;

import static io.bdrc.pdf.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;


public class ServerCache {
    
    public final static Logger log=LoggerFactory.getLogger(ServerCache.class.getName());
    
       
    public static void addToCache(String cacheName,String name,Object res) throws BDRCAPIException{        
        try{
            System.out.println("Added "+res+ " name :"+ name+" to "+cacheName);
            JCS.getInstance(cacheName).put(name, res );            
            res=null;
        }
        catch (CacheException e ){
            log.error("Problem putting object -->"+name+" in the cache >> "+cacheName+" Exception: "+e.getMessage());
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }
    
    public static Object getObjectFromCache(String cacheName,String name) {
        System.out.println("Got "+JCS.getInstance(cacheName).get(name)+ " with name :"+ name+" from "+cacheName);
        return JCS.getInstance(cacheName).get(name);
    }
}
