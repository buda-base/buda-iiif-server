package io.bdrc.iiif.archives;

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

import io.bdrc.iiif.core.EHServerCache;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageProviderService;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.IdentifierInfo;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "iiif_img";
    public final static Logger log = LoggerFactory.getLogger(ArchiveImageProducer.class);

    AmazonS3 s3;
    String id;
    String imageName;
    String origin;
    Dimension d;
    boolean isTiff = false;

    public ArchiveImageProducer(IdentifierInfo inf, String imgId, String origin) throws IIIFException {
        this.id = ImageProviderService.getKeyPrefix(inf) + imgId;
        this.imageName = id.substring(id.lastIndexOf("/") + 1);
        this.origin = origin;
        if (origin == null) {
            origin = "";
        }
        if (id.endsWith(".tif") || id.endsWith(".tiff")) {
            isTiff = true;
        }
    }

    public Object[] getImageAsBytes() throws MalformedURLException, IOException, IIIFException {
        Object[] obj = new Object[2];
        obj[1] = imageName;
        byte[] imgbytes = null;
        try {
            imgbytes = (byte[]) EHServerCache.get(IIIF_IMG, id);
            if (imgbytes != null) {
                log.debug("Got {} from cache ...", id);
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_COMMON, origin);
                obj[0] = imgbytes;
                return obj;
            }
            imgbytes = ImageProviderService.InstanceArchive.getFromApi(id);
            obj[0] = imgbytes;
            log.debug("Got {} from S3 ...added to cache", id);
            EHServerCache.put(IIIF_IMG, id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ARCHIVES, origin);
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
