package io.bdrc.iiif.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthService {

    @JsonProperty("@id")
    private String context;

    @JsonProperty("profile")
    private String profile;

    @JsonProperty("label")
    private String label;

    public AuthService(String context, String profile) {
        super();
        this.context = context;
        this.profile = profile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
