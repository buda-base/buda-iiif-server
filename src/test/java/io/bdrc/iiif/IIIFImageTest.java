package io.bdrc.iiif;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.junit.Test;

import de.digitalcollections.iiif.myhymir.Application;

public class IIIFImageTest {

    @Test
    public void iiiftext() throws IOException {
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType("image/png");
        for (Iterator<ImageWriter> iterator = imageWriters; iterator.hasNext();) {
            ImageWriter imageWriter = iterator.next();
            if (imageWriter == null) {
                System.out.println("(is null)");
            } else {
                System.out.println(imageWriter.getDefaultWriteParam().canWriteCompressed());
            }
        }
        InputStream input = IIIFImageTest.class.getClassLoader().getResourceAsStream("08860041.tif");
        ImageInputStream iis;
        try {
            iis = ImageIO.createImageInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        ImageReader reader = ImageIO.getImageReaders(iis).next();
        System.out.println("using reader " + (reader.getClass()));
        reader.setInput(iis);
        ImageReadParam readParam = reader.getDefaultReadParam();
        BufferedImage outImg = null;
        try {
            outImg = reader.read(0, readParam);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println("out " + outImg);
        Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType("image/jpeg");
        while (it.hasNext()) {
            Application.perf.debug("WRITER in list {}", it.next());
        }
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
        System.out.println("using writer " + writer);
        ImageWriteParam jpgWriteParam = writer.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.75f);
        OutputStream os = new FileOutputStream(new File("imgresult6.jpg"));
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);
        // writer.write(outImg);
        writer.write(null, new IIOImage(outImg, null, null), jpgWriteParam);
        ios.flush();
        ios.close();
        writer.dispose();
        // System.out.println("using reader ");
        // System.loadLibrary("libturbojpeg");
        /*
         * TJCompressor tj; try { tj = new TJCompressor();
         * System.out.println("compressor " + tj); tj.setJPEGQuality(75);
         * tj.setSubsamp(TJ.SAMP_420);
         * 
         * byte[] jpegBytes;
         * 
         * jpegBytes = tj.compress(outImg, 0);
         * 
         * System.out.println("jpegBytes " + jpegBytes); BufferedImage img;
         * 
         * img = ImageIO.read(new ByteArrayInputStream(jpegBytes)); File outputfile =
         * new File("image2.jpg"); ImageIO.write(img, "jpg", outputfile); } catch
         * (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); }
         * catch (Exception e) { // TODO Auto-generated catch block e.printStackTrace();
         * }
         */
    }

}
