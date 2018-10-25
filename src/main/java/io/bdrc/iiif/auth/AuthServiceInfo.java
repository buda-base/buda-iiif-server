package io.bdrc.iiif.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.Profile;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.Service;
import io.bdrc.auth.AuthProps;

@Component
public class AuthServiceInfo extends Service{

    @JsonProperty("service")
    private List<AuthService> services;

    @JsonProperty("header")
    private PropertyValue header;

    @JsonProperty("description")
    private PropertyValue description;

    @JsonProperty("confirmLabel")
    private PropertyValue confirmLabel;

    @JsonProperty("failureHeader")
    private PropertyValue failureHeader;

    private String loginSvc=AuthProps.getProperty("authLoginSvc");
    private String tokenSvc=AuthProps.getProperty("authTokenSvc");
    private String logoutSvc=AuthProps.getProperty("authLogoutSvc");

    public static String AUTH_LOGIN="http://iiif.io/api/auth/1/login";
    public static String AUTH_EXT="http://iiif.io/api/auth/1/external";
    public static String AUTH_CONTEXT="http://iiif.io/api/auth/1/context.json";


    public AuthServiceInfo() throws URISyntaxException {
        super(new URI(AUTH_CONTEXT));
        boolean useExternal=Boolean.parseBoolean(AuthProps.getProperty("authExternal"));
        if(hasValidProperties()) {
            setIdentifier(new URI(loginSvc));
            if(useExternal) {
                addProfile(new Profile(new URI(AUTH_EXT)));
            }
            else{
                addProfile(new Profile(new URI(AUTH_LOGIN)));
            }
            setLabel(new PropertyValue("Login to BDRC"));
            setHeader(new PropertyValue("Please Log In"));
            setDescription(new PropertyValue("Login to BDRC image resources"));
            setConfirmLabel(new PropertyValue("Login"));
            setFailureHeader(new PropertyValue("Authentication Failed"));
            services=new ArrayList<>();
            AuthService token=new AuthService(tokenSvc,
                    "http://iiif.io/api/auth/1/token");
            addService(token);
            AuthService logout=new AuthService(logoutSvc,
                    "http://iiif.io/api/auth/1/logout");
            addService(logout);
        }
    }

    public boolean hasValidProperties() {
        return (loginSvc!=null && tokenSvc!=null && logoutSvc!=null);
    }

    public void addService(AuthService auth) {
        services.add(auth);
    }

    public PropertyValue getHeader() {
        return header;
    }

    public void setHeader(PropertyValue header) {
        this.header = header;
    }

    public void setDescription(PropertyValue description) {
        this.description = description;
    }

    public void setConfirmLabel(PropertyValue confirmLabel) {
        this.confirmLabel = confirmLabel;
    }

    public void setFailureHeader(PropertyValue failureHeader) {
        this.failureHeader = failureHeader;
    }

    public List<AuthService> getServices() {
        return services;
    }

    public boolean authEnabled() {
        return Boolean.parseBoolean(AuthProps.getProperty("authEnabled"));
    }

}
