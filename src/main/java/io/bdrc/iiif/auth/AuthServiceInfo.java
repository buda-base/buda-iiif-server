package io.bdrc.iiif.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.Profile;
import de.digitalcollections.iiif.model.Service;

public class AuthServiceInfo extends Service{

    @JsonProperty("service")
    private List<AuthService> services;

    @JsonProperty("header")
    private String header;

    @JsonProperty("description")
    private String description;

    @JsonProperty("confirmLabel")
    private String confirmLabel;

    @JsonProperty("failureHeader")
    private String failureHeader;

    public AuthServiceInfo(URI context) throws URISyntaxException {
        super(context);
        setIdentifier(new URI("https://iiif.bdrc.io/auth/login"));
        addProfile(new Profile(new URI("http://iiif.io/api/auth/1/login")));
        setLabel("Login to BDRC");
        setHeader("Please Log In");
        setDescription("Login to BDRC image resources");
        setConfirmLabel("Login");
        setFailureHeader("Authentication Failed");
        services=new ArrayList<>();
        AuthService token=new AuthService("https://iiif.bdrc.io/auth/token",
                "http://iiif.io/api/auth/1/token");
        addService(token);
        AuthService logout=new AuthService("https://iiif.bdrc.io/auth/logout",
                "http://iiif.io/api/auth/1/logout");
        addService(logout);
    }

    public void addService(AuthService auth) {
        services.add(auth);
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfirmLabel() {
        return confirmLabel;
    }

    public void setConfirmLabel(String confirmLabel) {
        this.confirmLabel = confirmLabel;
    }

    public String getFailureHeader() {
        return failureHeader;
    }

    public void setFailureHeader(String failureHeader) {
        this.failureHeader = failureHeader;
    }

}
