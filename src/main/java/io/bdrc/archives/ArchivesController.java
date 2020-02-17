package io.bdrc.archives;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import de.digitalcollections.iiif.myhymir.ResourceAccessValidation;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.AppConstants;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageInfo;
import io.bdrc.iiif.resolver.ImageInfoListService;
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
        HttpHeaders headers = new HttpHeaders();
        HashMap<String, String> map = new HashMap<>();
        HashMap<String, HashMap<String, String>> jsonMap = new HashMap<>();
        StringSubstitutor s = null;
        String html = "";
        int subType = idf.getSubType();
        switch (subType) {
        // Case work item
        case 4:
            PdfItemInfo item = PdfItemInfo.getPdfItemInfo(idf.getItemId());
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
        case 5:
        case 6:
            int bPage = idf.getBPageNum().intValue();
            int ePage = idf.getEPageNum().intValue();
            IdentifierInfo inf = new IdentifierInfo(idf.getVolumeId());
            List<ImageInfo> ili = null;
            try {
                ili = ImageInfoListService.Instance.getAsync(inf.igi.imageInstanceId.substring(AppConstants.BDR_len), inf.igi.imageGroup).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), inf);
            AccessLevel al = accValidation.getAccess(request);
            if (al.equals(AccessLevel.NOACCESS) || al.equals(AccessLevel.MIXED)) {
                return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
            }
            List<String> imageList = getImageList(idf, inf, ili, al.equals(AccessLevel.FAIR_USE));
            if (al.equals(AccessLevel.FAIR_USE)) {
                output = idf.getVolumeId() + "FAIR_USE:" + bPage + "-" + ePage;// +"."+type;
            } else {
                output = idf.getVolumeId() + ":" + bPage + "-" + ePage;// +"."+type;
            }
            if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                Object pdf_cached = ServerCache.getObjectFromCache(IIIF, output);
                log.debug("PDF " + id + " from IIIF cache >>" + pdf_cached);
                if (pdf_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildPdf(imageList, inf, output, (String) request.getAttribute("origin"));
                }
            }
            if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                Object zip_cached = ServerCache.getObjectFromCache(IIIF_ZIP, output);
                log.debug("ZIP " + id + " from IIIF_ZIP cache >>" + zip_cached);
                if (zip_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildZip(imageList, inf, output, (String) request.getAttribute("origin"));
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
            array = (byte[]) ServerCache.getObjectFromCache(IIIF, name.substring(4));
            log.debug("READ from cache " + IIIF + " name=" + name + " " + array);
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            array = (byte[]) ServerCache.getObjectFromCache(IIIF_ZIP, name.substring(3));
        }
        HttpHeaders headers = new HttpHeaders();
        if (array == null) {
            headers.setContentType(MediaType.parseMediaType("text/plain"));
            array = new String("The link is wrong or has expired: please retry loading the archive and proceed to its download within 10 mn").getBytes();
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
        String url = "download/file/" + type + "/" + idf.getVolumeId() + ":" + idf.getBPageNum() + "-" + idf.getEPageNum() + "." + type;
        boolean done = false;
        if (type.equals(ArchiveBuilder.PDF_TYPE)) {
            done = ArchiveBuilder.isPdfDone(idf.getVolumeId() + ":" + idf.getBPageNum() + "-" + idf.getEPageNum() + ".pdf");
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            done = ArchiveBuilder.isZipDone(idf.getVolumeId() + ":" + idf.getBPageNum() + "-" + idf.getEPageNum() + ".zip");
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

    public String getVolumeDownLoadLinks(PdfItemInfo item, Identifier idf, String type) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String links = "";
        List<String> vlist = item.getItemVolumes();
        for (int i = 0; i<vlist.size(); i++) {
            String s = vlist.get(i);
            String shortName = getShortName(s);
            links = links + "<a type=\"application/" + type + "\" href=\"/download/" + type + "/v:" + "bdr:" + shortName + "::1-" + "\">" + Integer.toString(i) +" - " + "bdr:" + shortName
                    + "." + type + "</a><br/>";
        }
        return links;
    }

    public HashMap<String, HashMap<String, String>> getJsonVolumeLinks(PdfItemInfo item, String type) throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        List<String> vlist = item.getItemVolumes();
        for (int i = 0; i<vlist.size(); i++) {
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
    
    private List<String> getImageList(Identifier id, IdentifierInfo idf, List<ImageInfo> ili, boolean fairUse) {
        final int totalPages = ili.size();
        final List<String> res = new ArrayList<>();
        int beginIndex = id.getBPageNum() == null ? 1 + idf.igi.pagesIntroTbrc : id.getBPageNum().intValue();
        int endIndex = id.getEPageNum() == null ? totalPages : Math.min(totalPages, id.getEPageNum().intValue()); 
        if (!fairUse) {
            for (int imgSeqNum = beginIndex; imgSeqNum <= endIndex; imgSeqNum++) {
                res.add(ili.get(imgSeqNum).filename);
            }
            return res;
        }
        final int firstUnaccessiblePage = AppConstants.FAIRUSE_PAGES_S + idf.igi.pagesIntroTbrc + 1;
        final int lastUnaccessiblePage = totalPages - AppConstants.FAIRUSE_PAGES_E;
        // first part: min(firstUnaccessiblePage+1,beginIndex) to
        // min(endIndex,firstUnaccessiblePage+1)
        for (int imgSeqNum = Math.min(firstUnaccessiblePage, beginIndex); imgSeqNum <= Math.min(endIndex, firstUnaccessiblePage - 1); imgSeqNum++) {
            res.add(ili.get(imgSeqNum).filename);
        }
        // then copyright page, if either beginIndex or endIndex is
        // > FAIRUSE_PAGES_S+tbrcintro and < vi.totalPages-FAIRUSE_PAGES_E
        if ((beginIndex >= firstUnaccessiblePage && beginIndex <= lastUnaccessiblePage) || (endIndex >= firstUnaccessiblePage && endIndex <= lastUnaccessiblePage) || (beginIndex < firstUnaccessiblePage && endIndex > lastUnaccessiblePage)) {
            // TODO: add copyright page
        }
        // last part: max(beginIndex,lastUnaccessiblePage) to
        // max(endIndex,lastUnaccessiblePage)
        for (int imgSeqNum = Math.max(lastUnaccessiblePage + 1, beginIndex); imgSeqNum <= Math.max(endIndex, lastUnaccessiblePage); imgSeqNum++) {
            res.add(ili.get(imgSeqNum).filename);
        }
        return res;
    }

}