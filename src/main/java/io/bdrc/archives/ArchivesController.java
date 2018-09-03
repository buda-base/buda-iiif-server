package io.bdrc.archives;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.text.StrSubstitutor;
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

import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.auth.Access;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.pdf.presentation.ItemInfoService;
import io.bdrc.pdf.presentation.VolumeInfoService;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;
import io.bdrc.pdf.presentation.models.AccessType;
import io.bdrc.pdf.presentation.models.Identifier;
import io.bdrc.pdf.presentation.models.ItemInfo;
import io.bdrc.pdf.presentation.models.ItemInfo.VolumeInfoSmall;
import io.bdrc.pdf.presentation.models.VolumeInfo;

@Controller
public class ArchivesController {

    public static final String IIIF="IIIF";
    public static final String IIIF_ZIP="IIIF_ZIP";

    @RequestMapping(value = "/download/{type}/{id}", method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<String> getPdfLink(@PathVariable String id,@PathVariable String type,HttpServletRequest request) throws Exception {
        Access acc=(Access)request.getAttribute("access");
        String format=request.getHeader("Accept");
        boolean json=format.contains("application/json");
        String output =null;
        Identifier idf=new Identifier(id,Identifier.MANIFEST_ID);
        HttpHeaders headers = new HttpHeaders();
        HashMap<String,String> map=new HashMap<>();
        HashMap<String,HashMap<String,String>> jsonMap=new HashMap<>();
        StrSubstitutor s=null;
        String html="";
        AccessType access=null;
        int subType=idf.getSubType();
        switch(subType) {
            //Case work item
            case 4:
                ItemInfo item=ItemInfoService.fetchLdsVolumeInfo(idf.getItemId());
                access=item.getAccess();
                if(!acc.hasResourceAccess(getShortName(access.getUri()))) {
                    return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
                }
                if(json) {
                    jsonMap=getJsonVolumeLinks(item,type);
                    ObjectMapper mapper=new ObjectMapper();
                    html=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
                }else {
                    map.put("links", getVolumeDownLoadLinks(item,idf,type));
                    html=getTemplate("volumes.tpl");
                    s=new StrSubstitutor(map);
                    html=s.replace(html);
                }
                break;
            //Case volume imageRange
            case 5:
            case 6:
                int bPage=idf.getBPageNum().intValue();
                int ePage=idf.getEPageNum().intValue();
                VolumeInfo vi = VolumeInfoService.getVolumeInfo(idf.getVolumeId());
                access=vi.getAccess();
                if(!acc.hasResourceAccess(getShortName(access.getUri()))) {
                    return new ResponseEntity<>("Insufficient rights", HttpStatus.FORBIDDEN);
                }
                Iterator<String> idIterator = vi.getImageListIterator(bPage, ePage);
                output = idf.getVolumeId()+":"+bPage+"-"+ePage+"."+type;
                if(type.equals(ArchiveBuilder.PDF_TYPE)) {
                    Object pdf_cached =ServerCache.getObjectFromCache(IIIF,output);
                    //System.out.println("PDF "+id +" from IIIF cache >>"+pdf_cached);
                    if(pdf_cached==null) {
                        // Build pdf since the pdf file doesn't exist yet
                        ArchiveBuilder.buildPdf(idIterator,new IdentifierInfo(idf.getVolumeId()),output);
                    }
                }
                if(type.equals(ArchiveBuilder.ZIP_TYPE)) {
                    Object zip_cached =ServerCache.getObjectFromCache(IIIF,output);
                    //System.out.println("ZIP "+id +" from IIIF_ZIP cache >>"+zip_cached);
                    if(zip_cached==null) {
                        // Build pdf since the pdf file doesn't exist yet
                        ArchiveBuilder.buildZip(idIterator,new IdentifierInfo(idf.getVolumeId()),output);
                    }
                }
                // Create template and serve html link
                map.put("links", "/download/file/"+type+"/"+output);
                if(json) {
                    ObjectMapper mapper=new ObjectMapper();
                    html=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                }else {
                    html=getTemplate("downloadPdf.tpl");
                    map.put("file", output);
                    s=new StrSubstitutor(map);
                    html=s.replace(html);
                }
                break;
        }
        if(json) {
            headers.setContentType(MediaType.parseMediaType("application/json"));
        }else {
            headers.setContentType(MediaType.parseMediaType("text/html"));
        }
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;

    }

    @RequestMapping(value = "/download/file/{type}/{name}",
            method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<ByteArrayResource> downloadPdf(@PathVariable String name,@PathVariable String type) throws Exception {
        byte[] array=null;
        if(type.equals(ArchiveBuilder.PDF_TYPE)) {
            array=(byte[])ServerCache.getObjectFromCache(IIIF,name+".pdf");
        }
        if(type.equals(ArchiveBuilder.ZIP_TYPE)) {
            array=(byte[])ServerCache.getObjectFromCache(IIIF_ZIP,name+".zip");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/"+type));
        headers.setContentDispositionFormData("attachment", name+"."+type);
        ResponseEntity<ByteArrayResource> response = new ResponseEntity<ByteArrayResource>(
                new ByteArrayResource(array), headers, HttpStatus.OK);
        return response;
    }

    @RequestMapping(value = "/download/job/{type}/{id}",
            method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<String> jobState(@PathVariable String id,@PathVariable String type) throws Exception {
        Identifier idf=new Identifier(id,Identifier.MANIFEST_ID);
        String url="download/file/"+type+"/"+idf.getVolumeId()+":"+idf.getBPageNum()+"-"+idf.getEPageNum()+"."+type;
        boolean done=false;
        if(type.equals(ArchiveBuilder.PDF_TYPE)) {
            done= ArchiveBuilder.isPdfDone(idf.getVolumeId()+":"+idf.getBPageNum()+"-"+idf.getEPageNum()+".pdf");
        }
        if(type.equals(ArchiveBuilder.ZIP_TYPE)) {
            done= ArchiveBuilder.isZipDone(idf.getVolumeId()+":"+idf.getBPageNum()+"-"+idf.getEPageNum()+".zip");
        }
        HashMap<String,Object> json=new HashMap<>();
        json.put("done", done);
        json.put("link",url);
        ObjectMapper mapper=new ObjectMapper();
        String html=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json"));
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;
    }

    public static String getTemplate(String template) {
        InputStream stream = ArchivesController.class.getClassLoader().getResourceAsStream("templates/"+template);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb=new StringBuffer();
        try {
            String line=buffer.readLine();
            while(line!=null) {
                sb.append(line+System.lineSeparator());
                line=buffer.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String getVolumeDownLoadLinks(ItemInfo item,Identifier idf,String type) throws BDRCAPIException {
        String links="";
        List<VolumeInfoSmall> vlist=item.getVolumes();
        for(VolumeInfoSmall vis:vlist) {
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(vis.getPrefixedId());
            links=links+"<a href=\"/download/"+type+"/v:"+vis.getPrefixedId()+"::1-"+vi.totalPages+"\">Vol."+vis.getVolumeNumber()+" ("+vi.totalPages+" pages) - "+vis.getPrefixedId()+"</a><br/>";
        }
        return links;
    }

    public HashMap<String,HashMap<String,String>> getJsonVolumeLinks(ItemInfo item,String type) throws BDRCAPIException {
        HashMap<String,HashMap<String,String>> map=new HashMap<>();
        List<VolumeInfoSmall> vlist=item.getVolumes();
        for(VolumeInfoSmall vis:vlist) {
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(vis.getPrefixedId());
            HashMap<String,String> vol=new HashMap<>();
            vol.put("link","/download/"+type+"/v:"+vis.getPrefixedId()+"::1-"+vi.totalPages);
            vol.put("volume",vis.getVolumeNumber().toString());
            map.put(vis.getPrefixedId(),vol );
        }
        return map;
    }

    public static String getShortName(String st) {
        return st.substring(st.lastIndexOf("/")+1);
    }
}