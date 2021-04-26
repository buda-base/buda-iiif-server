package io.bdrc.iiif.archives;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.image.service.ImageProviderService;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.IdentifierInfo;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "iiif_img";
    public final static Logger log = LoggerFactory.getLogger(ArchiveImageProducer.class);
    
    private static boolean cacheImages = false;

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

    public Object[] getImageInputStream() throws IIIFException {
        return getImageInputStream(this.id, this.imageName, this.origin, this.isTiff);
    }
    
    static public Object[] getImageInputStream(IdentifierInfo inf, String imgId, String origin) throws IIIFException {
        String id = ImageProviderService.getKeyPrefix(inf) + imgId;
        String imageName = id.substring(id.lastIndexOf("/") + 1);
        return getImageInputStream(id, imageName, origin, false);
    }
    
    static public Object[] getImageInputStream(String id, String imageName, String origin, boolean isTiff) throws IIIFException {
        Object[] obj = new Object[2];
        obj[1] = imageName;
        InputStream imgis = null;
        final String s3key;
        final ImageProviderService service;
        if (id.startsWith("static::")) {
            s3key = id.substring(8);
            service = ImageProviderService.InstanceStatic;
        } else {
            IdentifierInfo idi = new IdentifierInfo(id);
            s3key = ImageProviderService.getKey(idi);
            service = ImageProviderService.InstanceArchive;
        }
        try {
            if (cacheImages) {
                try {
                    service.ensureCacheReady(s3key).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IIIFException(404, 5000, e);
                }
                imgis = service.getFromCache(s3key);
            } else {
                if (service.isInCache(s3key)) {
                    imgis = service.getFromCache(s3key);
                } else {
                    imgis = service.getNoCache(s3key);
                }
            }
            obj[0] = imgis;
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
        return getImageInputStream();
    }

}
