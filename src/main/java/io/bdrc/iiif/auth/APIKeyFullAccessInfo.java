package io.bdrc.iiif.auth;

import java.util.List;

import io.bdrc.auth.AccessInfo;

public class APIKeyFullAccessInfo implements AccessInfo {

    public APIKeyFullAccessInfo() {}
    
    public static final APIKeyFullAccessInfo INSTANCE = new APIKeyFullAccessInfo();
    
    @Override
    public AccessLevel hasResourcePDFAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri, String ipAddress, List<String> collections) {
        return AccessLevel.OPEN;
    }

    @Override
    public AccessLevel hasResourceAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri) {
        return AccessLevel.OPEN;
    }

    @Override
    public boolean isLogged() {
        return true;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

}
