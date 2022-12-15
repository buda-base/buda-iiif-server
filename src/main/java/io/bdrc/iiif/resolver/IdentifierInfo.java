package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AccessInfo.AccessLevel;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageGroupInfoService;
import io.bdrc.iiif.image.service.ImageInfoListService;

public class IdentifierInfo {

    public String identifier;
    public String imageId = "";
    public String volumeId;
    public boolean isStatic = false;
    public boolean isThumbnail = true;
    public String prefix = null;
    public String imageName = null;
    public int totalPages = 0;
    public boolean accessibleInFairUse = false;
    public ImageGroupInfo igi = null;
    private List<ImageInfo> ili = null;
    private HashMap<String, ImageInfo> imgMap = null;
    public Integer imageIndex = null;
    public String computedImageName = null;

    public final static Logger log = LoggerFactory.getLogger(IdentifierInfo.class.getName());

    public IdentifierInfo(String identifier) throws IIIFException {
        log.info("Instanciating identifierInfo with {}", identifier);
        parseIdentifier(identifier);
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        log.info("IdentifierInfo parsed volumeId= {}", this.volumeId);
        try {
            this.igi = ImageGroupInfoService.Instance.getAsync(this.volumeId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(404, 5000, e);
        }
        if (isFairUse() || AppConstants.IGSI.equals(prefix)) {
            // this could be optimized even further by initializing the
            // accessibleInFairUse thing only when the user can't see all the images
            if (isFairUse()) {
                this.igi.initAccessibleInFairUse(this.getImageList());
                this.accessibleInFairUse = this.igi.isAccessibleInFairUse(this.imageId);
            }
            if (AppConstants.IGSI.equals(prefix)) {
                computeImageName(this.ili);
            }
        }
    }

    public List<ImageInfo> getImageList() throws IIIFException {
        if (this.ili != null) {
            return this.ili;
        }
        try {
            log.info("get image list for {}", this.volumeId);
            this.ili = ImageInfoListService.Instance
                    .getAsync(igi.imageInstanceUri.substring(AppConstants.BDR_len), igi.imageGroup).get();
        } catch (InterruptedException | ExecutionException | IIIFException e) {
            throw new IIIFException(404, 5000, e);
        }
        return this.ili;
    }
    
    private HashMap<String, ImageInfo> buildImageMapAndAddImgNum(List<ImageInfo> inf) {
        HashMap<String, ImageInfo> map = new HashMap<>();
        int imgNum = 1;
        for (ImageInfo imf : inf) {
            imf.imgNum = imgNum;
            map.put(imf.filename, imf);
            imgNum += 1;
        }
        return map;
    }

    public ImageInfo getImageInfo(String filename) throws IIIFException {
        if (this.imgMap != null) {
            return imgMap.get(filename);
        }
        List<ImageInfo> ili = this.getImageList();
        this.imgMap = buildImageMapAndAddImgNum(ili);
        return imgMap.get(filename);
    }

    public boolean isFairUse() {
        return igi.access.equals(AccessType.FAIR_USE);
    }

    public List<ImageInfo> getImageListInfo(AccessLevel al, int start, int end) throws IIIFException {
        // get the full list;
        List<ImageInfo> info = null;
        try {
            log.info("get image list for {}", this.identifier);
            info = ImageInfoListService.Instance
                    .getAsync(this.igi.imageInstanceUri.substring(AppConstants.BDR_len), this.igi.imageGroup).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(e);
        }
        if (start < 1 || end > info.size())
            throw new IIIFException(404, 5001, "incorrect image range");
        // work is fair use but and user is not authorized to see it in full
        if (al.equals(AccessLevel.FAIR_USE)) {
            log.debug("fair use mode, start: {}, end: {}", start, end);
            final List<ImageInfo> res = getFairUseImageList(info, start, end);
            if (res.size() == 0)
                throw new IIIFException(403, 5001, "image range unaccessible");
            return res;
        }
        return getImageListRange(info, start, end);
    }
    
    public List<ImageInfo> getImageListRange(List<ImageInfo> src, int start, int end) {
        List<ImageInfo> info = new ArrayList<>();
        for (int x = start - 1; x < end; x++) {
            info.add(src.get(x));
        }
        return info;
    }

    public List<ImageInfo> getFairUseImageList(List<ImageInfo> src) {
        List<ImageInfo> info = new ArrayList<>();
        int max = src.size();
        if (src.size() <= 40) {
            info = src;
        } else {
            int k = 0;
            for (int x = 0; x < 20; x++) {
                info.add(k, src.get(x));
                k++;
            }
            for (int j = (max - 21); j < max; j++) {
                info.add(k, src.get(j));
                k++;
            }
        }
        return info;
    }

    public List<ImageInfo> getFairUseImageList(List<ImageInfo> src, int start, int end) {
        List<ImageInfo> info = new ArrayList<>();
        log.info("SRC size : {}", src.size());
        if (src.size() <= 40) {
            info = src;
        } else {
            int y = 0;
            for (int x = start - 1; x < 20; x++) {
                info.add(y, src.get(x));
                y++;
            }
            if (end > (src.size() - 20)) {
                for (int j = end - 20; j < src.size(); j++) {
                    info.add(y, src.get(j));
                    y++;
                }
            }
        }
        return info;
    }

    public Integer getTotalPages() throws IIIFException {
        List<ImageInfo> ili = this.getImageList();
        return ili.size();
    }

    public String getCanonical() throws ClientProtocolException, IOException, IIIFException {
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
        log.info("IdentifierInfo parsing {}", id);
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

}
