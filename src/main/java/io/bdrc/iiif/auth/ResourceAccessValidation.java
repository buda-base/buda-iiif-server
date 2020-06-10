
package io.bdrc.iiif.auth;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.GeoLocation;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageGroupInfo;

public class ResourceAccessValidation {

    private static final String CHINA = "China";
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessValidation.class);

    Access access;
    String accessShort;
    String statusShort;
    String imageInstanceUri;
    String imageFileName = null;
    ImageGroupInfo igi;
    boolean isRestrictedInChina;

    public ResourceAccessValidation(Access access, IdentifierInfo idInfo, String imageFileName) {
        super();
        this.access = access;
        final String accessUri = idInfo.igi.access.getUri();
        accessShort = accessUri.substring(accessUri.lastIndexOf('/') + 1);
        final String statusUri = idInfo.igi.statusUri;
        statusShort = statusUri.substring(statusUri.lastIndexOf('/') + 1);
        this.isRestrictedInChina = idInfo.igi.restrictedInChina;
        this.imageInstanceUri = idInfo.igi.imageInstanceId;
        this.igi = idInfo.igi;
        this.imageFileName = imageFileName;
    }

    public ResourceAccessValidation(Access access, IdentifierInfo idInfo) {
        super();
        this.access = access;
        final String accessUri = idInfo.igi.access.getUri();
        accessShort = accessUri.substring(accessUri.lastIndexOf('/') + 1);
        final String statusUri = idInfo.igi.statusUri;
        statusShort = statusUri.substring(statusUri.lastIndexOf('/') + 1);
        this.isRestrictedInChina = idInfo.igi.restrictedInChina;
        this.imageInstanceUri = idInfo.igi.imageInstanceId;
        this.igi = idInfo.igi;
    }

    public Access getAccess() {
        if (access == null) {
            return new Access();
        }
        return access;
    }

    public AccessLevel getAccessLevel(HttpServletRequest request) {
        if (access == null)
            access = new Access();
        if (isRestrictedInChina) {
            if (!Application.isInChina()) {
                String test = GeoLocation.getCountryName(request.getHeader("X-Real-IP"));
                log.info("TEST IP from X-Real-IP header: {} and country: {}", request.getHeader("X-Real-IP"), test);
                if (test == null || CHINA.equalsIgnoreCase(test)) {
                    // if Geolocation country name is null (i.e throws -for instance- an IP parsing
                    // exception)
                    // then access is denied
                    return AccessLevel.NOACCESS;
                }
            } else {
                return AccessLevel.NOACCESS;
            }
        }
        log.info("Getting access level for accessShort= {} and statusShort={} and imageUri '}", accessShort, statusShort, imageInstanceUri);
        return access.hasResourceAccess(accessShort, statusShort, imageInstanceUri);
    }

    public boolean isAccessible(HttpServletRequest request) {
        AccessLevel al = getAccessLevel(request);
        log.info("Is accessible accessLevel is {} and accessShort={}", al, accessShort);
        if (al.equals(AccessLevel.OPEN))
            return true;
        if (al.equals(AccessLevel.FAIR_USE)) {
            log.info("Matches Res Permissions is {}  for {} and Access {}", access.matchResourcePermissions(accessShort), access);
            if (access.matchResourcePermissions(accessShort)) {
                return true;
            }
            try {
                // This alone do not check against the user profile as the list
                // is built through identifierInfo regardless that user profile
                log.info("Does not match Res Permissions so returning new test from igi: {} ", this.igi.isAccessibleInFairUse(imageFileName));
                return this.igi.isAccessibleInFairUse(imageFileName);
            } catch (Exception e) {
                log.error("error when looking at fair use case: ", e);
                return false;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ResourceAccessValidation [access=" + access + ", accessShort=" + accessShort + ", statusShort=" + statusShort + ", imageInstanceUri="
                + imageInstanceUri + ", imageFileName=" + imageFileName + ", igi=" + igi + ", isRestrictedInChina=" + isRestrictedInChina + "]";
    }

}
