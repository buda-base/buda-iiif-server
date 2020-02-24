package io.bdrc.iiif;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.ColorTools;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;

public class ICCProfileTest {

    public static ICC_Profile readICCFromImage(String filename) throws IOException, UnsupportedFormatException, ImageReadException {
        // This is equivalent to getting bytes or input stream from S3
        // which is done anyway
        InputStream is = ICCProfileTest.class.getClassLoader().getResourceAsStream(filename);
        ICC_Profile icc = Imaging.getICCProfile(is, null);
        System.out.println("ICC for " + filename + " is " + icc);
        return icc;
    }

    public static void writeAndReadICCToImage(String filename, ICC_Profile icc) throws IOException, UnsupportedFormatException, ImageReadException {
        System.out.println("ICC to apply " + icc);
        InputStream is = ICCProfileTest.class.getClassLoader().getResourceAsStream(filename);
        BufferedImage img = ImageIO.read(is);
        // The code above produces the equivalent of the reader output (i.e a Buffered
        // image to be processed by the appropriate writer)

        // The code below shows what is actually added to the process (Convert the
        // reader output to a Buffered image with the original ICC before writing
        // process)
        BufferedImage img1 = new ColorTools().convertToICCProfile(img, icc);

        // check the icc profile is the same
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(img1, "jpg", outputStream);
        ICC_Profile icc1 = Imaging.getICCProfile(outputStream.toByteArray());
        System.out.println("ICC for converted is " + icc1);
    }

    public static void checkinPixel100(String filename, ICC_Profile icc, String readerClass) throws IOException, ImageReadException {
        System.out.println("ICC to apply " + icc);
        InputStream is = ICCProfileTest.class.getClassLoader().getResourceAsStream(filename);
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
        int pixel100 = img.getRGB(100, 100);
        String p = "(" + ((pixel100 & 0xff0000) >> 16) + "," + ((pixel100 & 0xff00) >> 8) + "," + (pixel100 & 0xff) + ")";
        System.out.println(p);

        // The code above produces the equivalent of the reader output (i.e a Buffered
        // image to be processed by the appropriate writer)

        // The code below shows what is actually added to the process (Convert the
        // reader output to a Buffered image with the original ICC before writing
        // process)
        BufferedImage img1 = new ColorTools().convertToICCProfile(img, icc);
        int pixel100S = img.getRGB(100, 100);
        String ps = "(" + ((pixel100S & 0xff0000) >> 16) + "," + ((pixel100S & 0xff00) >> 8) + "," + (pixel100S & 0xff) + ")";
        System.out.println(ps);

        // check the icc profile is the same
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(img1, "jpg", outputStream);
        ICC_Profile icc1 = Imaging.getICCProfile(outputStream.toByteArray());
        System.out.println("ICC for converted is " + icc1);
        System.out.println("Are Pixel 100 the same ? " + p.equals(ps));
        System.out.println(" ***********************Next test****************************/");
    }

    public static void main(String[] args) throws IOException, UnsupportedFormatException, ImageReadException {
        readICCFromImage("ORIGINAL_S3.jpg");
        readICCFromImage("S3_1500.jpg");
        writeAndReadICCToImage("S3_1500.jpg", readICCFromImage("ORIGINAL_S3.jpg"));
        checkinPixel100("S3_1500.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader");
        checkinPixel100("S3_1500.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader");
        checkinPixel100("S3_1500.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "com.sun.imageio.plugins.jpeg.JPEGImageReader");
        // Apply the ICC of the image to itself, after having read it with twelvemonkeys
        checkinPixel100("ORIGINAL_S3.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader");
        checkinPixel100("ORIGINAL_S3.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "de.digitalcollections.turbojpeg.imageio.TurboJpegImageReader");
        checkinPixel100("ORIGINAL_S3.jpg", readICCFromImage("ORIGINAL_S3.jpg"), "com.sun.imageio.plugins.jpeg.JPEGImageReader");

    }

}
