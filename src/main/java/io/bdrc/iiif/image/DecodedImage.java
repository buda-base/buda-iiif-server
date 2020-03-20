package io.bdrc.iiif.image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class DecodedImage {

    /** Decoded image **/
    final BufferedImage img;

    /** Final target size for scaling **/
    final Dimension targetSize;

    /** Rotation needed after decoding? **/
    final int rotation;

    // Small value type to hold information about decoding results
    protected DecodedImage(BufferedImage img, Dimension targetSize, int rotation) {
        this.img = img;
        this.targetSize = targetSize;
        this.rotation = rotation;
    }

}
