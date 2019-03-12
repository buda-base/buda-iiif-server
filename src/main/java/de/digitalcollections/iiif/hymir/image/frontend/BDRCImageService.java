package de.digitalcollections.iiif.hymir.image.frontend;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;

public class BDRCImageService extends ImageService {

    @JsonProperty("formatHints")
    private PropertyValue formatHints;  
    
    public BDRCImageService(String identifier, ImageApiProfile profile) {
        super(identifier, profile);
    }

    public BDRCImageService(String identifier) {
        super(identifier);
    }

    public void setFormatHints(PropertyValue formatHints) {
        this.formatHints = formatHints;
    }

}
