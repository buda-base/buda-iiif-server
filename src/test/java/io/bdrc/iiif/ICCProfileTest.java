package io.bdrc.iiif;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

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

    public static void main(String[] args) throws IOException, UnsupportedFormatException, ImageReadException {
        readICCFromImage("ORIGINAL_S3.jpg");
        readICCFromImage("S3_1500.jpg");
        writeAndReadICCToImage("S3_1500.jpg", readICCFromImage("ORIGINAL_S3.jpg"));

    }

}
