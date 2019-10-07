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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.digitalcollections.iiif.myhymir.Application;

public class IIIFImageTest {

    public final static Logger log = LoggerFactory.getLogger("default");

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
        System.out.println("APP PERF " + Application.perf);
        while (it.hasNext()) {
            System.out.println("WRITER in list :" + it.next());
        }
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
        System.out.println("using writer " + writer);
        ImageWriteParam jpgWriteParam = writer.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.75f);
        OutputStream os = new FileOutputStream(new File("imgresult.jpg"));
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);
        // writer.write(outImg);
        writer.write(null, new IIOImage(outImg, null, null), jpgWriteParam);
        ios.flush();
        ios.close();
        writer.dispose();
    }

}
