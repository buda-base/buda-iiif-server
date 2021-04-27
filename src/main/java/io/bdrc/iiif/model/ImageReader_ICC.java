package io.bdrc.iiif.model;

import java.awt.color.ICC_Profile;
import java.io.IOException;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ImageReader_ICC {

    ImageReader reader;
    ICC_Profile icc;

    public ImageReader_ICC(ImageReader reader, ICC_Profile icc) {
        super();
        this.reader = reader;
        this.icc = icc;
    }

    public ImageReader getReader() {
        return reader;
    }

    public ICC_Profile getIcc() {
        return icc;
    }

    public void closeAndDispose() throws IOException {
        //this.reader.g
        ImageInputStream iis = (ImageInputStream) reader.getInput();
        iis.close();
        reader.dispose();
    }
    
}
