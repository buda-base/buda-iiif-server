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
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;

import ch.qos.logback.classic.Logger;
import de.digitalcollections.iiif.myhymir.EHServerCache;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.metrics.ImageMetrics;
import io.bdrc.iiif.resolver.BdrcS3Resolver;

@SuppressWarnings("rawtypes")
public class ArchiveImageProducer implements Callable {

    final static String S3_BUCKET = "archive.tbrc.org";
    public static final String IIIF_IMG = "IIIF_IMG";
    public final static Logger log = (Logger) LoggerFactory.getLogger("default");

    AmazonS3 s3;
    String identifier;
    String id;
    String archiveType;
    String origin;
    Dimension d;
    boolean isTiff = false;

    public ArchiveImageProducer(AmazonS3 s3, String id, String archiveType, String origin) throws IIIFException {
        this.s3 = s3;
        this.id = id;
        this.archiveType = archiveType;
        this.origin = origin;
        BdrcS3Resolver resolver = new BdrcS3Resolver();
        try {
            this.identifier = resolver.getS3Identifier(id);
            log.info("IDENTIFIER " + identifier + "ID " + id);
            if (identifier.endsWith(".tif") || identifier.endsWith(".tiff")) {
                isTiff = true;
            }
        } catch (ResourceNotFoundException | IOException e) {
            log.error("Could not instantiate Archive Image producer for id:" + id, e.getMessage());
            throw new IIIFException(500, IIIFException.GENERIC_APP_ERROR_CODE, e);
        }
    }

    public Image getPdfImage() throws BadElementException, MalformedURLException, IOException {
        byte[] imgbytes = (byte[]) EHServerCache.IIIF_IMG.get(id);
        if (imgbytes != null) {
            log.info("Got " + id + " from cache ...");
            return Image.getInstance(imgbytes);
        }
        GetObjectRequest request = new GetObjectRequest(S3_BUCKET, identifier);
        imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
        log.info("Got " + id + " from S3 ...added to cache");
        EHServerCache.IIIF_IMG.put(id, imgbytes);
        return Image.getInstance(imgbytes);
    }

    public BufferedImage getBufferedPdfImage() throws IOException, IIIFException {
        BufferedImage bImg = null;
        try {
            byte[] imgbytes = (byte[]) EHServerCache.IIIF_IMG.get(id);
            if (imgbytes != null) {
                InputStream in = new ByteArrayInputStream(imgbytes);
                bImg = ImageIO.read(in);
                log.info("Got " + id + " from cache ...");
                in.close();
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_PDF, origin);
                return bImg;
            }
            GetObjectRequest request = new GetObjectRequest(S3_BUCKET, identifier);
            imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
            InputStream in = new ByteArrayInputStream(imgbytes);
            bImg = ImageIO.read(in);
            log.info("Got " + id + " from S3 ...added to cache");
            EHServerCache.IIIF_IMG.put(id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_PDF, origin);
        } catch (IOException | IIIFException e) {
            log.error("Could not get Buffered Pdf Image for id=" + id, e.getMessage());
            throw e;
        }
        return bImg;
    }

    public static Image getMissingImage(String text) throws BadElementException, IOException {
        BufferedImage bufferedImage = new BufferedImage(800, 200, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, 800, 200);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
        graphics.drawString(text, 300, 100);
        return Image.getInstance(bufferedImage, null);
    }

    public byte[] getImageAsBytes() throws MalformedURLException, IOException, IIIFException {
        byte[] imgbytes = null;
        try {
            imgbytes = (byte[]) EHServerCache.IIIF_IMG.get(id);
            if (imgbytes != null) {
                InputStream in = new ByteArrayInputStream(imgbytes);
                BufferedImage bImg = ImageIO.read(in);
                this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
                log.debug("Zip Got " + id + " from cache ...");
                ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ZIP, origin);
                return imgbytes;
            }
            GetObjectRequest request = new GetObjectRequest(S3_BUCKET, identifier);
            imgbytes = IOUtils.toByteArray(s3.getObject(request).getObjectContent());
            InputStream in = new ByteArrayInputStream(imgbytes);
            BufferedImage bImg = ImageIO.read(in);
            this.d = new Dimension(bImg.getWidth(), bImg.getHeight());
            log.debug("Zip Got " + id + " from S3 ...added to cache");
            EHServerCache.IIIF_IMG.put(id, imgbytes);
            ImageMetrics.imageCount(ImageMetrics.IMG_CALLS_ZIP, origin);
        } catch (IOException | IIIFException e) {
            log.error("Could not get Image as bytes for id=" + id, e.getMessage());
            throw e;
        }
        return imgbytes;
    }

    public String getIdentifier() {
        return identifier;
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
    public Object call() throws IIIFException, BadElementException {
        try {
            if (archiveType.equals(ArchiveBuilder.PDF_TYPE)) {
                return getPdfImage();
                // return getBufferedPdfImage();
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
