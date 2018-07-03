package io.bdrc.archives;

import java.util.HashMap;
import java.util.Set;

public class ArchivesServiceRegistry {
    
    private static HashMap<String,Boolean> PDF_REGISTRY;
    private static HashMap<String,Boolean> ZIP_REGISTRY;
        
    private ArchivesServiceRegistry() {
        PDF_REGISTRY=new HashMap<>();
        ZIP_REGISTRY=new HashMap<>();
    }
    
    private static class SingletonHolder {       
        private final static ArchivesServiceRegistry INSTANCE = new ArchivesServiceRegistry();
    }
    
    public static ArchivesServiceRegistry getInstance(){
        return SingletonHolder.INSTANCE;
    }
    
    public void addPdfService(String id, boolean done) {
        PDF_REGISTRY.put(id, done);
    }
    
    public void addZipService(String id, boolean done) {
        ZIP_REGISTRY.put(id, done);
    }
    
    public void setPdfCompleted(String id) {
        PDF_REGISTRY.put(id,true);
    }
    
    public void setZipCompleted(String id) {
        ZIP_REGISTRY.put(id,true);
    }
    
    public boolean isPdfDone(String id) {
        if(PDF_REGISTRY.get(id)==null) {
            return false;
        }
        return PDF_REGISTRY.get(id);
    }
    
    public boolean isZipDone(String id) {
        if(ZIP_REGISTRY.get(id)==null) {
            return false;
        }
        return ZIP_REGISTRY.get(id);
    }
    
    public void removePdfService(String id) {
        PDF_REGISTRY.remove(id);
    }
    
    public void removeZipService(String id) {
        ZIP_REGISTRY.remove(id);
    }
    
    public Set<String> getCurrentPdfRefs(){
        return PDF_REGISTRY.keySet();
    }
    
    public Set<String> getCurrentZipRefs(){
        return ZIP_REGISTRY.keySet();
    }
}
