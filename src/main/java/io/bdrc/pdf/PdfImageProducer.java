package io.bdrc.pdf;

import static io.bdrc.pdf.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.resolver.BdrcS3Resolver;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

@SuppressWarnings("rawtypes")
public class PdfImageProducer implements Callable{
    
    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG="IIIF_IMG";
    
    AmazonS3 s3;
    String identifier;
    String id;
    Image img;
    
    
    public PdfImageProducer(AmazonS3 s3, String id) throws BDRCAPIException {
        this.s3=s3;
        this.id=id;
        BdrcS3Resolver resolver=new BdrcS3Resolver();
        try {
            this.identifier=resolver.getS3Identifier(id);
        } catch (ResourceIOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

    public Image getImage() throws BadElementException, MalformedURLException, IOException, BDRCAPIException {        
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
    public Image call() throws BDRCAPIException {
        try {
            return getImage();
        } catch (BadElementException | IOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

}
