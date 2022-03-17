package io.bdrc.iiif.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class DiskCache {

    public int nbSecondsMax;
    int nbItemsMax;
    long sizeMaxMB;
    MessageDigest md;
    File dir;
    int cleanupClockS;
    String cacheName;
    
    Instant lastCleanup = null;
    
    final Logger logger;
    
    static final int STREAMTODISK = 0;
    static final int STREAMFROMDISK = 1;
    static final int DONE = 2;
    
    static final class Status implements Comparator<Status> {
        int status;
        Instant lastActivityDate;
        @JsonIgnore
        OutputStream os;
        long size = 0;
        
        Status(OutputStream os) {
            lastActivityDate = Instant.now();
            this.os = os;
            this.status = STREAMTODISK;
        }
        
        void setDone(long size) {
            this.status = DONE;
            lastActivityDate = Instant.now();
            this.os = null;
            this.size = size;
        }
        
        void setAccess() {
            //this.status = STREAMFROMDISK;
            lastActivityDate = Instant.now();
        }

        @Override
        public int compare(Status s0, Status s1) {
            return s0.lastActivityDate.compareTo(s1.lastActivityDate);
        }
    }
    
    public Map<String,Status> items;
    
    ExecutorService service;
    
    public DiskCache(String path, int cleanupClockS, int nbSecondsMax, int nbItemsMax, long sizeMaxMB, String cacheName) throws IOException {
        this.logger = LoggerFactory.getLogger("DiskCache@"+cacheName);
        this.cacheName = cacheName;
        this.items = new ConcurrentHashMap<>();
        this.nbSecondsMax = nbSecondsMax;
        this.nbItemsMax = nbItemsMax;
        this.sizeMaxMB = sizeMaxMB;
        this.cleanupClockS = cleanupClockS;
        this.service = Executors.newFixedThreadPool(1);
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // this is too stupid to throw
        }
        this.dir = new File(path);
        if (!this.dir.exists()) {
            logger.info("create cache directory {}", this.dir);
            Files.createDirectories(this.dir.toPath());
        }
        if (!this.dir.canWrite() || !this.dir.canRead()) {
            logger.error("can't write in {}", this.dir);
            throw new IOException("can't write in "+this.dir);
        }
        clear(true);
    }
    
    synchronized private File keyToFile(String key) {
        md.reset();
        md.update(key.getBytes(Charset.forName("UTF8")));
        String fbase = new String(Hex.encodeHex(md.digest()));
        return new File(this.dir, fbase);
    }
    
    public void clear(boolean force) throws IOException {
        if (force) {
            FileUtils.cleanDirectory(this.dir); 
        }
        // TODO: non-force case?
    }
    
    synchronized void cleanuptick() {
        Instant now = Instant.now();
        if (this.lastCleanup == null) {
            this.lastCleanup = now;
            return;
        }
        long secondsDiff = (now.getEpochSecond()-this.lastCleanup.getEpochSecond());
        if (secondsDiff > this.cleanupClockS) {
            this.cleanup();
        }
    }
     
    public void cleanup() {
        this.lastCleanup = Instant.now();
        DiskCacheCleanup dcc = new DiskCacheCleanup(this);
        this.service.submit(dcc);
    }
    
    synchronized public boolean hasKey(String key) {
        // return true if status is done
        Status s = this.items.get(key);
        if (s == null)
            return false;
        return s.status != STREAMTODISK;
    }
    
    synchronized public InputStream getIs(String key) {
        File inf = this.keyToFile(key);
        Status status = this.items.get(key);
        if (status == null) {
            if (inf.exists()) {
                this.logger.error("file exists on disk but not in queue for key {}", key);
                inf.delete();
            }
            return null;
        }
        if (status.status == STREAMTODISK) {
            this.logger.error("request inputstream for file not fully written, this shouldn't happen");
            return null;
        }
        try {
            FileInputStream res = new FileInputStream(inf);
            status.setAccess();
            return res;
        } catch (FileNotFoundException e) {
            this.logger.error("file in queue but doesn't exist for key {}", key);
            this.items.remove(key);
            return null;
        }
    }
    
    synchronized public OutputStream getOs(String key) {
        File inf = this.keyToFile(key);
        Status status = this.items.get(key);
        if (status != null) {
            if (status.status == STREAMTODISK) {
                this.logger.error("request outputstream for file not fully written, this shouldn't happen");
                return null;
            }
            if (inf.exists()) {
                this.logger.error("request outputstream for a file already fully written, this shouldn't happen");
                return null;
            }
            this.logger.error("file in queue but doesn't exist for key {}", key);
            this.items.remove(key);
        }
        if (inf.exists()) {
            this.logger.error("file exists on disk but not in queue for key {}", key);
            inf.delete();
        }
        FileOutputStream res;
        try {
            res = new FileOutputStream(inf);
        } catch (FileNotFoundException e) {
            this.logger.error("cannot create file {}, this is quite serious!", inf.getAbsolutePath());
            return null;
        }
        this.items.put(key, new Status(res));
        return res;
    }
    
    synchronized public void outputDone(String key) {
        File inf = this.keyToFile(key);
        Status status = this.items.get(key);
        if (status == null) {
            this.logger.error("calling outputDone but key not in the queue: {}", key);
            return;
        }
        if (!inf.exists()) {
            if (status.status != STREAMTODISK) {
                this.logger.error("key exists in queue but no file on disk: {}", key);
            } else {
                this.logger.error("key exists in queue but no file on disk, perhaps still streaming?: {}", key);
            }
            return;
        }
        if (status.status != STREAMTODISK) {
            this.logger.error("calling outputDone on file already done: {}", key);
        }
        long size = FileUtils.sizeOf(inf);
        if (size == 0) {
            this.logger.error("can't get size of: {}", inf.getAbsolutePath());
            return;
        }
        status.setDone(size);
        this.cleanuptick();
    }
    
    public void remove(String key, boolean force) {
        // remove the file from the cache
        File inf = this.keyToFile(key);
        Status status = this.items.get(key);
        if (status == null) {
            if (inf.exists()) {
                this.logger.error("file exists on disk but not in queue for key {}", key);
                inf.delete();
            }
            return;
        }
        if (status.status == STREAMTODISK && !force) {
            return;
        }
        this.items.remove(key);
        if (inf.exists()) {
            inf.delete();
        }
    }
}
