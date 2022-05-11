package io.bdrc.iiif.auth;

import java.util.List;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.rdf.RdfConstants;

public class APIKeyNormalAccessInfo implements AccessInfo {

    public APIKeyNormalAccessInfo() {}
    
    public static final APIKeyNormalAccessInfo INSTANCE = new APIKeyNormalAccessInfo();
    
    @Override
    public AccessLevel hasResourcePDFAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri, String ipAddress, List<String> collections) {
        if (RdfConstants.OPEN.equals(resourceAccessLocalName) && RdfConstants.STATUS_RELEASED.equals(resourceStatusLocalName))
            return AccessLevel.OPEN;
        return AccessLevel.NOACCESS;
    }

    @Override
    public AccessLevel hasResourceAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri) {
        if (RdfConstants.OPEN.equals(resourceAccessLocalName) && RdfConstants.STATUS_RELEASED.equals(resourceStatusLocalName))
            return AccessLevel.OPEN;
        return AccessLevel.NOACCESS;
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
