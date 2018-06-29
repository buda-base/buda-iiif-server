package io.bdrc.pdf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.resolver.BdrcS3Resolver;

public class PdfImageProducer implements Runnable{
    
    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG="IIIF_IMG";
    
    AmazonS3 s3;
    String identifier;
    String id;
    Image img;
    
    
    public PdfImageProducer(AmazonS3 s3, String id) throws NoSuchAlgorithmException, ResourceIOException {
        this.s3=s3;
        this.id=id;
        BdrcS3Resolver resolver=new BdrcS3Resolver();
        this.identifier=resolver.getS3Identifier(id);
    }

    public Image getImage() throws BadElementException, MalformedURLException, IOException {        
        byte[] b=(byte[])ServerCache.getObjectFromCache(IIIF_IMG,id);        
        if(b !=null) {            
            return Image.getInstance(b);
        }
        GetObjectRequest request = new GetObjectRequest(
                S3_BUCKET,
                identifier);
        byte[] imgbytes=IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        img=Image.getInstance(imgbytes);
        ServerCache.addToCache(IIIF_IMG,id, imgbytes);
        return img;              
    }
    
    public Image getImg() {
        return img;
    }

    @Override
    public void run() {
        try {
            img=getImage();
            //System.out.println("FINISHED >>> "+identifier);
        } catch (BadElementException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
