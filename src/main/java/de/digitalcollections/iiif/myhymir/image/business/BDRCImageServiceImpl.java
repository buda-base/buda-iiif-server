package de.digitalcollections.iiif.myhymir.image.business;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.PNGEncodeParam;

import de.digitalcollections.core.business.api.ResourceService;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.core.model.impl.resource.S3Resource;
import de.digitalcollections.iiif.hymir.image.business.api.ImageSecurityService;
import de.digitalcollections.iiif.hymir.image.business.api.ImageService;
import de.digitalcollections.iiif.hymir.model.exception.InvalidParametersException;
import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageApiProfile.Format;
import de.digitalcollections.iiif.model.image.ImageApiProfile.Quality;
import de.digitalcollections.iiif.model.image.ImageApiSelector;
import de.digitalcollections.iiif.model.image.RegionRequest;
import de.digitalcollections.iiif.model.image.ResolvingException;
import de.digitalcollections.iiif.model.image.Size;
import de.digitalcollections.iiif.model.image.SizeRequest;
import de.digitalcollections.iiif.model.image.TileInfo;
import de.digitalcollections.iiif.myhymir.Application;
import de.digitalcollections.iiif.myhymir.ServerCache;
import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReadParam;
import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader;
import io.bdrc.pdf.presentation.exceptions.BDRCAPIException;

@Service
@Primary
public class BDRCImageServiceImpl implements ImageService {

    public static final String IIIF_IMG = "IIIF_IMG";

    @Autowired(required = false)
    private ImageSecurityService imageSecurityService;

    @Autowired
    private ResourceService resourceService;

    @Value("${custom.iiif.image.maxWidth:65500}")
    private int maxWidth;

    @Value("${custom.iiif.image.maxHeight:65500}")
    private int maxHeight;

    private class DecodedImage {

        /** Decoded image **/
        final BufferedImage img;

        /** Final target size for scaling **/
        final Dimension targetSize;

        /** Rotation needed after decoding? **/
        final int rotation;

        // Small value type to hold information about decoding results
        protected DecodedImage(BufferedImage img, Dimension targetSize, int rotation) {
            this.img = img;
            this.targetSize = targetSize;
            this.rotation = rotation;
        }
    }

    /** Update ImageService based on the image **/
    private void enrichInfo(ImageReader reader, de.digitalcollections.iiif.model.image.ImageService info)
            throws IOException {

        ImageApiProfile profile = new ImageApiProfile();
        profile.addFeature(ImageApiProfile.Feature.BASE_URI_REDIRECT, ImageApiProfile.Feature.CORS,
                ImageApiProfile.Feature.JSONLD_MEDIA_TYPE, ImageApiProfile.Feature.PROFILE_LINK_HEADER,
                ImageApiProfile.Feature.CANONICAL_LINK_HEADER, ImageApiProfile.Feature.REGION_BY_PCT,
                ImageApiProfile.Feature.REGION_BY_PX, ImageApiProfile.Feature.REGION_SQUARE,
                ImageApiProfile.Feature.ROTATION_BY_90S, ImageApiProfile.Feature.MIRRORING,
                ImageApiProfile.Feature.SIZE_BY_CONFINED_WH, ImageApiProfile.Feature.SIZE_BY_DISTORTED_WH,
                ImageApiProfile.Feature.SIZE_BY_H, ImageApiProfile.Feature.SIZE_BY_PCT,
                ImageApiProfile.Feature.SIZE_BY_W, ImageApiProfile.Feature.SIZE_BY_WH);

        // Indicate to the client if we cannot deliver full resolution versions of the
        // image
        if (reader.getHeight(0) > maxHeight || reader.getWidth(0) > maxWidth) {
            profile.setMaxWidth(maxWidth);
            if (maxHeight != maxWidth) {
                profile.setMaxHeight(maxHeight);
            }
        }
        info.addProfile(ImageApiProfile.LEVEL_ONE, profile);

        info.setWidth(reader.getWidth(0));
        info.setHeight(reader.getHeight(0));

        // Check if multiple resolutions are supported
        int numImages = reader.getNumImages(true);
        if (numImages > 1) {
            for (int i = 0; i < numImages; i++) {
                int width = reader.getWidth(i);
                int height = reader.getHeight(i);
                if (width > 1 && height > 1 && width <= maxWidth && height <= maxHeight) {
                    info.addSize(new Size(reader.getWidth(i), reader.getHeight(i)));
                }
            }
        }
        // Check if tiling is supported
        if (reader.isImageTiled(0)) {
            int width = reader.getTileWidth(0);
            TileInfo tileInfo = new TileInfo(width);
            for (int i = 0; i < numImages; i++) {
                int scaledWidth = reader.getTileWidth(i);
                tileInfo.addScaleFactor(width / scaledWidth);
            }
            info.addTile(tileInfo);
        } else if (reader instanceof TurboJpegImageReader) {
            // Cropping aligned to MCUs is faster, and MCUs are either 4, 8 or 16 pixels, so
            // if we stick to multiples
            // of 16 for width/height, we are safe.
            if (reader.getWidth(0) >= 512 && reader.getHeight(0) >= 512) {
                TileInfo ti = new TileInfo(512);
                // Scale factors for JPEGs are not always integral, so we hardcode them
                ti.addScaleFactor(1, 2, 4, 8, 16);
                info.addTile(ti);
            }
            if (reader.getWidth(0) >= 1024 && reader.getHeight(0) >= 1024) {
                TileInfo ti = new TileInfo(1024);
                ti.addScaleFactor(1, 2, 4, 8, 16);
                info.addTile(ti);
            }
        }
        // BDRC default behavior : this way the Universal Viewer gets the image in one
        // request only
        // which is BDRC basic use case
        TileInfo tile = new TileInfo(reader.getWidth(0));
        tile.setHeight(reader.getHeight(0));
        tile.addScaleFactor(1, 2, 4, 8);
        info.addTile(tile);
    }

    /**
     * Try to obtain a {@link ImageReader} for a given identifier
     * 
     * @throws BDRCAPIException
     **/
    private ImageReader getReader(String identifier)
            throws ResourceNotFoundException, UnsupportedFormatException, IOException {
        long deb = System.currentTimeMillis();
        byte[] bytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, identifier);
        if (bytes != null) {
            Application.perf.debug("Image service image was cached {}", identifier);
        } else {
            Application.perf.debug("Image service reading {}", identifier);
            if (imageSecurityService != null && !imageSecurityService.isAccessAllowed(identifier)) {
                throw new ResourceNotFoundException();
            }
            Resource res;
            try {
                res = resourceService.get(identifier, ResourcePersistenceType.RESOLVED, MimeType.MIME_IMAGE);
            } catch (ResourceIOException e) {
                throw new ResourceNotFoundException();
            }
            InputStream S3input = resourceService.getInputStream((S3Resource) res);
            try {
                ServerCache.addToCache(IIIF_IMG, identifier, IOUtils.toByteArray(S3input));
            } catch (BDRCAPIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            bytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, identifier);
            Application.perf.debug("Image service read {} from s3 {}", S3input, identifier);
        }
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        ImageReader reader = Streams.stream(ImageIO.getImageReaders(iis)).findFirst()
                .orElseThrow(UnsupportedFormatException::new);
        reader.setInput(iis);
        Application.perf.debug("S3 object IIIS READER >> {}", reader);
        Application.perf
                .debug("Image service return reader at " + (System.currentTimeMillis() - deb) + " ms " + identifier);
        return reader;
    }

    @Override
    public void readImageInfo(String identifier, de.digitalcollections.iiif.model.image.ImageService info)
            throws UnsupportedFormatException, UnsupportedOperationException, ResourceNotFoundException, IOException {
        enrichInfo(getReader(identifier), info);
    }

    public ImageReader readImageInfo(String identifier, de.digitalcollections.iiif.model.image.ImageService info,
            ImageReader imgReader)
            throws UnsupportedFormatException, UnsupportedOperationException, ResourceNotFoundException, IOException {
        imgReader = getReader(identifier);
        enrichInfo(imgReader, info);
        return imgReader;
    }

    /**
     * Determine parameters for image reading based on the IIIF selector and a given
     * scaling factor
     **/
    private ImageReadParam getReadParam(ImageReader reader, ImageApiSelector selector, double decodeScaleFactor)
            throws IOException, InvalidParametersException {
        ImageReadParam readParam = reader.getDefaultReadParam();
        Application.perf.debug("Entering ReadParam with ImageReadParam {}", readParam);
        Dimension nativeDimensions = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (ResolvingException e) {
            throw new InvalidParametersException(e);
        }
        // IIIF regions are always relative to the native size, while ImageIO regions
        // are always relative to the decoded
        // image size, hence the conversion
        Rectangle decodeRegion = new Rectangle((int) Math.ceil(targetRegion.getX() * decodeScaleFactor),
                (int) Math.ceil(targetRegion.getY() * decodeScaleFactor),
                (int) Math.ceil(targetRegion.getWidth() * decodeScaleFactor),
                (int) Math.ceil(targetRegion.getHeight() * decodeScaleFactor));
        readParam.setSourceRegion(decodeRegion);
        // TurboJpegImageReader can rotate during decoding
        if (selector.getRotation().getRotation() != 0 && reader instanceof TurboJpegImageReader) {
            ((TurboJpegImageReadParam) readParam).setRotationDegree((int) selector.getRotation().getRotation());
        }
        return readParam;
    }

    private DecodedImage readImage(String identifier, ImageApiSelector selector, ImageApiProfile profile,
            ImageReader reader)
            throws IOException, ResourceNotFoundException, UnsupportedFormatException, InvalidParametersException {
        long deb = System.currentTimeMillis();
        Application.perf.debug("Entering readImage for creating DecodedImage");
        if ((selector.getRotation().getRotation() % 90) != 0) {
            throw new UnsupportedOperationException("Can only rotate by multiples of 90 degrees.");
        }
        Dimension nativeDimensions = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (ResolvingException e) {
            throw new InvalidParametersException(e);
        }
        Dimension croppedDimensions = new Dimension(targetRegion.width, targetRegion.height);
        Dimension targetSize;
        try {
            targetSize = selector.getSize().resolve(croppedDimensions, profile);
        } catch (ResolvingException e) {
            throw new InvalidParametersException(e);
        }

        // Determine the closest resolution to the target that can be decoded directly
        double targetScaleFactor = targetSize.width / targetRegion.getWidth();
        double decodeScaleFactor = 1.0;
        int imageIndex = 0;
        for (int idx = 0; idx < reader.getNumImages(true); idx++) {
            double factor = (double) reader.getWidth(idx) / nativeDimensions.width;
            if (factor < targetScaleFactor) {
                continue;
            }
            if (Math.abs(targetScaleFactor - factor) < Math.abs(targetScaleFactor - decodeScaleFactor)) {
                decodeScaleFactor = factor;
                imageIndex = idx;
            }
        }
        ImageReadParam readParam = getReadParam(reader, selector, decodeScaleFactor);
        int rotation = (int) selector.getRotation().getRotation();
        if (readParam instanceof TurboJpegImageReadParam
                && ((TurboJpegImageReadParam) readParam).getRotationDegree() != 0) {
            if (rotation == 90 || rotation == 270) {
                int w = targetSize.width;
                targetSize.width = targetSize.height;
                targetSize.height = w;
            }
            rotation = 0;
        }
        Application.perf.debug("Done readingImage computing DecodedImage after {} ms",
                System.currentTimeMillis() - deb);
        return new DecodedImage(reader.read(imageIndex, readParam), targetSize, rotation);
    }

    /** Apply transformations to an decoded image **/
    private BufferedImage transformImage(BufferedImage inputImage, Dimension targetSize, int rotation, boolean mirror,
            ImageApiProfile.Quality quality) {
        BufferedImage img = inputImage;
        int inType = img.getType();
        boolean needsAdditionalScaling = !new Dimension(img.getWidth(), img.getHeight()).equals(targetSize);
        if (needsAdditionalScaling) {
            img = Scalr.resize(img, Scalr.Method.BALANCED, Scalr.Mode.FIT_EXACT, targetSize.width, targetSize.height);
        }

        if (rotation != 0) {
            Scalr.Rotation rot = null;
            switch (rotation) {
            case 90:
                rot = Scalr.Rotation.CW_90;
                break;
            case 180:
                rot = Scalr.Rotation.CW_180;
                break;
            case 270:
                rot = Scalr.Rotation.CW_270;
                break;
            }
            img = Scalr.rotate(img, rot);
        }
        if (mirror) {
            img = Scalr.rotate(img, Scalr.Rotation.FLIP_HORZ);
        }
        // Quality
        int outType;
        switch (quality) {
        case GRAY:
            outType = BufferedImage.TYPE_BYTE_GRAY;
            break;
        case BITONAL:
            outType = BufferedImage.TYPE_BYTE_BINARY;
            break;
        case COLOR:
            outType = BufferedImage.TYPE_3BYTE_BGR;
            break;
        default:
            outType = inType;
        }
        if (outType != img.getType()) {
            BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), outType);
            Graphics2D g2d = newImg.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            img = newImg;
            g2d.dispose();
        }
        return img;
    }

    @Override
    public void processImage(String identifier, ImageApiSelector selector, ImageApiProfile profile, OutputStream os)
            throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException,
            ResourceNotFoundException, IOException {
        // unused
    }

    public static boolean formatDiffer(final String identifier, final ImageApiSelector selector) {
        final Format outputF = selector.getFormat();
        final String lastFour = identifier.substring(identifier.length() - 4).toLowerCase();
        if (outputF == Format.JPG && (lastFour.equals(".jpg") || lastFour.equals("jpeg")))
            return false;
        if (outputF == Format.PNG && lastFour.equals(".png"))
            return false;
        if (outputF == Format.TIF && (lastFour.equals(".tif")) || (lastFour.equals("tiff")))
            return false;
        return true;
    }

    private static final RegionRequest fullRegionRequest = new RegionRequest();
    private static final SizeRequest fullSizeRequest = new SizeRequest();
    private static final SizeRequest maxSizeRequest = new SizeRequest(true);

    // here we return a boolean telling us if the requested image is different from
    // the original image
    // on S3
    public static boolean requestDiffersFromOriginal(final String identifier, final ImageApiSelector selector) {
        if (formatDiffer(identifier, selector))
            return true;
        if (selector.getQuality() != Quality.DEFAULT) // TODO: this could be improved but we can keep that for later
            return true;
        if (selector.getRotation().getRotation() != 0.)
            return true;
        if (!selector.getRegion().equals(fullRegionRequest)) // TODO: same here, could be improved by reading the
                                                             // dimensions of the image
            return true;
        if (!selector.getSize().equals(fullSizeRequest) && !selector.getSize().equals(maxSizeRequest))
            return true;
        return false;
    }

    public void processImage(String identifier, ImageApiSelector selector, ImageApiProfile profile, OutputStream os,
            ImageReader imgReader, String uri) throws InvalidParametersException, UnsupportedOperationException,
            UnsupportedFormatException, ResourceNotFoundException, IOException {
        long deb = System.currentTimeMillis();
        Application.perf.debug("Entering Processimage.... with reader {} ", imgReader);
        DecodedImage img = readImage(identifier, selector, profile, imgReader);
        Application.perf.debug("Done readingImage DecodedImage created");
        BufferedImage outImg = transformImage(img.img, img.targetSize, img.rotation, selector.getRotation().isMirror(),
                selector.getQuality());
        /** Debugging code ***/
        Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType("image/png");
        while (it.hasNext()) {
            Application.perf.debug("WRITER in list {}", it.next());
        }
        /** end debugging code **/
        ImageWriter writer = null;
        Iterator<ImageWriter> writers = ImageIO
                .getImageWritersByMIMEType(selector.getFormat().getMimeType().getTypeName());
        while (writers.hasNext()) {
            ImageWriter w = writers.next();
            // if (writer == null) {
            // picks the first non null ImageWriter (they might be registered and null)
            writer = w;
            Application.perf.debug("FOUND REGISTERED WRITER in list {}", writer);
            // }

        }
        if (writer == null) {
            throw new UnsupportedFormatException(selector.getFormat().getMimeType().getTypeName());
        }
        switch (selector.getFormat()) {
        case PNG:
            Application.perf.debug("USING JAI PNG for {} ", identifier);
            ImageEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(outImg);
            String format = "PNG";
            ImageEncoder encoder = ImageCodec.createImageEncoder(format, os, param);
            encoder.encode(outImg);
            os.flush();
            break;

        case JPG:
            Application.perf.debug("USING JAI JPG for {} ", identifier);
            JPEGEncodeParam jpgparam = new JPEGEncodeParam();
            jpgparam.setQuality(0.7F);
            ImageEncoder jpgencoder = ImageCodec.createImageEncoder("JPEG", os, jpgparam);
            jpgencoder.encode(outImg);
            os.flush();
            break;

        default:
            Application.perf.debug("USING NON NULL WRITER {}", writer);
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);
            writer.write(outImg);
            writer.dispose();
            ios.flush();
        }
        Application.perf.debug("Done with Processimage.... in {} ms", System.currentTimeMillis() - deb);
    }

    @Override
    public Instant getImageModificationDate(String identifier) throws ResourceNotFoundException {
        if (imageSecurityService != null && !imageSecurityService.isAccessAllowed(identifier)) {
            throw new ResourceNotFoundException();
        }
        try {
            Resource res = resourceService.get(identifier, ResourcePersistenceType.RESOLVED, MimeType.MIME_IMAGE);
            return Instant.ofEpochMilli(res.getLastModified());
        } catch (ResourceIOException e) {
            throw new ResourceNotFoundException();
        }
    }
}