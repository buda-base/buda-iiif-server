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

import de.digitalcollections.iiif.myhymir.ServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageS3Service;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "IIIF_IMG";
    public final static Logger log = LoggerFactory.getLogger("default");

    AmazonS3 s3;
    String key;
    String id;
    String archiveType;
    String origin;
    Dimension d;
    boolean isTiff = false;

    public ArchiveImageProducer(AmazonS3 s3, String key, String archiveType, String origin) throws IIIFException {
        this.s3 = s3;
        this.archiveType = archiveType;
        this.origin = origin;
        log.info("IDENTIFIER {}, ID {}", key, id);
        if (key.endsWith(".tif") || key.endsWith(".tiff")) {
            isTiff = true;
        }
    }

    public BufferedImage getBufferedPdfImage() throws IOException, IIIFException {
        BufferedImage bImg = null;
        try {
            byte[] imgbytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, id);
            if (imgbytes != null) {
                InputStream in = new ByteArrayInputStream(imgbytes);
                bImg = ImageIO.read(in);
                log.debug("Got {} from cache ...", id);
                in.close();
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_PDF, origin);
                return bImg;
            }
            GetObjectRequest request = new GetObjectRequest(S3_BUCKET, key);
            imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
            InputStream in = new ByteArrayInputStream(imgbytes);
            bImg = ImageIO.read(in);
            log.debug("Got {} from S3 ...added to cache", id);
            ServerCache.addToCache(IIIF_IMG, id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_PDF, origin);
        } catch (IOException | IIIFException e) {
            log.error("Could not get Buffered Pdf Image for id=" + id, e.getMessage());
            throw e;
        }
        return bImg;
    }

    public byte[] getImageAsBytes() throws MalformedURLException, IOException, IIIFException {
        byte[] imgbytes = null;
        try {
            imgbytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, id);
            if (imgbytes != null) {
                InputStream in = new ByteArrayInputStream(imgbytes);
                BufferedImage bImg = ImageIO.read(in);
                this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
                log.debug("Zip Got {} from cache ...", id);
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ZIP, origin);
                return imgbytes;
            }
            GetObjectRequest request = new GetObjectRequest(S3_BUCKET, key);
            imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
            InputStream in = new ByteArrayInputStream(imgbytes);
            BufferedImage bImg = ImageIO.read(in);
            this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
            log.debug("Zip Got {} from S3 ...added to cache", id);
            ServerCache.addToCache(IIIF_IMG, id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ZIP, origin);
        } catch (IOException | IIIFException e) {
            log.error("Could not get Image as bytes for id=" + id, e.getMessage());
            throw e;
        }
        return imgbytes;
    }

    public String getIdentifier() {
        return key;
    }

    public static BufferedImage getBufferedMissingImage(String text) {
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
