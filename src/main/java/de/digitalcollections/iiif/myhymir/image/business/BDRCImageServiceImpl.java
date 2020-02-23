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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.logging.Log;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.luciad.imageio.webp.WebPWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
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
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReadParam;
import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader;
import io.bdrc.iiif.exceptions.IIIFException;

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
    private void enrichInfo(ImageReader reader, de.digitalcollections.iiif.model.image.ImageService info) throws IOException {

        ImageApiProfile profile = new ImageApiProfile();
        profile.addFeature(ImageApiProfile.Feature.BASE_URI_REDIRECT, ImageApiProfile.Feature.CORS, ImageApiProfile.Feature.JSONLD_MEDIA_TYPE,
                ImageApiProfile.Feature.PROFILE_LINK_HEADER, ImageApiProfile.Feature.CANONICAL_LINK_HEADER, ImageApiProfile.Feature.REGION_BY_PCT,
                ImageApiProfile.Feature.REGION_BY_PX, ImageApiProfile.Feature.REGION_SQUARE, ImageApiProfile.Feature.ROTATION_BY_90S,
                ImageApiProfile.Feature.MIRRORING, ImageApiProfile.Feature.SIZE_BY_CONFINED_WH, ImageApiProfile.Feature.SIZE_BY_DISTORTED_WH,
                ImageApiProfile.Feature.SIZE_BY_H, ImageApiProfile.Feature.SIZE_BY_PCT, ImageApiProfile.Feature.SIZE_BY_W,
                ImageApiProfile.Feature.SIZE_BY_WH);

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
     * @throws IIIFException
     * @throws ResourceNotFoundException
     **/
    private ImageReader getReader(String identifier) throws UnsupportedFormatException, IOException, IIIFException, ResourceNotFoundException {
        long deb = System.currentTimeMillis();
        byte[] bytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, identifier);
        if (bytes != null) {
            Application.logPerf("Image service image was cached {}", identifier);
        } else {
            Application.logPerf("Image service reading {}", identifier);
            if (imageSecurityService != null && !imageSecurityService.isAccessAllowed(identifier)) {
                throw new IIIFException();
            }
            S3Resource res;
            try {
                res = (S3Resource) resourceService.get(identifier, ResourcePersistenceType.RESOLVED, MimeType.MIME_IMAGE);
                if (identifier.startsWith("static::")) {
                    res.setStatic(true);
                }
            } catch (ResourceIOException e) {
                throw new ResourceNotFoundException(e);
            }
            InputStream S3input = resourceService.getInputStream((S3Resource) res);
            if (S3input == null) {
                throw new ResourceNotFoundException("No S3 resource could be found for identifier: " + identifier);
            }
            try {
                ServerCache.addToCache(IIIF_IMG, identifier, IOUtils.toByteArray(S3input));
            } catch (IIIFException e) {
                Log.error("Could not add bytearray image to cache", e.getMessage());
            }
            bytes = (byte[]) ServerCache.getObjectFromCache(IIIF_IMG, identifier);
            Application.logPerf("Image service read {} from s3 {}", S3input, identifier);
        }

        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
        while (itr.hasNext()) {
            System.out.println("FOUND READER >> " + itr.next());
        }
        ImageReader reader = Streams.stream(ImageIO.getImageReaders(iis)).findFirst().orElseThrow(UnsupportedFormatException::new);
        // reader.setInput(iis, false, false);
        reader.setInput(iis);
        Application.logPerf("S3 object IIIS READER >> {}", reader);
        Application.logPerf("Image service return reader at " + (System.currentTimeMillis() - deb) + " ms " + identifier);
        return reader;
    }

    @Override
    public void readImageInfo(String identifier, de.digitalcollections.iiif.model.image.ImageService info)
            throws UnsupportedFormatException, UnsupportedOperationException, IOException {
        try {
            enrichInfo(getReader(identifier), info);
        } catch (IIIFException | ResourceNotFoundException e) {
            Log.error("Could not get Image Info", e.getMessage());
            throw new ResourceIOException(e);
        }
    }

    public ImageReader readImageInfo(String identifier, de.digitalcollections.iiif.model.image.ImageService info, ImageReader imgReader)
            throws UnsupportedFormatException, UnsupportedOperationException, IOException {
        try {
            imgReader = getReader(identifier);
            enrichInfo(imgReader, info);
        } catch (IIIFException | ResourceNotFoundException e) {
            Log.error("Could not get Image Info", e.getMessage());
            throw new ResourceIOException(e);
        }
        return imgReader;
    }

    /**
     * Determine parameters for image reading based on the IIIF selector and a given
     * scaling factor
     **/
    private ImageReadParam getReadParam(ImageReader reader, ImageApiSelector selector, double decodeScaleFactor)
            throws IOException, InvalidParametersException {
        ImageReadParam readParam = reader.getDefaultReadParam();
        Application.logPerf("Entering ReadParam with ImageReadParam {}", readParam);
        Dimension nativeDimensions = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (ResolvingException e) {
            Log.error("Could not get image ReadParam", e.getMessage());
            throw new InvalidParametersException(e);
        }
        // IIIF regions are always relative to the native size, while ImageIO regions
        // are always relative to the decoded
        // image size, hence the conversion
        Rectangle decodeRegion = new Rectangle((int) Math.ceil(targetRegion.getX() * decodeScaleFactor),
                (int) Math.ceil(targetRegion.getY() * decodeScaleFactor), (int) Math.ceil(targetRegion.getWidth() * decodeScaleFactor),
                (int) Math.ceil(targetRegion.getHeight() * decodeScaleFactor));
        readParam.setSourceRegion(decodeRegion);
        // TurboJpegImageReader can rotate during decoding
        if (selector.getRotation().getRotation() != 0 && reader instanceof TurboJpegImageReader) {
            ((TurboJpegImageReadParam) readParam).setRotationDegree((int) selector.getRotation().getRotation());
        }
        return readParam;
    }

    private DecodedImage readImage(String identifier, ImageApiSelector selector, ImageApiProfile profile, ImageReader reader)
            throws IOException, UnsupportedFormatException, InvalidParametersException {
        long deb = System.currentTimeMillis();
        Application.logPerf("Entering readImage for creating DecodedImage");
        if ((selector.getRotation().getRotation() % 90) != 0) {
            Log.error("Rotation is not a multiple of 90 degrees for selector " + selector.toString(), "");
            throw new UnsupportedOperationException("Can only rotate by multiples of 90 degrees.");
        }
        Dimension nativeDimensions = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (ResolvingException e) {
            Log.error("Could not resolve selector region :" + selector.getRegion(), e.getMessage());
            throw new InvalidParametersException(e);
        }
        Dimension croppedDimensions = new Dimension(targetRegion.width, targetRegion.height);
        Dimension targetSize;
        try {
            targetSize = selector.getSize().resolve(croppedDimensions, profile);
        } catch (ResolvingException e) {
            Log.error("Could not resolve selector size :" + selector.getSize(), e.getMessage());
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
        System.out.println("READER " + reader);
        // readParam.setDestinationType(reader.getImageTypes(imageIndex).next());
        int rotation = (int) selector.getRotation().getRotation();
        if (readParam instanceof TurboJpegImageReadParam && ((TurboJpegImageReadParam) readParam).getRotationDegree() != 0) {
            if (rotation == 90 || rotation == 270) {
                int w = targetSize.width;
                targetSize.width = targetSize.height;
                targetSize.height = w;
            }
            rotation = 0;
        }
        Application.logPerf("Done readingImage computing DecodedImage after {} ms", System.currentTimeMillis() - deb);
        return new DecodedImage(reader.read(imageIndex, readParam), targetSize, rotation);
    }

    /** Apply transformations to an decoded image **/
    private BufferedImage transformImage(Format format, BufferedImage inputImage, Dimension targetSize, int rotation, boolean mirror,
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
        case DEFAULT:
            outType = BufferedImage.TYPE_3BYTE_BGR;
            Application.logPerf("Transform image DEFAULT quality >>" + quality + " OutType: " + outType + " format " + format);
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
            throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException, IOException {
        // unused
    }

    public static boolean formatDiffer(final String identifier, final ImageApiSelector selector) {
        final Format outputF = selector.getFormat();
        final String lastFour = identifier.substring(identifier.length() - 4).toLowerCase();
        if (outputF == Format.JPG && (lastFour.equals(".jpg") || lastFour.equals("jpeg")))
            return false;
        if (outputF == Format.PNG && lastFour.equals(".png"))
            return false;
        if (outputF == Format.TIF && (lastFour.equals(".tif") || lastFour.equals("tiff")))
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

    public void processImage(String identifier, ImageApiSelector selector, ImageApiProfile profile, OutputStream os, ImageReader imgReader,
            String uri) throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException, IOException {
        long deb = System.currentTimeMillis();
        // IIOMetadata streamMetadata = imgReader.getStreamMetadata();
        IIOMetadata imageMetadata = imgReader.getImageMetadata(0);
        try {
            Application.logPerf("Entering Processimage.... with reader {} ", imgReader);
            DecodedImage img = readImage(identifier, selector, profile, imgReader);
            Application.logPerf("Done readingImage DecodedImage created");
            BufferedImage outImg = transformImage(selector.getFormat(), img.img, img.targetSize, img.rotation, selector.getRotation().isMirror(),
                    selector.getQuality());
            ImageWriter writer = null;
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(selector.getFormat().getMimeType().getTypeName());
            while (writers.hasNext()) {
                ImageWriter w = writers.next();
                Application.logPerf("FOUND REGISTERED WRITER in list {}", w);
                writer = w;
            }
            if (writer == null) {
                throw new UnsupportedFormatException(selector.getFormat().getMimeType().getTypeName());
            }
            switch (selector.getFormat()) {

            case PNG:
                IIOImage iio = new IIOImage(outImg, null, imageMetadata);
                Application.logPerf("USING JAI PNG for {} ", identifier);
                // ImageEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(outImg);
                ImageEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(iio.getRenderedImage());
                String format = "PNG";
                ImageEncoder encoder = ImageCodec.createImageEncoder(format, os, param);
                encoder.encode(outImg);
                os.flush();
                break;

            case JPG:
                Iterator<ImageWriter> it1 = ImageIO.getImageWritersByMIMEType("image/jpeg");
                ImageWriter wtr = null;
                while (it1.hasNext()) {
                    ImageWriter w = it1.next();
                    if (w.getClass().getName().equals("com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter")) {
                        wtr = w;
                    }
                    Application.logPerf("Should use WRITERS for JPEG >> {} with quality {}", wtr, outImg.getType());
                    Application.logPerf("WRITERS---> in list {}", w.getClass().getName());
                }
                if (outImg.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                    wtr = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
                }
                Application.logPerf("USING JPEG WRITER {} for {}", wtr, identifier);
                Application.logPerf("use jpg writer vendor {}, version {}", wtr.getOriginatingProvider().getVendorName(),
                        wtr.getOriginatingProvider().getVersion());
                // This setting, using 0.75f as compression quality produces the same
                // as the default setting, with no writeParam --> writer.write(outImg)

                ImageWriteParam jpgWriteParam = wtr.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.75f);

                ImageOutputStream is = ImageIO.createImageOutputStream(os);
                wtr.setOutput(is);
                Application.logPerf("JPG WRITER WITH METADATA---> image: {}, stream: {}", imageMetadata, imageMetadata);
                wtr.write(null, new IIOImage(outImg, null, imageMetadata), jpgWriteParam);
                wtr.dispose();
                is.flush();
                break;

            case WEBP:
                writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
                Application.logPerf("Using WRITER for WEBP >>" + writer);
                ImageWriteParam pr = writer.getDefaultWriteParam();
                WebPWriteParam writeParam = (WebPWriteParam) pr;
                writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);
                ImageOutputStream iss = ImageIO.createImageOutputStream(os);
                writer.setOutput(iss);
                // writer.write(outImg);
                writer.write(null, new IIOImage(outImg, null, imageMetadata), writeParam);
                writer.dispose();
                iss.flush();
                break;

            default:
                IIOImage iii = new IIOImage(outImg, null, imageMetadata);
                Application.logPerf("USING NON NULL WRITER {}", writer);
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);
                writer.write(iii.getRenderedImage());
                writer.dispose();
                ios.flush();
            }
            Application.logPerf("Done with Processimage.... in {} ms", System.currentTimeMillis() - deb);
        } catch (InvalidParametersException | UnsupportedOperationException | UnsupportedFormatException | IOException e) {
            Log.error("Error while processing image", e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getImageModificationDate(String identifier) throws ResourceNotFoundException {
        if (imageSecurityService != null && !imageSecurityService.isAccessAllowed(identifier)) {
            Log.error("Could not get Image modification date for identifier " + identifier, "");
            throw new ResourceNotFoundException();
        }
        try {
            Resource res = resourceService.get(identifier, ResourcePersistenceType.RESOLVED, MimeType.MIME_IMAGE);
            return Instant.ofEpochMilli(res.getLastModified());
        } catch (ResourceIOException e) {
            Log.error("Could not get Image modification date from resource for identifier " + identifier, "");
            throw new ResourceNotFoundException();
        }
    }
}