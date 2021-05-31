package io.bdrc.iiif.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringSubstitutor;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.iiif.archives.ArchiveBuilder;
import io.bdrc.iiif.archives.ArchiveProducer;
import io.bdrc.iiif.archives.PdfItemInfo;
import io.bdrc.iiif.auth.AuthServiceInfo;
import io.bdrc.iiif.auth.ResourceAccessValidation;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.Identifier;

@Controller
public class ArchivesController {

    @Autowired
    private AuthServiceInfo serviceInfo;
    
    public static final String IIIF = "IIIF";
    public static final String IIIF_ZIP = "IIIF_ZIP";

    public final static Logger log = LoggerFactory.getLogger(ArchivesController.class.getName());

    @RequestMapping(value = "/download/{type}/{id}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> getPdfLink(@PathVariable String id, @PathVariable String type,
            HttpServletRequest request) throws Exception {
        log.info("getPdfLink(id {}, type{})", id, type);
        Access acc = (Access) request.getAttribute("access");
        if (acc == null) {
            acc = new Access();
        }
        String format = request.getHeader("Accept");
        boolean json = format.contains("application/json");
        String output = null;
        Identifier idf = new Identifier(id, Identifier.MANIFEST_ID);
        log.info("Building identifier for id {}", id);
        log.info("Building from page {} to {}", idf.getBPageNum(), idf.getEPageNum());
        log.info("Identifier object {}", idf);
        HttpHeaders headers = new HttpHeaders();
        HashMap<String, String> map = new HashMap<>();
        HashMap<String, HashMap<String, String>> jsonMap = new HashMap<>();
        StringSubstitutor s = null;
        String html = "";
        int subType = idf.getSubType();
        switch (subType) {
            // Case work in item
            case Identifier.MANIFEST_ID_WORK_IN_ITEM :
                PdfItemInfo item = PdfItemInfo.getPdfItemInfo(idf.getImageInstanceId());
                if (!acc.hasResourceAccess(item.getItemAccess())) {
                    final HttpStatus st = (serviceInfo.authEnabled() && serviceInfo.hasValidProperties() && !acc.isUserLoggedIn()) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                    final String msg = st == HttpStatus.UNAUTHORIZED ? "Please log in to download archive files" : "Insufficient rights";
                    return new ResponseEntity<>(msg, st);
                }
                if (json) {
                    jsonMap = getJsonVolumeLinks(item, type);
                    ObjectMapper mapper = new ObjectMapper();
                    html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
                } else {
                    map.put("links", getVolumeDownLoadLinks(item, idf, type));
                    html = getTemplate("volumes.tpl");
                    s = new StringSubstitutor(map);
                    html = s.replace(html);
                }
                break;
            // Case volume imageRange
            case Identifier.MANIFEST_ID_VOLUMEID :
            case Identifier.MANIFEST_ID_WORK_IN_VOLUMEID :
                IdentifierInfo inf = new IdentifierInfo(idf.getImageGroupId());
                Integer bPage = idf.getBPageNum();
                if (bPage == null) {
                    bPage = 1;
                }
                Integer ePage = idf.getEPageNum();
                if (ePage == null) {
                    ePage = inf.getTotalPages();
                }
                if (ePage < bPage) {
                    return new ResponseEntity<>("PDF would be empty", HttpStatus.NOT_FOUND);
                }
                log.debug("Pdf requested numPage in identifierInfo {}", inf);
                log.info("Pdf requested start page {} and end page {}", bPage.intValue(), ePage.intValue());
                ResourceAccessValidation accValidation = new ResourceAccessValidation(acc, inf);
                AccessLevel al = accValidation.getAccessLevel(request);
                if (al.equals(AccessLevel.NOACCESS) || al.equals(AccessLevel.MIXED)) {
                    final HttpStatus st = (serviceInfo.authEnabled() && serviceInfo.hasValidProperties() && !acc.isUserLoggedIn()) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                    final String msg = st == HttpStatus.UNAUTHORIZED ? "Please log in to download archive files" : "Insufficient rights";
                    return new ResponseEntity<>(msg, st);
                }
                if (al.equals(AccessLevel.FAIR_USE)) {
                    output = idf.getImageGroupId() + "FAIR_USE:" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
                } else {
                    output = idf.getImageGroupId() + ":" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
                }
                log.info("Built output is {}", output);
                Boolean cached = false;
                Double percentdone = null;
                if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                    percentdone = ArchiveBuilder.pdfjobs.get(output);
                    cached = EHServerCache.IIIF_PDF.hasKey(output);
                    log.info("PDF {} from IIIF cached {}, jobstarted: {}", id, cached, percentdone);
                    if (!cached && percentdone == null) {
                        // Start building pdf since the pdf file doesn't exist yet
                        if (!Application.isPdfSync()) {
                            ArchiveBuilder.service.submit(new ArchiveProducer(accValidation.getAccess(), inf, idf, output,
                                    (String) request.getAttribute("origin"), ArchiveProducer.PDF, EHServerCache.IIIF_PDF));
                            //ArchiveBuilder.buildPdf(accValidation.getAccess(), inf, idf, output,
                            //        (String) request.getAttribute("origin"));
                        }
                    }
                }
                if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                    cached = EHServerCache.IIIF_ZIP.hasKey(output);
                    percentdone = ArchiveBuilder.zipjobs.get(output);
                    log.debug("ZIP {} from IIIF_ZIP cached: {}, jobstarted: {}", id, cached, percentdone);
                    if (!cached &&  percentdone == null) {
                        // Build zip since the zip file doesn't exist yet
                        if (!Application.isPdfSync()) {
                            ArchiveBuilder.service.submit(new ArchiveProducer(accValidation.getAccess(), inf, idf, output,
                                    (String) request.getAttribute("origin"), ArchiveProducer.ZIP, EHServerCache.IIIF_ZIP));
                        }
                    }
                }
                //log.error("sync mode: {}", Application.isPdfSync());
                if (cached || Application.isPdfSync()) {
                    // Create template and serve html link
                    map.put("link", Application.getProperty("iiifserv_baseurl") + "download/file/" + type + "/" + output);
                    if (json) {
                        map.put("status", "done");
                        ObjectMapper mapper = new ObjectMapper();
                        html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                    } else {
                        html = getTemplate("downloadPdf.tpl");
                        map.put("file", output + "." + type);
                        s = new StringSubstitutor(map);
                        html = s.replace(html);
                    }
                } else {
                    if (percentdone == null)
                        percentdone = 0.;
                    final long percentint = Math.round(percentdone * 100);
                    map.put("percentdone", String.valueOf(percentint));
                    if (json) {
                        map.put("status", "generating");
                        ObjectMapper mapper = new ObjectMapper();
                        html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                    } else {
                        html = getTemplate("pdfGenerating.tpl");
                        s = new StringSubstitutor(map);
                        html = s.replace(html);
                    }
                }
                break;
        }
        if (json) {
            headers.setContentType(MediaType.parseMediaType("application/json"));
        } else {
            headers.setContentType(MediaType.parseMediaType("text/html"));
        }
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;

    }

    @RequestMapping(value = "/download/file/{type}/{name}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<StreamingResponseBody> downloadPdf(@PathVariable String name, @PathVariable String type,
            HttpServletRequest request) throws Exception {
        String[] nameParts = name.replace("FAIR_USE", "").split(":");
        HttpHeaders headers = new HttpHeaders();
        if (nameParts.length < 3) {
            log.error("invalid PDF download argument: {}", name);
            headers.setContentType(MediaType.parseMediaType("text/plain"));
            final String msg = "Invalid link";
            return ResponseEntity.ok().headers(headers).body(IIIFImageApiController.streamingResponseFrom(msg));
        }
        log.info("downloadPdf(name {} , type {})", name, type);
        Identifier idf = new Identifier("v:" + nameParts[0] + ":" + nameParts[1] + "::" + nameParts[2],
                Identifier.MANIFEST_ID);
        log.info("downloadPdf building identifier for id {}", "v:" + name);
        log.info("downloadPdf building from page {} to {}", idf.getBPageNum(), idf.getEPageNum());
        IdentifierInfo inf = new IdentifierInfo(nameParts[0] + ":" + nameParts[1]);
        ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"),
                inf);
        if (Application.isPdfSync()) {
            headers.setContentType(MediaType.parseMediaType("application/" + type));
            headers.setContentDispositionFormData("attachment", name.substring(4) + "." + type);
            if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                final StreamingResponseBody s = ArchiveBuilder.getPDFStreamingResponseBody(accValidation.getAccess(), inf, idf, name,
                        (String) request.getAttribute("origin"));
                return ResponseEntity.ok().headers(headers).body(s);
            } else { //if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                final StreamingResponseBody s = ArchiveBuilder.getZipStreamingResponseBody(accValidation.getAccess(), inf, idf, name,
                        (String) request.getAttribute("origin"));
                return ResponseEntity.ok().headers(headers).body(s);
            }
        }
        InputStream is = null;
        if (type.equals(ArchiveBuilder.PDF_TYPE)) {
            is = EHServerCache.IIIF_PDF.getIs(name);
        } else {
            is = EHServerCache.IIIF_ZIP.getIs(name);
        }
        if (is == null) {
            headers.setContentType(MediaType.parseMediaType("text/plain"));
            final String msg = "The link is wrong or has expired: please retry loading the archive and proceed to its download within 10 mn";
            return ResponseEntity.ok().headers(headers).body(IIIFImageApiController.streamingResponseFrom(msg));
        }
        headers.setContentType(MediaType.parseMediaType("application/" + type));
        headers.setContentDispositionFormData("attachment", name.substring(4) + "." + type);
        StreamingResponseBody stream = IIIFImageApiController.streamingResponseFrom(is);
        return ResponseEntity.ok().headers(headers).body(stream);
    }

    @RequestMapping(value = "/download/job/{type}/{id}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> jobState(@PathVariable String id, @PathVariable String type,
            HttpServletRequest request) throws Exception {
        log.info("jobState(id {}, type {})", id, type);
        Identifier idf = new Identifier(id, Identifier.MANIFEST_ID);
        Integer bPage = idf.getBPageNum();
        IdentifierInfo inf = new IdentifierInfo(idf.getImageGroupId());
        if (bPage == null) {
            bPage = 1;
        }
        Integer ePage = idf.getEPageNum();
        if (ePage == null) {
            ePage = inf.getTotalPages();
        }
        String url = Application.getProperty("iiifserv_baseurl") + "download/file/" + type + "/" + idf.getImageGroupId()
                + ":" + bPage + "-" + ePage + "." + type;
        Boolean cached = false;
        Double percentdone = null;
        ResourceAccessValidation accValidation = new ResourceAccessValidation(
                (Access) request.getAttribute("access"), inf);
        AccessLevel al = accValidation.getAccessLevel(request);
        final String output;
        if (al.equals(AccessLevel.FAIR_USE)) {
            output = idf.getImageGroupId() + "FAIR_USE:" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
        } else {
            output = idf.getImageGroupId() + ":" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
        }
        if (type.equals(ArchiveBuilder.PDF_TYPE)) {
            cached = EHServerCache.IIIF_PDF.hasKey(output);
            percentdone = ArchiveBuilder.pdfjobs.get(output);
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            cached = EHServerCache.IIIF_ZIP.hasKey(output);
            percentdone = ArchiveBuilder.zipjobs.get(output);
        }
        HashMap<String, Object> json = new HashMap<>();
        if (cached) {
            json.put("status", "done");
            json.put("link", url);
        } else if (percentdone != null) {
            json.put("status", "generating");
            final long percentint = Math.round(percentdone * 100);
            json.put("percentdone", String.valueOf(percentint));
        } else {
            json.put("status", "notrunning");
        }
        ObjectMapper mapper = new ObjectMapper();
        String html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json"));
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;
    }

    public static Map<String,String> templatesCache = new HashMap<>();
    public static String getTemplate(String template) {
        if (templatesCache.containsKey(template))
            return templatesCache.get(template);
        InputStream stream = ArchivesController.class.getClassLoader().getResourceAsStream("templates/" + template);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        try {
            String line = buffer.readLine();
            while (line != null) {
                sb.append(line + System.lineSeparator());
                line = buffer.readLine();
            }
            buffer.close();
        } catch (IOException e) {
            log.error("Could not get template as resource {}", template);
        }
        String res = sb.toString();
        templatesCache.put(template, res);
        return res;
    }

    public String getVolumeDownLoadLinks(PdfItemInfo item, Identifier idf, String type)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String links = "";
        List<String> vlist = item.getItemVolumes();
        for (int i = 0; i < vlist.size(); i++) {
            String s = vlist.get(i);
            String shortName = getShortName(s);
            IdentifierInfo vi = new IdentifierInfo("bdr:" + shortName);
            links = links + "<a type=\"application/" + type + "\" href=\"" + Application.getProperty("iiifserv_baseurl")
                    + "download/" + type + "/v:" + "bdr:" + shortName + "::1-" + vi.getTotalPages() + "\">Vol."
                    + vi.igi.volumeNumber + " (" + vi.getTotalPages() + " pages) - " + "bdr:" + shortName + "." + type
                    + "</a><br/>";
        }
        return links;
    }

    public HashMap<String, HashMap<String, String>> getJsonVolumeLinks(PdfItemInfo item, String type)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        List<String> vlist = item.getItemVolumes();
        for (int i = 0; i < vlist.size(); i++) {
            String s = vlist.get(i);
            String shortName = getShortName(s);
            HashMap<String, String> vol = new HashMap<>();
            vol.put("link", Application.getProperty("iiifserv_baseurl") + "download/" + type + "/v:" + "bdr:"
                    + shortName + "::1-");
            vol.put("volnum", Integer.toString(i));
            map.put("bdr:" + shortName, vol);
        }
        return map;
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }
    
    @RequestMapping(value = "/cachecleanup", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> cacheCleanup() throws Exception {
        EHServerCache.IIIF_IMG.cleanup();
        EHServerCache.IIIF_PDF.cleanup();
        EHServerCache.IIIF_ZIP.cleanup();
        ResponseEntity<String> response = new ResponseEntity<String>("OK", HttpStatus.OK);
        System.gc();
        return response;
    }
    
//    @RequestMapping(value = "/diskcacheinfo/{type}", method = {RequestMethod.GET, RequestMethod.HEAD})
//    public ResponseEntity<String> cacheCleanup() throws Exception {
//        EHServerCache.IIIF_IMG.cleanup();
//        EHServerCache.IIIF_PDF.cleanup();
//        EHServerCache.IIIF_ZIP.cleanup();
//        ResponseEntity<String> response = new ResponseEntity<String>("OK", HttpStatus.OK);
//        return response;
//    }

}