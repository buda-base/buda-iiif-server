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

    public final static String IGFN = "igfn";
    public final static String IGSI = "igsi";

    // they should all be two characters long + colon
    public static final String CACHEPREFIX_WI = "wi:";
    public static final String CACHEPREFIX_WO = "wo:";
    public static final String CACHEPREFIX_II = "ii:";
    public static final String CACHEPREFIX_IIL = "il:";
    public static final String CACHEPREFIX_VI = "vi:";

    public static final String LDS_QUERYPREFIX = AuthProps.getProperty("dataserver");
    public static final String LDS_IMAGEINSTANCEGRAPH_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFServ_imageInstanceGraph";

    public static final String LDS_VOLUME_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_volumeInfoGraph";
    public static final String LDS_VOLUME_OUTLINE_QUERY = LDS_QUERYPREFIX + "query/graph/IIIFPres_volumeOutline";

    public static final int FAIRUSE_PAGES_S = 20;
    public static final int FAIRUSE_PAGES_E = 20;

    static {
        IIIFMAPPER = new IiifObjectMapper();
    }

}
