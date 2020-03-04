package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.EHServerCache;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.libraries.ImageListIterator;

public class IdentifierInfo implements Serializable {

    public String identifier;
    public String work = "";
    public String asset = "";
    public String access = "";
    public String volumeId = "";
    public String imageList = "";
    public List<ImageInfo> imageInfoList;
    public String license = "";
    public String imageGroup;
    public ImageIdentifier imgId;
    public int volumeNumber;
    public int pagesIntroTbrc;
    public boolean isChinaRestricted = false;
    public int totalPages = 0;
    private ArrayList<String> fair_use;

    public final static Logger log = LoggerFactory.getLogger(IdentifierInfo.class.getName());

    @SuppressWarnings("unchecked")
    public IdentifierInfo(String identifier) throws IOException, ResourceNotFoundException, IIIFException {
        log.info("Instanciating identifierInfo with {}", identifier);
        try {
            this.imgId = new ImageIdentifier(identifier);
            fair_use = new ArrayList<>();
            this.identifier = identifier;
            long deb = System.currentTimeMillis();
            Application.logPerf("Creating ldspdi connexion " + identifier + " at " + System.currentTimeMillis());
            JSONObject object = new JSONObject();
            this.volumeId = imgId.getPart("imageGroup");
            imageInfoList = buildImageList(volumeId);
            log.info("IdentifierInfo volumeId = {}", volumeId);
            HttpPost request = new HttpPost(Application.getProperty("dataserver") + "/query/table/IIIFPres_volumeInfo");
            object.put("R_RES", volumeId);
            request.setEntity(new StringEntity(object.toString(), "UTF8"));
            request.setHeader("Content-type", "application/json");
            HttpResponse response = HttpClientBuilder.create().build().execute(request);
            log.info("IdentifierInfo ldspdi response code = {}", response.getStatusLine().getStatusCode());
            Application.logPerf("getting ldspdi response after " + (System.currentTimeMillis() - deb) + " ms " + identifier);
            JsonNode node = new ObjectMapper().readTree(response.getEntity().getContent());
            node = node.findPath("results").findPath("bindings");
            if (node != null) {
                if (isValidJson(node)) {
                    fair_use = new ArrayList<>();
                    this.work = parseValue(node.findValue("workId"));
                    this.asset = parseValue(node.findValue("itemId"));
                    this.access = parseValue(node.findValue("access"));
                    this.imageList = parseValue(node.findValue("imageList"));
                    log.info("IdentifierInfo Image List = {}", imageList);
                    this.license = parseValue(node.findValue("license"));
                    this.imageGroup = parseValue(node.findValue("imageGroup"));
                    this.totalPages = Integer.parseInt(parseValue(node.findValue("totalPages")));
                    this.volumeNumber = Integer.parseInt(parseValue(node.findValue("volumeNumber")));
                    this.pagesIntroTbrc = Integer.parseInt(parseValue(node.findValue("pagesIntroTbrc")));
                    this.isChinaRestricted = Boolean.parseBoolean(parseValue(node.findValue("ric")));
                } else {
                    throw new ResourceNotFoundException();
                }
                System.out.println(buildImageList(volumeId));
            } else {
                throw new ResourceNotFoundException();
            }
            if (getAccessShortName().equals(RdfConstants.FAIR_USE)) {
                initFairUse();
            }
        } catch (IOException | ResourceNotFoundException e) {
            log.error("Could not instantiate Identifier info for identifier:" + identifier, e.getMessage());
            throw e;
        }
    }

    private String parseValue(JsonNode n) {
        return n.findValue("value").textValue();
    }

    public static IdentifierInfo getIndentifierInfo(String identifier)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        ImageIdentifier imgId = new ImageIdentifier(identifier);
        String volumeId = imgId.getPart("imageGroup");

        IdentifierInfo info = EHServerCache.IDENTIFIER.get("ID_" + volumeId);
        if (info != null) {
            return info;
        } else {
            info = new IdentifierInfo(identifier);
            EHServerCache.IDENTIFIER.put("ID_" + volumeId, info);
            return info;
        }

    }

    private List<ImageInfo> buildImageList(String volId) throws IOException {
        List<ImageInfo> imageList = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("https://iiifpres.bdrc.io/il/v:" + volId);
            HttpResponse resp = client.execute(get);
            ObjectMapper om = new ObjectMapper();
            final InputStream objectData = resp.getEntity().getContent();
            imageList = om.readValue(objectData, new TypeReference<List<ImageInfo>>() {
            });
            objectData.close();
            imageList.removeIf(imageInfo -> imageInfo.filename.endsWith("json"));
            log.debug("List for volumeId {} >>> {}", volumeId, imageList);
        } catch (IOException e) {
            log.error("Could not get Image List from identifier " + identifier, e.getMessage());
            throw e;
        }
        return imageList;
    }

    public List<ImageInfo> getImageInfoList() {
        return imageInfoList;
    }

    public int getVolumeNumber() {
        return volumeNumber;
    }

    public String getImageGroup() {
        return imageGroup;
    }

    public boolean isChinaRestricted() {
        return isChinaRestricted;
    }

    public String getLicense() {
        return license;
    }

    private void initFairUse() throws ClientProtocolException, IOException {
        for (int x = 0; x < 20; x++) {
            fair_use.add(imageInfoList.get(x).filename);
        }
        for (int x = imageInfoList.size() - 20; x < imageInfoList.size(); x++) {
            fair_use.add(imageInfoList.get(x).filename);
        }
    }

    public boolean isFairUsePublicImage(String img) {
        if (fair_use.size() == 0) {
            return false;
        }
        return fair_use.contains(img);
    }

    public boolean isValidJson(JsonNode node) {
        return (node.findValue("workId") != null && node.findValue("itemId") != null && node.findValue("access") != null);
    }

    public String getImageList() {
        return imageList;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getWork() {
        return work;
    }

    public String getAsset() {
        return asset;
    }

    public int getPagesIntroTbrc() {
        return pagesIntroTbrc;
    }

    public String getAccessShortName() {
        return access.substring(access.lastIndexOf('/') + 1);
    }

    public String getVolumeId() {
        return volumeId;
    }

    public Iterator<String> getImageListIterator(int beginIdx, int endIdx) {
        return new ImageListIterator(getImageList(), beginIdx, endIdx);
    }

    @Override
    public String toString() {
        return "IdentifierInfo [identifier=" + identifier + ", work=" + work + ", asset=" + asset + ", access=" + access + ", volumeId=" + volumeId
                + ", imageList=" + imageList + ", imageGroup=" + imageGroup + ", isChinaRestricted=" + isChinaRestricted + ", totalPages="
                + totalPages + ", fair_use=" + fair_use + "]";
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        Application.initForTests();
        IdentifierInfo info = new IdentifierInfo("bdr:V1NLM7_I1NLM7_001::I1NLM7_0010003.jpg");
        System.out.println("INFO >> " + info);
        EHServerCache.IDENTIFIER.put("ID_" + 415289, info);
        info = EHServerCache.IDENTIFIER.get("ID_" + 415289);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(info);
    }
}
