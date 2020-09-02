package io.bdrc.iiif.resolver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageGroupInfoService;

public class ImageGroupInfo {

    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina = false;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("status")
    public String statusUri;
    @JsonProperty("instanceId")
    public String instanceId;
    @JsonProperty("imageInstanceId")
    public String imageInstanceId;
    @JsonProperty("pagesIntroTbrc")
    public Integer pagesIntroTbrc = 0;
    @JsonProperty("volumeNumber")
    public Integer volumeNumber = 1;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;
    @JsonProperty("accessibleInFairUseList")
    public Map<String, Boolean> accessibleInFairUseList = null;

    private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfoService.class);

    // result of volumeInfo query
    public ImageGroupInfo(final QuerySolution sol, final String volumeId) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        if (volumeId.startsWith("bdr:")) {
            this.imageGroup = volumeId.substring(4);
        } else {
            this.imageGroup = volumeId;
        }
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.statusUri = sol.getResource("status").getURI();
        this.license = LicenseType.fromString(sol.getResource("license").getURI());
        this.instanceId = sol.getResource("instanceId").getURI();
        this.imageInstanceId = sol.getResource("iinstanceId").getURI();
        if (sol.contains("?ric")) {
            this.restrictedInChina = sol.get("?ric").asLiteral().getBoolean();
        }
        if (sol.contains("?volumeNumber")) {
            this.volumeNumber = sol.get("?volumeNumber").asLiteral().getInt();
        }
        if (sol.contains("?pagesIntroTbrc")) {
            this.pagesIntroTbrc = sol.get("?pagesIntroTbrc").asLiteral().getInt();
        }
        if (sol.contains("iiifManifest")) {
            final String manifestURIString = sol.getResource("iiifManifest").getURI();
            try {
                this.iiifManifest = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: " + manifestURIString, e);
            }
        }
    }

    public void initAccessibleInFairUse(List<ImageInfo> ili) {
        if (this.accessibleInFairUseList != null)
            return;
        this.accessibleInFairUseList = new HashMap<>();
        for (int x = this.pagesIntroTbrc; x < this.pagesIntroTbrc + AppConstants.FAIRUSE_PAGES_S; x++) {
            this.accessibleInFairUseList.put(ili.get(x).filename, true);
        }
        final int listSize = ili.size();
        for (int x = listSize - AppConstants.FAIRUSE_PAGES_E; x < listSize; x++) {
            this.accessibleInFairUseList.put(ili.get(x).filename, true);
        }
    }

    public boolean isAccessibleInFairUse(final String imageFileName) throws IIIFException {
        if (this.accessibleInFairUseList == null) {
            throw new IIIFException(500, 5000, "isAccessibleInFairUse called but list not initialized");
        }
        return this.accessibleInFairUseList.containsKey(imageFileName);
    }

    public ImageGroupInfo() {
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }
}
