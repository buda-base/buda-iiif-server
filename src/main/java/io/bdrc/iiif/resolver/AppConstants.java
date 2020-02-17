package io.bdrc.iiif.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import io.bdrc.auth.AuthProps;

public class AppConstants {

    public final static ObjectMapper IIIFMAPPER;

    public static final String BDR = "http://purl.bdrc.io/resource/";
    public static final int BDR_len = BDR.length();
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMPPREFIX = "http://purl.bdrc.io/ontology/tmp/";

    public static final String CACHENAME = "iiifpres";

    // they should all be two characters long + colon
    public static final String CACHEPREFIX_WI = "wi:";
    public static final String CACHEPREFIX_WO = "wo:";
    public static final String CACHEPREFIX_II = "ii:";
    public static final String CACHEPREFIX_IIL = "il:";
    public static final String CACHEPREFIX_VI = "vi:";

    public static final String LDS_QUERYPREFIX = AuthProps.getProperty("dataserver");
    public static final String LDS_WORKGRAPH_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_workGraph_noItem";
    public static final String LDS_ITEMGRAPH_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_itemGraph";
    public static final String LDS_VOLUME_QUERY = LDS_QUERYPREFIX + "query/table/IIIFPres_volumeInfo";
    public static final String LDS_VOLUME_OUTLINE_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_volumeOutline";
    public static final String LDS_WORKOUTLINE_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_workOutline";

    public static final int FAIRUSE_PAGES_S = 20;
    public static final int FAIRUSE_PAGES_E = 20;

    public static final String COPYRIGHT_PAGE_IMG_ID = "static::error-copyright.png";
    public static final String COPYRIGHT_PAGE_CANVAS_ID = "static::error-copyright";
    public static final int COPYRIGHT_PAGE_W = 923;
    public static final int COPYRIGHT_PAGE_H = 202;
    public static final boolean COPYRIGHT_PAGE_IS_PNG = true;
    
     static {
        IIIFMAPPER = new IiifObjectMapper();
    }

}
