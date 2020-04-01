package io.bdrc.iiif.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class CacheAndMemoryMetrics {

    public CacheAndMemoryMetrics() {
        super();
    }

    /**
     * Cache config
     */

    /**
     * Cache stats
     */

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

    public MemoryUsage getCodeMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Code Cache")).getUsage();
    }

    public MemoryUsage getMetaMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Metaspace")).getUsage();
    }

    public MemoryUsage getCompressedMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=Compressed Class Space")).getUsage();
    }

    public MemoryUsage getEdenMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space")).getUsage();
    }

    public MemoryUsage getSurvivorMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Survivor Space")).getUsage();
    }

    public MemoryUsage getOldMemoryUsage() throws MalformedObjectNameException {
        return getMemoryPoolBean().get(new ObjectName("java.lang:type=MemoryPool,name=PS Old Gen")).getUsage();
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
