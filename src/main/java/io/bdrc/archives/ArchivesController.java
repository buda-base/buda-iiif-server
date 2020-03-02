package io.bdrc.archives;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.libraries.Identifier;
import io.bdrc.libraries.ImageListIterator;

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
            IdentifierInfo inf = IdentifierInfo.getIndentifierInfo(idf.getVolumeId());
            String accessType = inf.getAccessShortName();
            ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), accessType);
            if (!accValidation.isAccessible(request)) {
                return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
            }
            Iterator<String> idIterator = null;
            int introPages = inf.getPagesIntroTbrc();
            if (accValidation.isFairUse()) {
                idIterator = getFairUseImgListIterator(bPage, ePage, inf);
                output = idf.getVolumeId() + "FAIR_USE:" + bPage + "-" + ePage;// +"."+type;
            } else {
                if (introPages > 0) {
                    idIterator = inf.getImageListIterator(bPage + introPages, ePage);
                } else {
                    idIterator = inf.getImageListIterator(bPage, ePage);
                }
                output = idf.getVolumeId() + ":" + bPage + "-" + ePage;// +"."+type;
            }
            if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                Object pdf_cached = ServerCache.IIIF.get(output);
                log.debug("PDF " + id + " from IIIF cache >>" + pdf_cached);
                if (pdf_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildPdf(idIterator, inf, output, (String) request.getAttribute("origin"));
                }
            }
            if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                Object zip_cached = ServerCache.IIIF_ZIP.get(output);
                log.debug("ZIP " + id + " from IIIF_ZIP cache >>" + zip_cached);
                if (zip_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildZip(idIterator, inf, output, (String) request.getAttribute("origin"));
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
            array = (byte[]) ServerCache.IIIF.get(name.substring(4));
            log.debug("READ from cache " + IIIF + " name=" + name + " " + array);
        }
        if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
            array = (byte[]) ServerCache.IIIF_ZIP.get(name.substring(3));
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

    public String getVolumeDownLoadLinks(PdfItemInfo item, Identifier idf, String type)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String links = "";
        List<String> vlist = item.getItemVolumes();
        for (String s : vlist) {
            String shortName = getShortName(s);
            IdentifierInfo vi = IdentifierInfo.getIndentifierInfo("bdr:" + shortName);
            links = links + "<a type=\"application/" + type + "\" href=\"/download/" + type + "/v:" + "bdr:" + shortName + "::1-" + vi.getTotalPages()
                    + "\">Vol." + vi.getVolumeNumber() + " (" + vi.getTotalPages() + " pages) - " + "bdr:" + shortName + "." + type + "</a><br/>";
        }
        return links;
    }

    public HashMap<String, HashMap<String, String>> getJsonVolumeLinks(PdfItemInfo item, String type)
            throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        List<String> vlist = item.getItemVolumes();
        for (String s : vlist) {
            String shortName = getShortName(s);
            IdentifierInfo vi = IdentifierInfo.getIndentifierInfo("bdr:" + shortName);
            HashMap<String, String> vol = new HashMap<>();
            vol.put("link", "/download/" + type + "/v:" + "bdr:" + shortName + "::1-" + vi.getTotalPages());
            vol.put("volume", Integer.toString(vi.getVolumeNumber()));
            map.put("bdr:" + shortName, vol);
        }
        return map;
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

    private Iterator<String> getFairUseImgListIterator(int bPage, int ePage, IdentifierInfo inf) {
        ArrayList<String> img = new ArrayList<>();
        int x = 0;
        int introPages = inf.getPagesIntroTbrc();
        ImageListIterator it1 = null;
        if (bPage == 1 && introPages > 0) {
            it1 = new ImageListIterator(inf.getImageList(), bPage + introPages, 20 + introPages);
        } else {
            it1 = new ImageListIterator(inf.getImageList(), bPage, 20);
        }
        while (it1.hasNext()) {
            img.add(x, it1.next());
            x++;
        }
        ImageListIterator it2 = new ImageListIterator(inf.getImageList(), inf.getTotalPages() - 19, ePage);
        while (it2.hasNext()) {
            img.add(x, it2.next());
            x++;
        }
        return img.iterator();
    }
}