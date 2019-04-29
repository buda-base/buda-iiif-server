package io.bdrc.iiif;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import io.bdrc.webp.WebPWriteParam;

public class WebPLuciadTest {

    public static void main(String args[]) throws IOException {
        String inputPngPath = "src/test/resources/test.png";
        String inputJpgPath = "src/test/resources/default.jpg";

        // Obtain an image to encode from somewhere
        BufferedImage image = ImageIO.read(new File(inputJpgPath));

        // Obtain a WebP ImageWriter instance
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
        System.out.println("WRITER >>" + writer);
        // Configure encoding parameters
        WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
        writeParam.setCompressionMode(WebPWriteParam.MODE_DEFAULT);

        // Configure the output on the ImageWriter
        writer.setOutput(new FileImageOutputStream(new File("src/test/resources/outputJPG.webp")));

        // Encode
        long st = System.currentTimeMillis();
        writer.write(null, new IIOImage(image, null, null), writeParam);
        writer.write(new IIOImage(image, null, null));
        System.out.println("cost: " + (System.currentTimeMillis() - st));
    }

}
