package io.bdrc.pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.amazonaws.services.s3.AmazonS3;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class PdfBuilder {
    
    public static PdfServiceRegistry registry = PdfServiceRegistry.getInstance();
    
    public static void buildPdf(Iterator<String> idList,
                                IdentifierInfo inf,
                                String output) throws NoSuchAlgorithmException, FileNotFoundException, DocumentException, ResourceIOException {
        ExecutorService service=Executors.newFixedThreadPool(50);
        AmazonS3 s3=S3ResourceRepositoryImpl.getClientInstance();        
        TreeMap<Integer,PdfImageProducer> p_map=new TreeMap<>();
        TreeMap<Integer,Future<?>> t_map=new TreeMap<>();
        int i = 1;
        while(idList.hasNext()) {
            final String id = inf.getVolumeId()+"::"+idList.next();  
            PdfImageProducer tmp=new PdfImageProducer(s3, id);
            p_map.put(i,tmp);
            Future<?> fut=service.submit(tmp);
            t_map.put(i,fut);
            i += 1;
        }
        registry.addPdfService(output, false);
        Document document = new Document();
        FileOutputStream fos = new FileOutputStream("pdf/"+output);        
        PdfWriter writer = PdfWriter.getInstance(document, fos);
        writer.open();
        document.open();
        for(int k=1;k<=t_map.keySet().size();k++) {
            Future<?> tmp=t_map.get(k);
            while(!tmp.isDone()) {
                
            };
            Image img=p_map.get(k).getImg();
            if(img!=null) {
                document.setPageSize(new Rectangle(img.getWidth(),img.getHeight()));
                document.newPage();
                document.add(img);
            }
        }
        document.close();
        writer.close();
        registry.setCompleted(output);
    }
}
