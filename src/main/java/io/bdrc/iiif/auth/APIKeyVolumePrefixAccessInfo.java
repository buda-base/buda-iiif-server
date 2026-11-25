package io.bdrc.iiif.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.rdf.RdfConstants;

/**
 * Access info for a restricted API key: behaves like a regular logged-in user
 * (no special permission) for PDF / archive generation, but only on image
 * groups (volumes) whose local name starts with one of the configured
 * prefixes. For images it behaves like any regular user.
 *
 * The {@link AccessInfo} interface doesn't expose the image group, so the PDF
 * check must go through {@link #hasVolumePDFAccess(String, String, String)}
 * (called from {@link ResourceAccessValidation}); the generic
 * {@link #hasResourcePDFAccess} always denies.
 */
public class APIKeyVolumePrefixAccessInfo implements AccessInfo {

    private final List<String> volumePrefixes;

    public APIKeyVolumePrefixAccessInfo(final List<String> volumePrefixes) {
        final List<String> prefixes = new ArrayList<>();
        for (final String p : volumePrefixes) {
            final String trimmed = p.trim();
            if (!trimmed.isEmpty())
                prefixes.add(trimmed.startsWith("bdr:") ? trimmed.substring(4) : trimmed);
        }
        this.volumePrefixes = Collections.unmodifiableList(prefixes);
    }

    public List<String> getVolumePrefixes() {
        return this.volumePrefixes;
    }

    public boolean allowsVolume(final String imageGroupId) {
        if (imageGroupId == null)
            return false;
        final String lname = imageGroupId.startsWith("bdr:") ? imageGroupId.substring(4) : imageGroupId;
        for (final String prefix : this.volumePrefixes) {
            if (lname.startsWith(prefix))
                return true;
        }
        return false;
    }

    /**
     * Access level of a logged-in user with no specific permission (see
     * AccessInfoAuthImpl.hasResourceAccess).
     */
    static AccessLevel regularUserAccessLevel(final String resourceAccessLocalName, final String resourceStatusLocalName) {
        if (!RdfConstants.STATUS_RELEASED.equals(resourceStatusLocalName))
            return AccessLevel.NOACCESS;
        if (RdfConstants.OPEN.equals(resourceAccessLocalName))
            return AccessLevel.OPEN;
        if (RdfConstants.FAIR_USE.equals(resourceAccessLocalName))
            return AccessLevel.FAIR_USE;
        if (RdfConstants.MIXED.equals(resourceAccessLocalName))
            return AccessLevel.MIXED;
        return AccessLevel.NOACCESS;
    }

    public AccessLevel hasVolumePDFAccess(final String resourceAccessLocalName, final String resourceStatusLocalName,
            final String imageGroupId) {
        if (!allowsVolume(imageGroupId))
            return AccessLevel.NOACCESS;
        return regularUserAccessLevel(resourceAccessLocalName, resourceStatusLocalName);
    }

    @Override
    public AccessLevel hasResourcePDFAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri, String ipAddress, List<String> collections) {
        // the volume is unknown here, deny: use hasVolumePDFAccess instead
        return AccessLevel.NOACCESS;
    }

    @Override
    public AccessLevel hasResourceAccess(String resourceAccessLocalName, String resourceStatusLocalName,
            String resourceUri) {
        return regularUserAccessLevel(resourceAccessLocalName, resourceStatusLocalName);
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
        return "0f3c7e2a-5b8d-4c61-9a7e-2d4f6b8c1e35";
    }

    @Override
    public boolean hasEndpointAccess() {
        return false;
    }

    @Override
    public String toString() {
        return "APIKeyVolumePrefixAccessInfo [volumePrefixes=" + volumePrefixes + "]";
    }

}
