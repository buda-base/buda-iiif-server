package io.bdrc.archives;

import static io.bdrc.pdf.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.resolver.BdrcS3Resolver;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable{

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG="IIIF_IMG";
    public final static Logger log=LoggerFactory.getLogger(ArchiveImageProducer.class.getName());

    AmazonS3 s3;
    String identifier;
    String id;
    Image img;
    String archiveType;

    public ArchiveImageProducer(AmazonS3 s3, String id, String archiveType) throws BDRCAPIException {
        this.s3=s3;
        this.id=id;
        this.archiveType=archiveType;
        BdrcS3Resolver resolver=new BdrcS3Resolver();
        try {
            this.identifier=resolver.getS3Identifier(id);
        } catch (ResourceIOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

    public Image getPdfImage() throws BadElementException, MalformedURLException, IOException, BDRCAPIException {
        byte[] imgbytes=(byte[])ServerCache.getObjectFromCache(IIIF_IMG,id);
        if(imgbytes !=null) {
            log.debug("Got "+id +" from cache ...");
            return Image.getInstance(imgbytes);
        }
        GetObjectRequest request = new GetObjectRequest(
                S3_BUCKET,
                identifier);
        imgbytes=IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        img=Image.getInstance(imgbytes);
        log.debug("Got "+id +" from S3 ...added to cache");
        ServerCache.addToCache(IIIF_IMG,id, imgbytes);
        return img;
    }

    public byte[] getImageAsBytes() throws BadElementException, MalformedURLException, IOException, BDRCAPIException {
        byte[] imgbytes=(byte[])ServerCache.getObjectFromCache(IIIF_IMG,id);
        if(imgbytes !=null) {
            log.debug("Zip Got "+id +" from cache ...");
            return imgbytes;
        }
        GetObjectRequest request = new GetObjectRequest(
                S3_BUCKET,
                identifier);
        imgbytes=IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        log.debug("Zip Got "+id +" from S3 ...added to cache");
        ServerCache.addToCache(IIIF_IMG,id, imgbytes);
        return imgbytes;
    }

    public Image getImg() {
        return img;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static Image getMissingImage(String text) throws BadElementException, IOException {
        BufferedImage bufferedImage = new BufferedImage(800, 200,BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, 800, 200);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
        graphics.drawString(text, 300, 100);
        return Image.getInstance(bufferedImage,null);
    }

    @Override
    public Object call() throws BDRCAPIException {
        try {
            if(archiveType.equals(ArchiveBuilder.PDF_TYPE)) {
                return getPdfImage();
            }
            if(archiveType.equals(ArchiveBuilder.ZIP_TYPE)) {
                return getImageAsBytes();
            }
            return null;
        } catch (BadElementException | IOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, e);
        }
    }

}
