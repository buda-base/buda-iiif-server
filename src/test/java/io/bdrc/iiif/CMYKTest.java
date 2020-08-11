package io.bdrc.iiif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.ImageReadException;

import io.bdrc.iiif.exceptions.UnsupportedFormatException;

public class CMYKTest {

    public static void checkOpenCMYK(String filename, String readerClass) throws IOException, ImageReadException {
        //InputStream is = CMYKTest.class.getClassLoader().getResourceAsStream(filename);
        InputStream is = new FileInputStream("src/test/resources/"+filename);
        ImageInputStream iis = ImageIO.createImageInputStream(is);
        Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
        ImageReader r = null;
        while (itr.hasNext()) {
            ImageReader tmp = itr.next();
            if (tmp.toString().startsWith(readerClass)) {
                r = tmp;
            }
            // System.out.println("found reader: " + tmp);
        }
        // itr = ImageIO.getImageReaders(iis);
        // r = itr.next();
        System.out.println("WITH READER >>> " + r.toString());
        r.setInput(iis);
        BufferedImage img = r.read(0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", outputStream);
        System.out.println(" ********************** READER SUCCESSFUL ***************************/");
    }

    public static void main(String[] args) throws IOException, UnsupportedFormatException, ImageReadException {
        try {
            checkOpenCMYK("cmyk.jpg", "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader");
        } catch (Exception e) {
            System.out.println(" ********************** READER FAILED ***************************/");
        }
        try {
            checkOpenCMYK("cmyk.jpg", "de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader");
        } catch (Exception e) {
            System.out.println(" ********************** READER FAILED ***************************/");
        }
        try {
            checkOpenCMYK("cmyk.jpg", "com.sun.imageio.plugins.jpeg.JPEGImageReader");
        } catch (Exception e) {
            System.out.println(" ********************** READER FAILED ***************************/");
        }
    }

}