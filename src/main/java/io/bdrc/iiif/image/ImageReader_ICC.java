package io.bdrc.iiif.image;

import java.awt.color.ICC_Profile;

import javax.imageio.ImageReader;

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

}
