package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.iiif.exceptions.IIIFException;

public class IdentifierInfo {

    public String identifier;
    public String imageId = "";
    public String imageGroup;
    public String volumeId;
    public int totalPages = 0;
    public boolean accessibleInFairUse = false;
    public ImageGroupInfo igi = null;
    public List<ImageInfo> ili = null;

    public final static Logger log = LoggerFactory.getLogger(IdentifierInfo.class.getName());

    public IdentifierInfo(String identifier) throws IIIFException {
        log.info("Instanciating identifierInfo with {}", identifier);
        this.identifier = identifier;
        this.volumeId = identifier.split("::")[0];
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        try {
            this.igi = ImageGroupInfoService.Instance.getAsync(this.volumeId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(404, 5000, e);
        }
        if (igi.access.equals(AccessType.FAIR_USE)) {
            try {
                this.ili = ImageInfoListService.Instance.getAsync(igi.imageInstanceId.substring(AppConstants.BDR_len), igi.imageGroup).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            this.igi.initAccessibleInFairUse(this.ili);
            this.accessibleInFairUse = this.igi.isAccessibleInFairUse(this.imageId);
        }
    }

    public IdentifierInfo(String identifier, ImageGroupInfo igi) throws IIIFException {
        log.info("Instanciating identifierInfo with {}", identifier);
        this.identifier = identifier;
        this.volumeId = identifier.split("::")[0];
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        this.igi = igi;
        if (igi.access.equals(AccessType.FAIR_USE)) {
            this.accessibleInFairUse = this.igi.isAccessibleInFairUse(this.imageId);
        }
    }
    
    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        Application.initForTests();
        IdentifierInfo info = new IdentifierInfo("bdr:I1NLM7_001::I1NLM7_0010003.jpg");
        System.out.println("INFO >> " + info);
        ServerCache.addToCache("identifier", "ID_" + 415289, info);
        info = (IdentifierInfo) ServerCache.getObjectFromCache("identifier", "ID_" + 415289);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(info);
    }
}
