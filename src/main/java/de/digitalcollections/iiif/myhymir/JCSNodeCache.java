package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.maxmind.db.NodeCache;

public class JCSNodeCache implements NodeCache {
    
    // The raison d'être of this class is to be used by Geoloc's cache mechanism

    private CacheAccess<String, Object> jcscache;
    public final static Logger log = LoggerFactory.getLogger(JCSNodeCache.class.getName());

    public JCSNodeCache() {
        jcscache = JCS.getInstance("geoloc");
    }

    @Override
    public JsonNode get(int key, Loader loader) throws IOException {
        try {
            String k = Integer.toString(key);
            JsonNode value = (JsonNode) jcscache.get(k);
            if (value == null) {
                value = loader.load(key);
                jcscache.put(k, value);
            }
            return value;
        } catch (IOException e) {
            log.error("An issue occured while getting Json Node from cache for key " + key + " message: " + e.getMessage());
            throw e;
        }
    }

}
