package de.digitalcollections.iiif.myhymir;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.archives.ArchiveInfo;
import io.bdrc.archives.PdfItemInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ServerCache {

    public final static Logger log = LoggerFactory.getLogger(ServerCache.class.getName());

    public static CacheAccess<Object, byte[]> IIIF_IMG;
    public static CacheAccess<Object, byte[]> IIIF_ZIP;
    public static CacheAccess<Object, byte[]> IIIF;
    public static CacheAccess<Object, PdfItemInfo> PDF_ITEM_INFO;
    public static CacheAccess<Object, Boolean> PDF_JOBS;
    public static CacheAccess<Object, Boolean> ZIP_JOBS;
    public static CacheAccess<Object, ArchiveInfo> ARCHIVE_INFO;
    public static CacheAccess<Object, IdentifierInfo> IDENTIFIER;

    public static void init() {
        IIIF = JCS.getInstance("IIIF");
        IIIF_IMG = JCS.getInstance("IIIF_IMG");
        IIIF_ZIP = JCS.getInstance("IIIF_ZIP");
        PDF_ITEM_INFO = JCS.getInstance("info");
        PDF_JOBS = JCS.getInstance("pdfjobs");
        ZIP_JOBS = JCS.getInstance("zipjobs");
        ARCHIVE_INFO = JCS.getInstance("default");
        IDENTIFIER = JCS.getInstance("identifier");
    }

    public static boolean clearCache() {
        try {
            if (IIIF_IMG != null) {
                IIIF_IMG.clear();
            }
            if (IIIF != null) {
                IIIF.clear();
            }
            if (PDF_ITEM_INFO != null) {
                PDF_ITEM_INFO.clear();
            }
            if (ARCHIVE_INFO != null) {
                ARCHIVE_INFO.clear();
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
