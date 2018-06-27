package io.bdrc.iiif;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.admin.CacheRegionInfo;
import org.apache.commons.jcs.admin.JCSAdminBean;
import org.junit.Test;


public class CacheTest {
    
    public final static String TESTDIR = "src/test/";
    public static final String IIIF="IIIF";
    public static final String INFO="info";
    public static final String INFO_TEST="infotest";
    public static final String IIIF_TEST="test";
    public static final String IIIF_TEST_SPACE="testspace";
    
    JCSAdminBean bean=new JCSAdminBean();
    
    @Test
    public void checkLimitSize() throws Exception {  
        clearCaches();
        assert(getPdfCacheSize(IIIF_TEST)==0);
        assert(getPdfCacheSize(IIIF_TEST_SPACE)==0);
        CacheAccess<String,Object> cacheTest=JCS.getInstance(IIIF_TEST_SPACE);
        cacheTest.put("test", Files.readAllBytes(Paths.get(TESTDIR+"575k.pdf")));
        cacheTest.put("test1", Files.readAllBytes(Paths.get(TESTDIR+"2-1Mg.pdf")));
        cacheTest.put("test2", Files.readAllBytes(Paths.get(TESTDIR+"575k.pdf")));
        cacheTest.put("test3", Files.readAllBytes(Paths.get(TESTDIR+"3-9Mg.pdf")));
        cacheTest.put("test4", Files.readAllBytes(Paths.get(TESTDIR+"575k.pdf")));
        cacheTest.put("test5", Files.readAllBytes(Paths.get(TESTDIR+"2-1Mg.pdf")));
        cacheTest.put("test6", Files.readAllBytes(Paths.get(TESTDIR+"3-9Mg.pdf")));
        cacheTest.put("test7", Files.readAllBytes(Paths.get(TESTDIR+"2-1Mg.pdf")));
        cacheTest.put("test8", Files.readAllBytes(Paths.get(TESTDIR+"575k.pdf")));
        cacheTest.put("test9", Files.readAllBytes(Paths.get(TESTDIR+"2-1Mg.pdf")));
        double size=getPdfCacheSize(IIIF_TEST_SPACE);
        assert(size< 10000000); 
    }
    
    @Test
    public void testCacheAccess() throws Exception {
        assert(JCS.getInstance(IIIF)!=null);
        System.out.println(JCS.getInstance(IIIF).getCacheAttributes());
        assert(JCS.getInstance(INFO)!=null);
        assert(JCS.getInstance(INFO_TEST)!=null);
        assert(JCS.getInstance(IIIF_TEST)!=null);
        assert(JCS.getInstance(IIIF_TEST_SPACE)!=null);
    }
    
    public double getPdfCacheSize(String cache) {
        switch(cache) {
            case IIIF:
                return new File("pdf_cache/IIIF.data").length();
            case IIIF_TEST:
                return new File("pdf_cache_test/test.data").length();
            case IIIF_TEST_SPACE:
                return new File("pdf_cache_test_space/testspace.data").length();
            default:
                return -1.0;
        }
    }
    
    public void clearCaches() throws Exception {
        CacheRegionInfo[] info=bean.buildCacheInfo();
        for(CacheRegionInfo cri: info) {
            String name=cri.getCacheName();            
            if(name.equals(INFO_TEST)|| name.equals(IIIF_TEST)||name.equals(IIIF_TEST_SPACE)) {                
                bean.clearRegion(name);
            }
        }
    }
    
    public int getSize(CacheAccess<String,Object> cache) {
        return cache.getCacheControl().getSize();
    }
    
    /*public int getMissCountExpired(CacheAccess<String,Object> cache) {
        return cache.getCacheControl().getMissCountExpired();
    }
    
    public int getHitCountAux(CacheAccess<String,Object> cache) {
        return cache.getCacheControl().getHitCountAux();
    }
    
    public int getHitCountRam(CacheAccess<String,Object> cache) {
        return cache.getCacheControl().getHitCountRam();
    }
    
    public int getMissCountNotFound(CacheAccess<String,Object> cache) {
        return cache.getCacheControl().getMissCountNotFound();
    } */
    
       
    

}
