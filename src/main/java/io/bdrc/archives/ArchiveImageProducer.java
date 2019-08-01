package io.bdrc.archives;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.resolver.BdrcS3Resolver;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "IIIF_IMG";
    public final static Logger log = LoggerFactory.getLogger(ArchiveImageProducer.class.getName());

    AmazonS3 s3;
    String identifier;
    String id;
    String archiveType;
    Dimension d;
    boolean isTiff = false;

    public ArchiveImageProducer(AmazonS3 s3, String id, String archiveType) throws IIIFException {
        this.s3 = s3;
        this.id = id;
        this.archiveType = archiveType;
        BdrcS3Resolver resolver = new BdrcS3Resolver();
        try {
            this.identifier = resolver.getS3Identifier(id);
            System.out.println("IDENTIFIER " + identifier + "ID " + id);
            if (identifier.endsWith(".tif") || identifier.endsWith(".tiff")) {
                isTiff = true;
            }
        } catch (ResourceIOException e) {
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    public BufferedImage getBufferedPdfImage() throws MalformedURLException, IOException, IIIFException {
        byte[] imgbytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, id);
        if (imgbytes != null) {
            InputStream in = new ByteArrayInputStream(imgbytes);
            BufferedImage bImg = ImageIO.read(in);
            log.debug("Got " + id + " from cache ...");
            in.close();
            return bImg;
        }
        GetObjectRequest request = new GetObjectRequest(S3_BUCKET, identifier);
        imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        InputStream in = new ByteArrayInputStream(imgbytes);
        BufferedImage bImg = ImageIO.read(in);
        log.debug("Got " + id + " from S3 ...added to cache");
        ServerCache.addToCache(IIIF_IMG, id, imgbytes);
        return bImg;
    }

    public byte[] getImageAsBytes() throws MalformedURLException, IOException, IIIFException {
        byte[] imgbytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, id);
        if (imgbytes != null) {
            InputStream in = new ByteArrayInputStream(imgbytes);
            BufferedImage bImg = ImageIO.read(in);
            this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
            log.debug("Zip Got " + id + " from cache ...");
            return imgbytes;
        }
        GetObjectRequest request = new GetObjectRequest(S3_BUCKET, identifier);
        imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        InputStream in = new ByteArrayInputStream(imgbytes);
        BufferedImage bImg = ImageIO.read(in);
        this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
        log.debug("Zip Got " + id + " from S3 ...added to cache");
        ServerCache.addToCache(IIIF_IMG, id, imgbytes);
        return imgbytes;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static BufferedImage getBufferedMissingImage(String text) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(800, 200, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, 800, 200);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
        graphics.drawString(text, 300, 100);
        return bufferedImage;
    }

    @Override
    public Object call() throws IIIFException {
        try {
            if (archiveType.equals(ArchiveBuilder.PDF_TYPE)) {
                return getBufferedPdfImage();
            }
            if (archiveType.equals(ArchiveBuilder.ZIP_TYPE)) {
                return getImageAsBytes();
            }
            return null;
        } catch (IOException e) {
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

}
