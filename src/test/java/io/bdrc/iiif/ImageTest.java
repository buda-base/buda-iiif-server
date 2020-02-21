package io.bdrc.iiif;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.digitalcollections.iiif.hymir.model.exception.UnsupportedFormatException;

public class ImageTest {

    public ImageTest() {
        // TODO Auto-generated constructor stub
    }

    public static void readImageFromStream(String filename) throws IOException, UnsupportedFormatException {
        InputStream is = ImageTest.class.getClassLoader().getResourceAsStream(filename);
        ImageInputStream iis = ImageIO.createImageInputStream(is);
        Iterator<ImageReader> itr = ImageIO.getImageReaders(iis);
        while (itr.hasNext()) {
            is = ImageTest.class.getClassLoader().getResourceAsStream(filename);
            iis = ImageIO.createImageInputStream(is);
            ImageReader r = itr.next();
            r.setInput(iis);
            IIOMetadata meta = r.getImageMetadata(0);
            IIOMetadata streamMeta = r.getStreamMetadata();
            String[] names = meta.getMetadataFormatNames();

            System.out.println("FOUND READER >> " + r + " Meta >> " + meta + " Stream meta>> " + streamMeta);
            for (String s : names) {
                Node inode = meta.getAsTree(s);
                System.out.println("********** IOO Meta NODE NAME >>" + s + "********Reader ********" + r);
                printTree(inode);
            }
        }
        is.close();
        iis.close();
    }

    public static void printTree(Node doc) {
        if (doc == null) {
            System.out.println("Nothing to print!!");
            return;
        }
        try {
            System.out.println(doc.getNodeName() + "  " + doc.getNodeValue());
            NamedNodeMap cl = doc.getAttributes();
            for (int i = 0; i < cl.getLength(); i++) {
                Node node = cl.item(i);
                System.out.println("\t" + node.getNodeName() + " ->" + node.getNodeValue());
            }
            NodeList nl = doc.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                printTree(node);
            }
        } catch (Throwable e) {
            System.out.println("Cannot print!! " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException, UnsupportedFormatException {
        readImageFromStream("ORIGINAL_S3.jpg");
    }

}
