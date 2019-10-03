package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.libraries.ImageListIterator;

public class IdentifierInfo {

    public String identifier;
    public String work = "";
    public String asset = "";
    public String access = "";
    public String volumeId = "";
    public String imageList = "";
    public String imageId = "";
    public String license = "";
    public String imageGroup;
    public int volumeNumber;
    public int pagesIntroTbrc;
    public boolean isChinaRestricted = false;
    public int totalPages = 0;
    private HashMap<String, Class<Void>> fair_use;

    @SuppressWarnings("unchecked")
    public IdentifierInfo(String identifier) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        this.identifier = identifier;
        long deb = System.currentTimeMillis();
        Application.perf.debug("Creating ldspdi connexion " + identifier + " at " + System.currentTimeMillis());
        HttpClient httpClient = HttpClientBuilder.create().build();
        JSONObject object = new JSONObject();
        this.volumeId = identifier.split("::")[0];
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        HttpPost request = new HttpPost("http://purl.bdrc.io/query/table/IIIFPres_volumeInfo");
        object.put("R_RES", volumeId);
        String message = object.toString();
        request.setEntity(new StringEntity(message, "UTF8"));
        request.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(request);
        Application.perf.debug("getting ldspdi response after " + (System.currentTimeMillis() - deb) + " ms " + identifier);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response.getEntity().getContent());
        node = node.findPath("results").findPath("bindings");
        if (node != null) {
            if (isValidJson(node)) {
                fair_use = new HashMap<>();
                this.work = parseValue(node.findValue("workId"));
                this.asset = parseValue(node.findValue("itemId"));
                this.access = parseValue(node.findValue("access"));
                this.imageList = parseValue(node.findValue("imageList"));
                this.license = parseValue(node.findValue("license"));
                this.imageGroup = parseValue(node.findValue("imageGroup"));
                this.totalPages = Integer.parseInt(parseValue(node.findValue("totalPages")));
                this.volumeNumber = Integer.parseInt(parseValue(node.findValue("volumeNumber")));
                this.pagesIntroTbrc = Integer.parseInt(parseValue(node.findValue("pagesIntroTbrc")));
                this.isChinaRestricted = Boolean.parseBoolean(parseValue(node.findValue("ric")));
            } else {
                throw new ResourceNotFoundException();
            }
        } else {
            throw new ResourceNotFoundException();
        }
        if (getAccessShortName().equals(RdfConstants.FAIR_USE)) {
            initFairUse();
        }
    }

    private String parseValue(JsonNode n) {
        return n.findValue("value").textValue();
    }

    public static IdentifierInfo getIndentifierInfo(String identifier) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String volumeId = identifier.split("::")[0];
        System.out.println("ID INFO vol Id>>" + volumeId);
        IdentifierInfo info = (IdentifierInfo) ServerCache.getObjectFromCache("identifier", "ID_" + volumeId);
        System.out.println("IDENTIFIER INFO >>" + info);
        if (info != null) {
            return info;
        } else {
            System.out.println("IDENTIFIER INFO not found querying ldspdi >>");
            info = new IdentifierInfo(identifier);
            ServerCache.addToCache("identifier", "ID_" + volumeId, info);
            System.out.println("IDENTIFIER INFO ADDED TO CACHE >>" + info);
            return info;
        }
    }

    public int getVolumeNumber() {
        return volumeNumber;
    }

    public void setVolumeNumber(int volumeNumber) {
        this.volumeNumber = volumeNumber;
    }

    public String getImageGroup() {
        return imageGroup;
    }

    public void setImageGroup(String imageGroup) {
        this.imageGroup = imageGroup;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setFair_use(HashMap<String, Class<Void>> fair_use) {
        this.fair_use = fair_use;
    }

    public boolean isChinaRestricted() {
        return isChinaRestricted;
    }

    public String getLicense() {
        return license;
    }

    private void initFairUse() {
        fair_use = new HashMap<>();
        ImageListIterator it1 = new ImageListIterator(imageList, 1, 20);
        while (it1.hasNext()) {
            fair_use.put(it1.next(), Void.TYPE);
        }
        ImageListIterator it2 = new ImageListIterator(imageList, totalPages - 19, totalPages);
        while (it2.hasNext()) {
            fair_use.put(it2.next(), Void.TYPE);
        }
    }

    public boolean isFairUsePublicImage(String img) {
        if (fair_use == null) {
            return false;
        }
        return fair_use.containsKey(img);
    }

    public boolean isValidJson(JsonNode node) {
        return (node.findValue("workId") != null && node.findValue("itemId") != null && node.findValue("access") != null);
    }

    public String getImageId() {
        return imageId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getImageList() {
        return imageList;
    }

    public void setImageList(String imageList) {
        this.imageList = imageList;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public HashMap<String, Class<Void>> getFair_use() {
        return fair_use;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getAccess() {
        return access;
    }

    public int getPagesIntroTbrc() {
        return pagesIntroTbrc;
    }

    public void setPagesIntroTbrc(int pagesIntroTbrc) {
        this.pagesIntroTbrc = pagesIntroTbrc;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setChinaRestricted(boolean isChinaRestricted) {
        this.isChinaRestricted = isChinaRestricted;
    }

    public String getAccessShortName() {
        return access.substring(access.lastIndexOf('/') + 1);
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public Iterator<String> getImageListIterator(int beginIdx, int endIdx) {
        return new ImageListIterator(getImageList(), beginIdx, endIdx);
    }

    @Override
    public String toString() {
        return "IdentifierInfo [identifier=" + identifier + ", work=" + work + ", asset=" + asset + ", access=" + access + ", volumeId=" + volumeId + ", imageList=" + imageList + ", imageId=" + imageId + ", imageGroup=" + imageGroup
                + ", isChinaRestricted=" + isChinaRestricted + ", totalPages=" + totalPages + ", fair_use=" + fair_use + "]";
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        Application.initForTests();
        System.out.println(new IdentifierInfo("bdr:V1NLM7_I1NLM7_001::I1NLM7_0010003.jpg"));
    }
}
