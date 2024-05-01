package io.bdrc.iiif.resolver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.libraries.Models;

public class ImageGroupInfo {

    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina = false;
    @JsonProperty("status")
    public String statusUri;
    @JsonProperty("imageInstanceUri")
    public String imageInstanceUri;
    @JsonProperty("nbVolumes")
    public Integer nbVolumes = 1;
    @JsonProperty("pagesIntroTbrc")
    public Integer pagesIntroTbrc = 0;
    @JsonProperty("volumeNumber")
    public Integer volumeNumber = 1;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifest")
    public URI iiifManifest = null;
    @JsonProperty("inCollections")
    public List<String> inCollectionsLnames = null;
    @JsonProperty("thumbnailFname")
    public String thumbnailFname = null;
    @JsonProperty("copyrightStatus")
    public String copyrightStatusLname = null;
    @JsonProperty("accessibleInFairUseList")
    public Map<String, Boolean> accessibleInFairUseList = null;

    private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfo.class);

    static final Property instanceHasVolumeP = ResourceFactory.createProperty(Models.BDO + "instanceHasVolume");
    static final Property nbVolumesP = ResourceFactory.createProperty(Models.BDO + "numberOfVolumes");
    static final Property adminAboutP = ResourceFactory.createProperty(Models.ADM + "adminAbout");
    static final Property accessP = ResourceFactory.createProperty(Models.ADM + "access");
    static final Property statusP = ResourceFactory.createProperty(Models.ADM + "status");
    static final Property restrictedInChinaP = ResourceFactory.createProperty(Models.ADM + "restrictedInChina");
    static final Property inCollectionP = ResourceFactory.createProperty(Models.BDO + "inCollection");
    static final Property volumeNumberP = ResourceFactory.createProperty(Models.BDO + "volumeNumber");
    static final Property volumePagesTbrcIntroP = ResourceFactory.createProperty(Models.BDO + "volumePagesTbrcIntro");
    static final Property hasIIIFManifestP = ResourceFactory.createProperty(Models.BDO + "hasIIIFManifest");
    static final Property copyrightStatus = ResourceFactory.createProperty(Models.BDO + "copyrightStatus");
    static final Property tmpThumbnail = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/tmp/thumbnailIIIFService");
    
    // result of volumeInfo query
    public ImageGroupInfo(final Model m, final String volumeId) {
        if (volumeId.startsWith("bdr:")) {
            this.imageGroup = volumeId.substring(4);
        } else {
            this.imageGroup = volumeId;
        }
        final Resource ig = m.getResource(Models.BDR+this.imageGroup);
        final ResIterator iiL = m.listSubjectsWithProperty(instanceHasVolumeP, ig);
        if (!iiL.hasNext()) {
            logger.error("can't find image instance in model");
            return;
        }
        final Resource ii = iiL.next();
        final ResIterator admAboutiiIt = m.listResourcesWithProperty(adminAboutP, ii);
        if (!admAboutiiIt.hasNext()) {
            logger.error("can't find admin data in model");
            return;
        }
        final Statement nbVolsS = ii.getProperty(nbVolumesP);
        if (nbVolsS != null)
            this.nbVolumes = nbVolsS.getInt();
        final Statement thumbnailS = ii.getProperty(tmpThumbnail);
        if (thumbnailS != null) {
            final String thUri = thumbnailS.getResource().getURI();
            final int idx = thUri.lastIndexOf(this.imageGroup+"::");
            if (idx > 1)
                this.thumbnailFname = thUri.substring(idx+this.imageGroup.length()+2);
        }
        final Statement copyrightS = ii.getProperty(copyrightStatus);
        if (copyrightS != null)
            this.copyrightStatusLname = copyrightS.getResource().getLocalName();
        final Resource iiAdm = admAboutiiIt.next();
        this.restrictedInChina = iiAdm.hasLiteral(restrictedInChinaP, true);
        final StmtIterator collectionIt = ii.listProperties(inCollectionP);
        while (collectionIt.hasNext()) {
            final Resource colR = collectionIt.next().getObject().asResource();
            if (this.inCollectionsLnames == null) {
                this.inCollectionsLnames = new ArrayList<String>();
            }
            this.inCollectionsLnames.add(colR.getLocalName());
        }
        this.access = AccessType.fromString(iiAdm.getPropertyResourceValue(accessP).getURI());
        this.statusUri = iiAdm.getPropertyResourceValue(statusP).getURI();
        this.imageInstanceUri = ii.getURI();
        final StmtIterator volNumIt = ig.listProperties(volumeNumberP);
        if (volNumIt.hasNext()) {
            this.volumeNumber = volNumIt.next().getObject().asLiteral().getInt();
            if (this.volumeNumber > this.nbVolumes)
                this.nbVolumes = this.volumeNumber;
        }
        final StmtIterator pagesIntroTbrcIt = ig.listProperties(volumePagesTbrcIntroP);
        if (pagesIntroTbrcIt.hasNext()) {
            this.pagesIntroTbrc = pagesIntroTbrcIt.next().getObject().asLiteral().getInt();
        }
        final StmtIterator iiifManifestIt = ig.listProperties(hasIIIFManifestP);
        if (iiifManifestIt.hasNext()) {
            final String manifUri = iiifManifestIt.next().getObject().asResource().getURI();
            try {
                this.iiifManifest = new URI(manifUri);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: " + manifUri, e);
            }
        }
    }
    
    public void initAccessibleInFairUse(List<ImageInfo> ili) {
        if (this.accessibleInFairUseList != null)
            return;
        this.accessibleInFairUseList = new HashMap<>();
        final int listSize = ili.size();
        final int lasti = Math.min(listSize, this.pagesIntroTbrc + AppConstants.FAIRUSE_PAGES_S);
        for (int x = this.pagesIntroTbrc; x < lasti; x++) {
            this.accessibleInFairUseList.put(ili.get(x).filename, true);
        }
        for (int x = Math.max(0, listSize - AppConstants.FAIRUSE_PAGES_E); x < listSize; x++) {
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
