package io.bdrc.pdf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;

import de.digitalcollections.iiif.myhymir.backend.impl.repository.S3ResourceRepositoryImpl;
import io.bdrc.iiif.resolver.BdrcS3Resolver;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class PdfImageProducer implements Runnable{
    
    final static String S3_BUCKET = "archive.tbrc.org";
    
    AmazonS3 s3;
    String identifier;
    Image img;
    
    public PdfImageProducer(AmazonS3 s3, String identifier,IdentifierInfo inf) throws NoSuchAlgorithmException {
        this.s3=s3;        
        BdrcS3Resolver resolver=new BdrcS3Resolver();
        this.identifier=resolver.getIdentifier(identifier, inf);
        System.out.println("PDF Producer >>>>>> "+identifier);
    }

    public Image getImage() throws BadElementException, MalformedURLException, IOException {
        GetObjectRequest request = new GetObjectRequest(
                S3_BUCKET,
                identifier);
        return Image.getInstance(IOUtils.toByteArray(s3.getObject(request).getObjectContent()));              
    }
    
    
    
    public Image getImg() {
        return img;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            img=getImage();
            System.out.println("FINISHED >>> "+identifier);
        } catch (BadElementException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
