package io.bdrc.pdf;

import java.util.HashMap;
import java.util.Set;

public class PdfServiceRegistry {
    
    private static HashMap<String,Boolean> REGISTRY;        
        
    private PdfServiceRegistry() {
        REGISTRY=new HashMap<>();
    }
    
    private static class SingletonHolder {       
        private final static PdfServiceRegistry INSTANCE = new PdfServiceRegistry();
    }
    
    public static PdfServiceRegistry getInstance(){
        return SingletonHolder.INSTANCE;
    }
    
    public void addPdfService(String id, boolean done) {
        REGISTRY.put(id, done);
    }
    
    public void setCompleted(String id) {
        REGISTRY.put(id,true);
    }
    
    public boolean isDone(String id) {
        return REGISTRY.get(id);
    }
    
    public void removePdfService(String id) {
        REGISTRY.remove(id);
    }
    
    public Set<String> getCurrentRefs(){
        return REGISTRY.keySet();
    }
}
