package de.digitalcollections.iiif.myhymir;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;

import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;
import io.bdrc.auth.Access;
import io.bdrc.auth.rdf.RdfConstants;
import io.bdrc.iiif.resolver.IdentifierInfo;

public class ResourceAccessValidation {

    private static final String CHINA="China";

    Access access;
    IdentifierInfo idinfo;
    String accessType;

    public ResourceAccessValidation(Access access, String identifier) throws ClientProtocolException, IOException, ResourceNotFoundException {
        super();
        this.access = access;
        this.idinfo=new IdentifierInfo(identifier);
        accessType=idinfo.getAccessShortName();
    }

    public boolean isAccessible(HttpServletRequest request) {
        boolean accessible=true;
        if(accessType.equals(RdfConstants.RESTRICTED_CHINA)) {
            if (CHINA.equalsIgnoreCase(GeoLocation.getCountryName(request.getRemoteAddr()))) {
                //if Geolocation country name is null (i.e throws -for instance- an IP parsing exception)
                //then access is denied
                accessible=false;
            }
        }
        return (accessible && access.hasResourceAccess(accessType) || idinfo.isFairUsePublicImage());
    }

    public boolean isOpenAccess() {
        return accessType.equals(RdfConstants.OPEN);
    }

}
