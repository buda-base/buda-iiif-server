
package io.bdrc.iiif.auth;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.AccessInfo.AccessLevel;
import io.bdrc.iiif.core.Application;
import io.bdrc.iiif.core.GeoLocation;
import io.bdrc.iiif.resolver.IdentifierInfo;
import io.bdrc.iiif.resolver.ImageGroupInfo;

public class ResourceAccessValidation {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessValidation.class);

    AccessInfo access;
    String accessShort;
    String statusShort;
    String imageInstanceUri;
    String copyrightStatusLname = null;
    String imageFileName = null;
    boolean isPDFRequest = false;
    ImageGroupInfo igi;
    boolean isRestrictedInChina;
    Boolean isInChinaB = null;

    public ResourceAccessValidation(AccessInfo access, IdentifierInfo idInfo, String imageFileName) {
        super();
        this.access = access;
        final String accessUri = idInfo.igi.access.getUri();
        accessShort = accessUri.substring(accessUri.lastIndexOf('/') + 1);
        copyrightStatusLname = idInfo.igi.copyrightStatusLname;
        final String statusUri = idInfo.igi.statusUri;
        statusShort = statusUri.substring(statusUri.lastIndexOf('/') + 1);
        this.isRestrictedInChina = idInfo.igi.restrictedInChina;
        this.imageInstanceUri = idInfo.igi.imageInstanceUri;
        this.igi = idInfo.igi;
        this.imageFileName = imageFileName;
    }

    public ResourceAccessValidation(AccessInfo access, IdentifierInfo idInfo) {
        super();
        this.isPDFRequest = true;
        this.access = access;
        final String accessUri = idInfo.igi.access.getUri();
        accessShort = accessUri.substring(accessUri.lastIndexOf('/') + 1);
        copyrightStatusLname = idInfo.igi.copyrightStatusLname;
        final String statusUri = idInfo.igi.statusUri;
        statusShort = statusUri.substring(statusUri.lastIndexOf('/') + 1);
        this.isRestrictedInChina = idInfo.igi.restrictedInChina;
        this.imageInstanceUri = idInfo.igi.imageInstanceUri;
        this.igi = idInfo.igi;
    }

    public AccessInfo getAccess() {
        if (access == null) {
            return new AccessInfoAuthImpl();
        }
        return access;
    }
    
    public boolean isInChina(HttpServletRequest request) {
        if (isInChinaB != null)
            return isInChinaB;
        if (Application.isInChina()) {
            this.isInChinaB = true;
            return true;
        }
        final String test = GeoLocation.getCountryCode(request.getHeader(GeoLocation.HEADER_NAME));
        if (test == null || "CN".equalsIgnoreCase(test)) {
            this.isInChinaB = true;
            return true;
        }
        this.isInChinaB = false;
        return false;
    }

    public AccessLevel getAccessLevel(HttpServletRequest request) {
        if (this.access == null)
            this.access = new AccessInfoAuthImpl();
        final boolean inChina = isInChina(request); 
        if (inChina && this.isRestrictedInChina) 
                return AccessLevel.NOACCESS;
        log.info("Getting access level for accessShort= {} and statusShort={} and imageUri '}", accessShort, statusShort, imageInstanceUri);
        if (this.isPDFRequest) {
            final AccessLevel res = access.hasResourcePDFAccess(accessShort, statusShort, imageInstanceUri, request.getHeader("X-Real-IP"), this.igi.inCollectionsLnames);
            if (inChina && (res == AccessLevel.FAIR_USE || "CopyrightInCopyright".equals(this.copyrightStatusLname) || "CopyrightClaimed".equals(this.copyrightStatusLname)))
                return AccessLevel.NOACCESS;
            return res;
        }
        final AccessLevel res = access.hasResourceAccess(accessShort, statusShort, imageInstanceUri);
        if (inChina && (res == AccessLevel.FAIR_USE || "CopyrightInCopyright".equals(this.copyrightStatusLname) || "CopyrightClaimed".equals(this.copyrightStatusLname)))
            return AccessLevel.THUMBNAIL;
        return res;
    }

    public boolean isAccessible(HttpServletRequest request) {
        AccessLevel al = getAccessLevel(request);
        log.info("Is accessible accessLevel is {} and accessShort={}", al, accessShort);
        if (al.equals(AccessLevel.OPEN))
            return true;
        if (al.equals(AccessLevel.FAIR_USE)) {
            try {
                // This alone does not check against the user profile as the list
                // is built through identifierInfo regardless that user profile
                final boolean isAccessibleInFairUse = this.igi.isAccessibleInFairUse(imageFileName);
                log.info("Does not match Res Permissions so returning new test from igi (fairUse mode): {} ", isAccessibleInFairUse);
                return isAccessibleInFairUse;
            } catch (Exception e) {
                log.error("error when looking at fair use case: ", e);
                return false;
            }
        }
        if (al.equals(AccessLevel.THUMBNAIL)) {
            final boolean isTh = imageFileName.equals(this.igi.thumbnailFname);
            log.info("Does not match Res Permissions so returning new test from igi (thumbnail mode): {}, {} != {}", isTh, imageFileName, this.igi.thumbnailFname);
            return isTh;    
        }
        return false;
    }

    @Override
    public String toString() {
        return "ResourceAccessValidation [access=" + access + ", accessShort=" + accessShort + ", statusShort=" + statusShort + ", imageInstanceUri="
                + imageInstanceUri + ", imageFileName=" + imageFileName + ", igi=" + igi + ", isRestrictedInChina=" + isRestrictedInChina + "]";
    }

}
