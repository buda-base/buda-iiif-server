package io.bdrc.iiif;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ColorTools;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.icc.IccProfileParser;

import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;

public class ICCProfileTest {

    public static ICC_Profile readICCFromImage(String filename) throws IOException, UnsupportedFormatException, ImageReadException {
        String file = System.getProperty("user.dir") + "/src/test/resources/" + filename;
        File f = new File(file);
        ICC_Profile icc = Imaging.getICCProfile(f);
        IccProfileParser ipp = new IccProfileParser();
        System.out.println("Is SRGB for " + filename + " is " + ipp.issRGB(f));
        System.out.println("ICC for " + filename + " is " + icc);
        return icc;
    }

    public static void writeICCToImage(String filename, String outputFile, ICC_Profile icc)
            throws IOException, UnsupportedFormatException, ImageReadException {
        String file = System.getProperty("user.dir") + "/src/test/resources/" + filename;
        String outFile = System.getProperty("user.dir") + "/src/test/resources/" + outputFile;
        File f = new File(file);
        BufferedImage img = ImageIO.read(f);
        ColorTools ct = new ColorTools();
        BufferedImage img1 = ct.convertToICCProfile(img, icc);
        File outputfile = new File(outFile);
        FileOutputStream fos = new FileOutputStream(outputfile);
        ImageIO.write(img1, "jpg", fos);
        fos.close();
    }

    public static void main(String[] args) throws IOException, UnsupportedFormatException, ImageReadException {
        // readImageFromStream("08860041.tif");
        readICCFromImage("ORIGINAL_S3.jpg");
        readICCFromImage("S3_1500.jpg");
        writeICCToImage("S3_1500.jpg", "S3_1500_Converted.jpg", readICCFromImage("ORIGINAL_S3.jpg"));
        readICCFromImage("S3_1500_Converted.jpg");
    }

}
