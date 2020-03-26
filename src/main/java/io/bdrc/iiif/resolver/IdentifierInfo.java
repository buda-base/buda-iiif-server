package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageGroupInfoService;
import io.bdrc.iiif.image.service.ImageInfoListService;

public class IdentifierInfo {

    public String identifier;
    public String imageId = "";
    public String volumeId;
    public String prefix = null;
    public String imageName = null;
    public int totalPages = 0;
    public boolean accessibleInFairUse = false;
    public ImageGroupInfo igi = null;
    public List<ImageInfo> ili = null;
    public Integer imageIndex = null;
    public String computedImageName = null;

    public final static Logger log = LoggerFactory.getLogger(IdentifierInfo.class.getName());

    public IdentifierInfo(String identifier) throws IIIFException {
        log.info("Instanciating identifierInfo with {}", identifier);
        parseIdentifier(identifier);
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        try {
            this.igi = ImageGroupInfoService.Instance.getAsync(this.volumeId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(404, 5000, e);
        }
        if (igi.access.equals(AccessType.FAIR_USE) || AppConstants.IGSI.equals(prefix)) {
            try {
                this.ili = ImageInfoListService.Instance.getAsync(igi.imageInstanceId.substring(AppConstants.BDR_len), igi.imageGroup).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            if (igi.access.equals(AccessType.FAIR_USE)) {
                this.igi.initAccessibleInFairUse(this.ili);
                this.accessibleInFairUse = this.igi.isAccessibleInFairUse(this.imageId);
            }
            if (AppConstants.IGSI.equals(prefix)) {
                computeImageName(this.ili);
            }
        }
    }

    /*
     * public List<ImageInfo> ensureImageListInfo(Access acc) throws IIIFException {
     * boolean fairUseOk = acc.hasResourceAccess(RdfConstants.FAIR_USE);
     * System.out.println("ACCESS in IDENTIFIER INFO >>" + acc +
     * " has faireuse access =" + acc.hasResourceAccess(RdfConstants.FAIR_USE)); if
     * (!igi.access.equals(AccessType.FAIR_USE)) { return ili; } else { if (ili ==
     * null) { try { ili =
     * ImageInfoListService.Instance.getAsync(igi.imageInstanceId.substring(
     * AppConstants.BDR_len), igi.imageGroup).get(); } catch (InterruptedException |
     * ExecutionException e) { throw new IIIFException(404, 5000, e); } } if
     * (!fairUseOk) { List<ImageInfo> fairUse = new ArrayList<>(); if (ili.size() <=
     * 40) { return ili; } int k = 0; for (int x = 0; x < 20; x++) { fairUse.add(k,
     * ili.get(x)); k++; } for (int t = ili.size() - 20; t < ili.size(); t++) {
     * fairUse.add(k, ili.get(t)); k++; } return fairUse; } else { return ili; } } }
     */

    public List<ImageInfo> ensureImageListInfo(Access acc) throws IIIFException {
        if (ili == null) {
            try {
                ili = ImageInfoListService.Instance.getAsync(igi.imageInstanceId.substring(AppConstants.BDR_len), igi.imageGroup).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
        }
        return ili;
    }

    public int getTotalPages() {
        return igi.totalPages;
    }

    public String getCanonical() throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String in = imageName == null ? computedImageName : imageName;
        return AppConstants.IGFN + ":" + volumeId + ":" + in;
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

    private void parseIdentifier(String id) throws IIIFException {
        this.identifier = id;
        try {
            if (!id.contains("::")) {
                volumeId = fixImageGroupId(id);
            } else {
                String[] chunks = id.split("::");
                String[] start = chunks[0].split(":");
                boolean hasPrefix = false;
                if (start.length == 3) {
                    prefix = start[0];
                    volumeId = fixImageGroupId(start[1] + ":" + start[2]);
                    hasPrefix = true;
                } else {
                    volumeId = fixImageGroupId(chunks[0]);
                }
                if (hasPrefix) {
                    if (prefix.equals(AppConstants.IGFN)) {
                        imageName = chunks[1];
                    }
                    if (prefix.equals(AppConstants.IGSI)) {
                        try {
                            imageIndex = Integer.parseInt(chunks[1]);
                        } catch (Exception e) {
                            throw new IIIFException(404, 5000, "cannot convert " + chunks[1] + " into integer");
                        }
                    }
                } else {
                    imageName = chunks[1];
                }
            }
        } catch (Exception e) {
            throw new IIIFException(e);
        }
    }

    private String fixImageGroupId(final String igId) {
        if (igId.startsWith("bdr:V")) {
            int undIidx = igId.lastIndexOf("_I");
            if (undIidx != -1) {
                return "bdr:" + igId.substring(undIidx + 1);
            }
        }
        return igId;
    }

    public void computeImageName(List<ImageInfo> inf) {
        if (imageIndex != null && imageIndex < inf.size()) {
            computedImageName = inf.get(imageIndex).filename;
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
        IdentifierInfo info = new IdentifierInfo("bdr:I0988");
        System.out.println("INFO >> " + info);
        // EHServerCache.IDENTIFIER.put("ID_" + 415289, info);
        // info = EHServerCache.IDENTIFIER.get("ID_" + 415289);
        // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
        // System.out.println(info);
    }
}
