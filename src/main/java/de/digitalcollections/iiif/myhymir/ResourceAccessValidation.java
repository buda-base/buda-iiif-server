
package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;

import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ResourceAccessValidation {

    private static final String CHINA = "China";

    Access access;
    String accessType;
    boolean fairUse;

    public ResourceAccessValidation(Access access, IdentifierInfo idInfo, String img) throws ClientProtocolException, IOException, ResourceNotFoundException {
        super();
        this.access = access;
        accessType = idInfo.getAccessShortName();
        fairUse = RdfConstants.FAIR_USE.equals(accessType) || RdfConstants.RESTRICTED_CHINA.equals(accessType);
        if (fairUse) {
            fairUse = idInfo.isFairUsePublicImage(img);
        }
    }

    public ResourceAccessValidation(Access access, String accessType) throws ClientProtocolException, IOException, ResourceNotFoundException {
        super();
        this.access = access;
        this.accessType = accessType;
        fairUse = RdfConstants.FAIR_USE.equals(accessType) || RdfConstants.RESTRICTED_CHINA.equals(accessType);
    }

    public boolean isFairUse() {
        return fairUse;
    }

    public boolean isAccessible(HttpServletRequest request) {
        if (access == null) {
            access = new Access();
        }
        boolean accessible = true;
        if (accessType.equals(RdfConstants.RESTRICTED_CHINA)) {
            if (CHINA.equalsIgnoreCase(GeoLocation.getCountryName(request.getRemoteAddr()))) {
                // if Geolocation country name is null (i.e throws -for instance- an IP parsing
                // exception)
                // then access is denied
                accessible = false;
            }
        }
        return (accessible && access.hasResourceAccess(accessType) || fairUse);
    }

    public boolean isOpenAccess() {
        return accessType.equals(RdfConstants.OPEN);
    }

}
