package io.bdrc.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.context.request.WebRequest;

import com.amazonaws.services.s3.AmazonS3;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.pdf.presentation.VolumeInfoService;
import io.bdrc.pdf.presentation.models.Identifier;
import io.bdrc.pdf.presentation.models.VolumeInfo;

@Controller
@RequestMapping("/pdfdownload/")
public class PdfController {
    
    final static String S3_BUCKET = "archive.tbrc.org";

    @RequestMapping(value = "{id}",
            method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<String> getPdfLink(@PathVariable String id) throws Exception {
        Identifier idf=new Identifier(id,Identifier.MANIFEST_ID);
        int bPage=idf.getBPageNum().intValue();
        int ePage=idf.getEPageNum().intValue();
        VolumeInfo vi = VolumeInfoService.getVolumeInfo(idf.getVolumeId());
        Iterator<String> idIterator = vi.getImageListIterator(bPage, ePage);
        String output = idf.getVolumeId()+":"+bPage+"-"+ePage+".pdf";
     // Build pdf
        PdfBuilder.buildPdf(idIterator, new IdentifierInfo(idf.getVolumeId()),output);
        HashMap<String,String> map=new HashMap<>();
        map.put("pdf", output);
        map.put("link", "/pdfdownload/file/"+output);
        
        // Create template and serve html link
        String html=getTemplate("downloadPdf.tpl");
        StrSubstitutor s=new StrSubstitutor(map);
        html=s.replace(html);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/html"));
        ResponseEntity<String> response = new ResponseEntity<String>(html, headers, HttpStatus.OK);
        return response;  
    }
    
    @RequestMapping(value = "file/{pdf}",
            method = {RequestMethod.GET,RequestMethod.HEAD})
    public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable String pdf) throws Exception {
        
        File pdfFile=new File(pdf+".pdf");
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
    
    
}