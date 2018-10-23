package de.digitalcollections.core.backend.api.resource;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import org.w3c.dom.Document;

import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.iiif.hymir.model.exception.ResourceNotFoundException;

public interface ResourceRepository<R extends Resource> {

  Resource create(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException, ResourceNotFoundException;

  default Resource create(String key, ResourcePersistenceType resourcePersistenceType, String filenameExtension) throws ResourceIOException, ResourceNotFoundException {
    return create(key, resourcePersistenceType, MimeType.fromExtension(filenameExtension));
  }

  void delete(R resource) throws ResourceIOException;

  Resource find(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException, ResourceNotFoundException;

  default Resource find(String key, ResourcePersistenceType resourcePersistenceType, String filenameExtension) throws ResourceIOException, ResourceNotFoundException {
    return find(key, resourcePersistenceType, MimeType.fromExtension(filenameExtension));
  }

  byte[] getBytes(R resource) throws ResourceIOException;

  Document getDocument(R resource) throws ResourceIOException;

  default Document getDocument(String key, ResourcePersistenceType resourcePersistenceType) throws ResourceIOException, ResourceNotFoundException {
    Resource resource = find(key, resourcePersistenceType, MimeType.fromExtension("xml"));
    return getDocument((R) resource);
  }

  InputStream getInputStream(URI resourceUri) throws ResourceIOException, ResourceNotFoundException;

  InputStream getInputStream(R resource) throws ResourceIOException, ResourceNotFoundException;

  Reader getReader(R resource) throws ResourceIOException, ResourceNotFoundException;

  void write(Resource resource, String input) throws ResourceIOException;

  void write(Resource resource, InputStream inputStream) throws ResourceIOException;

  void assertDocument(Resource resource) throws ResourceIOException;
}
