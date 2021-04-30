package io.bdrc.iiif.archives;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageProviderService;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ArchiveImageProducer {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "iiif_img";
    public final static Logger log = LoggerFactory.getLogger(ArchiveImageProducer.class);
    
    private static boolean cacheImages = false;
    
    static public Object[] getImageInputStream(IdentifierInfo volumeInf, String imgId, String origin) throws IIIFException {
        String s3key = ImageProviderService.getKeyPrefix(volumeInf) + imgId;
        return getImageInputStream(s3key, imgId, origin, false);
    }
    
    static public Object[] getImageInputStream(String s3key, String imageName, String origin, boolean isTiff) throws IIIFException {
        Object[] obj = new Object[2];
        obj[1] = imageName;
        InputStream imgis = null;
        final ImageProviderService service = ImageProviderService.InstanceArchive;
        if (cacheImages) {
            try {
                log.info("ensuring cache is ready for {}", s3key);
                service.ensureCacheReady(s3key).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IIIFException(404, 5000, e);
            }
            log.info("get from cache {}", s3key);
            imgis = service.getFromCache(s3key);
        } else {
            if (service.isInCache(s3key)) {
                log.info("get from cache {}", s3key);
                imgis = service.getFromCache(s3key);
            } else {
                log.info("get image without cache {}", s3key);
                imgis = service.getNoCache(s3key);
            }
        }
        obj[0] = imgis;
        ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ARCHIVES, origin);
        return obj;
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

}
