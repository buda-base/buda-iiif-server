package io.bdrc.iiif.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class SimpleBVM {
    
    public static final class TrueFilter {

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof Boolean)) {
                return false;
            }
            final Boolean v = (Boolean) obj;
            return Boolean.TRUE.equals(v);
        }
    }

    public static final class BVMPagination {
        @JsonProperty(value="id", required=true)
        public String id = null;
      
        public BVMPagination() { }
    }

    public static final class BVMSection {
        @JsonProperty(value="id", required=true)
        public String id = null;
      
        public BVMSection() { }
    }

    public static final class BVMPaginationItem {
        @JsonProperty(value="value", required=true)
        public String value = null;
        @JsonProperty("section")
        public String section = null;
      
        public BVMPaginationItem() { }
        
        public BVMSection getSection(final SimpleBVM root) {
            return root.getSection(section);
        }
    }
    
    @JsonIgnoreProperties({"id"})
    public static final class BVMImageInfo {
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("filename")
        public String filename = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("imggroup")
        public String imggroupQname = null;
        @JsonInclude(value = Include.NON_NULL)
        @JsonProperty(value="sourcePath")
        public String sourcePath = null;
        @JsonInclude(Include.NON_NULL)
        @JsonProperty("pagination")
        public Map<String,BVMPaginationItem> pagination = null;
        
        public BVMImageInfo() {}
        
        @JsonIgnore
        public BVMPaginationItem getDefaultPaginationValue(SimpleBVM root) {
            if (this.pagination == null) return null;
            for (final BVMPagination p : root.pagination) {
                if (this.pagination.containsKey(p.id)) {
                    return pagination.get(p.id);
                }
            }
            return null;
        }
        
    }

    public static final class BVMView {
        @JsonProperty(value="imagelist", required=true)
        public List<BVMImageInfo> imageList = null;
    }
    
    @JsonProperty(value="rev", required=true)
    public String rev = null;
    @JsonProperty(value="imggroup", required=true)
    public String imageGroupQname = null;
    @JsonProperty(value="spec-version", required=true)
    public String specVersion = null;
    @JsonProperty(value="default-view", required=true)
    public String defaultView = null;
    @JsonProperty(value="view", required=true)
    public Map<String,BVMView> views = null;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty(value="sections")
    public List<BVMSection> sections = null;
    @JsonProperty(value="pagination", required=true)
    public List<BVMPagination> pagination = null;
    @JsonIgnore
    private Map<String,BVMImageInfo> fnMap = null;
    
    @JsonIgnore
    public BVMSection getSection(final String sectionId) {
        if (sections == null) return null;
        for (BVMSection s : sections) {
            if (s.id.equals(sectionId))
                return s;
        }
        return null;
    }
    
    @JsonIgnore
    public Map<String,BVMImageInfo> getFnMap() {
        final List<BVMImageInfo> defaultImageList = this.views.get(this.defaultView).imageList;
        final Map<String,BVMImageInfo> res = new HashMap<>();
        for (final BVMImageInfo ii : defaultImageList) {
            res.put(ii.filename, ii);
        }
        return res;
    }

    @JsonIgnore
    public BVMImageInfo getInfoForFn(final String fn) {
        if (this.fnMap == null) {
            this.fnMap = getFnMap();
        }
        return this.fnMap.get(fn);
    }
    
    @JsonIgnore
    public String getSourcePathForFn(final String fn) {
        if (this.fnMap == null) {
            this.fnMap = getFnMap();
        }
        final BVMImageInfo ii = this.fnMap.get(fn);
        if (ii == null)
            return null;
        return ii.sourcePath;
    }
    
}
