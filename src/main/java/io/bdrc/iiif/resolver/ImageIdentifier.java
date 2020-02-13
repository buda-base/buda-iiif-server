package io.bdrc.iiif.resolver;

import java.util.HashMap;
import java.util.List;

import io.bdrc.iiif.exceptions.IIIFException;

public class ImageIdentifier {

    public static String IGFN = "igfn";
    public static String IGFI = "igfi";
    String identifier;
    HashMap<String, String> parts;

    public ImageIdentifier(String identifier) throws IIIFException {
        this.identifier = identifier;
        this.parts = parseIdentifier(identifier);
    }

    private HashMap<String, String> parseIdentifier(String id) throws IIIFException {
        try {
            parts = new HashMap<>();
            String[] chunks = id.split("::");
            String[] start = chunks[0].split(":");
            boolean hasPrefix = false;
            if (start.length == 3) {
                parts.put("prefix", start[0]);
                parts.put("imageGroup", start[1] + ":" + start[2]);
                hasPrefix = true;
            } else {
                parts.put("imageGroup", chunks[0]);
            }
            if (hasPrefix) {
                if (parts.get("prefix").equals(IGFN)) {
                    parts.put("imageName", chunks[1]);
                }
                if (parts.get("prefix").equals(IGFI)) {
                    parts.put("imageIndex", chunks[1]);
                }
            } else {
                parts.put("imageName", chunks[1]);
            }
        } catch (Exception e) {
            throw new IIIFException(e);
        }
        return parts;
    }

    public String getImageName(List<ImageInfo> inf) {
        if (parts.get("imageIndex") != null) {
            return inf.get(Integer.parseInt(parts.get("imageIndex"))).filename;
        } else {
            return parts.get("imageName");
        }
    }

    public String getCanonical() {
        return IGFN + ":" + parts.get("imageGroup") + ":" + parts.get("imageName");
    }

    public String getPart(String key) {
        return parts.get(key);
    }

    public boolean hasPrefix() {
        return (parts.get("prefix") != null);
    }

    public boolean isDeprecated() {
        return (parts.get("prefix") == null);
    }

}
