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
import io.bdrc.auth.Access;
import io.bdrc.iiif.presentation.ItemInfoService;
import io.bdrc.iiif.presentation.VolumeInfoService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.AccessType;
import io.bdrc.iiif.presentation.models.Identifier;
import io.bdrc.iiif.presentation.models.ImageListIterator;
import io.bdrc.iiif.presentation.models.ItemInfo;
import io.bdrc.iiif.presentation.models.ItemInfo.VolumeInfoSmall;
import io.bdrc.iiif.presentation.models.VolumeInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;

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
        AccessType access = null;
        int subType = idf.getSubType();
        switch (subType) {
        // Case work item
        case 4:
            ItemInfo item = ItemInfoService.getItemInfo(idf.getItemId());
            access = item.access;
            if (!acc.hasResourceAccess(getShortName(access.getUri()))) {
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
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(idf.getVolumeId(), false);
            access = vi.getAccess();
            String accessType = getShortName(access.getUri());
            ResourceAccessValidation accValidation = new ResourceAccessValidation((Access) request.getAttribute("access"), accessType);
            if (!accValidation.isAccessible(request)) {
                return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
            }
            Iterator<String> idIterator = null;
            int introPages = vi.getPagesIntroTbrc().intValue();
            if (accValidation.isFairUse()) {
                idIterator = getFairUseImgListIterator(bPage, ePage, vi);
                output = idf.getVolumeId() + "FAIR_USE:" + bPage + "-" + ePage;// +"."+type;
            } else {
                if (introPages > 0) {
                    idIterator = vi.getImageListIterator(bPage + introPages, ePage);
                } else {
                    idIterator = vi.getImageListIterator(bPage, ePage);
                }
                output = idf.getVolumeId() + ":" + bPage + "-" + ePage;// +"."+type;
            }
            if (type.equals(ArchiveBuilder.PDF_TYPE)) {
                Object pdf_cached = ServerCache.getObjectFromCache(IIIF, output);
                log.debug("PDF " + id + " from IIIF cache >>" + pdf_cached);
                if (pdf_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildPdf(idIterator, inf, output, vi);
                }
            }
            if (type.equals(ArchiveBuilder.ZIP_TYPE)) {
                Object zip_cached = ServerCache.getObjectFromCache(IIIF_ZIP, output);
                log.debug("ZIP " + id + " from IIIF_ZIP cache >>" + zip_cached);
                if (zip_cached == null) {
                    // Build pdf since the pdf file doesn't exist yet
                    ArchiveBuilder.buildZip(idIterator, inf, output);
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
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String getVolumeDownLoadLinks(ItemInfo item, Identifier idf, String type) throws BDRCAPIException {
        String links = "";
        List<VolumeInfoSmall> vlist = item.volumes;
        for (VolumeInfoSmall vis : vlist) {
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(vis.prefixedId, false);
            links = links + "<a type=\"application/" + type + "\" href=\"/download/" + type + "/v:" + vis.prefixedId + "::1-" + vi.totalPages + "\">Vol." + vis.volumeNumber + " (" + vi.totalPages + " pages) - " + vis.prefixedId + "." + type
                    + "</a><br/>";
        }
        return links;
    }

    public HashMap<String, HashMap<String, String>> getJsonVolumeLinks(ItemInfo item, String type) throws BDRCAPIException {
        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        List<VolumeInfoSmall> vlist = item.volumes;
        for (VolumeInfoSmall vis : vlist) {
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(vis.prefixedId, false);
            HashMap<String, String> vol = new HashMap<>();
            vol.put("link", "/download/" + type + "/v:" + vis.prefixedId + "::1-" + vi.totalPages);
            vol.put("volume", vis.volumeNumber.toString());
            map.put(vis.prefixedId, vol);
        }
        return map;
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/") + 1);
    }

    private Iterator<String> getFairUseImgListIterator(int bPage, int ePage, VolumeInfo vi) {
        ArrayList<String> img = new ArrayList<>();
        int x = 0;
        int introPages = vi.getPagesIntroTbrc().intValue();
        ImageListIterator it1 = null;
        if (bPage == 1 && introPages > 0) {
            it1 = new ImageListIterator(vi.getImageList(), bPage + introPages, 20 + introPages);
        } else {
            it1 = new ImageListIterator(vi.getImageList(), bPage, 20);
        }
        while (it1.hasNext()) {
            img.add(x, it1.next());
            x++;
        }
        ImageListIterator it2 = new ImageListIterator(vi.getImageList(), vi.getTotalPages().intValue() - 19, ePage);
        while (it2.hasNext()) {
            img.add(x, it2.next());
            x++;
        }
        return img.iterator();
    }
}