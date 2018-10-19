package de.digitalcollections.iiif.myhymir.backend.impl.repository;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import de.digitalcollections.core.backend.api.resource.ResourceRepository;
import de.digitalcollections.core.backend.impl.file.repository.resource.util.S3ResourcePersistenceTypeHandler;
import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;
import de.digitalcollections.core.model.api.resource.enums.ResourcePersistenceType;
import de.digitalcollections.core.model.api.resource.exceptions.ResourceIOException;
import de.digitalcollections.core.model.impl.resource.S3Resource;


/**
 * A ResourceRepository implementation to use with Amazon S3 services
 *
 * @author @marcagate - Buddhist Digital Resource Center
 *
 */
@Primary
@Repository
public class S3ResourceRepositoryImpl implements ResourceRepository<Resource> {

    private static final Logger log = LoggerFactory.getLogger(S3ResourceRepositoryImpl.class);

    @Autowired
    S3ResourcePersistenceTypeHandler spt;
    final static String S3_BUCKET = "archive.tbrc.org";

    @Override
    public S3Resource create(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException {
        S3Resource resource = new S3Resource();
        if (mimeType != null) {
          if (mimeType.getExtensions() != null && !mimeType.getExtensions().isEmpty()) {
            resource.setFilenameExtension(mimeType.getExtensions().get(0));
          }
          resource.setMimeType(mimeType);
        }
        if (ResourcePersistenceType.REFERENCED.equals(resourcePersistenceType)) {
          resource.setReadonly(true);
        }
        if (ResourcePersistenceType.MANAGED.equals(resourcePersistenceType)) {
          resource.setUuid(UUID.fromString(key));
        }
        resource.setIdentifier(spt.getIdentifier(key));
        return resource;
    }

    @Override
    public void delete(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public S3Resource find(String key, ResourcePersistenceType resourcePersistenceType, MimeType mimeType) throws ResourceIOException {
        S3Resource resource = create(key, resourcePersistenceType, mimeType);
        return resource;
    }

    @Override
    public byte[] getBytes(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InputStream getInputStream(URI uri) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InputStream getInputStream(Resource r) throws ResourceIOException {
        return getInputStream((S3Resource) r);
    }

    public InputStream getInputStream(S3Resource r) throws ResourceIOException{
        log.info("Getting input stream for resource >> "+r.toString());
        AmazonS3 s3=S3ResourceRepositoryImpl.getClientInstance();
        S3Object obj=null;
        try {
            GetObjectRequest request = new GetObjectRequest(
                    S3_BUCKET,
                    r.getIdentifier());
            obj=s3.getObject(request);
            log.info("Obj from s3 >> "+obj);
        }
        catch (AmazonS3Exception e) {
            String msg=r.getIdentifier();
            //System.out.println("S3 client failed for identifier >> "+msg);
            throw new ResourceIOException(msg+System.lineSeparator()+e.getMessage());
        }
        InputStream stream=obj.getObjectContent();
        log.info("Obj from s3 >> "+stream);
        return stream;
    }

    @Override
    public Reader getReader(Resource r) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Resource rsrc, String string) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(Resource rsrc, InputStream in) throws ResourceIOException {
      throw new UnsupportedOperationException("Not supported yet.");
      //To change body of generated methods, choose Tools | Templates.
    }

   /**
     * @return a client to interact with S3 bucket
     */
    public static synchronized AmazonS3 getClientInstance() {
        ClientConfiguration config=new ClientConfiguration();
        config.setConnectionTimeout(300000);
        config.setMaxConnections(50);
        config.setMaxErrorRetry(100);
        config.setSocketTimeout(300000);
        AmazonS3ClientBuilder.standard().withClientConfiguration(config);
        return AmazonS3ClientBuilder.defaultClient();
    }

@Override
public void assertDocument(Resource arg0) throws ResourceIOException {
    // TODO Auto-generated method stub

}

@Override
public Document getDocument(Resource arg0) throws ResourceIOException {
    // TODO Auto-generated method stub
    return null;
}

}