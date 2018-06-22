package io.bdrc.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.pdf.presentation.ItemInfoService;
import io.bdrc.pdf.presentation.VolumeInfoService;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;
import io.bdrc.pdf.presentation.models.Identifier;
import io.bdrc.pdf.presentation.models.ItemInfo;
import io.bdrc.pdf.presentation.models.ItemInfo.VolumeInfoSmall;
import io.bdrc.pdf.presentation.models.VolumeInfo;

@Controller
@RequestMapping("/pdfdownload/")
public class PdfController {

    @RequestMapping(value = "{id}", method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<String> getPdfLink(@PathVariable String id) throws Exception {
        String output =null;
        Identifier idf=new Identifier(id,Identifier.MANIFEST_ID);
        System.out.println(idf);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/html"));
        HashMap<String,String> map=new HashMap<>();
        StrSubstitutor s=null;
        
        int subType=idf.getSubType();
        switch(subType) {
            //Case work item   
            case 4:
                ItemInfo item=ItemInfoService.fetchLdsVolumeInfo(idf.getItemId());
                String html=getTemplate("volumes.tpl");                
                map.put("links", getVolumeDownLoadLinks(item,idf)); 
                s=new StrSubstitutor(map);
                html=s.replace(html);
                ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
                return response; 
            //Case volume imageRange
            case 5:
            case 6:
                int bPage=idf.getBPageNum().intValue();
                int ePage=idf.getEPageNum().intValue();
                VolumeInfo vi = VolumeInfoService.getVolumeInfo(idf.getVolumeId());                
                Iterator<String> idIterator = vi.getImageListIterator(bPage, ePage);
                output = idf.getVolumeId()+":"+bPage+"-"+ePage+".pdf";
                
                // Build pdf
                PdfBuilder.buildPdf(idIterator,new IdentifierInfo(idf.getVolumeId()),output);                
                
                // Create template and serve html link
                String html1=getTemplate("downloadPdf.tpl");
                map.put("pdf", output);
                map.put("link", "/pdfdownload/file/"+output);
                s=new StrSubstitutor(map);
                html1=s.replace(html1);
                ResponseEntity<String> response1 = new ResponseEntity<String>(html1, headers, HttpStatus.OK);
                return response1;
        } 
        ResponseEntity<String> response = new ResponseEntity<String>("Resource Not found", headers, HttpStatus.NOT_FOUND);
        return response;
          
    }
    
    @RequestMapping(value = "file/{pdf}",
            method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable String pdf) throws Exception {
        
        File pdfFile=new File("pdf/"+pdf+".pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        headers.setContentDispositionFormData("attachment", pdfFile.getName());
        ResponseEntity<InputStreamResource> response = new ResponseEntity<InputStreamResource>(
                new InputStreamResource(new FileInputStream(pdfFile)), headers, HttpStatus.OK);
        return response;
    }

    public static String getTemplate(String template) {
        InputStream stream = PdfController.class.getClassLoader().getResourceAsStream("templates/"+template);
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
    
    public String getVolumeDownLoadLinks(ItemInfo item,Identifier idf) throws BDRCAPIException {
        String links="";
        List<VolumeInfoSmall> vlist=item.getVolumes();
        for(VolumeInfoSmall vis:vlist) {
            VolumeInfo vi = VolumeInfoService.getVolumeInfo(vis.getPrefixedId());
            links=links+"<a href=\"/pdfdownload/v:"+vis.getPrefixedId()+"::1-"+vi.totalPages+"\">Vol."+vis.getVolumeNumber()+" ("+vi.totalPages+" pages) - "+vis.getPrefixedId()+"</a><br/>";
        }
        return links;
    }
    
    
}