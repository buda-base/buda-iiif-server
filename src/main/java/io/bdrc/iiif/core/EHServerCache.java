package io.bdrc.iiif.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.DefaultStatisticsService;
import org.ehcache.core.statistics.TierStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.archives.ArchiveInfo;
import io.bdrc.iiif.archives.PdfItemInfo;
import io.bdrc.iiif.resolver.ImageGroupInfo;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EHServerCache {

    private static final Logger log = LoggerFactory.getLogger(EHServerCache.class);

    public static Cache<String, byte[]> IIIF_IMG;
    public static Cache<String, byte[]> IIIF_ZIP;
    public static Cache<String, byte[]> IIIF;
    public static Cache<String, PdfItemInfo> PDF_ITEM_INFO;
    public static Cache<String, Boolean> PDF_JOBS;
    public static Cache<String, Boolean> ZIP_JOBS;
    public static Cache<String, ArchiveInfo> ARCHIVE_INFO;
    public static Cache<String, ImageGroupInfo> IMAGE_GROUP_INFO;
    public static Cache<String, List> IMAGE_LIST_INFO;
    private static HashMap<String, CacheWrapper> MAP;
    private static HashMap<String, CacheWrapper> MAP_DISK;
    private static HashMap<String, CacheWrapper> MAP_MEM;
    private static StatisticsService statsService;
    private static HashMap<String, CacheStatistics> CACHE_STATS;

    public static void init() {
        MAP = new HashMap<>();
        MAP_DISK = new HashMap<>();
        MAP_MEM = new HashMap<>();
        // We must reference CacheStatistics object
        // so they are not garbaed collected before being passed to prometheus
        CACHE_STATS = new HashMap<>();
        statsService = new DefaultStatisticsService();
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().using(statsService).build();
        cacheManager.init();

        /**** PERSISTENT CACHES ***/
        PersistentCacheManager iiif_img = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF_IMG")).build(true);
        IIIF_IMG = iiif_img.createCache("iiif_img", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2000, EntryUnit.ENTRIES).disk(15000, MemoryUnit.MB, true)));
        MAP.put("iiif_img", new CacheWrapper(IIIF_IMG, "iiif_img"));
        MAP_DISK.put("iiif_img", new CacheWrapper(IIIF_IMG, "iiif_img"));
        CACHE_STATS.put("iiif_img", statsService.getCacheStatistics("iiif_img"));

        PersistentCacheManager iiif_zip = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF_ZIP")).build(true);
        IIIF_ZIP = iiif_zip.createCache("iiif_zip", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));
        MAP.put("iiif_zip", new CacheWrapper(IIIF_ZIP, "iiif_zip"));
        MAP_DISK.put("iiif_zip", new CacheWrapper(IIIF_ZIP, "iiif_zip"));
        CACHE_STATS.put("iiif_zip", statsService.getCacheStatistics("iiif_zip"));

        PersistentCacheManager iiif = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF")).build(true);
        IIIF = iiif.createCache("iiif", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));
        MAP.put("iiif", new CacheWrapper(IIIF, "iiif"));
        MAP_DISK.put("iiif", new CacheWrapper(IIIF, "iiif"));
        CACHE_STATS.put("iiif", statsService.getCacheStatistics("iiif"));

        /**** MEMORY CACHES ***/
        PDF_ITEM_INFO = cacheManager.createCache("pdfItemInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                PdfItemInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("pdfItemInfo", new CacheWrapper(PDF_ITEM_INFO, "pdfItemInfo"));
        MAP_MEM.put("pdfItemInfo", new CacheWrapper(PDF_ITEM_INFO, "pdfItemInfo"));
        CACHE_STATS.put("pdfItemInfo", statsService.getCacheStatistics("pdfItemInfo"));

        PDF_JOBS = cacheManager.createCache("pdfjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("pdfjobs", new CacheWrapper(PDF_JOBS, "pdfjobs"));
        MAP_MEM.put("pdfjobs", new CacheWrapper(PDF_JOBS, "pdfjobs"));
        CACHE_STATS.put("pdfjobs", statsService.getCacheStatistics("pdfjobs"));

        ZIP_JOBS = cacheManager.createCache("zipjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("zipjobs", new CacheWrapper(ZIP_JOBS, "zipjobs"));
        MAP_MEM.put("zipjobs", new CacheWrapper(ZIP_JOBS, "zipjobs"));
        CACHE_STATS.put("zipjobs", statsService.getCacheStatistics("zipjobs"));

        ARCHIVE_INFO = cacheManager.createCache("archiveInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArchiveInfo.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("archiveInfo", new CacheWrapper(ARCHIVE_INFO, "archiveInfo"));
        MAP_MEM.put("archiveInfo", new CacheWrapper(ARCHIVE_INFO, "archiveInfo"));
        CACHE_STATS.put("archiveInfo", statsService.getCacheStatistics("archiveInfo"));

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
        log.info("CACHE STATISTICS FOR {} are {}", name, CACHE_STATS.get(name));
        return CACHE_STATS.get(name);
    }

    public static Map<String, TierStatistics> getTierStatistics(String name) {
        log.info("TIER STATISTICS FOR {} are {}", name, CACHE_STATS.get(name).getTierStatistics());
        return CACHE_STATS.get(name).getTierStatistics();
    }

    public static boolean clearCache() {
        try {
            IIIF_IMG.clear();
            IIIF_ZIP.clear();
            IIIF.clear();
            PDF_ITEM_INFO.clear();
            PDF_JOBS.clear();
            ZIP_JOBS.clear();
            ARCHIVE_INFO.clear();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}