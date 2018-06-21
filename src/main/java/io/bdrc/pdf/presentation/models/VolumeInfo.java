package io.bdrc.pdf.presentation.models;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.jena.query.QuerySolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.pdf.presentation.VolumeInfoService;

public class VolumeInfo {
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("itemId")
    public String itemId;
    @JsonProperty("imageList")
    public String imageList;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;
    
    private static final Logger logger = LoggerFactory.getLogger(VolumeInfoService.class);
    
    public VolumeInfo(QuerySolution sol) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.license = LicenseType.fromString(sol.getResource("license").getURI());
        this.workId = sol.getResource("workId").getURI();
        this.itemId = sol.getResource("itemId").getURI();
        this.imageList = sol.get("?imageList").toString();
        if (sol.contains("imageGroup")) {
            this.imageGroup = sol.getLiteral("imageGroup").getString();
        }
        if (sol.contains("iiifManifest")) {
            final String manifestURIString = sol.getResource("iiifManifest").getURI(); 
            try {
                this.iiifManifest = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: "+manifestURIString, e);
            }
        }
    }

    public Iterator<String> getImageListIterator(int beginIdx, int endIdx) {
        return new ImageListIterator(imageList, beginIdx, endIdx);
    }

    public VolumeInfo() { }

    @Override
    public String toString() {
        return "VolumeInfo [access=" + access + ", license=" + license + ", workId=" + workId + ", itemId=" + itemId
                + ", imageList=" + imageList + ", imageGroup=" + imageGroup + ", iiifManifest=" + iiifManifest + "]";
    }
    
}
