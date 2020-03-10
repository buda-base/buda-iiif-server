package de.digitalcollections.core.backend.impl.file.repository.resource.util;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;

@Component
public class S3ResourcePersistenceTypeHandler implements ResourcePersistenceTypeHandler {

    static Properties local = new Properties();
    final String S3_RESOLVER = "S3_RESOLVER";
    @Value("${buda.S3resolver}")
    String s3Resolver;

    public S3ResourcePersistenceTypeHandler() {

    }

    @Override
    public ResourcePersistenceType getResourcePersistenceType() {
        // to Adjust I added S3 to
        // de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType
        return ResourcePersistenceType.S3;
    }

    @Override
    public List<URI> getUris(String resolvingKey, MimeType mimeType) throws ResourceIOException {
        List<URI> list = null;
        URI[] uri = new URI[1];
        try {
            S3Resolver resolver = (S3Resolver) Class.forName(s3Resolver).newInstance();
            uri[0] = new URI(resolver.getS3Identifier(resolvingKey));
            list = Arrays.asList(uri);
        } catch (Exception ex) {
            String msg = "";
            throw new ResourceIOException(msg + " " + ex.getMessage());
        }
        return list;
    }

    public String getIdentifier(String resolvingKey) throws ResourceIOException {
        try {
            S3Resolver resolver = (S3Resolver) Class.forName(s3Resolver).newInstance();
            return resolver.getS3Identifier(resolvingKey);
        } catch (Exception ex) {
            String msg = "";
            throw new ResourceIOException(msg + " " + ex.getMessage());
        }
    }

}