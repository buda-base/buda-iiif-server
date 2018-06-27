package io.bdrc.iiif;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.admin.CacheElementInfo;
import org.apache.commons.jcs.admin.CacheRegionInfo;
import org.apache.commons.jcs.admin.JCSAdminBean;
import org.junit.Test;

public class CacheTest {
    
    
    public static final String IIIF="IIIF";
    public static final String INFO="info";
    public static final String INFO_TEST="infotest";
    public static final String IIIF_TEST="IIIFtest";
    public static final String IIIF_TEST_SPACE="IIIFtestspace";
    JCSAdminBean bean=new JCSAdminBean();
    
    @Test
    public void testCacheAccess() throws Exception {
        assert(JCS.getInstance(IIIF)!=null);
        System.out.println(JCS.getInstance(IIIF).getCacheAttributes());
        CacheRegionInfo[] info=bean.buildCacheInfo();
       
        for(CacheRegionInfo cri: info) {
            System.out.println("REGION INFO NAME >>> "+cri.getCacheName());
            System.out.println("REGION INFO BYTES >>> "+cri.getByteCount());
            System.out.println("REGION INFO BYTES >>> "+cri.getCacheStatistics());
        }
        assert(JCS.getInstance(INFO)!=null);
        assert(JCS.getInstance(INFO_TEST)!=null);
        assert(JCS.getInstance(IIIF_TEST)!=null);
        assert(JCS.getInstance(IIIF_TEST_SPACE)!=null);
    }

}
