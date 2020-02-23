package io.bdrc.iiif;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;


import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;

public class ImageTest {

    public ImageTest() {
        // TODO Auto-generated constructor stub
    }

    public static class MyIIOReadWarningListener implements IIOReadWarningListener {

        @Override
        public void warningOccurred(ImageReader arg0, String arg1) {
            System.out.println(arg1);
        }
        
    }
    
    public static void readAndWriteTwelveMonkeys(String filename) throws IOException, UnsupportedFormatException {
        InputStream is = ImageTest.class.getClassLoader().getResourceAsStream(filename);
        ImageInputStream iis = ImageIO.createImageInputStream(is);
        Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
        ImageReader r = itr.next();
        is = ImageTest.class.getClassLoader().getResourceAsStream(filename);
        iis = ImageIO.createImageInputStream(is);
        
        // no visible warning
        r.addIIOReadWarningListener(new MyIIOReadWarningListener());
        r.setInput(iis);
        
        // empty <app2ICC/>
        printMetadata(r.getImageMetadata(0));
        ImageTypeSpecifier its = r.getRawImageType(0);
        
        // color space is RGB (not sRGB), not sure if it's relevant
        System.out.println("is ColorSpace RGB? "+(its.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB));
                
        BufferedImage bi = r.read(0);
        
        // no writer for the raw image type of the input image:
        Iterator<ImageWriter> itw = ImageIO.getImageWriters(its, "jpeg");
        ImageWriter iw = itw.next();
        if (iw == null) {
            System.out.println("no writer for the raw image type of the input");
        } else {
            ImageOutputStream out = ImageIO.createImageOutputStream(new File("test-inputrawimagetype.jpg"));
            iw.setOutput(out);
            iw.write(bi);
        }
        
        // when writing with a more regular image type, the icc profile is not kept
        Iterator<ImageWriter> itw2 = ImageIO.getImageWriters(new ImageTypeSpecifier(bi), "jpeg");
        ImageWriter iw2 = itw2.next();
        if (iw2 == null) {
            System.out.println("no writer for the image type of the buffered image");
        } else {
            ImageOutputStream out = ImageIO.createImageOutputStream(new File("test-regularjpg.jpg"));
            //iw2.
            iw2.setOutput(out);
            iw2.write(r.read(0));
        }
            
        is.close();
        iis.close();
    }
    
    public static void printMetadata(IIOMetadata meta) {
        String[] names = meta.getMetadataFormatNames();
        // Print image metadata
        System.out.println("image metadata:");
        for (String s : names) {
            Node inode = meta.getAsTree(s);
            StringWriter writer = new StringWriter();
            Transformer transformer;
            try {
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(new DOMSource(inode), new StreamResult(writer));
            } catch (TransformerFactoryConfigurationError | TransformerException e) {
                e.printStackTrace();
                return;
            }
            System.out.println(writer.toString());        }
    }

    public static void main(String[] args) throws IOException, UnsupportedFormatException {
        readAndWriteTwelveMonkeys("ORIGINAL_S3.jpg");
    }

}
