package io.bdrc.iiif.image.service;

import java.awt.Dimension;
import java.awt.Graphics2D;
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

import com.luciad.imageio.webp.WebPWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import io.bdrc.iiif.core.Application;
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
            throws InvalidParametersException, UnsupportedOperationException, UnsupportedFormatException, IOException, ImageReadException {
        long deb = System.currentTimeMillis();
        try {
            log.info("Processing Image for identifier >> {} ", identifier);

            BufferedImage outImg = transformImage(selector.getFormat(), img.getImg(), img.getTargetSize(), img.getRotation(),
                    selector.getRotation().isMirror(), selector.getQuality());
            if (imgReader.getIcc() != null) {
                outImg = new ColorTools().relabelColorSpace(outImg, imgReader.getIcc());
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
            switch (selector.getFormat()) {

            case PNG:
                // in case of bintonal images, we encode using pngj, see
                // https://github.com/buda-base/buda-iiif-server/issues/74
                // we could do it for other types too but it would require new functions
                // and these cases are not very common
                if (outImg.getColorModel().getPixelSize() == 1) {
                    try {
                        Application.logPerf("USING PNGJ ENCODER for {} ", identifier);
                        bitonalPngToOS_pngj(os, outImg);
                    } catch (Exception e) {
                        log.error("tried to use the pngj encoder on {} but failed", identifier);
                        Application.logPerf("USING PNG JAI ENCODER for {} ", identifier);
                        pngToOS(os, outImg);
                    }
                } else {
                    Application.logPerf("USING PNG JAI ENCODER for {} ", identifier);
                    pngToOS(os, outImg);
                }
                
                os.flush();
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
                wtr.dispose();
                is.flush();
                break;

            case WEBP:
                writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
                log.info("WRITER for WEBP >> {} with quality {}", writer, outImg.getType());
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