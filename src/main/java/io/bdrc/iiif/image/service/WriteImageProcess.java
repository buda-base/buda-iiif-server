package io.bdrc.iiif.image.service;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.jena.atlas.logging.Log;
import org.imgscalr.Scalr;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.luciad.imageio.webp.WebPWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.InvalidParametersException;
import io.bdrc.iiif.exceptions.UnsupportedFormatException;
import io.bdrc.iiif.model.DecodedImage;
import io.bdrc.iiif.model.ImageApiProfile;
import io.bdrc.iiif.model.ImageApiProfile.Format;
import io.bdrc.iiif.model.ImageApiSelector;

@Service
@Primary
public class WriteImageProcess {

    /** Apply transformations to an decoded image **/
    private static BufferedImage transformImage(Format format, BufferedImage inputImage, Dimension targetSize, int rotation, boolean mirror,
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

    public static void processImage(DecodedImage img, String identifier, ImageApiSelector selector, OutputStream os, String uri)
            throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException, IOException, ImageReadException {
        long deb = System.currentTimeMillis();
        try {
            BufferedImage outImg = transformImage(selector.getFormat(), img.getImg(), img.getTargetSize(), img.getRotation(),
                    selector.getRotation().isMirror(), selector.getQuality());
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
                Application.logPerf("USING JAI PNG for {} ", identifier);
                ImageEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(outImg);
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
                wtr.write(null, new IIOImage(outImg, null, null), jpgWriteParam);
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
                writer.write(null, new IIOImage(outImg, null, null), writeParam);
                writer.dispose();
                iss.flush();
                break;

            default:
                Application.logPerf("USING NON NULL WRITER {}", writer);
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);
                writer.write(outImg);
                writer.dispose();
                ios.flush();
            }
            Application.logPerf("Done with Processimage.... in {} ms", System.currentTimeMillis() - deb);
        } catch (UnsupportedOperationException | UnsupportedFormatException | IOException e) {
            Log.error("Error while processing image", e.getMessage());
            throw e;
        }
    }

}