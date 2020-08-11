package io.bdrc.iiif.image.service;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Streams;

import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReadParam;
import de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.exceptions.InvalidParametersException;
import io.bdrc.iiif.exceptions.UnsupportedFormatException;
import io.bdrc.iiif.model.DecodedImage;
import io.bdrc.iiif.model.ImageApiProfile;
import io.bdrc.iiif.model.ImageApiSelector;
import io.bdrc.iiif.model.ImageReader_ICC;
import io.bdrc.iiif.model.Size;
import io.bdrc.iiif.model.TileInfo;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ReadImageProcess {

    private static final Logger log = LoggerFactory.getLogger(ReadImageProcess.class);

    @Value("${custom.iiif.image.maxWidth:65500}")
    private static int maxWidth;

    @Value("${custom.iiif.image.maxHeight:65500}")
    private static int maxHeight;

    /** Update ImageService based on the image **/
    private static void enrichInfo(ImageReader reader, ImageService info) throws IOException {

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
        // request only, which is BDRC basic use case
        TileInfo tile = new TileInfo(reader.getWidth(0));
        tile.setHeight(reader.getHeight(0));
        tile.addScaleFactor(1, 2, 4, 8);
        info.addTile(tile);
    }

    public static ImageReader_ICC readImageInfo(String identifier, ImageService info, ImageReader_ICC imgReader)
            throws IOException, IIIFException, UnsupportedFormatException {
        try {
            imgReader = getReader(identifier);
            enrichInfo(imgReader.getReader(), info);
        } catch (IIIFException e) {
            log.error("Could not get Image Info", e.getMessage());
            throw new IIIFException(e);
        }
        return imgReader;
    }
    
    public static ImageReader_ICC readImageInfoFailover(String identifier, ImageService info, ImageReader_ICC imgReader)
            throws IOException, IIIFException, UnsupportedFormatException {
        try {
            imgReader = getFailoverReader(identifier);
            enrichInfo(imgReader.getReader(), info);
        } catch (IIIFException e) {
            log.error("Could not get Image Info", e.getMessage());
            throw new IIIFException(e);
        }
        return imgReader;
    }

    /**
     * Try to obtain a {@link ImageReader} for a given identifier
     * 
     * @throws IIIFException
     * @throws ResourceNotFoundException
     * @throws ImageReadException
     **/
    private static ImageReader_ICC getReader(String identifier) throws UnsupportedFormatException, IOException, IIIFException {
        long deb = System.currentTimeMillis();
        ICC_Profile icc = null;
        final String s3key;
        String ext = "";
        final ImageProviderService service;
        if (identifier.startsWith("static::")) {
            s3key = identifier.substring(8);
            service = ImageProviderService.InstanceStatic;
        } else {
            IdentifierInfo idf = new IdentifierInfo(identifier);
            ext = idf.imageName.substring(idf.imageName.lastIndexOf(".") + 1);
            s3key = ImageProviderService.getKey(idf);
            service = ImageProviderService.InstanceArchive;
            log.debug("IN READ IDENTIFIER IMAGE NAME >> {}", idf.imageName);
            if (idf.imageName != null) {
                Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(ext);
                while (it.hasNext()) {
                    log.debug("IN READ READER >> {}", it.next());
                }
            }
        }
        byte[] bytes = null;
        try {
            bytes = service.getAsync(s3key).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(404, 5000, e);
        }
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        ImageReader reader = null;
        if (ext.equals("jpg")) {
            Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
            while (itr.hasNext()) {
                reader = itr.next();
                if (reader.getClass().equals(TurboJpegImageReader.class)) {
                    break;
                }
            }
        } else {
            reader = Streams.stream(ImageIO.getImageReaders(iis)).findFirst().orElseThrow(UnsupportedFormatException::new);
        }
        reader.setInput(iis);
        if (reader.getClass().equals(TurboJpegImageReader.class)) {
            try {
                icc = Imaging.getICCProfile(bytes);
            } catch (ImageReadException e) {
                e.printStackTrace();
            }
        }
        Application.logPerf("S3 object IIIS READER >> {}", reader);
        Application.logPerf("Image service return reader at {} ms {}", System.currentTimeMillis() - deb, identifier);
        return new ImageReader_ICC(reader, icc);
    }
    
    private static ImageReader_ICC getFailoverReader(String identifier) throws UnsupportedFormatException, IOException, IIIFException {
        long deb = System.currentTimeMillis();
        ICC_Profile icc = null;
        final String s3key;
        String ext = "";
        final ImageProviderService service;
        if (identifier.startsWith("static::")) {
            s3key = identifier.substring(8);
            service = ImageProviderService.InstanceStatic;
        } else {
            IdentifierInfo idf = new IdentifierInfo(identifier);
            ext = idf.imageName.substring(idf.imageName.lastIndexOf(".") + 1);
            s3key = ImageProviderService.getKey(idf);
            service = ImageProviderService.InstanceArchive;
            log.debug("IN READ IDENTIFIER IMAGE NAME >> {}", idf.imageName);
            if (idf.imageName != null) {
                Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(ext);
                while (it.hasNext()) {
                    log.debug("IN READ READER >> {}", it.next());
                }
            }
        }
        byte[] bytes = null;
        try {
            bytes = service.getAsync(s3key).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IIIFException(404, 5000, e);
        }
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        ImageReader reader = null;
        if (ext.equals("jpg")) {
            Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
            while (itr.hasNext()) {
                reader = itr.next();
                if (reader.getClass().equals(com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader.class)) {
                    break;
                }
            }
        } else {
            reader = Streams.stream(ImageIO.getImageReaders(iis)).findFirst().orElseThrow(UnsupportedFormatException::new);
        }
        reader.setInput(iis);
        if (reader.getClass().equals(TurboJpegImageReader.class)) {
            try {
                icc = Imaging.getICCProfile(bytes);
            } catch (ImageReadException e) {
                e.printStackTrace();
            }
        }
        Application.logPerf("S3 object IIIS READER >> {}", reader);
        Application.logPerf("Image service return reader at {} ms {}", System.currentTimeMillis() - deb, identifier);
        return new ImageReader_ICC(reader, icc);
    }

    public static DecodedImage readImage(String identifier, ImageApiSelector selector, ImageApiProfile profile, ImageReader_ICC imgReader)
            throws IOException, UnsupportedFormatException, InvalidParametersException, ImageReadException {
        long deb = System.currentTimeMillis();
        Application.logPerf("Entering readImage for creating DecodedImage");
        if ((selector.getRotation().getRotation() % 90) != 0) {
            log.error("Rotation is not a multiple of 90 degrees for selector {}", selector.toString(), "");
            throw new UnsupportedOperationException("Can only rotate by multiples of 90 degrees.");
        }
        Dimension nativeDimensions = new Dimension(imgReader.getReader().getWidth(0), imgReader.getReader().getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (IIIFException e) {
            log.error("Could not resolve selector region : {}", selector.getRegion(), e.getMessage());
            throw new InvalidParametersException(e);
        }
        Dimension croppedDimensions = new Dimension(targetRegion.width, targetRegion.height);
        Dimension targetSize;
        try {
            targetSize = selector.getSize().resolve(croppedDimensions, profile);
        } catch (IIIFException e) {
            log.error("Could not resolve selector size : {}", selector.getSize(), e.getMessage());
            throw new InvalidParametersException(e);
        }

        // Determine the closest resolution to the target that can be decoded directly
        double targetScaleFactor = targetSize.width / targetRegion.getWidth();
        double decodeScaleFactor = 1.0;
        int imageIndex = 0;
        for (int idx = 0; idx < imgReader.getReader().getNumImages(true); idx++) {
            double factor = (double) imgReader.getReader().getWidth(idx) / nativeDimensions.width;
            if (factor < targetScaleFactor) {
                continue;
            }
            if (Math.abs(targetScaleFactor - factor) < Math.abs(targetScaleFactor - decodeScaleFactor)) {
                decodeScaleFactor = factor;
                imageIndex = idx;
            }
        }
        ImageReadParam readParam = getReadParam(imgReader.getReader(), selector, decodeScaleFactor);
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
        DecodedImage dimg = null;        
        try {
            dimg = new DecodedImage(imgReader.getReader().read(imageIndex, readParam), targetSize, rotation);            
        } catch (Exception ex) { 
            RandomAccessFile raf = new RandomAccessFile(new File("notRead.txt"), "rw");
            raf.seek(raf.length());
            raf.writeBytes(identifier + System.lineSeparator());
            raf.close();
            log.error("Could not read image >> " + identifier, ex);
        } 
        return dimg;
    }
    
    

    /**
     * Determine parameters for image reading based on the IIIF selector and a given
     * scaling factor
     **/
    private static ImageReadParam getReadParam(ImageReader reader, ImageApiSelector selector, double decodeScaleFactor)
            throws IOException, InvalidParametersException {
        ImageReadParam readParam = reader.getDefaultReadParam();
        Application.logPerf("Entering ReadParam with ImageReadParam {}", readParam);
        Dimension nativeDimensions = new Dimension(reader.getWidth(0), reader.getHeight(0));
        Rectangle targetRegion;
        try {
            targetRegion = selector.getRegion().resolve(nativeDimensions);
        } catch (IIIFException e) {
            log.error("Could not get image ReadParam {}", e.getMessage());
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

}