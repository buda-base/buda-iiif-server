package io.bdrc.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
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

@Controller
@RequestMapping("/pdfdownload/")
public class PdfController {
    
    final static String S3_BUCKET = "archive.tbrc.org";
    
       
    @RequestMapping(value = "{volume}/pdf/{imageList}/{numPage}",
            method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> getPdf(@PathVariable String volume, 
                                @PathVariable String imageList,
                                @PathVariable String numPage,
                                HttpServletRequest req,
                                WebRequest webRequest) throws Exception {  
        
        
        // Getting volume info
        System.out.println("call to getPdf() >>> imgList >> "+imageList+" volume >> "+volume+ " numPage >> "+numPage);  
        IdentifierInfo inf=new IdentifierInfo(volume);
        ExecutorService service=Executors.newFixedThreadPool(50);
        AmazonS3 s3=S3ResourceRepositoryImpl.getClientInstance();
        TreeMap<Integer,String> idList=getIdentifierList(volume,imageList,numPage);
        TreeMap<Integer,PdfImageProducer> p_map=new TreeMap<>();
        TreeMap<Integer,Future<?>> t_map=new TreeMap<>();
        
        for(Entry<Integer,String> e:idList.entrySet()) {
            PdfImageProducer tmp=new PdfImageProducer(s3,e.getValue(), inf);
            p_map.put(e.getKey(),tmp);
            Future<?> fut=service.submit(tmp);
            t_map.put(e.getKey(),fut);
        }
        
        Document document = new Document();
        String output = volume+":1-"+numPage+".pdf";
        
        FileOutputStream fos = new FileOutputStream(output);        
        PdfWriter writer = PdfWriter.getInstance(document, fos);
        writer.open();
        document.open();
        for(int k=1;k<=t_map.keySet().size();k++) {
            Future<?> tmp=t_map.get(k);
            while(!tmp.isDone()) {
                
            };
            Image i=p_map.get(k).getImg();
            if(i!=null) {
                document.setPageSize(new Rectangle(i.getWidth(),i.getHeight()));
                document.newPage();
                document.add(i);
            }
        }
        document.close();
        writer.close();
        
        HashMap<String,String> map=new HashMap<>();
        map.put("pdf", output);
        map.put("link", "/pdfdownload/file/"+output);
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
    
    public TreeMap<Integer,String> getIdentifierList(String volume,String imgList,String numPage) {
        TreeMap<Integer,String> idt=new TreeMap<>();
        String[] part=imgList.split(":");
        String pages[]=part[0].split("\\.");
        String firstPage=pages[0].substring(pages[0].length()-4);
        String root=pages[0].substring(0,pages[0].length()-4);
        //for(int x=Integer.parseInt(firstPage);x<Integer.parseInt(part[1])+1;x++) {
        for(int x=Integer.parseInt(firstPage);x<Integer.parseInt(numPage)+1;x++) {
            idt.put(x,volume+"::"+root+String.format("%04d", x)+"."+pages[1]);
        }
        return idt;
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
