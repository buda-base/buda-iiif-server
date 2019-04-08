package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.text.StringSubstitutor;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;
import io.bdrc.pdf.presentation.models.ImageListIterator;

public class IdentifierInfo {

    public String identifier;
    public String work = "";
    public String asset = "";
    public String access = "";
    public String volumeId = "";
    public String imageList = "";
    public String imageId = "";
    public String imageGroup;
    public int totalPages = 0;
    private HashMap<String, Class<Void>> fair_use;

    @SuppressWarnings("unchecked")
    public IdentifierInfo(String identifier) throws ClientProtocolException, IOException, ResourceNotFoundException {
        this.identifier = identifier;
        long deb = System.currentTimeMillis();
        Application.perf.debug("Creating ldspdi connexion " + identifier + " at " + System.currentTimeMillis());
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("http://purl.bdrc.io/query/IIIFPres_volumeInfo");
        JSONObject object = new JSONObject();
        this.volumeId = identifier.split("::")[0];
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
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
                this.work = node.findValue("workId").findValue("value").toString().replaceAll("\"", "");
                this.asset = node.findValue("itemId").findValue("value").toString().replaceAll("\"", "");
                this.access = node.findValue("access").findValue("value").toString().replaceAll("\"", "");
                this.imageList = node.findValue("imageList").findValue("value").toString().replaceAll("\"", "");
                this.imageGroup = node.findValue("imageGroup").findValue("value").toString().replaceAll("\"", "");
                this.totalPages = Integer.parseInt(node.findValue("totalPages").findValue("value").toString().replaceAll("\"", ""));
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

    public static IdentifierInfo getIndentifierInfo(String identifier) throws ClientProtocolException, IOException, ResourceNotFoundException, BDRCAPIException {
        String volumeId = identifier.split("::")[0];
        IdentifierInfo info = (IdentifierInfo) ServerCache.getObjectFromCache("identifier", "ID_" + volumeId);
        if (info != null) {
            return info;
        } else {
            info = new IdentifierInfo(identifier);
            ServerCache.addToCache("identifier", "ID_" + volumeId, info);
            return info;
        }
    }

    // unused and unefficient direct connection to fuseki
    public IdentifierInfo(String identifier, String s) {
        long deb = System.currentTimeMillis();
        Application.perf.debug("Creating fuseki connexion " + identifier + " at " + deb);
        this.identifier = identifier;
        this.volumeId = identifier.split("::")[0];
        if (identifier.split("::").length > 1) {
            this.imageId = identifier.split("::")[1];
        }
        String request = "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n" + "  prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n" + "  prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "  prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n" + "  prefix bdr:   <http://purl.bdrc.io/resource/>\n"
                + " select distinct ?workId ?itemId ?access ?license ?imageGroup ?imageList ?totalPages ?pagesText ?pagesIntroTbrc ?pagesIntro ?volumeNumber ?volumeLabel ?iiifManifest where {\n" + "  ${res} a :VolumeImageAsset .\n"
                + "  optional { ${res} adm:legacyImageGroupRID ?imageGroup . }\n" + "  optional { ${res} :volumeNumber ?volumeNumber . }\n" + "  optional { ${res} bdo:imageList ?imageList . }\n"
                + "  optional { ${res} bdo:volumePagesTotal ?totalPages . }\n" + "  optional { ${res} bdo:volumePagesText ?pagesText . }\n" + "  optional { ${res} bdo:volumePagesTbrcIntro ?pagesIntroTbrc . }\n"
                + "  optional { ${res} bdo:volumePagesIntro ?pagesIntro . }\n" + "  optional { ${res} skos:prefLabel ?volumeLabel . }\n" + "  optional { ${res} :hasIIIFManifest ?iiifManifest . }\n" + "  ?itemId :itemForWork ?workId ;\n"
                + "      :itemHasVolume ${res} .\n" + "  ?workId adm:access ?access ;\n" + "    adm:license ?license .\n" + "}";

        HashMap<String, String> substitutes = new HashMap<>();
        substitutes.put("res", this.volumeId);
        StringSubstitutor sub = new StringSubstitutor(substitutes);
        QueryExecution qe = QueryExecutionFactory.sparqlService(Application.getProperty("fusekiUrl"), QueryFactory.create(sub.replace(request)));
        ResultSet rs = qe.execSelect();
        QuerySolution qs = rs.next();
        this.work = qs.get("?workId").toString();
        this.asset = qs.get("?itemId").toString();
        this.access = qs.get("?access").toString();
        this.imageList = qs.get("?imageList").toString();
        this.imageGroup = qs.get("?imageGroup").toString();
        this.totalPages = qs.get("?totalPages").asLiteral().getInt();
        qe.close();
        Application.perf.debug("getting fuseki response after " + (System.currentTimeMillis() - deb) + " ms " + identifier);
        if (getAccessShortName().equals(RdfConstants.FAIR_USE)) {
            initFairUse();
        }
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
        System.out.println("FAIR USE IMAGES>>" + fair_use);
    }

    public boolean isFairUsePublicImage(String img) {
        if (fair_use == null) {
            return false;
        }
        return fair_use.containsKey(img);
    }

    public boolean isValidJson(JsonNode node) {
        // return (node.findValue("work")!=null && node.findValue("asset")!= null &&
        // node.findValue("access")!=null);
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

    public String getAccessShortName() {
        return access.substring(access.lastIndexOf('/') + 1);
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getVolumeId() {
        return volumeId;
    }

    @Override
    public String toString() {
        return "IdentifierInfo [identifier=" + identifier + ", work=" + work + ", asset=" + asset + ", access=" + access + ", volumeId=" + volumeId + ", imageList=" + imageList + ", imageId=" + imageId + ", imageGroup=" + imageGroup + ", totalPages="
                + totalPages + ", fair_use=" + fair_use + "]";
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, ResourceNotFoundException {
        Application.initForTests();
        System.out.println(new IdentifierInfo("bdr:V1NLM7_I1NLM7_001::I1NLM7_0010003.jpg"));
        // System.out.println(new IdentifierInfo("bdr:V22084_I0888::08880587.tif", ""));
    }
}
