package io.bdrc.iiif.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import io.bdrc.iiif.core.DiskCache.Status;

public class DiskCacheCleanup implements Callable<Void> {

    DiskCache dc;
    Instant now;
    
    
    public DiskCacheCleanup(DiskCache dc) {
        this.dc = dc;
        this.now = Instant.now();
    }
    
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
    
    static final class MapEntryComparatorByDateAsc implements Comparator<Entry<String,Status>> {
        @Override
        public int compare(Entry<String,Status> o1, Entry<String,Status> o2) {
            return o1.getValue().lastActivityDate.compareTo(o2.getValue().lastActivityDate);
        }
    }
    
    public static LinkedHashMap<String,Status> getKeyQueue(Map<String,Status> items) {
        List<Entry<String, Status>> list = new ArrayList<>(items.entrySet());
        Collections.sort(list, new MapEntryComparatorByDateAsc());

        LinkedHashMap<String, Status> result = new LinkedHashMap<>();
        for (Entry<String, Status> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public Void call() {
        // first remove old entries plus the ones that are too numerous
        LinkedHashMap<String,Status> queue = getKeyQueue(this.dc.items);
        int totalItems = queue.size();
        if (totalItems == 0)
            return null;
        final Instant removeAllBefore = this.now.minusSeconds(this.dc.nbSecondsMax);
        for (Entry<String,Status> e : queue.entrySet()) {
            // if we don't need to remove more items, break:
            if (e.getValue().lastActivityDate.compareTo(removeAllBefore) <= 0 || (this.dc.nbItemsMax > 0 && totalItems <= this.dc.nbItemsMax)) {
                this.dc.remove(e.getKey(), true);
            }
            totalItems -= 1;
        }
        if (this.dc.sizeMaxMB == 0)
            return null;
        // compute remaining size on disk:
        // recompute the queue first
        queue = getKeyQueue(this.dc.items);
        long totalSize = 0;
        for (Status s : queue.values()) {
            totalSize += s.size;
        }
        long sizeToRemove = totalSize - (this.dc.sizeMaxMB * 1048576);
        if (sizeToRemove <= 0)
            return null;
        for (Entry<String,Status> e : queue.entrySet()) {
            long thissize = e.getValue().size;
            this.dc.remove(e.getKey(), true);
            sizeToRemove -= thissize;
            if (sizeToRemove <= 0)
                break;
        }
        return null;
    }

}
