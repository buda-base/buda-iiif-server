package io.bdrc.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

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
    public ResponseEntity<InputStreamResource> getPdf(@PathVariable String volume, 
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
        String output = volume+".pdf";
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
        
        HttpHeaders headers = new HttpHeaders();
        File pdfFile = new File(output);       
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
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
    

}
