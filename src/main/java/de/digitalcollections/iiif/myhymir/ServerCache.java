package de.digitalcollections.iiif.myhymir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCache {

    public final static Logger log = LoggerFactory.getLogger(ServerCache.class.getName());

    public static void init() {

    }

    public static boolean clearCache() {
        try {

            return true;
        } catch (Exception e) {
            log.error("There was an issue while clearing caches; Message:" + e.getMessage());
            return false;
        }
    }
}
