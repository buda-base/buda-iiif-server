package io.bdrc.iiif.resolver;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.iiif.exceptions.IIIFException;

public class ImageIdentifier {

    public final static String IGFN = "igfn";
    public final static String IGSI = "igsi";
    String identifier;
    public String imageGroup = null;
    public String prefix = null;
    public String imageName = null;
    public String computedImageName = null;
    public Integer imageIndex = null;

    public ImageIdentifier(String identifier) throws IIIFException {
        this.identifier = identifier;
        parseIdentifier(identifier);
    }

    private String fixImageGroupId(final String igId) {
        if (igId.startsWith("bdr:V")) {
            int undIidx = igId.lastIndexOf("_I");
            if (undIidx != -1) {
                return igId.substring(undIidx+1);
            }
        }
        return igId;
    }
    
    private void parseIdentifier(String id) throws IIIFException {
        try {
            if (!id.contains("::")) {
                imageGroup = fixImageGroupId(id);
            } else {
                String[] chunks = id.split("::");
                String[] start = chunks[0].split(":");
                boolean hasPrefix = false;
                if (start.length == 3) {
                    prefix = start[0];
                    imageGroup = fixImageGroupId(start[1] + ":" + start[2]);
                    hasPrefix = true;
                } else {
                    imageGroup = fixImageGroupId(chunks[0]);
                }
                if (hasPrefix) {
                    if (prefix.equals(IGFN)) {
                        imageName = chunks[1];
                    }
                    if (prefix.equals(IGSI)) {
                        try {
                            imageIndex = Integer.parseInt(chunks[1]);
                        } catch (Exception e) {
                            throw new IIIFException(404, 5000, "cannot convert "+chunks[1]+" into integer");
                        }
                    }
                } else {
                    imageName = chunks[1];
                }
            }
        } catch (Exception e) {
            throw new IIIFException(e);
        }
    }

    public void computeImageName(List<ImageInfo> inf) {
        if (imageIndex != null && imageIndex < inf.size()) {
            computedImageName = inf.get(imageIndex).filename;
        }
    }

    public String getCanonical() throws ClientProtocolException, IOException, IIIFException, ResourceNotFoundException {
        String in = imageName == null ? computedImageName : imageName;
        return IGFN + ":" + imageGroup + ":" + in;
    }

    public boolean hasPrefix() {
        return prefix != null;
    }

    public boolean isDeprecated() {
        return prefix == null;
    }

}
