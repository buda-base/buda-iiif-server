package io.bdrc.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

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

@Controller
@RequestMapping("/pdfdownload/")
public class PdfController {
    
    private String getUrlBase(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
          scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
          host = request.getHeader("Host");
        }
        if (host == null) {
          host = request.getRemoteHost();
        }
        String base = String.format("%s://%s", scheme, host);
        if (!request.getContextPath().isEmpty()) {
          base += request.getContextPath();
        }
        return base;
      }
    
    @RequestMapping(value = "{identifier}/pdf",
            method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<InputStreamResource> getInfo(@PathVariable String identifier, HttpServletRequest req,
            WebRequest webRequest) throws Exception {        
        //System.out.println("PDF >>>>>>>>> "+getUrlBase(req)+"/image/v2/"+identifier+"/full/full/0/default.jpg");
        System.out.println("DEB >>>>>>>>>>> "+System.currentTimeMillis());
        Image img=Image.getInstance(new URL(getUrlBase(req)+"/image/v2/"+identifier+"/full/full/0/default.jpg"));
        Image img1=Image.getInstance(new URL(getUrlBase(req)+"/image/v2/bdr:V29329_I1KG15042::I1KG150420323.jpg/full/full/0/default.jpg"));
        //Image img=Image.getInstance("/home/marc/0.jpg");
        //Image img1=Image.getInstance("/home/marc/1.jpg");
        String output = "/home/marc/capture.pdf";
        System.out.println("DEB1 >>>>>>>>>>> "+System.currentTimeMillis());
        Document document = new Document(new Rectangle(img.getWidth(),img.getHeight()),0f,0f,0f,0f);
        FileOutputStream fos = new FileOutputStream(output);
        PdfWriter writer = PdfWriter.getInstance(document, fos);
        writer.open();
        document.open();
        document.add(img);
        document.add(img1);
        document.close();
        writer.close();
        System.out.println("Fin1 >>>>>>>>>>> "+System.currentTimeMillis());
        HttpHeaders headers = new HttpHeaders();
        File pdfFile = new File(output);
        System.out.println("Fin >>>>>>>>>>> "+System.currentTimeMillis());
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        ResponseEntity<InputStreamResource> response = new ResponseEntity<InputStreamResource>(
                new InputStreamResource(new FileInputStream(pdfFile)), headers, HttpStatus.OK);
        return response;
    }

}
