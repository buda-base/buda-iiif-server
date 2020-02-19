package de.digitalcollections.iiif.myhymir;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;

public class CacheAccessModel {

    private final static String DEFAULT = "IIIF_IMG";

    public CacheAccessModel() {
        super();
    }

    /**
     * Cache config
     */
    private ICompositeCacheAttributes getConfig() {
        return ServerCache.getCacheAccess("IIIF_IMG").getCacheAttributes();
    }

    private IElementAttributes getElementConfig() {
        return ServerCache.getCacheAccess("IIIF_IMG").getDefaultElementAttributes();
    }

    private ICompositeCacheAttributes getConfig(String cacheName) {
        return ServerCache.getCacheAccess(cacheName).getCacheAttributes();
    }

    private IElementAttributes getElementConfig(String cacheName) {
        return ServerCache.getCacheAccess(cacheName).getDefaultElementAttributes();
    }

    public String getCacheName() {
        return getConfig().getCacheName();
    }

    public ICompositeCacheAttributes.DiskUsagePattern getDiskUsagePattern() {
        return getConfig().getDiskUsagePattern();
    }

    public ICompositeCacheAttributes.DiskUsagePattern getDiskUsagePattern(String cacheName) {
        return getConfig(cacheName).getDiskUsagePattern();
    }

    public long getMaxLife() {
        return getElementConfig().getMaxLife();
    }

    public long getMaxLife(String cacheName) {
        return getElementConfig(cacheName).getMaxLife();
    }

    public long getMaxMemoryIdleTimeSeconds() {
        return getConfig().getMaxMemoryIdleTimeSeconds();
    }

    public long getMaxMemoryIdleTimeSeconds(String cacheName) {
        return getConfig(cacheName).getMaxMemoryIdleTimeSeconds();
    }

    public int getMaxObjects() {
        return getConfig().getMaxObjects();
    }

    public int getMaxObjects(String cacheName) {
        return getConfig(cacheName).getMaxObjects();
    }

    public int getMaxSpoolPerRun() {
        return getConfig().getMaxSpoolPerRun();
    }

    public int getMaxSpoolPerRun(String cacheName) {
        return getConfig(cacheName).getMaxSpoolPerRun();
    }

    public String getMemoryCacheName() {
        String tmp = getConfig().getMemoryCacheName();
        return tmp.substring(tmp.lastIndexOf('.') + 1);
    }

    public String getMemoryCacheName(String cacheName) {
        String tmp = getConfig(cacheName).getMemoryCacheName();
        return tmp.substring(tmp.lastIndexOf('.') + 1);
    }

    public long getShrinkerIntervalSeconds() {
        return getConfig().getShrinkerIntervalSeconds();
    }

    public long getShrinkerIntervalSeconds(String cacheName) {
        return getConfig(cacheName).getShrinkerIntervalSeconds();
    }

    public boolean isUseDisk() {
        return getConfig().isUseDisk();
    }

    public boolean isUseDisk(String cacheName) {
        return getConfig(cacheName).isUseDisk();
    }

    public boolean isUseLateral() {
        return getConfig().isUseLateral();
    }

    public boolean isUseLateral(String cacheName) {
        return getConfig(cacheName).isUseLateral();
    }

    public boolean isUseMemoryShrinker() {
        return getConfig().isUseMemoryShrinker();
    }

    public boolean isUseMemoryShrinker(String cacheName) {
        return getConfig(cacheName).isUseMemoryShrinker();
    }

    public boolean isUseRemote() {
        return getConfig().isUseRemote();
    }

    public boolean isUseRemote(String cacheName) {
        return getConfig(cacheName).isUseRemote();
    }

    /**
     * Cache stats
     */
    private IElementAttributes getRegionConfig() {
        return ServerCache.getCacheAccess(DEFAULT).getDefaultElementAttributes();
    }

    private CompositeCache<Object, Object> getCacheControl() {
        return ServerCache.getCacheAccess(DEFAULT).getCacheControl();
    }

    private IElementAttributes getRegionConfig(String cacheName) {
        return ServerCache.getCacheAccess(cacheName).getDefaultElementAttributes();
    }

    private CompositeCache<Object, Object> getCacheControl(String cacheName) {
        return ServerCache.getCacheAccess(cacheName).getCacheControl();
    }

    public CacheStatus getStatus() {
        return getCacheControl().getStatus();
    }

    public CacheStatus getStatus(String cacheName) {
        return getCacheControl(cacheName).getStatus();
    }

    public String getCreateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        return simpleDateFormat.format(getRegionConfig().getCreateTime());
    }

    public String getCreateTime(String cacheName) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        return simpleDateFormat.format(getRegionConfig(cacheName).getCreateTime());
    }

    public String getLastAccessTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        return simpleDateFormat.format(getRegionConfig().getLastAccessTime());
    }

    public String getLastAccessTime(String cacheName) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        return simpleDateFormat.format(getRegionConfig(cacheName).getLastAccessTime());
    }

    public int getSize() {
        return getCacheControl().getSize();
    }

    public int getSize(String cacheName) {
        return getCacheControl(cacheName).getSize();
    }

    public int getMissCountExpired() {
        return getCacheControl().getMissCountExpired();
    }

    public int getMissCountExpired(String cacheName) {
        return getCacheControl(cacheName).getMissCountExpired();
    }

    public int getHitCountAux() {
        return getCacheControl().getHitCountAux();
    }

    public int getHitCountAux(String cacheName) {
        return getCacheControl(cacheName).getHitCountAux();
    }

    public int getHitCountRam() {
        return getCacheControl().getHitCountRam();
    }

    public int getHitCountRam(String cacheName) {
        return getCacheControl(cacheName).getHitCountRam();
    }

    public int getMissCountNotFound() {
        return getCacheControl().getMissCountNotFound();
    }

    public int getMissCountNotFound(String cacheName) {
        return getCacheControl(cacheName).getMissCountNotFound();
    }

    /**
     * Memory stuff
     */

    public MemoryUsage getHeap() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    public MemoryUsage getNonHeap() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
    }

    private static HashMap<ObjectName, MemoryPoolMXBean> getMemoryPoolBean() {
        List<MemoryPoolMXBean> l = ManagementFactory.getMemoryPoolMXBeans();
        HashMap<ObjectName, MemoryPoolMXBean> map = new HashMap<>();
        for (MemoryPoolMXBean mx : l) {
            map.put(mx.getObjectName(), mx);
        }
        return map;
    }

    public MemoryUsage getCodeMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Code Cache")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public MemoryUsage getMetaMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Metaspace")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public MemoryUsage getCompressedMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Compressed Class Space")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public MemoryUsage getEdenMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public MemoryUsage getSurvivorMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Survivor Space")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public MemoryUsage getOldMemoryUsage() {
        try {
            return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Old Gen")).getUsage();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public int getPending() {
        return ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount();
    }

    public String getHeapCommitted() {
        return format(getHeap().getCommitted());
    }

    public String getNonHeapCommitted() {
        return format(getNonHeap().getCommitted());
    }

    public String getHeapInit() {

        return format(getHeap().getInit());
    }

    public String getNonHeapInit() {
        return format(getNonHeap().getInit());
    }

    public String getHeapMax() {
        return format(getHeap().getMax());
    }

    public String getNonHeapMax() {
        return format(getNonHeap().getMax());
    }

    public String getHeapUsed() {
        return format(getHeap().getUsed());
    }

    public String getNonHeapUsed() {
        return format(getNonHeap().getUsed());
    }
    /*
     * Utilities
     */

    public String format(long l) {
        return new DecimalFormat("#,###,###").format(l);
    }

}
