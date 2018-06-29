package io.bdrc.pdf;

import static io.bdrc.pdf.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

public class PdfBuilder {
    
    public static PdfServiceRegistry registry = PdfServiceRegistry.getInstance();
    public static final String IIIF="IIIF";
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void buildPdf(Iterator<String> idList,IdentifierInfo inf,String output)  
                                        throws BDRCAPIException {
        ExecutorService service=Executors.newFixedThreadPool(50);
        AmazonS3 s3=S3ResourceRepositoryImpl.getClientInstance();
        TreeMap<Integer,Future<?>> t_map=new TreeMap<>();
        int i = 1;
        while(idList.hasNext()) {
            final String id = inf.getVolumeId()+"::"+idList.next();  
            PdfImageProducer tmp=null;
            tmp = new PdfImageProducer(s3, id);
            Future<?> fut=service.submit((Callable)tmp);
            t_map.put(i,fut);
            i += 1;
        }
        registry.addPdfService(output, false);
        Document document = new Document();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PdfWriter writer=null;
        try {
            writer = PdfWriter.getInstance(document, stream);
        } catch (DocumentException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
        writer.open();
        document.open();
        for(int k=1;k<=t_map.keySet().size();k++) {
            Future<?> tmp=t_map.get(k);
            Image img=null;
            try {
                img = (Image)tmp.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
            }
            if(img!=null) {
                document.setPageSize(new Rectangle(img.getWidth(),img.getHeight()));
                document.newPage();
                try {
                    document.add(img);
                } catch (DocumentException e) {
                    throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
                }
            }
        }
        document.close();
        ServerCache.addToCache(IIIF,output, stream.toByteArray());
        writer.close();
        registry.setCompleted(output); 
    }
}
