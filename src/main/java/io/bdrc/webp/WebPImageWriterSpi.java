package io.bdrc.webp;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 */
public class WebPImageWriterSpi extends ImageWriterSpi {
    public WebPImageWriterSpi() {
        super("Luciad", "1.0", new String[] { "WebP", "webp" }, new String[] { "webp" }, new String[] { "image/webp" }, WebPReader.class.getName(), new Class[] { ImageOutputStream.class }, new String[] { WebPImageReaderSpi.class.getName() }, false,
                null, null, null, null, false, null, null, null, null);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        ColorModel colorModel = type.getColorModel();
        SampleModel sampleModel = type.getSampleModel();
        int transferType = sampleModel.getTransferType();

        if (colorModel instanceof ComponentColorModel) {
            if (!(sampleModel instanceof ComponentSampleModel)) {
                return false;
            }

            if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_INT) {
                return false;
            }
        } else if (colorModel instanceof DirectColorModel) {
            if (!(sampleModel instanceof SinglePixelPackedSampleModel)) {
                return false;
            }

            if (transferType != DataBuffer.TYPE_INT) {
                return false;
            }
        }

        ColorSpace colorSpace = colorModel.getColorSpace();
        if (!(colorSpace.isCS_sRGB())) {
            return false;
        }

        int[] sampleSize = sampleModel.getSampleSize();
        for (int i = 0; i < sampleSize.length; i++) {
            if (sampleSize[i] > 8) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new WebPWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "WebP Writer";
    }
}
