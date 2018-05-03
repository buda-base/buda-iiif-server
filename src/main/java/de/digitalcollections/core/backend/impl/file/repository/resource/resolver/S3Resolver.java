package de.digitalcollections.core.backend.impl.file.repository.resource.resolver;


import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;

public interface S3Resolver {
    
    String getS3Identifier(String identifier) throws ResourceIOException;

}
