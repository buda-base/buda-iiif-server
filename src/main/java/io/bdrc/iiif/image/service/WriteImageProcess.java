package io.bdrc.iiif.image.service;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.imaging.ColorTools;
import org.apache.commons.imaging.ImageReadException;
import org.apache.jena.atlas.logging.Log;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.exceptions.IIIFException;
import io.bdrc.iiif.exceptions.InvalidParametersException;
import io.bdrc.iiif.exceptions.UnsupportedFormatException;
import io.bdrc.iiif.model.DecodedImage;
import io.bdrc.iiif.model.ImageApiProfile;
import io.bdrc.iiif.model.ImageApiProfile.Format;
import io.bdrc.iiif.model.ImageApiSelector;
import io.bdrc.iiif.model.ImageReader_ICC;

@Service
@Primary
public class WriteImageProcess {

    private static final Logger log = LoggerFactory.getLogger(WriteImageProcess.class);

    public static BufferedImage grayScale(BufferedImage inputImage, int width, int height) {
        // TODO: strange behavior: if we replace BufferedImage.TYPE_BYTE_GRAY with inputImage.getType()
        // this creates a binary image, but also has some weird side effect when converting to PNG, see
        // end of transformImage function
        final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D = res.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(inputImage, 0, 0, width, height, null);
        graphics2D.dispose();
        return res;
    }
    
    /** Apply transformations to an decoded image 
     * @throws IIIFException **/
    private static BufferedImage transformImage(Format format, BufferedImage inputImage, Dimension targetSize, int rotation, boolean mirror,
            ImageApiProfile.Quality quality) throws IIIFException {
        BufferedImage img = inputImage;
        int inType = img.getType();
        log.info("img type: {}", inType);
        boolean needsAdditionalScaling = !new Dimension(img.getWidth(), img.getHeight()).equals(targetSize);
        if (needsAdditionalScaling) {
            // this sucks! it converts into RGB before resizing which is totally stupid for Gray images, and also
            // means we can't reapply the ICC profile if it's a gray one (because of some weird feature of AWT)
            if (inType == BufferedImage.TYPE_BYTE_GRAY || inType == BufferedImage.TYPE_BYTE_BINARY) {
                log.info("resize image using homemade grayscale method");
                BufferedImage newImg = grayScale(img, targetSize.width, targetSize.height);
                img.flush();
                img = newImg;
                // TODO: this introduces some sort of padding on the resulting PNG on 
                // http://iiif.bdrc.io/bdr:I0886::08860003.tif/full/!2000,500/0/default.png
                // in some cases... I can't find any rational explanation!
                log.info("new imgType {}", img.getType());
            } else {
                log.info("resize image using homemade scalr");
                img = Scalr.resize(img, Scalr.Method.BALANCED, Scalr.Mode.FIT_EXACT, targetSize.width, targetSize.height);
            }
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
            log.info("rotate image");
            img = Scalr.rotate(img, rot);
        }
        if (mirror) {
            log.info("mirror image");
            // TODO: this also converts gray images to RGB, I'm not sure it's a good idea...
            img = Scalr.rotate(img, Scalr.Rotation.FLIP_HORZ);
        }
        // Quality
        int outType = inType;
        switch (quality) {
        case GRAY:
            if (inType == BufferedImage.TYPE_BYTE_BINARY) {
                throw new IIIFException(400, 5000, "cannot give gray representation of a binary image");
            }
            outType = BufferedImage.TYPE_BYTE_GRAY;
            break;
        case BITONAL:
            outType = BufferedImage.TYPE_BYTE_BINARY;
            break;
        case COLOR:
            if (inType == BufferedImage.TYPE_BYTE_GRAY || inType == BufferedImage.TYPE_BYTE_BINARY) {
                throw new IIIFException(400, 5000, "cannot give color representation of a non-color image");
            }
            outType = BufferedImage.TYPE_3BYTE_BGR;
            break;
        default:
            outType = inType;
        }
        log.info("outType {}, imgGetType {}", outType, img.getType());
        // TODO: this is rather odd: at this stage in the case of images that are originally
        // binary, we might have img.getType() == TYPE_BYTE_GRAY because of the resizing that
        // converts to gray... but here if we convert back to binary, we get a PNG file with inverted
        // colors, which I haven't managed to really debug... so we just don't convert back to binary
        // This is not ideal as this means we are producing PNGs that are a bit bigger
        if (outType != inType) {
            log.info("transform image type {} into type {}", img.getType(), outType);
            BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), outType);
            Graphics2D g2d = newImg.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            img.flush();
            img = newImg;
            g2d.dispose();
        }
        return img;
    }
    
    public static void bitonalPngToOS_pngj(OutputStream os, BufferedImage bi) {
        ImageInfo imi = new ImageInfo(bi.getWidth(), bi.getHeight(), 1, false, true, false);
        PngWriter pngw = new PngWriter(os, imi);
        // pngw.setCompLevel(6); // tuning
        // pngw.setFilterType(FilterType.FILTER_PAETH); // tuning
        DataBufferByte db =((DataBufferByte) bi.getRaster().getDataBuffer());
        if(db.getNumBanks()!=1) {
            pngw.close();
            throw new PngjException("This method expects one bank");
        }
        MultiPixelPackedSampleModel samplemodel =  (MultiPixelPackedSampleModel) bi.getSampleModel();
        ImageLineByte line = new ImageLineByte(imi);
        byte[] dbbuf = db.getData();
        int len = dbbuf.length;
        for (int row = 0; row < imi.rows; row++) {
            int elem=samplemodel.getOffset(0,row);
            for (int col = 0,j=0; col < imi.cols/8; col++) {
                if (elem >= len) {
                    break;
                }
                //int sample = dbbuf[elem++];
                int sample = ~dbbuf[elem++];
                line.getScanline()[j++] =  (byte) (sample >> 7);
                line.getScanline()[j++] =  (byte) (sample >> 6);
                line.getScanline()[j++] =  (byte) (sample >> 5);
                line.getScanline()[j++] =  (byte) (sample >> 4);
                line.getScanline()[j++] =  (byte) (sample >> 3);
                line.getScanline()[j++] =  (byte) (sample >> 2);
                line.getScanline()[j++] =  (byte) (sample >> 1);
                line.getScanline()[j++] =  (byte) (sample);
            }
            pngw.writeRow(line, row);
        }
        pngw.end();
        pngw.close();
    }

    public static void pngToOS(OutputStream os, BufferedImage bi) throws IOException {
        // this is pretty slow
        ImageEncodeParam param = PNGEncodeParam.getDefaultEncodeParam(bi);
        ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", os, param);
        encoder.encode(bi);
    }
    
    
    public static void processImage(DecodedImage img, String identifier, ImageApiSelector selector, ImageApiProfile profile, OutputStream os,
            ImageReader_ICC imgReader)
            throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException, IOException, ImageReadException, IIIFException {
        long deb = System.currentTimeMillis();
        try {
            log.info("Processing Image for identifier >> {} ", identifier);
            log.info("color space: {}", img.getImg().getColorModel());
            BufferedImage outImg = transformImage(selector.getFormat(), img.getImg(), img.getTargetSize(), img.getRotation(),
                    selector.getRotation().isMirror(), selector.getQuality());
            log.info("color space: {}", img.getImg().getColorModel());
            if (imgReader.getIcc() != null) {
                try {
                    log.info("relabel color space");
                    BufferedImage newImg = new ColorTools().relabelColorSpace(outImg, imgReader.getIcc());
                    outImg.flush();
                    outImg = newImg;
                } catch (Exception e) {
                    log.error("ignored error when applying icc profile: ", e);
                }
            }
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
            writer.dispose();
            switch (selector.getFormat()) {

            case PNG:
                // in case of bintonal images, we encode using pngj, see
                // https://github.com/buda-base/buda-iiif-server/issues/74
                // we could do it for other types too but it would require new functions
                // and these cases are not very common
                if (outImg.getType() == BufferedImage.TYPE_BYTE_BINARY) {
                    try {
                        log.info("using PNGJ encoder for {} ", identifier);
                        bitonalPngToOS_pngj(os, outImg);
                    } catch (Exception e) {
                        log.error("tried to use the pngj encoder on {} but failed", identifier);
                        log.info("using PNG JAI encoder for {} ", identifier);
                        pngToOS(os, outImg);
                    }
                } else {
                    log.info("using PNG JAI encoder for {} ", identifier);
                    pngToOS(os, outImg);
                }
                outImg.flush();
                outImg = null;
                os.flush();
                os.close();
                break;

            case JPG:
                Iterator<ImageWriter> it1 = ImageIO.getImageWritersByMIMEType("image/jpeg");
                ImageWriter wtr = null;
                while (it1.hasNext()) {
                    ImageWriter w = it1.next();
                    if (w.getClass().getName().equals("com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter")) {
                        wtr = w;
                        break;
                    }
                }
                log.info("WRITER for JPEG >> {} with quality {}", wtr, outImg.getType());
                ImageWriteParam jpgWriteParam = wtr.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.75f);
                ImageOutputStream is = ImageIO.createImageOutputStream(os);
                wtr.setOutput(is);
                wtr.write(null, new IIOImage(outImg, null, null), jpgWriteParam);
                is.flush();
                is.close();
                os.flush();
                os.close();
                log.info("disposing JPEG writer");
                wtr.dispose();
                outImg.flush();
                outImg = null;
                break;

            case WEBP:
                writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
                log.info("WRITER for WEBP >> {} with quality {}", writer, outImg.getType());
                ImageWriteParam pr = writer.getDefaultWriteParam();
                ImageOutputStream iss = ImageIO.createImageOutputStream(os);
                writer.setOutput(iss);
                writer.write(null, new IIOImage(outImg, null, null), null);
                iss.flush();
                iss.close();
                writer.dispose();
                outImg.flush();
                outImg = null;
                break;

            default:
                Application.logPerf("USING NON NULL WRITER {}", writer);
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);
                writer.write(outImg);
                ios.flush();
                ios.close();
                writer.dispose();
                outImg.flush();
                outImg = null;
            }
            Application.logPerf("Done with Processimage.... in {} ms", System.currentTimeMillis() - deb);
        } catch (UnsupportedOperationException | UnsupportedFormatException | IOException e) {
            Log.error("Error while processing image", e.getMessage());
            throw e;
        }
    }

}