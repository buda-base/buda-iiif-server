package io.bdrc.iiif.core;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

public class DiskCache {

    int nbSecondsMax;
    int nbItemsMax;
    int sizeMaxMB;
    MessageDigest md;
    File dir;
    
    static final int STREAMTODISK = 0;
    static final int STREAMFROMDISK = 1;
    static final int DONE = 2;
    
    static final class Status {
        int status;
        Date lastActivityDate;
        OutputStream os;
        int sizeMB = 0;
        
        Status(OutputStream os) {
            lastActivityDate = new Date();
            this.os = os;
            this.status = STREAMTODISK;
        }
        
        void setDone(int sizeMB) {
            this.status = DONE;
            lastActivityDate = new Date();
            this.os = null;
            this.sizeMB = sizeMB;
        }
        
        void setAccess() {
            //this.status = STREAMFROMDISK;
            lastActivityDate = new Date();
        }
    }
    
    public Map<String,Status> items;
    
    public DiskCache(String path, int nbSecondsMax, int nbItemsMax, int sizeMaxMB) {
        // create directory on path
        clear(); // or perhaps cleanup()?
        this.items = new HashMap<>();
        this.nbSecondsMax = nbSecondsMax;
        this.nbItemsMax = nbItemsMax;
        this.sizeMaxMB = sizeMaxMB;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // this is too stupid to throw
        }
        this.dir = new File(path);
    }
    
    synchronized private File keyToFile(String key) {
        md.reset();
        md.update(key.getBytes(Charset.forName("UTF8")));
        String fbase = new String(Hex.encodeHex(md.digest()));
        return new File(this.dir, fbase);
    }
    
    public void clear() {
        // remove all the files, except those being written?
    }
    
    public void cleanup() {
        // remove all the files with a date of more than 1h
        // do it in a thread?
        // also remove stream to files that seem stall and are streaming for more than 10mn
    }
    
    public boolean hasKey(String key) {
        // return true if status is done
        Status s = this.items.get(key);
        if (s == null)
            return false;
        return s.status != STREAMTODISK;
    }
    
    public InputStream getIs(String key) {
        return null;
        // return null if streaming to the file is not finished yet
    }
    
    public OutputStream getOs(String key) {
        // return null if streaming the file is not finished yet
        return null;
    }
    
    public void outputDone(String key) {
        // called when the streaming to a file is finished
    }
    
    public void remove(String key, boolean force) {
        // remove the file from the cache
        
    }
}
