package io.bdrc.iiif.image;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.model.ImageApiProfile;
import io.bdrc.iiif.model.ImageService;
import io.bdrc.iiif.model.PropertyValue;

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
