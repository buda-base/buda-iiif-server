package io.bdrc.iiif.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.statistics.TierStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.archives.PdfItemInfo;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.metrics.CacheMetrics;
import io.bdrc.iiif.model.SimpleBVM;
import io.bdrc.iiif.resolver.ImageGroupInfo;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EHServerCache {

    private static final Logger log = LoggerFactory.getLogger(EHServerCache.class);

    public static DiskCache IIIF_IMG;
    public static DiskCache IIIF_ZIP;
    public static DiskCache IIIF_PDF;
    public static Cache<String, PdfItemInfo> PDF_ITEM_INFO;
    public static Cache<String, ImageGroupInfo> IMAGE_GROUP_INFO;
    public static Cache<String, List> IMAGE_LIST_INFO;
    public static Cache<String, SimpleBVM> BVM;
    private static HashMap<String, CacheWrapper> MAP;
    private static HashMap<String, CacheWrapper> MAP_DISK;
    private static HashMap<String, CacheWrapper> MAP_MEM;
    private static StatisticsService statsService;
    private static HashMap<String, CacheStatistics> CACHE_STATS;

    public static DiskCache getDiskCache(String shortName) throws IOException {
        String path = Application.props.getOrDefault("diskCache."+shortName+".path", shortName.toUpperCase()+"CACHE").toString();
        String nbSecondsMaxS = Application.props.getOrDefault("diskCache."+shortName+".nbSecondsMax", "1800").toString();
        Integer nbSecondsMax = Integer.valueOf(nbSecondsMaxS);
        String sizeMaxMBS = Application.props.getOrDefault("diskCache."+shortName+".sizeMaxMB", "20000").toString();
        Long sizeMaxMB = Long.valueOf(sizeMaxMBS);
        String nbItemsMaxS = Application.props.getOrDefault("diskCache."+shortName+".nbItemsMax", "100").toString();
        Integer nbItemsMax = Integer.valueOf(nbItemsMaxS);
        String cleanupClockSS = Application.props.getOrDefault("diskCache."+shortName+".cleanupClockS", "600").toString();
        Integer cleanupClockS = Integer.valueOf(cleanupClockSS);
        return new DiskCache(path, cleanupClockS, nbSecondsMax, nbItemsMax, sizeMaxMB, shortName);
    }
    
    public static void init() throws IIIFException {
        MAP = new HashMap<>();
        MAP_DISK = new HashMap<>();
        MAP_MEM = new HashMap<>();
        
        try {
            IIIF_IMG = getDiskCache("img");
            IIIF_PDF = getDiskCache("pdf");
            IIIF_ZIP = getDiskCache("zip");
        } catch (IOException e) {
            log.error("error when creating caches: ", e);
            throw new IIIFException(e);
        }
        
        // We must reference CacheStatistics object
        // so they are not garbaed collected before being passed to prometheus
        CACHE_STATS = new HashMap<>();
        statsService = new DefaultStatisticsService();
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().using(statsService).build();
        cacheManager.init();

        /**** MEMORY CACHES ***/
        PDF_ITEM_INFO = cacheManager.createCache("pdfItemInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                PdfItemInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("pdfItemInfo", new CacheWrapper(PDF_ITEM_INFO, "pdfItemInfo"));
        MAP_MEM.put("pdfItemInfo", new CacheWrapper(PDF_ITEM_INFO, "pdfItemInfo"));
        CACHE_STATS.put("pdfItemInfo", statsService.getCacheStatistics("pdfItemInfo"));

        IMAGE_GROUP_INFO = cacheManager.createCache("imageGroupInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImageGroupInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("imageGroupInfo", new CacheWrapper(IMAGE_GROUP_INFO, "imageGroupInfo"));
        MAP_MEM.put("imageGroupInfo", new CacheWrapper(IMAGE_GROUP_INFO, "imageGroupInfo"));
        CACHE_STATS.put("imageGroupInfo", statsService.getCacheStatistics("imageGroupInfo"));

        IMAGE_LIST_INFO = cacheManager.createCache("imageListInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, List.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("imageListInfo", new CacheWrapper(IMAGE_LIST_INFO, "imageListInfo"));
        MAP_MEM.put("imageListInfo", new CacheWrapper(IMAGE_LIST_INFO, "imageListInfo"));
        CACHE_STATS.put("imageListInfo", statsService.getCacheStatistics("imageListInfo"));
        
        BVM = cacheManager.createCache("bvm", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, SimpleBVM.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, EntryUnit.ENTRIES)));
        MAP.put("bvm", new CacheWrapper(BVM, "bvm"));
        MAP_MEM.put("bvm", new CacheWrapper(BVM, "bvm"));
        CACHE_STATS.put("bvm", statsService.getCacheStatistics("bvm"));
        
        //cacheManager.close();

    }

    public static CacheWrapper getCache(String name) {
        return MAP.get(name);
    }

    public static Set<String> getDiskCachesNames() {
        return MAP_DISK.keySet();
    }

    public static List<CacheWrapper> getAllDiskCaches() {
        List<CacheWrapper> caches = new ArrayList<>();
        for (String name : getDiskCachesNames()) {
            caches.add(MAP_DISK.get(name));
        }
        return caches;
    }

    public static Set<String> getMemoryCachesNames() {
        return MAP_MEM.keySet();
    }

    public static List<CacheWrapper> getAllMemoryCaches() {
        List<CacheWrapper> caches = new ArrayList<>();
        for (String name : getMemoryCachesNames()) {
            caches.add(MAP_MEM.get(name));
        }
        return caches;
    }

    public static Set<String> getAllCachesNames() {
        return MAP.keySet();
    }

    public static List<CacheWrapper> getAllAnyCaches() {
        List<CacheWrapper> caches = new ArrayList<>();
        for (String name : getAllCachesNames()) {
            caches.add(MAP.get(name));
        }
        return caches;
    }

    public static HashMap<String, CacheWrapper> getDiskCaches() {
        return MAP_DISK;
    }

    public static HashMap<String, CacheWrapper> getMemoryCaches() {
        return MAP_MEM;
    }

    public static HashMap<String, CacheWrapper> getAllCaches() {
        return MAP;
    }

    public static CacheStatistics getCacheStatistics(String name) {
        log.debug("CACHE STATISTICS FOR {} are {}", name, CACHE_STATS.get(name));
        return CACHE_STATS.get(name);
    }

    public static Map<String, TierStatistics> getTierStatistics(String name) {
        log.debug("TIER STATISTICS FOR {} are {}", name, CACHE_STATS.get(name).getTierStatistics());
        return CACHE_STATS.get(name).getTierStatistics();
    }

    public static Object get(String cacheName, String key) throws IIIFException {
        log.debug("EHServerCACHE get from {} with key {}", cacheName, key);
        CacheMetrics.cacheGet(cacheName);
        return getCache(cacheName).get(key);
    }

    public static boolean constainsKey(String cacheName, String key) throws IIIFException {
        log.debug("EHServerCACHE isInCache from {} with key {}", cacheName, key);
        return getCache(cacheName).containsKey(key);
    }
    
    public static void put(String cacheName, String key, Object obj) throws IIIFException {
        log.debug("EHServerCACHE put in {} for key {}", cacheName, key);
        CacheMetrics.cachePut(cacheName);
        getCache(cacheName).put(key, obj);
    }

    public static boolean clearCache() {
        try {
            IIIF_IMG.clear(false);
            IIIF_ZIP.clear(false);
            IIIF_PDF.clear(false);
            PDF_ITEM_INFO.clear();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}