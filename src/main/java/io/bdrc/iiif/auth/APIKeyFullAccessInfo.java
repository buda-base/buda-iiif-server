package io.bdrc.iiif.auth;

import java.util.List;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.rdf.RdfConstants;

public class APIKeyFullAccessInfo implements AccessInfo {

    public APIKeyFullAccessInfo() {}
    
    public static final APIKeyFullAccessInfo INSTANCE = new APIKeyFullAccessInfo();
    
    @Override
    public AccessLevel hasResourcePDFAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri, String ipAddress, List<String> collections) {
        if (RdfConstants.RESTRICTED_BY_TBRC.equals(resourceAccessLocalName))
            return AccessLevel.NOACCESS;
        return AccessLevel.OPEN;
    }

    @Override
    public AccessLevel hasResourceAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri) {
        if (RdfConstants.RESTRICTED_BY_TBRC.equals(resourceAccessLocalName))
            return AccessLevel.NOACCESS;
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

    @Override
    public boolean isEditor() {
        return false;
    }

    @Override
    public boolean isContributor() {
        return false;
    }

    @Override
    public String getId() {
        return "569b6045-1e1c-4896-a32f-d962e996682e";
    }

    @Override
    public boolean hasEndpointAccess() {
        return false;
    }

}
