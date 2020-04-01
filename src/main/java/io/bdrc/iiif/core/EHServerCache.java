package io.bdrc.iiif.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
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

import io.bdrc.iiif.archives.ArchiveInfo;
import io.bdrc.iiif.archives.PdfItemInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageGroupInfo;

@SuppressWarnings("rawtypes")
public class EHServerCache {

    public static Cache<String, byte[]> IIIF_IMG;
    public static Cache<String, byte[]> IIIF_ZIP;
    public static Cache<String, byte[]> IIIF;
    public static Cache<String, PdfItemInfo> PDF_ITEM_INFO;
    public static Cache<String, Boolean> PDF_JOBS;
    public static Cache<String, Boolean> ZIP_JOBS;
    public static Cache<String, ArchiveInfo> ARCHIVE_INFO;
    public static Cache<String, IdentifierInfo> IDENTIFIER;
    public static Cache<String, ImageGroupInfo> IMAGE_GROUP_INFO;
    public static Cache<String, List> IMAGE_LIST_INFO;
    private static HashMap<String, Cache> MAP;
    private static HashMap<String, Cache> MAP_DISK;
    private static HashMap<String, Cache> MAP_MEM;
    private static StatisticsService statsService;

    static {
        MAP = new HashMap<>();
        statsService = new DefaultStatisticsService();
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().using(statsService).build();
        cacheManager.init();

        /**** PERSISTENT CACHES ***/
        PersistentCacheManager iiif_img = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF_IMG")).build(true);
        IIIF_IMG = iiif_img.createCache("iiif_img", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2000, EntryUnit.ENTRIES).disk(15000, MemoryUnit.MB, true)));
        MAP.put("iiif_img", IIIF_IMG);
        MAP_DISK.put("iiif_img", IIIF_IMG);

        PersistentCacheManager iiif_zip = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF_ZIP")).build(true);
        IIIF_ZIP = iiif_zip.createCache("iiif_zip", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));
        MAP.put("iiif_zip", IIIF_ZIP);
        MAP_DISK.put("iiif_zip", IIIF_ZIP);

        PersistentCacheManager iiif = CacheManagerBuilder.newCacheManagerBuilder().using(statsService)
                .with(CacheManagerBuilder.persistence(System.getProperty("user.dir") + File.separator + "EH_IIIF")).build(true);
        IIIF = iiif.createCache("iiif", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));
        MAP.put("iiif", IIIF);
        MAP_DISK.put("iiif", IIIF);

        /**** MEMORY CACHES ***/
        PDF_ITEM_INFO = cacheManager.createCache("pdfItemInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                PdfItemInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("pdfItemInfo", PDF_ITEM_INFO);
        MAP_MEM.put("pdfItemInfo", PDF_ITEM_INFO);

        PDF_JOBS = cacheManager.createCache("pdfjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("pdfjobs", PDF_JOBS);
        MAP_MEM.put("pdfjobs", PDF_JOBS);

        ZIP_JOBS = cacheManager.createCache("zipjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("zipjobs", ZIP_JOBS);
        MAP_MEM.put("zipjobs", ZIP_JOBS);

        ARCHIVE_INFO = cacheManager.createCache("archiveInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArchiveInfo.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("archiveInfo", ARCHIVE_INFO);
        MAP_MEM.put("archiveInfo", ARCHIVE_INFO);

        IDENTIFIER = cacheManager.createCache("identifierInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                IdentifierInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("identifierInfo", IDENTIFIER);
        MAP_MEM.put("identifierInfo", IDENTIFIER);

        IMAGE_GROUP_INFO = cacheManager.createCache("imageGroupInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImageGroupInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("imageGroupInfo", IMAGE_GROUP_INFO);
        MAP_MEM.put("imageGroupInfo", IMAGE_GROUP_INFO);

        IMAGE_LIST_INFO = cacheManager.createCache("imageListInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, List.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        MAP.put("imageListInfo", IMAGE_LIST_INFO);
        MAP_MEM.put("imageListInfo", IMAGE_LIST_INFO);

    }

    public static Cache getCache(String name) {
        return MAP.get(name);
    }

    public static Set<String> getDiskCachesNames() {
        return MAP_DISK.keySet();
    }

    public static Set<String> getMemoryCachesNames() {
        return MAP_MEM.keySet();
    }

    public static Set<String> getAllCachesNames() {
        return MAP.keySet();
    }

    public static HashMap<String, Cache> getDiskCaches(String name) {
        return MAP_DISK;
    }

    public static HashMap<String, Cache> getMemoryCaches() {
        return MAP_MEM;
    }

    public static HashMap<String, Cache> getAllCaches() {
        return MAP;
    }

    public static CacheStatistics getCacheStatistics(String name) {
        return statsService.getCacheStatistics(name);
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
            IDENTIFIER.clear();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}