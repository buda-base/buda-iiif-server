package io.bdrc.archives;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;

import de.digitalcollections.iiif.myhymir.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageS3Service;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "IIIF_IMG";
    public final static Logger log = LoggerFactory.getLogger(ArchiveImageProducer.class);

    AmazonS3 s3;
    String id;
    String imageName;
    String origin;
    Dimension d;
    boolean isTiff = false;

    public ArchiveImageProducer(AmazonS3 s3, IdentifierInfo inf, String imgId, String origin) throws IIIFException {
        this.s3 = s3;
        this.id = ImageS3Service.getKeyPrefix(inf) + imgId;
        this.imageName = id.substring(id.lastIndexOf(":") + 1);
        this.origin = origin;
        log.info("IDENTIFIER : {} ", id);
        if (id.endsWith(".tif") || id.endsWith(".tiff")) {
            isTiff = true;
        }
    }

    public Object[] getImageAsBytes() throws MalformedURLException, IOException, IIIFException {
        Object[] obj = new Object[2];
        obj[1] = imageName;
        byte[] imgbytes = null;
        try {
            imgbytes = (byte[]) EHServerCache.IIIF_IMG.get(id);
            if (imgbytes != null) {
                log.debug("Got " + id + " from cache ...");
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, origin);
                obj[0] = imgbytes;
                return obj;
            }
            imgbytes = ImageS3Service.InstanceArchive.getFromApi(id);
            // GetObjectRequest request = new GetObjectRequest(S3_BUCKET, id);
            // imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
            obj[0] = imgbytes;
            log.debug("Got " + id + " from S3 ...added to cache");
            EHServerCache.IIIF_IMG.put(id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, origin);
        } catch (IIIFException e) {
            log.error("Could not get Image as bytes for id=" + id, e.getMessage());
            throw e;
        }
        return obj;
    }

    public String getIdentifier() {
        return id;
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
            return getImageAsBytes();
        } catch (IOException e) {
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

}
