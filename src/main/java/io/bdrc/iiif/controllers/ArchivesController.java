package io.bdrc.iiif.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringSubstitutor;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.iiif.archives.ArchiveBuilder;
import io.bdrc.iiif.archives.PdfItemInfo;
import io.bdrc.iiif.auth.ResourceAccessValidation;
import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageInfoListService;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;
import io.bdrc.libraries.Identifier;

@Controller
public class ArchivesController {

    public static final String IIIF = "IIIF";
    public static final String IIIF_ZIP = "IIIF_ZIP";

    public final static Logger log = LoggerFactory.getLogger(ArchivesController.class.getName());

    @RequestMapping(value = "/download/{type}/{id}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> getPdfLink(@PathVariable String id, @PathVariable String type, HttpServletRequest request) throws Exception {
        Access acc = (Access) request.getAttribute("access");
        if (acc == null) {
            acc = new Access();
        }
        String format = request.getHeader("Accept");
        boolean json = format.contains("application/json");
        String output = null;
        Identifier idf = new Identifier(id, Identifier.MANIFEST_ID);
        log.info("Identifier object {}", idf);
        HttpHeaders headers = new HttpHeaders();
        HashMap<String, String> map = new HashMap<>();
        HashMap<String, HashMap<String, String>> jsonMap = new HashMap<>();
        StringSubstitutor s = null;
        String html = "";
        int subType = idf.getSubType();
        switch (subType) {
        // Case work in item
        case Identifier.MANIFEST_ID_WORK_IN_ITEM:
            PdfItemInfo item = PdfItemInfo.getPdfItemInfo(idf.getImageInstanceId());
            if (!acc.hasResourceAccess(item.getItemAccess())) {
                return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
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
        case Identifier.MANIFEST_ID_VOLUMEID:
        case Identifier.MANIFEST_ID_WORK_IN_VOLUMEID:
            IdentifierInfo inf = new IdentifierInfo(idf.getImageGroupId());
            Integer bPage = idf.getBPageNum();
            if (bPage == null) {
                bPage = new Integer(1);
            }
            Integer ePage = idf.getEPageNum();
            if (ePage == null) {
                ePage = inf.getTotalPages();
            }
            log.info("Pdf requested numPage in identifierInfo {}", inf);
            log.info("Pdf requested start page {} and end page {}", bPage.intValue(), ePage.intValue());

            List<ImageInfo> ili = null;
            try {
                ili = ImageInfoListService.Instance.getAsync(inf.igi.imageInstanceId.substring(AppConstants.BDR_len), inf.igi.imageGroup).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), inf);
            AccessLevel al = accValidation.getAccessLevel(request);
            if (al.equals(AccessLevel.NOACCESS) || al.equals(AccessLevel.MIXED)) {
                return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
            }
            if (al.equals(AccessLevel.FAIR_USE)) {
                output = idf.getImageGroupId() + "FAIR_USE:" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
            } else {
                output = idf.getImageGroupId() + ":" + bPage.intValue() + "-" + ePage.intValue();// +"."+type;
            }
            if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                Object pdf_cached = EHServerCache.IIIF.get(output);
                log.debug("PDF " + id + " from IIIF cache >>" + pdf_cached);
                if (pdf_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildPdf(accValidation.getAccess(), inf, idf, output, (String) request.getAttribute("origin"));
                }
            }
            if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                Object zip_cached = EHServerCache.IIIF_ZIP.get(output);
                log.debug("ZIP " + id + " from IIIF_ZIP cache >>" + zip_cached);
                if (zip_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildZip(accValidation.getAccess(), inf, idf, output, (String) request.getAttribute("origin"));
                }
            }
            // Create template and serve html link
            map.put("links", "/download/file/" + type + "/" + output);
            if (json) {
                ObjectMapper mapper = new ObjectMapper();
                html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
            } else {
                html = getTemplate("downloadPdf.tpl");
                map.put("file", output + "." + type);
                s = new StringSubstitutor(map);
                html = s.replace(html);
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

    @RequestMapping(value = "/download/file/{type}/{name}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<ByteArrayResource> downloadPdf(@PathVariable String name, @PathVariable String type) throws Exception {
        byte[] array = null;
        if (type.equals(ArchiveBuilder.PDF_TYPE)) {
            array = (byte[]) EHServerCache.IIIF.get(name.substring(4));
            log.info("READ for key {} from cache " + IIIF + " name=" + name + " " + array, name.substring(4));
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            array = (byte[]) EHServerCache.IIIF_ZIP.get(name.substring(3));
            log.info("READ for key {} from cache " + IIIF_ZIP + " name=" + name + " " + array, name.substring(3));
        }
        HttpHeaders headers = new HttpHeaders();
        if (array == null) {
            headers.setContentType(MediaType.parseMediaType("text/plain"));
            array = new String("The link is wrong or has expired: please retry loading the archive and proceed to its download within 10 mn")
                    .getBytes();
            return new ResponseEntity<ByteArrayResource>(new ByteArrayResource(array), headers, HttpStatus.NOT_FOUND);
        }
        headers.setContentType(MediaType.parseMediaType("application/" + type));
        headers.setContentDispositionFormData("attachment", name.substring(4) + "." + type);
        ResponseEntity<ByteArrayResource> response = new ResponseEntity<ByteArrayResource>(new ByteArrayResource(array), headers, HttpStatus.OK);
        return response;
    }

    @RequestMapping(value = "/download/job/{type}/{id}", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> jobState(@PathVariable String id, @PathVariable String type) throws Exception {
        Identifier idf = new Identifier(id, Identifier.MANIFEST_ID);
        Integer bPage = idf.getBPageNum();
        if (bPage == null) {
            bPage = new Integer(1);
        }
        Integer ePage = idf.getEPageNum();
        if (ePage == null) {
            IdentifierInfo inf = new IdentifierInfo(idf.getImageGroupId());
            ePage = inf.getTotalPages();
        }
        String url = "download/file/" + type + "/" + idf.getImageGroupId() + ":" + bPage + "-" + ePage + "." + type;
        boolean done = false;
        if (type.equals(ArchiveBuilder.PDF_TYPE)) {
            done = ArchiveBuilder.isPdfDone(idf.getImageGroupId() + ":" + bPage + "-" + ePage + ".pdf");
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            done = ArchiveBuilder.isZipDone(idf.getImageGroupId() + ":" + bPage + "-" + ePage + ".zip");
        }
        HashMap<String, Object> json = new HashMap<>();
        json.put("done", done);
        json.put("link", url);
        ObjectMapper mapper = new ObjectMapper();
        String html = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json"));
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;
    }

    public static String getTemplate(String template) {
        InputStream stream = ArchivesController.class.getClassLoader().getResourceAsStream("templates/" + template);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        try {
            String line = buffer.readLine();
            while (line != null) {
                sb.append(line + System.lineSeparator());
                line = buffer.readLine();
            }
        } catch (IOException e) {
            log.error("Could not get template as resource {}", template);
        }
        return sb.toString();
    }

    public String getVolumeDownLoadLinks(PdfItemInfo item, Identifier idf, String type)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String links = "";
        List<String> vlist = item.getItemVolumes();
        for (int i = 0; i < vlist.size(); i++) {
            String s = vlist.get(i);
            String shortName = getShortName(s);
            IdentifierInfo vi = new IdentifierInfo("bdr:" + shortName);
            links = links + "<a type=\"application/" + type + "\" href=\"/download/" + type + "/v:" + "bdr:" + shortName + "::1-" + vi.getTotalPages()
                    + "\">Vol." + vi.igi.volumeNumber + " (" + vi.getTotalPages() + " pages) - " + "bdr:" + shortName + "." + type + "</a><br/>";
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
            vol.put("link", "/download/" + type + "/v:" + "bdr:" + shortName + "::1-");
            vol.put("volume", Integer.toString(i));
            map.put("bdr:" + shortName, vol);
        }
        return map;
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

}