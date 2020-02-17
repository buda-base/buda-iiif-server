package de.digitalcollections.iiif.hymir.image.frontend;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;

public class BDRCImageService extends ImageService {

    // this class extends the info.json with preferredFormats property
    
    @JsonProperty("preferredFormats")
    private PropertyValue preferredFormats;  
    
    public BDRCImageService(String identifier, ImageApiProfile profile) {
        super(identifier, profile);
    }

    public BDRCImageService(String identifier) {
        super(identifier);
    }

    public void setPreferredFormats(PropertyValue preferredFormats) {
        this.preferredFormats = preferredFormats;
    }

}
