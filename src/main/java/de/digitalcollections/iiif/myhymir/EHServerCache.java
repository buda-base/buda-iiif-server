package de.digitalcollections.iiif.myhymir;

import java.io.File;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import io.bdrc.archives.ArchiveInfo;
import io.bdrc.archives.PdfItemInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class EHServerCache {

    public static Cache<String, byte[]> IIIF_IMG;
    public static Cache<String, byte[]> IIIF_ZIP;
    public static Cache<String, byte[]> IIIF;
    public static Cache<String, PdfItemInfo> PDF_ITEM_INFO;
    public static Cache<String, Boolean> PDF_JOBS;
    public static Cache<String, Boolean> ZIP_JOBS;
    public static Cache<String, ArchiveInfo> ARCHIVE_INFO;
    public static Cache<String, IdentifierInfo> IDENTIFIER;

    static {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();

        /**** PERSISTENT CACHES ***/
        PersistentCacheManager iiif_img = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(System.getProperty("iiifserv.configpath") + File.separator + "EH_IIIF_IMG")).build(true);
        IIIF_IMG = iiif_img.createCache("iiif_img", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(10000, MemoryUnit.MB, true)));

        PersistentCacheManager iiif_zip = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(System.getProperty("iiifserv.configpath") + File.separator + "EH_IIIF_ZIP")).build(true);
        IIIF_ZIP = iiif_zip.createCache("iiif_zip", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));

        PersistentCacheManager iiif = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(System.getProperty("iiifserv.configpath") + File.separator + "EH_IIIF")).build(true);
        IIIF = iiif_img.createCache("iiif", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES).disk(5000, MemoryUnit.MB, true)));

        /**** MEMORY CACHES ***/
        PDF_ITEM_INFO = cacheManager.createCache("pdfItemInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                PdfItemInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));

        PDF_JOBS = cacheManager.createCache("pdfjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));

        ZIP_JOBS = cacheManager.createCache("zipjobs", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));

        ARCHIVE_INFO = cacheManager.createCache("archiveInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArchiveInfo.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));

        IDENTIFIER = cacheManager.createCache("identifierInfo", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                IdentifierInfo.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));

    }

}
