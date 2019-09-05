package de.digitalcollections.core.backend.impl.file.repository.resource.resolver;

import java.io.IOException;

import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.model.api.identifiable.resource.exceptions.ResourceNotFoundException;
import io.bdrc.iiif.exceptions.IIIFException;

public interface S3Resolver {

    String getS3Identifier(String identifier) throws ResourceIOException, ResourceNotFoundException, IOException, IIIFException;

}
